package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.capability.ShieldCapability;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.events.ShieldCapabilityHandler;

public class ShieldProtectionHelper {

    public static final float IGNORE_PER_LEVEL = 0.5f;

    public static float ignoredDamage(int amplifier) {
        return (amplifier + 1) * IGNORE_PER_LEVEL;
    }

    public static float applyDamageIgnore(LivingEntity entity, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        if (entity.level().isClientSide()) return incomingDamage;

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION);
        if (effect == null) return incomingDamage;

        float ignore = ignoredDamage(effect.getAmplifier());
        if (incomingDamage <= ignore) return 0;
        return incomingDamage - ignore;
    }

    public static float applyShieldPool(LivingEntity entity, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        if (entity.level().isClientSide()) return incomingDamage;

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION);
        if (effect == null) return incomingDamage;

        ShieldCapability shieldCap = entity.getExistingDataOrNull(ShieldCapabilityHandler.SHIELD_CAP);
        if (shieldCap == null) return incomingDamage;

        float currentShield = shieldCap.getShield();
        if (currentShield >= incomingDamage) {

            shieldCap.consumeShield(incomingDamage);
            syncShieldToDuration(entity);
            return 0;
        } else {

            float remaining = incomingDamage - currentShield;
            shieldCap.setShield(0);
            syncShieldToDuration(entity);
            return remaining;
        }
    }

    public static void syncShieldToDuration(LivingEntity entity) {

        if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (serverPlayer.connection == null) {
                return;
            }
        }

        ShieldCapability shieldCap = entity.getExistingDataOrNull(ShieldCapabilityHandler.SHIELD_CAP);
        if (shieldCap == null) return;

        float currentShield = shieldCap.getShield();
        int newDuration = (int) (currentShield / 0.05f);

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION);
        if (effect == null) {
            if (currentShield > 0) shieldCap.setShield(0);
            return;
        }

        if (newDuration <= 0) {
            entity.removeEffect(ModEffects.SOUL_PROTECTION);
            shieldCap.setShield(0);
        } else {
            MobEffectInstance newEffect = new MobEffectInstance(
                    ModEffects.SOUL_PROTECTION,
                    newDuration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            );
            entity.addEffect(newEffect);
        }
    }
}
