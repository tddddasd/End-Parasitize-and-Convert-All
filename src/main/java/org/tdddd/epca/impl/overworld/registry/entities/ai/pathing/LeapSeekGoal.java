package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Movement initiative for a gap-jumping parasite. Two jobs, both of which only ever run while the mob's
 * own navigation has <b>no path</b>, so real pathing and combat are never touched:
 *
 * <ol>
 *   <li><b>seek</b> - walk at a landing the leap's own bounded scan has proven it can leap to;</li>
 *   <li><b>approach</b> - walk at the mob's target, so a pit ahead never makes it stand still.</li>
 * </ol>
 *
 * <h2>Why this exists</h2>
 * <p>{@link ParasiteGapJump} lives in the move control, so it runs only while the mob is actually being
 * steered. {@code MoveControl.operation} is a <em>consumed</em> state: vanilla {@code MoveControl.tick()}
 * turns {@code MOVE_TO} into {@code WAIT} after the step, and only {@code PathNavigation} sets it again -
 * on a new path, and it sets {@code WAIT} when {@code PathFinder} returns nothing at all. A mob whose
 * target is across a pit therefore ends up with no path, no movement intent and no leap: it just stands
 * there. This goal supplies the missing intent.</p>
 *
 * <h2>Anti-pit safety</h2>
 * <p>Approach steps are clamped ({@link #APPROACH_STEP}) and every step is checked before it is issued:
 * {@link #isSafeStep} refuses a step whose column has no floor within
 * {@link ParasitePathing#MAX_LEAP_DROP} below the mob (void, lava, a cliff) and refuses a step that would
 * move the mob <em>closer to</em> a column with no floor once it is already within a step of it. That is
 * a two-column lookahead which only runs on the roughly {@link #APPROACH_REISSUE_TICKS}-tick re-issue,
 * not per tick. The pit is crossed by the leap and only by the leap; this goal merely walks the mob up
 * to it.</p>
 *
 * <h2>Order of evaluation</h2>
 * <p>Goals tick before the move control, so inside one tick: this goal issues {@code moveTo} or
 * {@code setWantedPosition} (or the safe-step fallback) -&gt; {@code MoveControl.tick()} consumes it and
 * steps -&gt; {@link ParasiteGapJump#tick} evaluates the leap and, if its 40-tick progress window has
 * closed with a validated landing, overwrites the movement with the arcing leap. Because the goal refuses
 * unsafe steps, the mob can never be walked off the lip by it; and if the goal declines to step, the
 * leap's window still closes and fires on its own.</p>
 *
 * <p><b>Cost.</b> No block lookups on a normal tick. The landing scan runs at most once per
 * {@link #RETRY_TICKS} ticks, the approach direction is arithmetic, and the two-column safety check runs
 * once per {@link #APPROACH_REISSUE_TICKS} ticks while an approach is actually active.</p>
 */
public class LeapSeekGoal extends Goal {

    /** Ticks between two landing searches, so the scan is never run per tick. */
    public static final int RETRY_TICKS = 60;

    /** Give up on a waypoint once the mob has walked this close to it without leaping. */
    private static final double ARRIVED_SQR = 2.25D;

    /** Stop if the mob never got anywhere near the waypoint (blocked by something else entirely). */
    private static final int WALK_TIMEOUT_TICKS = 140;

    /** Abandon the current waypoint if it drops out of sight in the world. */
    public static final double KEEP_RANGE = 12.0D;

    /** Ticks a forced move-control order is re-issued, so a run of path failures cannot starve it. */
    private static final int FORCE_REISSUE_TICKS = 20;

    /** How far ahead of the mob an approach step aims, so it walks to the lip instead of teleporting. */
    private static final double APPROACH_STEP = 1.5D;

    /** Ticks between two approach re-issues (the safety lookahead runs then, not every tick). */
    private static final int APPROACH_REISSUE_TICKS = 8;

    private final Mob mob;
    private final ParasiteGapJump gapJump;
    private final double speed;

    private final BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
    private boolean hasTarget;
    /** True while the waypoint is a leap landing (seek mode) rather than the target itself. */
    private boolean hasLanding;
    private int cooldown;
    private int walkTicks;
    /** The navigation had no path last tick, so a forced order is allowed to stop it. */
    private boolean navigationIdle;

    public LeapSeekGoal(Mob mob, int maxGapJumpWidth) {
        this(mob, maxGapJumpWidth, 1.0D);
    }

    public LeapSeekGoal(Mob mob, int maxGapJumpWidth, double speed) {
        this.mob = mob;
        // A second ParasiteGapJump is used purely as the lookahead engine (its own retry cooldown
        // keeps the scan off the per-tick path). The leap itself still runs in the move control's
        // instance; this one never leaps.
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
        // Only look for work when the mob has no path: a mob that is already walking is either making
        // progress on its own goal or being handled by it. A real path (combat included) is never
        // interrupted; only "cannot path to where I want to go" gets help.
        if (!this.mob.getNavigation().isDone()) {
            this.cooldown = RETRY_TICKS;
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = RETRY_TICKS;

        // Prefer the leap landing: it is validated reachable and it is what makes the leap fire.
        BlockPos landing = this.gapJump.findLandingTarget(this.mob);
        if (landing != null) {
            this.target.set(landing);
            this.hasLanding = true;
        } else {
            // No landing means no pit to cross, but the mob may still have a target it cannot path to
            // (a wall, a lip it is standing on, a partly-loaded chunk): walk at the target itself.
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
        // Seek mode ends when the target dies or leaves; approach mode also ends if the goal is gone.
        if (!this.hasLanding) {
            LivingEntity goal = this.mob.getTarget();
            if (goal == null || !goal.isAlive()) {
                return false;
            }
            // Already in reach: hand control back so nothing this goal does interferes with the
            // attack goals (interaction range is ~2 blocks).
            if (this.mob.distanceToSqr(goal) < ARRIVED_SQR) {
                return false;
            }
        }
        double dx = this.target.getX() + 0.5D - this.mob.getX();
        double dy = this.target.getY() - this.mob.getY();
        double dz = this.target.getZ() + 0.5D - this.mob.getZ();
        if (this.hasLanding && dx * dx + dz * dz <= ARRIVED_SQR && dy <= 1.5D) {
            // Walked up to the landing point (or already landed on it): hand control back. The
            // vertical part matters: a mob in mid-leap hangs above the landing with dy well
            // negative, and ending the goal there would drop its movement intent while airborne.
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
        // The pathfinder may have refused entirely, in which case the navigation keeps reporting
        // "done" and the move control is left in WAIT. Push the waypoint straight at the move control
        // instead, so MOVE_TO is guaranteed and the leap gets its chance. Only ever done when no path
        // exists at all (a working path is never disturbed).
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

    /**
     * Natural approach: one clamped step toward the target, refused when that step would take the mob
     * closer to a column with no floor. Sequential (never simultaneous) with the navigation request, so
     * a failed {@code moveTo} for the landing does not also overwrite the step.
     */
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
        // The direct step is unsafe (the pit is right there): stand still and let the leap's window
        // close rather than walking off the lip. Asking the pathfinder again is still allowed to try.
        this.mob.getNavigation().moveTo(goal, this.speed);
    }

    /**
     * True when stepping {@code (dirX, dirZ)} from the mob's position is safe. Two-column lookahead: the
     * step's own column, and the column one step beyond it, must each have a floor within
     * {@link ParasitePathing#MAX_LEAP_DROP} below the mob's feet - except that a missing floor beyond is
     * tolerated while the goal is still farther than one step away, which is exactly what lets the mob
     * walk up to the lip and then stop there for the leap.
     */
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

    /** True when the column at {@code (x, z)} has a floor within MAX_LEAP_DROP below {@code floorY}. */
    private boolean hasFloorNear(Mob mob, int floorY, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        // Re-set the scratch position with the public three-argument set(); setX/setY/setZ are
        // protected Vec3i members in 1.20.1 and a fresh BlockPos would allocate per column.
        for (int drop = 0; drop < ParasitePathing.MAX_LEAP_DROP; drop++) {
            if (mob.level().getBlockState(this.scratch.set(blockX, floorY - drop, blockZ)).isSolid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Drive the move control directly at the validated waypoint. The navigation is stopped first (it
     * has already reported no path, so it cannot fight the order) and then the move control is given
     * the target, which is what sets {@code operation = MOVE_TO} for the leap.
     */
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
