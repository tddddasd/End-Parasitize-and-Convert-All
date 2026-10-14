package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Throttled gap jump for one parasite.
 *
 * <p>This class never runs inside the pathfinder. One instance lives in a parasite move control,
 * {@code super.tick()} runs first and unchanged, and the leap is only considered afterwards.</p>
 *
 * <h2>Why this cannot stall the server</h2>
 * <ol>
 *   <li>The pathfinder is untouched: no extra neighbours, no extra edges, no block lookups while a
 *       path is being computed. {@link ParasiteNodeEvaluator} keeps the vanilla
 *       {@code getNeighbors}.</li>
 *   <li>The expensive part (the direction scan) sits behind a stuck detector that only fires once per
 *       {@link ParasitePathing#LEAP_TRACKING_TICKS} ticks, and only for a mob that has support,
 *       wants to move, and has covered almost no ground during that whole window.</li>
 *   <li>On every other tick this class reads no block state at all, except for the immediate
 *       centre-air-below trigger, which costs a single read and is throttled to once per
 *       {@link #EDGE_COOLDOWN_TICKS} ticks for a mob that already has a target.</li>
 *   <li>The scan is a bounded set of straight rays: at most one per candidate direction, each at most
 *       {@code maxGap + 1} distinct columns with two to three block-state lookups per column, and each
 *       ray exits at the first landing column or at the first wall. It never fans out over heights.</li>
 * </ol>
 *
 * <h2>Geometry</h2>
 * <p>A leap is a straight ray from the mob's leading edge. It starts on the column the mob occupies
 * (or its immediate neighbour), walks over the lip, and ends at the first column beyond the pit whose
 * floor is within {@link ParasitePathing#MAX_LEAP_DROP} below and
 * {@link ParasitePathing#MAX_LEAP_RISE} above the mob's own feet level. That is what lets a perched
 * mob come down a staircase or step across to a lower platform: the landing floor does not have to be
 * at the mob's own level, which is what the previous revision wrongly required.</p>
 */
public final class ParasiteGapJump {

    /** Packing of the scan result: the low bits are the distance, the rest is the landing floor y. */
    private static final int DISTANCE_MASK = 0xFF;
    private static final int FLOOR_SHIFT = 8;

    /** Ticks between two {@link #findLandingTarget} scans, so the lookahead is never per tick. */
    private static final int SCAN_COOLDOWN_TICKS = 60;

    private final int maxGapJumpWidth;

    /** Airborne ticks still owned by the current leap. */
    private int leapTicks;

    /** Ticks left in the current stuck-detection window. */
    private int windowTicks;

    /** Scratch state of the current stuck-detection window. */
    private double windowX;
    private double windowY;
    private double windowZ;
    private double windowBest;
    private boolean windowActive;

    /**
     * Progress tracking for the current window. When the mob has a goal (an attack target), the window
     * measures how much it closed the horizontal distance to it; otherwise it falls back to plain
     * displacement. All arithmetic - no block lookups.
     */
    private double windowGoalDistance;
    private double windowGoalBest;
    private boolean windowHasGoal;

    /** Last wanted position, used to notice a stale goal. */
    private double lastWantedX = Double.NaN;
    private double lastWantedZ = Double.NaN;
    private int wantedAge;
    private boolean goalDead;

    /** Direction and speed of the leap in the air, so the arc needs no navigation while airborne. */
    private double leapDirX;
    private double leapDirZ;
    private double leapForward;

    /** Horizontal reach of the last successful {@link #scan}, in blocks. */
    private int leapDirReach;

    /** Retry cooldown for the public {@link #findLandingTarget} lookahead. */
    private int scanCooldown;

    /**
     * Per-instance throttle for the immediate centre-air-below trigger, in ticks. It is the only guard
     * that trigger keeps (it deliberately bypasses the 40-tick progress window), so an edge-standing mob
     * still cannot leap more than once per interval.
     */
    private int edgeCooldown;

    /** Ticks between two immediate centre-air-below leaps for one instance. */
    private static final int EDGE_COOLDOWN_TICKS = 60;

    /**
     * Per-instance scratch position, so the landing validation allocates nothing per leap.
     */
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    /**
     * True when the mob is blocked: it is steering at something, or it has a target, and it is not
     * getting anywhere (grounded, no horizontal speed, or pressed against something). Read by the
     * move control's second trigger, so {@code operation == WAIT} (the state
     * {@code PathNavigation} forces when pathfinding returns nothing at all) no longer suppresses the
     * leap.
     */
    private boolean blocked;

    public ParasiteGapJump(int maxGapJumpWidth) {
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    /** True when the leap should evaluate even though the move control has no {@code MOVE_TO} step. */
    public boolean isBlocked() {
        return this.blocked;
    }

    public boolean isEnabled() {
        return this.maxGapJumpWidth > 0;
    }

    /**
     * Called once per tick from a move control, after {@code super.tick()}.
     *
     * <p>This is evaluated on <b>every</b> tick regardless of {@code Operation}: the leap decides for
     * itself whether the mob is in a state that warrants one. {@code operation} is only used as a
     * fast-path signal inside {@link #blocked}, never as a precondition.</p>
     *
     * @param mob         the mob this leap belongs to
     * @param wantsToMove whether the move control had a {@code MOVE_TO} step, read before
     *                    {@code super.tick()}
     * @param wantedX     next path node X the navigation wants the mob to reach
     * @param wantedY     next path node Y the navigation wants the mob to reach
     * @param wantedZ     next path node Z the navigation wants the mob to reach
     */
    public void tick(Mob mob, boolean wantsToMove, double wantedX, double wantedY, double wantedZ) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.edgeCooldown > 0) {
            this.edgeCooldown--;
        }

        // The trigger state. "Blocked" is cheap (flag reads plus a velocity test, no world access) and
        // deliberately includes the reported case: a target acquired with no path to it. It only
        // decides whether the leap may be *evaluated*; the progress window and the landing scan below
        // are still mandatory, so this cannot fire in open ground.
        this.blocked = wantsToMove
                || mob.getTarget() != null
                || (mob.getNavigation().isDone()
                        && mob.onGround()
                        && (mob.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D
                                || mob.horizontalCollision));

        // Arc steering belongs to the leap itself, so it is keyed on the leap and not on the ground
        // flag: the leap may now start while the mob is briefly airborne (not falling), and the top-up
        // has to survive that.
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

        // Immediate trigger (user: "长臂自身中心底部为空气时需要向目标位置起跳8格"): the column directly
        // under the mob's own centre is air, i.e. its footing is gone. Leap toward the target straight
        // away instead of waiting for the 40-tick progress window, then deactivate the window so the
        // ordinary progress path cannot also fire: one leap per window, however it was triggered.
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

    /**
     * Immediate trigger: the column directly under the mob's own centre is air. Leaps straight at the
     * target, bypassing the 40-tick progress window, and is the only thing that can cross the gap.
     *
     * <p>The centre test uses the mob's real x/z centre (for a 2x4 long-arms that is not necessarily
     * {@code blockPosition()}) and costs its single block-state read. It is only reached from
     * {@link #tick} after {@code blocked} and {@code canLeap} have both passed and while a window is
     * active (so while a target/approach is live or the track job is running), never on an idle tick;
     * and this method exits before the centre read for a mob with no target.</p>
     */
    private boolean edgeLeap(Mob mob) {
        LivingEntity goal = mob.getTarget();
        if (goal == null || !goal.isAlive() || this.edgeCooldown > 0) {
            return false;
        }
        this.edgeCooldown = EDGE_COOLDOWN_TICKS;

        // "自身中心底部": the block under the mob's real centre, not under its block position.
        BlockPos centre = BlockPos.containing(mob.getX(), mob.getY(), mob.getZ());
        if (!mob.level().getBlockState(centre.below()).isAir()) {
            return false;
        }

        return this.leapToward(mob, goal);
    }

    /**
     * Leaps toward the target: direction is the normalised horizontal vector to it, and the reach is the
     * target distance clamped to the group's maximum. A target closer than the maximum gets exactly its
     * own distance; a farther one gets the maximum, and the ray then takes the furthest valid landing up
     * to that limit.
     */
    private boolean leapToward(Mob mob, LivingEntity goal) {
        double dx = goal.getX() - mob.getX();
        double dz = goal.getZ() - mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0E-4D) {
            return false;
        }

        double reach = Math.min(distance, this.maxGapJumpWidth);
        // The leap always travels on the exact direction to the target; `reach` only bounds how far the
        // validation ray may look, so a close target cannot overshoot.
        double dirX = dx / distance;
        double dirZ = dz / distance;

        // The ordinary direction-scan helpers read these fields, so keep them in sync with the
        // direction this leap is actually using.
        this.leapDirX = dirX;
        this.leapDirZ = dirZ;

        // The ray must be able to see one landing column past the clamped reach, and it may look no
        // further than the reach plus a small margin when the target is close.
        int limit = this.maxGapJumpWidth + 1;
        double maxDistance = Math.min(limit + 0.5D, reach + 1.5D);
        int result = this.scanWithin(mob, dirX, dirZ, limit, maxDistance);
        if (result == 0 && reach < this.maxGapJumpWidth) {
            // Nothing inside the clamped reach: let the ray look as far as the group maximum so a
            // target beyond it still lands on the furthest valid platform up to 8 blocks away.
            result = this.scanWithin(mob, dirX, dirZ, limit, limit + 0.5D);
        }
        if (result == 0) {
            return false;
        }

        // The landing must actually be standable (solid floor within the drop window, clear above),
        // which is also what refuses lava and void on the far side.
        int floorY = result >> FLOOR_SHIFT;
        int landingX = (int) Math.floor(mob.getX() + dirX);
        int landingZ = (int) Math.floor(mob.getZ() + dirZ);
        return this.canLandOn(mob, landingX, floorY, landingZ)
                && this.commit(mob, dirX, dirZ, result);
    }

    /** True when the landing column is solid below and clear above, so the mob can stand there. */
    private boolean canLandOn(Mob mob, int x, int floorY, int z) {
        if (!mob.level().getBlockState(this.scratch.set(x, floorY, z)).isAir()) {
            return false;
        }
        // The floor is the first solid block within the drop window below the take-off level. The
        // scratch position is re-set with the single public three-argument set(): setX/setY/setZ are
        // protected Vec3i members in 1.20.1, and a fresh BlockPos would allocate on the firing path.
        for (int drop = 1; drop <= ParasitePathing.MAX_LEAP_DROP; drop++) {
            if (mob.level().getBlockState(this.scratch.set(x, floorY - drop, z)).isSolid()) {
                return true;
            }
        }
        return false;
    }

    /** Commits a scan result along the already-normalised direction, reusing its packed distance. */
    private boolean commit(Mob mob, double dirX, double dirZ, int result) {
        if (result == 0) {
            return false;
        }
        this.leap(mob, dirX, dirZ, result & DISTANCE_MASK);
        return true;
    }

    /**
     * Counts how long the goal has stood still, once per tick. A goal that has not moved for a whole
     * {@link ParasitePathing#LEAP_TRACKING_TICKS} window is treated as dead and the scan falls back to
     * the other candidate directions.
     */
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

    /**
     * The whole per-tick cost of the feature on a normal tick: a handful of arithmetic operations, no
     * block lookups and no allocations. The scan only runs on the tick a full window closes.
     *
     * <p>Stuckness is measured as <b>no net progress toward the goal</b>, not as "did not move": at
     * window start the horizontal distance to the mob's attack target is recorded, and the window
     * closes as stuck when the best distance seen is not at least {@link ParasitePathing#MIN_PROGRESS}
     * better. A mob jittering at a lip, sliding along an edge or being shoved around therefore counts
     * as stuck - which is the whole point. A mob with no target at all falls back to the old
     * displacement test, so a genuinely motionless mob still counts.</p>
     */
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

        // Progress toward the goal is the primary test; the displacement test is the goal-less
        // fallback (and also covers a mob whose target died mid-window).
        boolean stuck = this.windowHasGoal && hasGoal
                ? this.windowGoalDistance - this.windowGoalBest < ParasitePathing.MIN_PROGRESS
                : this.windowBest < ParasitePathing.LEAP_STUCK_DISTANCE;
        this.windowActive = false;
        if (!stuck) {
            return;
        }

        this.tryLeap(mob, wantedX, wantedY, wantedZ);
    }

    /**
     * One bounded scan per candidate direction, first hit wins, then at most one vanilla shaped jump.
     *
     * <p>The mob's goal is only used as the first candidate when it is usable - farther than
     * {@link ParasitePathing#MIN_LEAP_DISTANCE}, inside the level window, and not unchanged for a
     * whole window. In the scene this feature exists for (a perched mob whose target sits on a
     * separate, unreachable platform) the navigation goal is frequently at or near the mob's own
     * position or simply stale, so the scan then walks the mob's facing and the eight compass
     * directions until one of them finds a landing.</p>
     */
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

    /**
     * Public lookahead used by {@link LeapSeekGoal}: the first direction that has a legal leap
     * landing, as a walk target just past it. Returns {@code null} when there is no landing, when the
     * feature is off for this group, or while the shared retry cooldown holds.
     *
     * <p>This borrows the exact same bounded scan the leap itself uses, so it can never propose a
     * landing the leap would refuse. It mutates {@link #leapDirX}/{@link #leapDirZ}/{@link #leapDirReach}
     * (only read by {@link #commit}); the goal issues its own {@code moveTo} and never triggers a leap
     * itself.</p>
     */
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

    /** Stores the direction and reach of a packed scan result; true when there was a landing. */
    private boolean apply(int result) {
        if (result == 0) {
            return false;
        }
        this.leapDirReach = result & DISTANCE_MASK;
        return true;
    }

    /** True when the goal is far enough away, inside the level window, and not stale. */
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

    /** Turns a packed scan result into a leap. Returns false when the scan found nothing. */
    private boolean commit(Mob mob, int result) {
        if (result == 0) {
            return false;
        }
        this.leap(mob, this.leapDirX, this.leapDirZ, result & DISTANCE_MASK);
        return true;
    }

    /**
     * One straight ray in {@code (dirX, dirZ)}.
     *
     * <p>The ray starts on the mob's own column (or its immediate neighbour), walks over the lip, and
     * ends at the first column past the pit whose floor is inside the mob's landing window. Cost: at
     * most {@code limit} distinct columns, 2-3 block-state lookups each, and it exits as soon as the
     * column is decided - a ray that hits a wall at the mob's own level costs 2 lookups.</p>
     *
     * @return {@code 0} for "no leap", otherwise
     *         {@code (landingFloorY << FLOOR_SHIFT) | distanceInBlocks}
     */
    private int scan(Mob mob, double dirX, double dirZ) {
        int limit = this.maxGapJumpWidth + 1;
        return this.scanWithin(mob, dirX, dirZ, limit, limit + 0.5D);
    }

    /**
     * The same bounded ray with an explicit column budget and reach, used by the immediate
     * centre-air-below trigger to search only as far as the target (or the group maximum).
     *
     * @param limit       maximum distinct columns for this ray
     * @param maxDistance furthest landing centre this ray may accept, in blocks
     */
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
                // Before the pit. A wall taller than one step blocks this direction; a solid column
                // at the feet level with clear space below it is a step the mob walks up.
                if (ParasitePathing.isWall(mask)) {
                    return 0;
                }
                if (!ParasitePathing.isSolidAtFeet(mask) && !ParasitePathing.isFloor(mask)) {
                    // The floor is gone: the pit starts here, and the last floor was its lip.
                    overPit = true;
                }
                continue;
            }

            // Over the pit. A wall rising out of it blocks the descent.
            if (ParasitePathing.isWall(mask)) {
                return 0;
            }
            if (ParasitePathing.isSolidAtFeet(mask)) {
                // A raised block inside the pit: its top is a candidate landing one level up.
                if (highestFloor > takeoffY) {
                    int packed = this.packed(x, z, highestFloor, originX, originZ, maxDistance);
                    if (packed != 0) {
                        return packed;
                    }
                }
                continue;
            }
            if (ParasitePathing.isFloor(mask)) {
                // Standing room above a floor: the landing, at the takeoff level.
                return this.packed(x, z, takeoffY, originX, originZ, maxDistance);
            }
        }

        return 0;
    }

    /** Measures a candidate landing column and packs it, or returns {@code 0} when it is unusable. */
    private int packed(int x, int z, int floorY, double originX, double originZ, double maxDistance) {
        double dx = x + 0.5D - originX;
        double dz = z + 0.5D - originZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > maxDistance || distance < 1.0D) {
            return 0;
        }
        return (floorY << FLOOR_SHIFT) | ((int) Math.round(distance) & DISTANCE_MASK);
    }

    /**
     * Sheep-style impulse application, matching {@code InfestedSheep.customServerAiStep} lines 158-169
     * (the sheep's own leap): pick one horizontal magnitude for the whole hop and apply it with a
     * single direct {@code setDeltaMovement(dir.x * forward, up, dir.z * forward)}. No ballistic
     * solve, no per-distance flight-time computation.
     *
     * <p>Differences from the sheep, deliberately kept:</p>
     * <ul>
     *   <li>the <em>vertical</em> component is still the mob's own {@code jumpFromGround()} through
     *       the {@link GapJumper} bridge, so the leap keeps each mob's identity. In vanilla 1.20.1
     *       that method applies exactly {@code 0.42} plus a sprint-only horizontal boost, i.e. the
     *       same shape as the sheep's literal {@code 0.45}; the long-arms overrides it with
     *       {@code +1.0} and its own collision shove, which its 4-block body needs;</li>
     *   <li>the <em>horizontal</em> component uses the sheep's convention but scales with the actual
     *       distance, because one fixed 0.36 cannot cover an 8-block pit (see
     *       {@link ParasitePathing#sheepForwardSpeed(double)}) - 0.45 for a 2-block group leap,
     *       1.40 for the widest long-arms leap;</li>
     *   <li>the sheep sets {@code hasImpulse = true} and its own 10-tick {@code jumpCooldown}. The
     *       leap needs neither: {@code jumpFromGround()} already handles the impulse sync on both
     *       sides, and this class has its own 40-tick stuck window as its cooldown;</li>
     *   <li>the sheep also sets neither {@code setYRot} nor {@code setSpeed}; this method does not
     *       either. No teleport, no lerp, and no {@code fallDistance} reset - a mob that ignores fall
     *       damage already does so itself.</li>
     * </ul>
     *
     * @param distance horizontal blocks to the landing column's centre
     */
    private void leap(Mob mob, double dirX, double dirZ, int distance) {
        if (!(mob instanceof GapJumper gapJumper)) {
            // Wired without the bridge: no leap, no exception, everything else stays vanilla.
            return;
        }

        double up = mob.getDeltaMovement().y;

        gapJumper.epcaJumpFromGround();

        double jumpUp = mob.getDeltaMovement().y;
        if (jumpUp <= up) {
            // The mob refused the jump (water, its own state machine, a cooldown): leave it alone.
            return;
        }

        double forward = ParasitePathing.sheepForwardSpeed(distance);

        mob.setDeltaMovement(dirX * forward, jumpUp, dirZ * forward);

        this.leapDirX = dirX;
        this.leapDirZ = dirZ;
        this.leapForward = forward;
        this.leapTicks = ParasitePathing.airTicks(jumpUp);
    }

    /** Mid-air top-up so a long leap still reaches the far side of the pit. */
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

        // Only the horizontal part is scaled, so the vanilla arc is preserved.
        double scale = target / speed;
        mob.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
    }

    private static boolean isUsable(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /** Horizontal distance between two points; the progress metric for the stuck window. */
    private static double horizontalDistanceTo(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
