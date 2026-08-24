package org.tdddd.epca.impl.utils.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility methods for entity movement - floating, water travel, cliff jumping, etc.
 */
public final class EntityMovementUtils {

    private EntityMovementUtils() {}

    /**
     * Apply floating behavior for entities in water.
     * Call from tick() to make entity float in water instead of sinking.
     */
    public static void updateWaterFloating(LivingEntity entity, FloatingState state) {
        if (!entity.isInWater()) {
            state.floatingTime = 0;
            return;
        }

        Vec3 vel = entity.getDeltaMovement();
        if (vel.y < 0.0D) {
            entity.setDeltaMovement(vel.x, Math.max(vel.y * 0.8D, -0.05D), vel.z);
        }

        state.floatingTime++;
        if (state.floatingTime > 10) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, 0.4D, 0.0D));
            state.floatingTime = 0;
        }
    }

    /**
     * Apply water travel (slow movement while in water).
     * Call from travel() override.
     */
    public static void applyWaterTravel(LivingEntity entity, Vec3 travelVector) {
        entity.moveRelative(0.01F, travelVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.9D));
    }

    /**
     * Attempt to jump over a cliff towards a target.
     * Returns true if a jump was performed.
     */
    public static boolean tryCliffJump(LivingEntity entity, LivingEntity target,
                                        BlockPos currentPos, double jumpPower, double horizontalSpeed) {
        if (target == null || !entity.onGround() || entity.isInWater()) return false;

        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        double hDistSq = dx * dx + dz * dz;
        if (hDistSq <= 1.0) return false;

        Vec3 dir = new Vec3(dx, 0, dz).normalize();

        // Check if forward is clear and ground drops ahead (cliff)
        Level level = entity.level();
        double forward1Y = EntityBreakUtils.getGroundHeightAt(level,
                BlockPos.containing(entity.getX() + dir.x, entity.getY(), entity.getZ() + dir.z), 5);
        double forward2Y = EntityBreakUtils.getGroundHeightAt(level,
                BlockPos.containing(entity.getX() + dir.x * 2, entity.getY(), entity.getZ() + dir.z * 2), 5);
        double currentGroundY = EntityBreakUtils.getGroundHeightAt(level, currentPos, 5);

        boolean isCliffToJump = (currentGroundY - forward1Y > 0.5) && (currentGroundY - forward2Y <= 0.5);
        if (!isCliffToJump) return false;

        entity.setDeltaMovement(dir.x * horizontalSpeed, jumpPower, dir.z * horizontalSpeed);
        entity.hasImpulse = true;
        return true;
    }

    /**
     * State holder for floating behavior. Store one instance per entity.
     */
    public static class FloatingState {
        public int floatingTime;
    }
}
