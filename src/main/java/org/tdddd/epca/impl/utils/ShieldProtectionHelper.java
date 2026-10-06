package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.capability.ShieldCapability;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.events.ShieldCapabilityHandler;

/**
 * {@code epca:soul_protection}
 *
 * <p><b></b></p>
 * <ol>
 *   <li><b></b>  {@link #applyDamageIgnore(LivingEntity, float)}
 *        {@code ShieldEventHandler#onLivingHurt} 1.20.1  {@code LivingDamageEvent}
 *        {@code LivingEntity#actuallyHurt} / {@code Player#actuallyHurt}
 *       26.1.2  {@code LivingIncomingDamageEvent} + {@code DamageContainer}
 *       </li>
 *   <li><b></b>  {@link #applyShieldPool(LivingEntity, float)}
 *       {@code mixin/common/LivingEntityMixin#onSetHealth}  {@code setHealth}
 *        {@link LivingEntity#setHealth(float)}
 *       setHealthvanilla  {@code setHealth}
 *       {@code Mth.clamp(health, 0.0F, getMaxHealth())}
 *        {@code hurt()} / </li>
 * </ol>
 *
 * <p>  {@code setHealth}
 *
 * {@code applyShieldProtection} +
 * {@code amplifier * 0.5f} I </p>
 */
public class ShieldProtectionHelper {

    /**
     *  I  {@code amplifier == 0}
     *
     * <p>1.20.1  26.1.2  {@code amplifier * 0.5f}
     * {@code ShieldEventHandler#computeShieldForEntity}  {@code (amplifier + 1) * 0.5f}
     *  0.5=  {@code amplifier}
     *  {@code amplifier + 1} I {@code SPEC-*.md}
     * {@code ModConfig}langjson 0.5</p>
     *
     * <p>  I  {@link #ignoredDamage(int)}
     * {@code amplifier * IGNORE_PER_LEVEL} </p>
     */
    public static final float IGNORE_PER_LEVEL = 0.5f;

    /**
     * {@code amplifier}
     *
     * <p> mod  {@code amplifier + 1}
     * {@code ShieldEventHandler#computeShieldForEntity}  {@code (amplifier + 1) * 0.5f}
     *  {@code amplifier + 1} I  0.5 II  1.0 III amplifier 2 1.5</p>
     */
    public static float ignoredDamage(int amplifier) {
        return (amplifier + 1) * IGNORE_PER_LEVEL;
    }

    /**
     *
     *
     * <p>1.20.1  {@code ShieldEventHandler#onLivingHurt(LivingDamageEvent)}//
     * 26.1.2  {@code LivingIncomingDamageEvent}
     * {@code ShieldEventHandler#onLivingHurt} <b></b></p>
     *
     * <p>   {@code 0}</p>
     *
     * <p> {@code LocalPlayer#hurtTo}  {@code setHealth}
     * </p>
     *
     * @param incomingDamage
     * @return  0{@code 0}
     */
    public static float applyDamageIgnore(LivingEntity entity, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        if (entity.level().isClientSide()) return incomingDamage;

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION);
        if (effect == null) return incomingDamage;

        float ignore = ignoredDamage(effect.getAmplifier());
        if (incomingDamage <= ignore) return 0;
        return incomingDamage - ignore;
    }

    /**
     *  {@link ShieldCapability}
     *
     * <p>{@code LivingEntityMixin#onSetHealth}
     * {@link LivingEntity#setHealth(float)}   0</p>
     *
     * <p><b></b> {@code LocalPlayer#hurtTo}  {@code setHealth}
     *
     *  {@code isClientSide()} </p>
     *
     * <p> {@link #syncShieldToDuration(LivingEntity)}
     *  0   </p>
     *
     * <p>26.1.2  API 1.20.1  Forge Capability{@code entity.getCapability(...).orElse(null)}
     * {@code entity.getExistingDataOrNull(ShieldCapabilityHandler.SHIELD_CAP)}
     *  {@code null}</p>
     *
     * @param incomingDamage
     * @return  0
     */
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