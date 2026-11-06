package org.tdddd.epca.impl.utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;

public final class ConvertedEntitySupport {

    public static final float MIN_CONVERTED_MAX_HEALTH = 1.0F;

    private static final float MIN_CONVERTED_HEALTH = 1.0F;

    private ConvertedEntitySupport() {
    }

    public static void initializeConvertedEntity(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        boolean hasMaxHealth = hasMaxHealthAttribute(entity);
        if (hasMaxHealth) {
            applyDifficultyModifier(entity);
        }

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double base = maxHealth.getBaseValue();
            if (!(base >= (double) MIN_CONVERTED_MAX_HEALTH)) {
                maxHealth.setBaseValue((double) MIN_CONVERTED_MAX_HEALTH);
            }
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }

        float finalMaxHealth = entity.getMaxHealth();
        float health = (finalMaxHealth > 0.0F && !Float.isNaN(finalMaxHealth))
                ? finalMaxHealth
                : MIN_CONVERTED_HEALTH;
        entity.setHealth(health);
    }

    private static void applyDifficultyModifier(LivingEntity entity) {
        float multiplier = DifficultyEffects.getParasiteStatMultiplier(entity.level());
        if (!(multiplier > 0.0F) || Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return;
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

    private static boolean hasMaxHealthAttribute(LivingEntity entity) {
        return entity.getAttribute(Attributes.MAX_HEALTH) != null;
    }
}
