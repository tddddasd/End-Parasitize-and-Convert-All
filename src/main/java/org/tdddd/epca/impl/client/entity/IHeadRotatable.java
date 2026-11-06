package org.tdddd.epca.impl.client.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public interface IHeadRotatable {

    default float getHeadYawTarget(float partialTick) {
        LivingEntity self = (LivingEntity) this;

        if (this instanceof Mob mob) {
            var target = mob.getTarget();
            if (target != null) {
                double dx = target.getX() - self.getX();
                double dz = target.getZ() - self.getZ();
                double angle = Math.atan2(dz, dx) * (180.0 / Math.PI);
                return (float) (angle - 90);
            }
        }

        double hSpeedSq = self.getDeltaMovement().horizontalDistanceSqr();
        if (hSpeedSq > 0.001) {
            double dx = self.getDeltaMovement().x;
            double dz = self.getDeltaMovement().z;
            double angle = Math.atan2(dz, dx) * (180.0 / Math.PI);
            return (float) (angle + 90);
        }

        return Float.MAX_VALUE;
    }

    default String getHeadBoneName() {
        return "head";
    }

    default float getMaxHeadYaw() {
        return 60.0f;
    }

    default float getHeadRotationSpeed() {
        return 3.0f;
    }

    default boolean shouldRotateHead() {
        LivingEntity self = (LivingEntity) this;
        boolean hasTarget = (this instanceof Mob mob) && mob.getTarget() != null;
        return hasTarget || self.getDeltaMovement().horizontalDistanceSqr() > 0.001;
    }
}
