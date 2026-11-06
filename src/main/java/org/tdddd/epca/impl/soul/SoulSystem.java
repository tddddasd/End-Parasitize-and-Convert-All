package org.tdddd.epca.impl.soul;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulSystem {

    private static final String KEY_SOUL = "EpcaSoul";
    private static final String KEY_BLOCK = "EpcaSoulBlockedUntil";

    private SoulSystem() { }

    public static double maxSoul(LivingEntity entity) {
        double health = entity.getMaxHealth();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        double multiplier;
        if (ParasiteEvents.isParasiteEntity(entity)) {
            multiplier = 5.0;
        } else if (entity instanceof Player) {
            multiplier = 2.5;
        } else {
            multiplier = switch (id) {
                case "minecraft:warden", "minecraft:ender_dragon", "minecraft:wither" -> 5.0;
                case "minecraft:iron_golem", "minecraft:snow_golem", "minecraft:copper_golem",
                     "minecraft:guardian", "minecraft:elder_guardian" -> 1.25;
                case "minecraft:vex", "minecraft:allay" -> 0.75;
                default -> 1.0;
            };
        }
        return Math.max(0.0, health * multiplier);
    }

    public static double getSoul(LivingEntity entity) {
        if (!entity.getPersistentData().contains(KEY_SOUL)) {
            setSoulRaw(entity, maxSoul(entity));
        }
        return entity.getPersistentData().getDouble(KEY_SOUL);
    }

    public static double getMaxSoul(LivingEntity entity) {
        return maxSoul(entity);
    }

    public static void setSoul(LivingEntity entity, double value) {
        double previous = entity.getPersistentData().getDouble(KEY_SOUL);
        setSoulRaw(entity, value);
        if (value <= 0.0 && previous > 0.0 && !entity.isRemoved() && !entity.isDeadOrDying()) {
            killBySoul(entity, entity.damageSources().genericKill());
        }
    }

    private static void setSoulRaw(LivingEntity entity, double value) {
        double max = maxSoul(entity);
        entity.getPersistentData().putDouble(KEY_SOUL, Math.max(0.0, Math.min(max, value)));
    }

    public static void addSoul(LivingEntity entity, double value) {
        setSoul(entity, getSoul(entity) + value);
    }

    public static float soulDamageFor(LivingEntity attacker) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
        return switch (id) {
            case "epca:bio_tort_incarnation" -> 46.0F;
            case "epca:reshape_longarms", "epca:reshape_yelloweye" -> 1.0F;
            case "epca:ripper", "epca:fins", "epca:mozzie",
                 "epca:flying_carrier", "epca:light_carrier" -> 0.1F;
            default -> 0.5F;
        };
    }

    public static int protectionLevel(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.SOUL_PROTECTION.get());
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    public static float applySoulDamage(LivingEntity target, DamageSource source, float amount) {
        if (target == null || amount <= 0.0F) return 0.0F;
        if (target.level().isClientSide) return 0.0F;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return 0.0F;
        if (ParasiteEvents.isParasiteEntity(target)) return 0.0F;
        if (target.isDeadOrDying()) return 0.0F;
        float reduction = Math.min(0.6F, protectionLevel(target) * 0.05F);
        float dealt = amount * (1.0F - reduction);
        target.getPersistentData().putLong(KEY_BLOCK, target.level().getGameTime() + 40);
        double next = getSoul(target) - dealt;
        if (next <= 0.0) {
            setSoulRaw(target, 0.0);
            killBySoul(target, source);
        } else {
            setSoul(target, next);
        }
        return dealt;
    }

    private static void killBySoul(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity instanceof Player player) {
            if (player.isDeadOrDying() || player.deathTime > 0) return;
            ((org.tdddd.yawning_neko_api.damages.HealthDataWriter) player).yna$setHealthData(0.0F);
            player.die(source);
            return;
        }
        ((org.tdddd.yawning_neko_api.damages.HealthDataWriter) entity).yna$setHealthData(0.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!ParasiteEvents.isParasiteByTagOrInterface(attacker)) return;
        applySoulDamage(event.getEntity(), event.getSource(), soulDamageFor(attacker));
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        double max = getMaxSoul(entity);
        if (max <= 0.0) return;
        if (entity.tickCount % 10 == 0 && entity.getHealth() >= entity.getMaxHealth() * 0.2F
                && entity.level().getGameTime() >= entity.getPersistentData().getLong(KEY_BLOCK)) {
            double deficit = 1.0 - (entity.getHealth() / entity.getMaxHealth());
            double gain = (0.1 + 1.9 * deficit) * (1.0 + 0.2 * protectionLevel(entity));
            addSoul(entity, gain);
        }
        if (entity.tickCount % 60 == 0 && getSoul(entity) <= max * 0.25 && entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL,
                    entity.getX() + (level.random.nextDouble() - 0.5) * 0.6,
                    entity.getY() + level.random.nextDouble() * entity.getBbHeight(),
                    entity.getZ() + (level.random.nextDouble() - 0.5) * 0.6,
                    1, 0.0, 0.0, 0.0, 0.01);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_soul")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .executes(ctx -> get(ctx.getSource(),
                                ctx.getSource().getEntity() instanceof LivingEntity living ? living : null))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> {
                                    var target = EntityArgument.getEntity(ctx, "target");
                                    return get(ctx.getSource(), target instanceof LivingEntity living ? living : null);
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(SoulSystem::set)))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                .executes(SoulSystem::set)))
                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(SoulSystem::add)))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes(SoulSystem::add))));
    }

    private static int get(CommandSourceStack source, LivingEntity entity) {
        if (entity == null) return 0;
        double soul = getSoul(entity);
        double max = getMaxSoul(entity);
        source.sendSuccess(() -> Component.literal("soul: " + String.format("%.2f", soul)
                + " / " + String.format("%.2f", max)).withStyle(ChatFormatting.AQUA), false);
        return (int) soul;
    }

    private static LivingEntity resolve(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var target = EntityArgument.getEntity(ctx, "target");
            if (target instanceof LivingEntity living) return living;
            return null;
        } catch (Exception ignored) {
            return ctx.getSource().getEntity() instanceof LivingEntity living ? living : null;
        }
    }

    private static int set(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        LivingEntity entity = resolve(ctx);
        if (entity == null) return 0;
        double value = DoubleArgumentType.getDouble(ctx, "value");
        setSoul(entity, value);
        final double shown = getSoul(entity);
        ctx.getSource().sendSuccess(() -> Component.literal("soul set to " + String.format("%.2f", shown))
                .withStyle(ChatFormatting.AQUA), false);
        return (int) shown;
    }

    private static int add(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        LivingEntity entity = resolve(ctx);
        if (entity == null) return 0;
        double value = DoubleArgumentType.getDouble(ctx, "value");
        addSoul(entity, value);
        final double shown = getSoul(entity);
        ctx.getSource().sendSuccess(() -> Component.literal("soul now " + String.format("%.2f", shown))
                .withStyle(ChatFormatting.AQUA), false);
        return (int) shown;
    }
}
