package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.capability.IShieldCapability;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.events.ShieldCapabilityHandler;

/**
 * {@code epca:soul_protection}
 *
 * <p><b></b></p>
 * <ol>
 *   <li><b></b>  {@link #applyDamageIgnore(LivingEntity, float)}
 *        {@code ShieldEventHandler#onLivingHurt(LivingDamageEvent)}
 *       LivingDamageEvent  1.20.1 Forge
 *       {@code LivingEntity#actuallyHurt} / {@code Player#actuallyHurt}
 *       {@code _tmp_vanilla_src/net/minecraft/world/entity/LivingEntity.java:1632}
 *       {@code .../player/Player.java:915}</li>
 *   <li><b></b>  {@link #applyShieldPool(LivingEntity, float)}
 *       {@code mixin/common/LivingEntityMixin#onSetHealth}  {@code setHealth}
 *        {@link LivingEntity#setHealth(float)}
 *       setHealth1.20.1  {@code setHealth}
 *       {@code Mth.clamp(health, 0.0F, getMaxHealth())}
 *       {@code _tmp_vanilla_src/net/minecraft/world/entity/LivingEntity.java:1050-1052}
 *        {@code hurt()} / </li>
 * </ol>
 *
 * <p>  {@code LivingDamageEvent}  {@code setHealth}
 *  {@code applyShieldProtection}
 *  + </p>
 */
public class ShieldProtectionHelper {

    /**
     *  I  {@code amplifier == 0}
     *
     * <p> {@code amplifier * 0.5f}
     * {@code ShieldEventHandler#computeShieldForEntity}{@code ShieldEventHandler.java:99}
     * {@code (amplifier + 1) * 0.5f} 0.5=
     *  {@code amplifier} {@code amplifier + 1} I
     * {@code SPEC-*.md}{@code ModConfig}langjson
     *  0.5</p>
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
     * {@code LivingHurtEventHandler#onLivingHurt}
     * {@code 1.0f + (amplifier + 1) * 0.5f} {@code amplifier + 1}
     *  I  0.5 II  1.0 III amplifier 2 1.5</p>
     */
    public static float ignoredDamage(int amplifier) {
        return (amplifier + 1) * IGNORE_PER_LEVEL;
    }

    /**
     *
     *
     * <p>   {@code 0}</p>
     *
     * <p> {@code LocalPlayer#hurtTo}
     * {@code _tmp_vanilla_src/net/minecraft/client/player/LocalPlayer.java:313-331}
     * {@code setHealth}
     * </p>
     *
     * @param incomingDamage
     * @return  0{@code 0}
     */
    public static float applyDamageIgnore(LivingEntity entity, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        if (entity.level().isClientSide()) return incomingDamage;

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION.get());
        if (effect == null) return incomingDamage;

        float ignore = ignoredDamage(effect.getAmplifier());
        if (incomingDamage <= ignore) return 0;
        return incomingDamage - ignore;
    }

    /**
     *  {@link IShieldCapability}
     *
     * <p>{@code LivingEntityMixin#onSetHealth}
     * {@link LivingEntity#setHealth(float)}   0</p>
     *
     * <p> {@link #syncShieldToDuration(LivingEntity)}
     *  0   </p>
     *
     * @param incomingDamage
     * @return  0
     */
    public static float applyShieldPool(LivingEntity entity, float incomingDamage) {
        if (incomingDamage <= 0) return 0;
        if (entity.level().isClientSide()) return incomingDamage;

        MobEffectInstance effect = entity.getEffect(ModEffects.SOUL_PROTECTION.get());
        if (effect == null) return incomingDamage;

        IShieldCapability shieldCap = entity.getCapability(ShieldCapabilityHandler.SHIELD_CAP).orElse(null);
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

