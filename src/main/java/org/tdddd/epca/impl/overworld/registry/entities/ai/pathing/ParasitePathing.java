package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class ParasitePathing {

    public static final int MAX_GAP_JUMP_HUMANOID = 2;

    public static final int MAX_GAP_JUMP_FOX = 4;

    public static final int MAX_GAP_JUMP_RESHAPE_LONGARMS = 8;

    public static final int MAX_GAP_JUMP_NONE = 0;

    public static final int LEAP_TRACKING_TICKS = 40;

    public static final double LEAP_STUCK_DISTANCE = 0.35D;

    public static final double MIN_PROGRESS = 0.5D;

    public static final double FALL_EPSILON = 0.01D;

    public static final double MIN_LEAP_DISTANCE = 1.0D;

    public static final int MAX_LEAP_DROP = 3;

    public static final int MAX_LEAP_RISE = 1;

    private static final double[][] COMPASS_DIRECTIONS = {
            {0.0D, 1.0D},
            {0.70710678D, 0.70710678D},
            {1.0D, 0.0D},
            {0.70710678D, -0.70710678D},
            {0.0D, -1.0D},
            {-0.70710678D, -0.70710678D},
            {-1.0D, 0.0D},
            {-0.70710678D, 0.70710678D}
    };

    public static final int COMPASS_COUNT = COMPASS_DIRECTIONS.length;

    public static double compassX(int index) {
        return COMPASS_DIRECTIONS[index][0];
    }

    public static double compassZ(int index) {
        return COMPASS_DIRECTIONS[index][1];
    }

    public static final double SHEEP_FORWARD_SPEED = 0.36D;

    public static final double SHEEP_FORWARD_REACH = 2.0D;

    public static final double MAX_LAUNCH_FORWARD = 1.40D;

    private ParasitePathing() {
    }

    private static final int SOLID_AT_FEET = 1;

    private static final int SOLID_BELOW = 2;

    private static final int WALKABLE = 4;

    public static int blockMask(Level level, int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) {
            return -1;
        }

        boolean feetSolid = solidAt(level, x, y, z);
        if (feetSolid) {
            return SOLID_AT_FEET;
        }

        int mask = WALKABLE;
        if (solidAt(level, x, y - 1, z)) {
            mask |= SOLID_BELOW;
        }
        return mask;
    }

    public static boolean isSolidAtFeet(int mask) {
        return mask < 0 || (mask & SOLID_AT_FEET) != 0;
    }

    public static boolean isWalkable(int mask) {
        return mask >= 0 && (mask & WALKABLE) != 0;
    }

    public static boolean isFloor(int mask) {
        return mask >= 0 && (mask & SOLID_BELOW) != 0;
    }

    public static boolean isWall(int mask) {
        return isSolidAtFeet(mask) && isFloor(mask);
    }

    private static boolean solidAt(Level level, int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) {
            return true;
        }
        return !level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    public static double sheepForwardSpeed(double distance) {
        double scaled = SHEEP_FORWARD_SPEED * distance / SHEEP_FORWARD_REACH;
        if (scaled < SHEEP_FORWARD_SPEED) {
            return SHEEP_FORWARD_SPEED;
        }
        return scaled > MAX_LAUNCH_FORWARD ? MAX_LAUNCH_FORWARD : scaled;
    }

    private static final double GRAVITY = 0.08D;

    public static int airTicks(double up) {
        int ticks = (int) Math.ceil(2.0D * Math.max(0.0D, up) / GRAVITY);
        if (ticks < 10) {
            return 10;
        }
        return ticks > 40 ? 40 : ticks;
    }

    public static boolean canLeap(Mob mob, int maxGapJumpWidth) {
        if (maxGapJumpWidth <= 0 || mob.isInWater()) {
            return false;
        }
        return mob.getDeltaMovement().y > -FALL_EPSILON;
    }
}
