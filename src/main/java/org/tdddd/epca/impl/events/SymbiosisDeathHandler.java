package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class SymbiosisDeathHandler {
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 20 != 0) return;
        if (!entity.getPersistentData().getBoolean(org.tdddd.epca.impl.overworld.registry.effects.buff.HyperplasiaEffect.HEALTH_MARK)) return;
        if (entity.hasEffect(ModEffects.HYPERPLASIA.get())) return;
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(org.tdddd.epca.impl.overworld.registry.effects.buff.HyperplasiaEffect.HEALTH_MOD);
        }
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        entity.getPersistentData().remove(org.tdddd.epca.impl.overworld.registry.effects.buff.HyperplasiaEffect.HEALTH_MARK);
    }

    @SubscribeEvent
    public static void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        var target = event.getEntity();
        var source = event.getSource();
        if (!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)) return;
        if (attacker == target || !attacker.hasEffect(ModEffects.SYMBIOSIS_LIE.get())) return;
        if (source.is(org.tdddd.yawning_neko_api.damages.ModDamageTypes.MINIMUM)) return;
        // Only a same-kind victim that was actually hit (melee or projectile) can be hurt.
        if (target.getClass() != attacker.getClass()) return;
        if (attacker.level().isClientSide) return;
        if (attacker.level().random.nextFloat() < 0.25F) {
            var registry = attacker.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
            var minimum = new net.minecraft.world.damagesource.DamageSource(
                    registry.getHolderOrThrow(org.tdddd.yawning_neko_api.damages.ModDamageTypes.MINIMUM));
            target.hurt(minimum, Math.max(0.5F, event.getAmount() * 0.25F));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        var entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.SYMBIOSIS_LIE.get())) return;

        float strength = Math.max(1.0F, Math.min(4.0F,
                (entity.getBbWidth() + entity.getBbHeight()) * 0.75F));
        level.explode(entity, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                strength, Level.ExplosionInteraction.NONE);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                strength >= 2.5F ? org.tdddd.epca.impl.overworld.registry.ModSoundEvents.BIG_EXPLOSION.get()
                        : org.tdddd.epca.impl.overworld.registry.ModSoundEvents.SMALL_EXPLOSION.get(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
        org.tdddd.epca.impl.overworld.registry.ModParticles.spawnConversion(level,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 5, 0x3F5A2A);

        try {
            Class.forName("me.ichun.mods.mobdismemberment.common.MobDismemberment")
                    .getMethod("trigger", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, entity);
        } catch (Throwable ignored) {
        }

        // Randomly infest blocks inside a 1.5x body-size cube around the death position.
        double radiusX = Math.max(1.0D, entity.getBbWidth() * 0.75D);
        double radiusY = Math.max(1.0D, entity.getBbHeight() * 0.75D);
        for (int i = 0; i < 24; i++) {
            double bx = entity.getX() + (level.random.nextDouble() * 2.0D - 1.0D) * radiusX;
            double by = entity.getY() + (level.random.nextDouble() * 2.0D - 1.0D) * radiusY;
            double bz = entity.getZ() + (level.random.nextDouble() * 2.0D - 1.0D) * radiusX;
            var pos = net.minecraft.core.BlockPos.containing(bx, by, bz);
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager.getInstance()
                    .convertBlockUsingGeneralConfig(level, pos, state);
        }

        int curbugs = 1 + level.random.nextInt(2);
        for (int i = 0; i < curbugs; i++) spawn(level, ModEntities.CURBUG.get(), entity.getX(), entity.getY(), entity.getZ());
        if (entity.isInWater()) {
            spawn(level, ModEntities.FINS.get(), entity.getX(), entity.getY(), entity.getZ());
        } else {
            spawn(level, ModEntities.RIPPER.get(), entity.getX(), entity.getY(), entity.getZ());
        }
        if (level.random.nextFloat() < 0.25F) {
            spawn(level, ModEntities.MOZZIE.get(), entity.getX(), entity.getY(), entity.getZ());
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double y, double z) {
        Mob mob = type.create(level);
        if (mob == null) return;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(mob);
    }
}