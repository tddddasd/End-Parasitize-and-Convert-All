package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class LeapSeekGoal extends Goal {

    public static final int RETRY_TICKS = 60;

    private static final double ARRIVED_SQR = 2.25D;

    private static final int WALK_TIMEOUT_TICKS = 140;

    public static final double KEEP_RANGE = 12.0D;

    private static final int FORCE_REISSUE_TICKS = 20;

    private static final double APPROACH_STEP = 1.5D;

    private static final int APPROACH_REISSUE_TICKS = 8;

    private final Mob mob;
    private final ParasiteGapJump gapJump;
    private final double speed;

    private final BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
    private boolean hasTarget;

    private boolean hasLanding;
    private int cooldown;
    private int walkTicks;

    private boolean navigationIdle;

    public LeapSeekGoal(Mob mob, int maxGapJumpWidth) {
        this(mob, maxGapJumpWidth, 1.0D);
    }

    public LeapSeekGoal(Mob mob, int maxGapJumpWidth, double speed) {
        this.mob = mob;

        this.gapJump = new ParasiteGapJump(maxGapJumpWidth);
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.gapJump.isEnabled()) {
            return false;
        }
        if (!this.mob.isAlive() || this.mob.isNoAi()) {
            return false;
        }

        if (!this.mob.getNavigation().isDone()) {
            this.cooldown = RETRY_TICKS;
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = RETRY_TICKS;

        BlockPos landing = this.gapJump.findLandingTarget(this.mob);
        if (landing != null) {
            this.target.set(landing);
            this.hasLanding = true;
        } else {

            LivingEntity goal = this.mob.getTarget();
            if (goal == null || !goal.isAlive()
                    || this.mob.distanceToSqr(goal) > KEEP_RANGE * KEEP_RANGE) {
                return false;
            }
            this.target.set((int) Math.floor(goal.getX()), (int) Math.floor(goal.getY()),
                    (int) Math.floor(goal.getZ()));
            this.hasLanding = false;
        }

        this.hasTarget = true;
        this.walkTicks = 0;
        this.navigationIdle = false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.hasTarget || !this.mob.isAlive() || this.mob.isNoAi()) {
            return false;
        }
        if (++this.walkTicks > WALK_TIMEOUT_TICKS) {
            return false;
        }

        if (!this.hasLanding) {
            LivingEntity goal = this.mob.getTarget();
            if (goal == null || !goal.isAlive()) {
                return false;
            }

            if (this.mob.distanceToSqr(goal) < ARRIVED_SQR) {
                return false;
            }
        }
        double dx = this.target.getX() + 0.5D - this.mob.getX();
        double dy = this.target.getY() - this.mob.getY();
        double dz = this.target.getZ() + 0.5D - this.mob.getZ();
        if (this.hasLanding && dx * dx + dz * dz <= ARRIVED_SQR && dy <= 1.5D) {

            return false;
        }
        return this.mob.distanceToSqr(this.target.getX() + 0.5D, this.target.getY(),
                this.target.getZ() + 0.5D) <= KEEP_RANGE * KEEP_RANGE
                && Math.abs(dy) <= ParasitePathing.MAX_LEAP_DROP;
    }

    @Override
    public void start() {
        this.walkTicks = 0;
        this.navigationIdle = false;
        this.moveTowardTarget();
    }

    @Override
    public void tick() {
        int reissue = this.hasLanding ? 30 : APPROACH_REISSUE_TICKS;
        if (this.walkTicks % reissue == 0) {
            this.moveTowardTarget();
        }

        if (this.mob.getNavigation().isDone()) {
            if (!this.navigationIdle || (this.hasLanding && this.walkTicks % FORCE_REISSUE_TICKS == 0)) {
                this.forceMoveTowardTarget();
            }
            this.navigationIdle = true;
        } else {
            this.navigationIdle = false;
        }
    }

    private void moveTowardTarget() {
        if (this.hasLanding) {
            this.mob.getNavigation().moveTo(
                    this.target.getX() + 0.5D,
                    this.target.getY(),
                    this.target.getZ() + 0.5D,
                    this.speed);
            return;
        }
        this.approachTarget();
    }

    private void approachTarget() {
        LivingEntity goal = this.mob.getTarget();
        if (goal == null) {
            return;
        }
        double dx = goal.getX() - this.mob.getX();
        double dz = goal.getZ() - this.mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > 1.0E-4D) {
            double step = Math.min(distance, APPROACH_STEP);
            double dirX = dx / distance * step;
            double dirZ = dz / distance * step;
            if (this.isSafeStep(this.mob, goal, dirX, dirZ)) {
                this.mob.getMoveControl().setWantedPosition(
                        this.mob.getX() + dirX, this.mob.getY(), this.mob.getZ() + dirZ, this.speed);
                return;
            }
        }

        this.mob.getNavigation().moveTo(goal, this.speed);
    }

    private boolean isSafeStep(Mob mob, LivingEntity goal, double dirX, double dirZ) {
        int floorY = (int) Math.floor(mob.getY());
        if (!this.hasFloorNear(mob, floorY, mob.getX() + dirX, mob.getZ() + dirZ)) {
            return false;
        }
        double goalDx = goal.getX() - mob.getX();
        double goalDz = goal.getZ() - mob.getZ();
        double goalLength = Math.sqrt(goalDx * goalDx + goalDz * goalDz);
        if (goalLength < 1.0E-4D) {
            return true;
        }
        double aheadX = mob.getX() + dirX + goalDx / goalLength * APPROACH_STEP;
        double aheadZ = mob.getZ() + dirZ + goalDz / goalLength * APPROACH_STEP;
        return this.hasFloorNear(mob, floorY, aheadX, aheadZ) || goalLength > APPROACH_STEP;
    }

    private boolean hasFloorNear(Mob mob, int floorY, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        for (int drop = 0; drop < ParasitePathing.MAX_LEAP_DROP; drop++) {
            if (mob.level().getBlockState(this.scratch.set(blockX, floorY - drop, blockZ)).isSolid()) {
                return true;
            }
        }
        return false;
    }

    private void forceMoveTowardTarget() {
        this.mob.getNavigation().stop();
        this.mob.getMoveControl().setWantedPosition(
                this.target.getX() + 0.5D,
                this.target.getY(),
                this.target.getZ() + 0.5D,
                this.speed);
    }

    @Override
    public void stop() {
        this.hasTarget = false;
        this.hasLanding = false;
        this.navigationIdle = false;
        this.mob.getNavigation().stop();
    }
}
