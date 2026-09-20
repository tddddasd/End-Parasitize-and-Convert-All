package org.tdddd.epca.impl.utils;

import net.minecraft.world.entity.LivingEntity;

/**
 * Helpers for the health values used by fake death / self destruct.
 */
public final class EntityHealthUtils {

    /** Lowest health a non-burning entity may be left at during fake death / self destruct. */
    public static final float MIN_BURST_HEALTH = 0.02F;

    private EntityHealthUtils() {
    }

    /**
     * Applies the fake death / self destruct health floor.
     *
     * <p>26.1.2: Entity#isOnFire() is still the vanilla burning check, so the rule is
     * "if the entity is not burning and the requested health is below 0.02, use 0.02".</p>
     */
    public static float burstHealth(LivingEntity entity, float requested) {
        if (!entity.isOnFire() && requested < MIN_BURST_HEALTH) {
            return MIN_BURST_HEALTH;
        }
        return requested;
    }
}
