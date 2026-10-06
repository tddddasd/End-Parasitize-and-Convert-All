package org.tdddd.epca.impl.utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;

/**
 *
 *
 * <h2></h2>
 * <p>{@code EntityType#create(level, EntitySpawnReason.CONVERSION)}
 *     {@code ServerLevel#addFreshEntity}<b>
 * </b></p>
 * <ul>
 *   <li>{@link EntityConversionUtil#convertTo(LivingEntity, net.minecraft.world.entity.EntityType)}
 *        {@code CothEffect}  generic //</li>
 *   <li>{@code CothEffect#convertUsingDataPackRule}   {@code entity_conversions}
 *        {@code epca:infested_slime_size1}</li>
 * </ul>
 *
 * <p>{@code LivingEntity}
 * {@code this.setHealth(this.getMaxHealth())}
 * {@code setHealth(EntityHealthUtils.burstHealth(this, 0.02F))}
 *  {@code MAX_HEALTH}
 *  0 0  1.20.1  26.1.2 </p>
 * <ul>
 *   <li>{@code LivingEntity#hurt}  {@code isDeadOrDying()}  false</li>
 *   <li>{@code LivingEntity#aiStep}  {@code isImmobile()}= {@code isDeadOrDying()}
 *        {@code jumping/xxa/zza} AI </li>
 * </ul>
 *
 * <p><b> {@code MAX_HEALTH} </b>
 * <b></b></p>
 *
 * <p><b>26.1.2  API </b>{@code Attributes.MAX_HEALTH} / {@code Attributes.ARMOR}
 * {@code AttributeInstance#getBaseValue/setBaseValue} {@code Mob#setNoAi(boolean)}
 * {@code LivingEntityMixin} 1.20.1
 * {@code AttributeSupplier}{@code EntityAttributeCreationEvent}  26.1.2
 *  {@code getAttribute(...)}  null</p>
 *
 * @see #MIN_CONVERTED_MAX_HEALTH
 */
public final class ConvertedEntitySupport {

    /**
     * {@code generic.max_health}
     *
     * <p><b></b> </p>
     * <ul>
     *   <li>{@code 1.0F} 1  =
     *       {@code Slime#setSize}  {@code size * size}size  1..127
     *        1</li>
     *   <li> 0  0  {@code isDeadOrDying()}   + </li>
     *   <li> 2  4<b></b>
     *       </li>
     * </ul>
     */
    public static final float MIN_CONVERTED_MAX_HEALTH = 1.0F;

    /**
     *  {@link #MIN_CONVERTED_MAX_HEALTH}
     * {@code MAX_HEALTH}  0 0
     */
    private static final float MIN_CONVERTED_HEALTH = 1.0F;

    private ConvertedEntitySupport() {
    }

    /**
     * /
     *
     * <p> {@code addFreshEntity} <b></b>
     * {@code EntityJoinLevelEvent} {@code DifficultyApplier}
     * {@code ParasiteNbtEffectHandler}
     * </p>
     *
     * <p>
     *  {@code EntityJoinLevelEvent}
     * {@code IParasite#applyDifficultyStatModifiers}
     * </p>
     *
     * @param entity  create
     */
    public static void initializeConvertedEntity(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        // 1)  IParasite#applyDifficultyStatModifiers
        //     MAX_HEALTH  0 * multiplier
        //     0
        boolean hasMaxHealth = hasMaxHealthAttribute(entity);
        if (hasMaxHealth) {
            applyDifficultyModifier(entity);
        }

        // 2)
        //    double  < NaN
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double base = maxHealth.getBaseValue();
            if (!(base >= (double) MIN_CONVERTED_MAX_HEALTH)) {  //  NaN / 0 /
                maxHealth.setBaseValue((double) MIN_CONVERTED_MAX_HEALTH);
            }
        }

        // 3)  AI / noAi
        //     noAi
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }

        // 4)
        //     LivingEntity#setHealth  [0, getMaxHealth()]
        //     getMaxHealth() /
        //     MIN_CONVERTED_HEALTH  isDeadOrDying()  false
        float finalMaxHealth = entity.getMaxHealth();
        float health = (finalMaxHealth > 0.0F && !Float.isNaN(finalMaxHealth))
                ? finalMaxHealth
                : MIN_CONVERTED_HEALTH;
        entity.setHealth(health);
    }

    /**  {@code IParasite#applyDifficultyStatModifiers}  */
    private static void applyDifficultyModifier(LivingEntity entity) {
        float multiplier = DifficultyEffects.getParasiteStatMultiplier(entity.level());
        if (!(multiplier > 0.0F) || Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return;  //  0/NaN
        }

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double scaled = maxHealth.getBaseValue() * (double) multiplier;
            if (Double.isNaN(scaled) || Double.isInfinite(scaled) || scaled < 0.0D) {
                return;
            }
            maxHealth.setBaseValue(scaled);
        }

        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            double scaledArmor = armor.getBaseValue() * (double) multiplier;
            if (!Double.isNaN(scaledArmor) && !Double.isInfinite(scaledArmor) && scaledArmor >= 0.0D) {
                armor.setBaseValue(scaledArmor);
            }
        }
    }

    /**
     * {@code MAX_HEALTH}
     *
     * <p> {@code EntityAttributeCreationEvent}
     *  {@code EpcaEntityManager#createAttributes}
     * {@code getAttribute(...)}  null {@code AttributeSupplier}
     * </p>
     */
    private static boolean hasMaxHealthAttribute(LivingEntity entity) {
        return entity.getAttribute(Attributes.MAX_HEALTH) != null;
    }
}

