package org.tdddd.epca.impl.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.yawning_neko_api.damages.ModDamageTypes;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NestLeaderHandler {
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        if (NestLeaderManager.isNestLeader(player.getUUID())) {
            event.setDisplayname(Component.literal(event.getDisplayname().getString())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        BiomassEventHandler.syncBiomass(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;

        Level level = player.level();
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(ModDamageTypes.MINIMUM);
        DamageSource minimumSource = new DamageSource(holder);

        event.setAmount(event.getAmount() + 1.0f);

        if (canApplyExtraDamage(player)) {
            event.getEntity().hurt(minimumSource, 1.0f);
            setExtraDamageCooldown(player);
        }

        if (player.level().random.nextFloat() < 0.7f) {
            LivingEntity target = event.getEntity();
            int currentAmp = target.getEffect(ModEffects.COTH.get()) != null ?
                    target.getEffect(ModEffects.COTH.get()).getAmplifier() : -1;
            int newAmp = Math.min(currentAmp + 1, 2);
            target.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, newAmp));
            target.addEffect(new MobEffectInstance(ModEffects.FEAR.get(), 300, 1));
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER || effect == MobEffects.CONFUSION ||
                effect == ModEffects.COTH.get() || effect == ModEffects.VIRAL.get() || effect == ModEffects.FEAR.get() ||
                effect == ModEffects.SOLIDIFY.get() || effect == ModEffects.CORROSIVE.get() || effect == ModEffects.CONTEMPT_INORGANIC.get() ||
                effect == ModEffects.NEEDLER.get()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!NestLeaderManager.isNestLeader(player.getUUID())) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.getMainHandItem().isEmpty()) return;
        Entity target = event.getTarget();
        if (target instanceof IParasite && target instanceof LivingEntity) {
            ((IParasite) target).setFollowTarget(player.getUUID());
            event.setCanceled(true);
        }
    }

    private static boolean canApplyExtraDamage(Player player) {
        long last = player.getPersistentData().getLong("ExtraDamageLastTick");
        long now = player.level().getGameTime();
        return (now - last) >= 40;
    }
    private static void setExtraDamageCooldown(Player player) {
        player.getPersistentData().putLong("ExtraDamageLastTick", player.level().getGameTime());
    }
}