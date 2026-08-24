package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.capability.IShieldCapability;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.events.ShieldCapabilityHandler;

public class ShieldProtectionHelper {

    
    public static float applyShieldProtection(LivingEntity entity, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION.get());
        if (effect == null) return incomingDamage;

        int amplifier = effect.getAmplifier();
        float threshold = amplifier * 0.5f;
        if (incomingDamage <= threshold) return 0;

        float damageAfterThreshold = incomingDamage - threshold;
        IShieldCapability shieldCap = entity.getCapability(ShieldCapabilityHandler.SHIELD_CAP).orElse(null);
        if (shieldCap == null) return damageAfterThreshold;

        float currentShield = shieldCap.getShield();
        if (currentShield >= damageAfterThreshold) {
            
            shieldCap.consumeShield(damageAfterThreshold);
            syncShieldToDuration(entity);   
            return 0;
        } else {
            
            float remaining = damageAfterThreshold - currentShield;
            shieldCap.setShield(0);
            syncShieldToDuration(entity);   
            return remaining;
        }
    }

    public static void syncShieldToDuration(LivingEntity entity) {
        // 【新增】如果实体是 ServerPlayer 且连接未初始化，则跳过
        if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (serverPlayer.connection == null) {
                return; // 安全退出，避免后续操作触发 NPE
            }
        }

        IShieldCapability shieldCap = entity.getCapability(ShieldCapabilityHandler.SHIELD_CAP).orElse(null);
        if (shieldCap == null) return;

        float currentShield = shieldCap.getShield();
        int newDuration = (int) (currentShield / 0.05f);

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION.get());
        if (effect == null) {
            if (currentShield > 0) shieldCap.setShield(0);
            return;
        }

        if (newDuration <= 0) {
            entity.removeEffect(ModEffects.SOUL_PROTECTION.get());
            shieldCap.setShield(0);
        } else {
            MobEffectInstance newEffect = new MobEffectInstance(
                    ModEffects.SOUL_PROTECTION.get(),
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