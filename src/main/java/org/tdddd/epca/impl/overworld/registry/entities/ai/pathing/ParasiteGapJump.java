package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class ParasiteGapJump {

    private static final int DISTANCE_MASK = 0xFF;
    private static final int FLOOR_SHIFT = 8;

    private static final int SCAN_COOLDOWN_TICKS = 60;

    private final int maxGapJumpWidth;

    private int leapTicks;

    private int windowTicks;

    private double windowX;
    private double windowY;
    private double windowZ;
    private double windowBest;
    private boolean windowActive;

    private double windowGoalDistance;
    private double windowGoalBest;
    private boolean windowHasGoal;

    private double lastWantedX = Double.NaN;
    private double lastWantedZ = Double.NaN;
    private int wantedAge;
    private boolean goalDead;

    private double leapDirX;
    private double leapDirZ;
    private double leapForward;

    private int leapDirReach;

    private int scanCooldown;

    private int edgeCooldown;

    private static final int EDGE_COOLDOWN_TICKS = 60;

    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    private boolean blocked;

    public ParasiteGapJump(int maxGapJumpWidth) {
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    public boolean isEnabled() {
        return this.maxGapJumpWidth > 0;
    }

    public boolean isBlocked() {
        return this.blocked;
    }

    public void tick(Mob mob, boolean wantsToMove, double wantedX, double wantedY, double wantedZ) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.edgeCooldown > 0) {
            this.edgeCooldown--;
        }

        this.blocked = wantsToMove
                || mob.getTarget() != null
                || (mob.getNavigation().isDone()
                        && mob.onGround()
                        && (mob.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D
                                || mob.horizontalCollision));

        if (!mob.onGround()) {
            this.keepArc(mob);
        } else {
            this.leapTicks = 0;
        }

        if (!isUsable(wantedX) || !isUsable(wantedZ)) {
            this.resetWindow();
            return;
        }
        if (!this.blocked) {
            this.resetWindow();
            return;
        }

        if (!ParasitePathing.canLeap(mob, this.maxGapJumpWidth)) {
            this.resetWindow();
            return;
        }

        this.trackGoal(wantedX, wantedZ);

        if (this.edgeLeap(mob)) {
            this.resetWindow();
            return;
        }

        this.trackStuck(mob, wantedX, wantedY, wantedZ);
    }

    private void resetWindow() {
        this.windowTicks = 0;
        this.windowActive = false;
    }

    private boolean edgeLeap(Mob mob) {
        LivingEntity goal = mob.getTarget();
        if (goal == null || !goal.isAlive() || this.edgeCooldown > 0) {
            return false;
        }
        this.edgeCooldown = EDGE_COOLDOWN_TICKS;

        BlockPos centre = BlockPos.containing(mob.getX(), mob.getY(), mob.getZ());
        if (!mob.level().getBlockState(centre.below()).isAir()) {
            return false;
        }

        return this.leapToward(mob, goal);
    }

    private boolean leapToward(Mob mob, LivingEntity goal) {
        double dx = goal.getX() - mob.getX();
        double dz = goal.getZ() - mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0E-4D) {
            return false;
        }

        double reach = Math.min(distance, this.maxGapJumpWidth);

        double dirX = dx / distance;
        double dirZ = dz / distance;

        this.leapDirX = dirX;
        this.leapDirZ = dirZ;

        int limit = this.maxGapJumpWidth + 1;
        double maxDistance = Math.min(limit + 0.5D, reach + 1.5D);
        int result = this.scanWithin(mob, dirX, dirZ, limit, maxDistance);
        if (result == 0 && reach < this.maxGapJumpWidth) {

            result = this.scanWithin(mob, dirX, dirZ, limit, limit + 0.5D);
        }
        if (result == 0) {
            return false;
        }

        int floorY = result >> FLOOR_SHIFT;
        int landingX = (int) Math.floor(mob.getX() + dirX);
        int landingZ = (int) Math.floor(mob.getZ() + dirZ);
        return this.canLandOn(mob, landingX, floorY, landingZ)
                && this.commit(mob, dirX, dirZ, result);
    }

    private boolean canLandOn(Mob mob, int x, int floorY, int z) {
        if (!mob.level().getBlockState(this.scratch.set(x, floorY, z)).isAir()) {
            return false;
        }

        for (int drop = 1; drop <= ParasitePathing.MAX_LEAP_DROP; drop++) {
            if (mob.level().getBlockState(this.scratch.set(x, floorY - drop, z)).isSolid()) {
                return true;
            }
        }
        return false;
    }

    private boolean commit(Mob mob, double dirX, double dirZ, int result) {
        if (result == 0) {
            return false;
        }
        this.leap(mob, dirX, dirZ, result & DISTANCE_MASK);
        return true;
    }

    private void trackGoal(double wantedX, double wantedZ) {
        if (wantedX == this.lastWantedX && wantedZ == this.lastWantedZ) {
            if (this.wantedAge < ParasitePathing.LEAP_TRACKING_TICKS) {
                this.wantedAge++;
            }
        } else {
            this.lastWantedX = wantedX;
            this.lastWantedZ = wantedZ;
            this.wantedAge = 0;
        }
        this.goalDead = this.wantedAge >= ParasitePathing.LEAP_TRACKING_TICKS;
    }

    private void trackStuck(Mob mob, double wantedX, double wantedY, double wantedZ) {
        double x = mob.getX();
        double y = mob.getY();
        double z = mob.getZ();

        LivingEntity target = mob.getTarget();
        boolean hasGoal = target != null && target.isAlive();
        double goalDistance = hasGoal ? horizontalDistanceTo(x, z, target.getX(), target.getZ()) : 0.0D;

        if (!this.windowActive || this.windowTicks <= 0) {
            this.windowActive = true;
            this.windowTicks = ParasitePathing.LEAP_TRACKING_TICKS;
            this.windowX = x;
            this.windowY = y;
            this.windowZ = z;
            this.windowBest = 0.0D;
            this.windowHasGoal = hasGoal;
            this.windowGoalDistance = goalDistance;
            this.windowGoalBest = goalDistance;
            return;
        }

        this.windowTicks--;
        double dx = x - this.windowX;
        double dy = y - this.windowY;
        double dz = z - this.windowZ;
        double covered = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (covered > this.windowBest) {
            this.windowBest = covered;
        }
        if (hasGoal && goalDistance < this.windowGoalBest) {
            this.windowGoalBest = goalDistance;
        }

        if (this.windowTicks > 0) {
            return;
        }

        boolean stuck = this.windowHasGoal && hasGoal
                ? this.windowGoalDistance - this.windowGoalBest < ParasitePathing.MIN_PROGRESS
                : this.windowBest < ParasitePathing.LEAP_STUCK_DISTANCE;
        this.windowActive = false;
        if (!stuck) {
            return;
        }

        this.tryLeap(mob, wantedX, wantedY, wantedZ);
    }

    private void tryLeap(Mob mob, double wantedX, double wantedY, double wantedZ) {
        if (this.goalUsable(mob, wantedX, wantedY, wantedZ)
                && this.commit(mob, this.scan(mob, wantedX - mob.getX(), wantedZ - mob.getZ()))) {
            return;
        }

        double moveX = mob.getX() - mob.xo;
        double moveZ = mob.getZ() - mob.zo;
        if (moveX * moveX + moveZ * moveZ > 2.5E-3D
                && this.commit(mob, this.scan(mob, moveX, moveZ))) {
            return;
        }

        double yawRad = Math.toRadians(mob.getYRot());
        if (this.commit(mob, this.scan(mob, -Math.sin(yawRad), Math.cos(yawRad)))) {
            return;
        }

        for (int i = 0; i < ParasitePathing.COMPASS_COUNT; i++) {
            if (this.commit(mob, this.scan(mob, ParasitePathing.compassX(i),
                    ParasitePathing.compassZ(i)))) {
                return;
            }
        }
    }

    private boolean goalUsable(Mob mob, double wantedX, double wantedY, double wantedZ) {
        if (this.goalDead) {
            return false;
        }
        double dx = wantedX - mob.getX();
        double dz = wantedZ - mob.getZ();
        if (Math.sqrt(dx * dx + dz * dz) < ParasitePathing.MIN_LEAP_DISTANCE) {
            return false;
        }
        double dy = wantedY - mob.getY();
        return dy >= -ParasitePathing.MAX_LEAP_DROP && dy <= ParasitePathing.MAX_LEAP_RISE;
    }

    public BlockPos findLandingTarget(Mob mob) {
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return null;
        }
        this.scanCooldown = SCAN_COOLDOWN_TICKS;
        if (!this.isEnabled() || !ParasitePathing.canLeap(mob, this.maxGapJumpWidth)) {
            return null;
        }

        this.leapDirReach = 0;
        if (!this.tryDirection(mob, mob.getX() - mob.xo, mob.getZ() - mob.zo)
                && !this.tryYaw(mob)) {
            for (int i = 0; i < ParasitePathing.COMPASS_COUNT; i++) {
                if (this.tryDirection(mob, ParasitePathing.compassX(i),
                        ParasitePathing.compassZ(i))) {
                    break;
                }
            }
        }

        if (this.leapDirReach <= 0) {
            return null;
        }
        int targetX = (int) Math.floor(mob.getX() + this.leapDirX * (this.leapDirReach + 1.0D));
        int targetZ = (int) Math.floor(mob.getZ() + this.leapDirZ * (this.leapDirReach + 1.0D));
        return new BlockPos(targetX, (int) Math.floor(mob.getY()), targetZ);
    }

    private boolean tryDirection(Mob mob, double dx, double dz) {
        return this.apply(this.scan(mob, dx, dz));
    }

    private boolean tryYaw(Mob mob) {
        double yawRad = Math.toRadians(mob.getYRot());
        return this.tryDirection(mob, -Math.sin(yawRad), Math.cos(yawRad));
    }

    private boolean apply(int result) {
        if (result == 0) {
            return false;
        }
        this.leapDirReach = result & DISTANCE_MASK;
        return true;
    }

    private boolean commit(Mob mob, int result) {
        if (result == 0) {
            return false;
        }
        this.leap(mob, this.leapDirX, this.leapDirZ, result & DISTANCE_MASK);
        return true;
    }

    private int scan(Mob mob, double dirX, double dirZ) {
        int limit = this.maxGapJumpWidth + 1;
        return this.scanWithin(mob, dirX, dirZ, limit, limit + 0.5D);
    }

    private int scanWithin(Mob mob, double dirX, double dirZ, int limit, double maxDistance) {
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length < 1.0E-4D) {
            return 0;
        }
        dirX /= length;
        dirZ /= length;

        int takeoffY = (int) Math.floor(mob.getY());
        int highestFloor = takeoffY + ParasitePathing.MAX_LEAP_RISE;

        double originX = mob.getX();
        double originZ = mob.getZ();

        int lastX = mob.getBlockX();
        int lastZ = mob.getBlockZ();
        int columns = 0;
        boolean overPit = false;

        int steps = 0;
        int maxSteps = limit * 2 + 2;
        while (steps < maxSteps && columns < limit) {
            steps++;
            int x = (int) Math.floor(originX + dirX * steps);
            int z = (int) Math.floor(originZ + dirZ * steps);
            if (x == lastX && z == lastZ) {
                continue;
            }
            lastX = x;
            lastZ = z;
            columns++;

            int mask = ParasitePathing.blockMask(mob.level(), x, takeoffY, z);

            if (!overPit) {

                if (ParasitePathing.isWall(mask)) {
                    return 0;
                }
                if (!ParasitePathing.isSolidAtFeet(mask) && !ParasitePathing.isFloor(mask)) {

                    overPit = true;
                }
                continue;
            }

            if (ParasitePathing.isWall(mask)) {
                return 0;
            }
            if (ParasitePathing.isSolidAtFeet(mask)) {

                if (highestFloor > takeoffY) {
                    int packed = this.packed(x, z, highestFloor, originX, originZ, maxDistance);
                    if (packed != 0) {
                        return packed;
                    }
                }
                continue;
            }
            if (ParasitePathing.isFloor(mask)) {

                return this.packed(x, z, takeoffY, originX, originZ, maxDistance);
            }
        }

        return 0;
    }

    private int packed(int x, int z, int floorY, double originX, double originZ, double maxDistance) {
        double dx = x + 0.5D - originX;
        double dz = z + 0.5D - originZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > maxDistance || distance < 1.0D) {
            return 0;
        }
        return (floorY << FLOOR_SHIFT) | ((int) Math.round(distance) & DISTANCE_MASK);
    }

    private void leap(Mob mob, double dirX, double dirZ, int distance) {
        if (!(mob instanceof GapJumper gapJumper)) {

            return;
        }

        double up = mob.getDeltaMovement().y;

        gapJumper.epcaJumpFromGround();

        double jumpUp = mob.getDeltaMovement().y;
        if (jumpUp <= up) {

            return;
        }

        double forward = ParasitePathing.sheepForwardSpeed(distance);

        mob.setDeltaMovement(dirX * forward, jumpUp, dirZ * forward);

        this.leapDirX = dirX;
        this.leapDirZ = dirZ;
        this.leapForward = forward;
        this.leapTicks = ParasitePathing.airTicks(jumpUp);
    }

    private void keepArc(Mob mob) {
        if (this.leapTicks <= 0) {
            return;
        }
        this.leapTicks--;

        double target = this.leapForward;
        if (target <= 0.0D) {
            return;
        }

        Vec3 delta = mob.getDeltaMovement();
        double speed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (speed >= target || speed < 1.0E-4D) {
            return;
        }

        double scale = target / speed;
        mob.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
    }

    private static boolean isUsable(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static double horizontalDistanceTo(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
