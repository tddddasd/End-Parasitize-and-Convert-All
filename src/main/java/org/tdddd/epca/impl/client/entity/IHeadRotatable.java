package org.tdddd.epca.impl.client.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Interface for entities that support dynamic head rotation towards their look target.
 * The actual rotation is applied by {@link HeadRotationHandler} via client events,
 * keeping head rotation logic out of model classes.
 */
public interface IHeadRotatable {

    /**
     * Get the current head yaw rotation target for this entity.
     * Called each frame on the client to compute head bone rotation.
     *
     * @param partialTick the partial tick for smooth interpolation
     * @return the desired head yaw in degrees, or Float.MAX_VALUE for no rotation
     */
    default float getHeadYawTarget(float partialTick) {
        LivingEntity self = (LivingEntity) this;

        // Look at target if available (getTarget() is on Mob)
        if (this instanceof Mob mob) {
            var target = mob.getTarget();
            if (target != null) {
                double dx = target.getX() - self.getX();
                double dz = target.getZ() - self.getZ();
                double angle = Math.atan2(dz, dx) * (180.0 / Math.PI);
                return (float) (angle - 90);
            }
        }

        // Look in movement direction
        double hSpeedSq = self.getDeltaMovement().horizontalDistanceSqr();
        if (hSpeedSq > 0.001) {
            double dx = self.getDeltaMovement().x;
            double dz = self.getDeltaMovement().z;
            double angle = Math.atan2(dz, dx) * (180.0 / Math.PI);
            return (float) (angle + 90);
        }

        return Float.MAX_VALUE;
    }

    /** Bone name to rotate for head look. Default "head". */
    default String getHeadBoneName() {
        return "head";
    }

    /** Maximum head yaw rotation in degrees. Default ±60°. */
    default float getMaxHeadYaw() {
        return 60.0f;
    }

    /** Maximum rotation speed in degrees per tick. Default 3.0. */
    default float getHeadRotationSpeed() {
        return 3.0f;
    }

    /** Whether head rotation should be applied right now. */
    default boolean shouldRotateHead() {
        LivingEntity self = (LivingEntity) this;
        boolean hasTarget = (this instanceof Mob mob) && mob.getTarget() != null;
        return hasTarget || self.getDeltaMovement().horizontalDistanceSqr() > 0.001;
    }
}
