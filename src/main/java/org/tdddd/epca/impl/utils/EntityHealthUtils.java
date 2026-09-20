package org.tdddd.epca.impl.utils;

import net.minecraft.world.entity.LivingEntity;


public final class EntityHealthUtils {

    
    public static final float MIN_BURST_HEALTH = 0.02F;

    private EntityHealthUtils() {
    }

    
    public static float burstHealth(LivingEntity entity, float requested) {
        if (!entity.isOnFire() && requested < MIN_BURST_HEALTH) return MIN_BURST_HEALTH;
        return requested;
    }
}
