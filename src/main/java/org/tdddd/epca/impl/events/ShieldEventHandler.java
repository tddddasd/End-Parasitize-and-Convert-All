package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.effects.buff.SoulProtectionEffect;
import org.tdddd.epca.impl.utils.ShieldProtectionHelper;
import net.minecraftforge.event.entity.living.MobEffectEvent;

@Mod.EventBusSubscriber
public class ShieldEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        float original = event.getAmount();
        if (original <= 0) return;

        float remaining = ShieldProtectionHelper.applyShieldProtection(entity, original);
        if (remaining <= 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(remaining);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        if (!(instance.getEffect() instanceof SoulProtectionEffect)) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        float totalShield = computeShieldForEntity(entity, instance.getAmplifier());
        entity.getCapability(ShieldCapabilityHandler.SHIELD_CAP).ifPresent(cap -> {
            cap.setShield(totalShield);
        });
    }

    
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        if (!(instance.getEffect() instanceof SoulProtectionEffect)) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        entity.getCapability(ShieldCapabilityHandler.SHIELD_CAP).ifPresent(cap -> cap.setShield(0));
    }

    
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        if (!(instance.getEffect() instanceof SoulProtectionEffect)) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        entity.getCapability(ShieldCapabilityHandler.SHIELD_CAP).ifPresent(cap -> cap.setShield(0));
    }

    
    private static float computeShieldForEntity(LivingEntity entity, int amplifier) {
        float baseShield = (amplifier + 1) * 0.5f;
        float maxHealth = entity.getMaxHealth();
        float extraShield = 0f;
        if (maxHealth > 25f) {
            int extraSteps = (int) ((maxHealth - 25f) / 50f);
            extraShield = extraSteps * 0.5f;
        }
        return baseShield + (extraShield * (amplifier + 1));
    }
}