package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Tuning constants and the pure helpers used by the parasite gap jump.
 *
 * <p>Every parasite still runs the <em>vanilla</em> pathfinder. Nothing in this class, and nothing
 * in {@link ParasiteGapJump}, runs during a path expansion: the leap is decided inside the move
 * control, once per mob per {@link #LEAP_TRACKING_TICKS} ticks at most, and the direction scan it
 * uses reads at most {@link #MAX_GAP_JUMP_RESHAPE_LONGARMS} + 1 distinct columns per direction with
 * two to three block-state lookups each.</p>
 *
 * <p>A gap of N blocks means N empty columns between the take-off column and the landing column,
 * so the landing column sits N + 1 blocks away from the take-off column.</p>
 */
public final class ParasitePathing {

    /**
     * Group 1: infested sheep and every humanoid infested parasite except the infested skeleton
     * (infested zombie, husk, drowned, villager, zombie villager, pillager, vindicator). Clears pits
     * up to 2 blocks wide.
     */
    public static final int MAX_GAP_JUMP_HUMANOID = 2;

    /**
     * Group 2: infested fox. Clears pits up to 4 blocks wide - its own width, between the humanoid 2
     * and the long-arms 8. The scan turns this into 5 columns per ray (see {@code ParasiteGapJump.scan}),
     * so the per-ray ceiling is 5 x 3 = 15 lookups.
     *
     * <p>Force check for this width: a 4-wide pit puts the landing centre ~5.5 blocks away, and
     * {@code sheepForwardSpeed(5.5) = 0.36 * 5.5 / 2.0 = 0.99}, comfortably under
     * {@link #MAX_LAUNCH_FORWARD} (1.40) and above the sheep floor, so no new force constant is needed.
     * Covered distance over the fox's own airtime is {@code 0.99 * (1 - 0.91^ticks) / 0.09}: ~7.0 blocks
     * at the vanilla 11-tick hop, clearing the 5.5 needed with ~1.5 blocks of margin.</p>
     */
    public static final int MAX_GAP_JUMP_FOX = 4;

    /**
     * Group 3: reshape long-arms. Clears pits up to 8 blocks wide (raised from 7).
     *
     * <p>Every consumer derives from this value, so the widening propagates by itself:</p>
     * <ul>
     *   <li>{@code LongarmsGroundNavigation.maxGapJumpWidth()} returns it, which is what the
     *       navigation's evaluator and the mob's {@code ParasiteMoveControl} are constructed with;</li>
     *   <li>{@code ParasiteGapJump.scan} uses {@code maxGapJumpWidth + 1} = <b>9</b> distinct columns
     *       per ray, and its {@code maxDistance = limit + 0.5} becomes 9.5 blocks - exactly what an
     *       8-wide pit needs (pit columns 1..8, landing column 9);</li>
     *   <li>{@code LeapSeekGoal} is constructed with it as well.</li>
     * </ul>
     * There is no hard-coded 7 anywhere, no other offset, and no other read site.
     */
    public static final int MAX_GAP_JUMP_RESHAPE_LONGARMS = 8;

    /**
     * Group 3: infested enderman (vanilla gap handling) and the infested skeleton (no gap jump at
     * all), plus the "feature off" value for the whole chain. Keep at 0.
     */
    public static final int MAX_GAP_JUMP_NONE = 0;

    /**
     * Ticks between two leap checks for one mob instance, and therefore the cooldown window between
     * two leaps. The mob must be effectively stuck for the whole window before the scan runs, so a
     * walking mob never pays for a single block lookup.
     */
    public static final int LEAP_TRACKING_TICKS = 40;

    /**
     * Horizontal travel below this over one whole window counts as "stuck" <b>when the mob has no goal
     * to measure progress against</b> (no attack target). A vanilla walking parasite covers roughly
     * four blocks in that time, so this is deliberately far below that.
     *
     * <p>It is kept as the secondary, goal-less fallback only. Jittering at a lip, sliding along a
     * wall or being pushed around all move more than this, which is why the primary test is
     * {@link #MIN_PROGRESS}.</p>
     */
    public static final double LEAP_STUCK_DISTANCE = 0.35D;

    /**
     * Minimum closing of the horizontal distance to the mob's goal over one window for the mob to
     * count as making progress. If the best distance seen during the window is not at least this much
     * smaller than the distance at window start, the mob is stuck and a leap attempt is allowed.
     *
     * <p>This replaces "did not move" as the primary test: a mob pressed against the lip, jittering,
     * sliding along the edge or nudged by other mobs makes no progress toward its goal and is exactly
     * the mob that should leap.</p>
     */
    public static final double MIN_PROGRESS = 0.5D;

    /**
     * Vertical-velocity tolerance for the "not falling" placement gate: the leap is allowed while
     * {@code deltaMovement.y > -FALL_EPSILON}. A mob resting on ground has its gravity cancelled by
     * collision and its vertical velocity jitters around zero, so a small negative band must be
     * tolerated; a real fall exceeds it within a tick because vanilla gravity alone is 0.08/tick.
     */
    public static final double FALL_EPSILON = 0.01D;

    /**
     * Minimum distance at which the mob's own goal is used as the first scan direction. Closer than
     * this the goal is the mob's own next footstep (or the mob itself), so the scan falls back to the
     * mob's facing and then to the eight compass directions.
     *
     * <p>This is <b>not</b> a leap-distance limit: whether a leap happens, and how far, is decided by
     * the scan, whose length is this mob group's width + 1 columns (9 for the long-arms' 8-block
     * pits).</p>
     */
    public static final double MIN_LEAP_DISTANCE = 1.0D;

    /**
     * Deepest drop a leap may end on, relative to the mob's own feet level. A landing column whose
     * floor is between {@code ownY - MAX_LEAP_DROP + 1} and {@code ownY + MAX_LEAP_RISE} counts as a
     * landing; anything deeper is not a leap, it is a wall the mob cannot clear.
     *
     * <p>3 blocks is the safe vanilla threshold (no fall damage) and is what lets a perched mob come
     * down the staircase / platform chain in play. Reshape long-arms already ignores fall damage
     * entirely; for the 2-block group the drop still has to be within the same ray.</p>
     */
    public static final int MAX_LEAP_DROP = 3;

    /** Highest step-up a leap may end on, relative to the mob's own feet level. */
    public static final int MAX_LEAP_RISE = 1;

    /**
     * Directions the scan tries when the mob's goal is unusable (at/near its own position, at an
     * implausible level, unreachable, or stale). The mob's facing is tried first, then these, in
     * order, stopping at the first direction that finds a landing.
     */
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

    /** Number of compass fallback directions in {@link #COMPASS_DIRECTIONS}. */
    public static final int COMPASS_COUNT = COMPASS_DIRECTIONS.length;

    /** Compass fallback direction X, normalised. */
    public static double compassX(int index) {
        return COMPASS_DIRECTIONS[index][0];
    }

    /** Compass fallback direction Z, normalised. */
    public static double compassZ(int index) {
        return COMPASS_DIRECTIONS[index][1];
    }

    /**
     * Forward speed of the infested sheep's own leap, taken verbatim from
     * {@code InfestedSheep.customServerAiStep} (line 161): it sets
     * {@code setDeltaMovement(dir.x * 0.36, 0.45, dir.z * 0.36)} plus {@code hasImpulse = true} for a
     * 1-2 block hop. The gap jump reuses this convention: one fixed forward speed for a plain hop,
     * scaled only when the landing is farther away than the sheep's hop can reach.
     */
    public static final double SHEEP_FORWARD_SPEED = 0.36D;

    /** Horizontal reach the sheep's own 0.36 forward speed is tuned for, in blocks. */
    public static final double SHEEP_FORWARD_REACH = 2.0D;

    /**
     * Upper bound on the scaled forward speed. Raised from 1.15 to <b>1.40</b> for the 8-block pit.
     *
     * <p>Arithmetic (all per tick, vanilla units):</p>
     * <ul>
     *   <li>an 8-wide pit puts the landing column's centre ~9.5 blocks away, and an 8-block group-1
     *       hop sits at ~2.5;</li>
     *   <li>{@code sheepForwardSpeed(9.5)} wants 0.36 * 9.5 / 2.0 = 1.71, so any cap below that is
     *       the real limiter. At the old 1.15 the covered distance was
     *       {@code 1.15 * (1 - 0.91^airtime) / 0.09};</li>
     *   <li>the long-arms' own {@code jumpFromGround()} forces {@code deltaMovement.y = 1.0}, so
     *       {@code airTicks(1.0)} = 25 ticks (and 27 when the landing is a full
     *       {@link #MAX_LEAP_DROP} below), giving {@code 1.15 * 10.54 = 12.1} blocks of ideal
     *       coverage - which looks sufficient only because that ignores that the arc is still high
     *       when it must land, drag is not the only loss, and the mob is 2 blocks wide at take-off;</li>
     *   <li>at <b>1.40</b> the same arc covers {@code 1.40 * 10.54 = 14.8} blocks ideal, and
     *       realistically {@code 1.40 * 9.15 = 12.8} blocks over 25 ticks (drag 0.91, keeping the
     *       2-block body clear of the far lip). That leaves ~3 blocks of margin over the 9.5 needed,
     *       enough that a half-block of steering loss or a late landing still clears;</li>
     *   <li>the lower clamp is unchanged, so group 1 (a 2-block pit needs 0.45) never sees this cap:
     *       {@code sheepForwardSpeed(2.5) = 0.45}.</li>
     * </ul>
     *
     * <p>The cap only binds for the long-arms and only past ~7.8 blocks of required travel, so the
     * sheep and humanoid groups are untouched.</p>
     */
    public static final double MAX_LAUNCH_FORWARD = 1.40D;

    private ParasitePathing() {
    }

    /**
     * Column description used by the hole scan in {@link ParasiteGapJump}, for a take-off feet level
     * {@code y}: bit 0 "solid at {@code y}" (a wall, or a step), bit 1 "solid at {@code y - 1}" (a
     * floor), bit 2 "air at {@code y} and {@code y + 1}" (the mob's body could stand there).
     * {@code -1} means outside the build height and is treated as solid.
     */
    private static final int SOLID_AT_FEET = 1;
    private static final int SOLID_BELOW = 2;
    private static final int WALKABLE = 4;

    /**
     * Classifies one column of a leap ray. Position-dependent, which is what makes the relative
     * descent work: the same block is a floor for a mob one level higher and a step for a mob level
     * with it.
     *
     * <p>Cost: 2 {@code getBlockState} calls (3 only when the feet level is solid), or 0 outside the
     * build height.</p>
     */
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

    /** True when the feet level has a solid floor. */
    public static boolean isSolidAtFeet(int mask) {
        return mask < 0 || (mask & SOLID_AT_FEET) != 0;
    }

    /** True when the mob could stand in this column (air at feet and head). */
    public static boolean isWalkable(int mask) {
        return mask >= 0 && (mask & WALKABLE) != 0;
    }

    /** True when the column has a floor under the feet level, so it can be stood on. */
    public static boolean isFloor(int mask) {
        return mask >= 0 && (mask & SOLID_BELOW) != 0;
    }

    /** True when solids sit both at and below the feet level, i.e. a wall taller than one step. */
    public static boolean isWall(int mask) {
        return isSolidAtFeet(mask) && isFloor(mask);
    }

    /** Single block-state read, treating out-of-world as solid. */
    private static boolean solidAt(Level level, int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) {
            return true;
        }
        return !level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    /**
     * Sheep-style forward speed for a leap that has to cover {@code distance} blocks of horizontal
     * travel.
     *
     * <p>The sheep uses one fixed magnitude ({@link #SHEEP_FORWARD_SPEED}) for a
     * {@link #SHEEP_FORWARD_REACH}-block hop, which cannot cover a wide pit at all: the same impulse
     * would fall four blocks short of an 8-wide one. The gap jump therefore keeps the sheep's
     * convention (a plain proportional forward impulse, no ballistic solve) and scales it linearly
     * with the required distance:
     * {@code forward = clamp(SHEEP_FORWARD_SPEED * distance / SHEEP_FORWARD_REACH, SHEEP_FORWARD_SPEED, MAX_LAUNCH_FORWARD)}.
     * </p>
     *
     * <p>Resulting speeds: 1.5 blocks (1-wide pit) -> 0.36 (lower clamp, the sheep's own magnitude);
     * 2.5 blocks (a 2-block pit, group 1) -> 0.45; 9.5 blocks (an 8-block long-arms pit) -> 1.40
     * (upper clamp; the raw formula wants 1.71). The lower clamp means the horizontal impulse is never
     * weaker than the sheep's own, so the smallest leap still reads exactly like a sheep hop.</p>
     */
    public static double sheepForwardSpeed(double distance) {
        double scaled = SHEEP_FORWARD_SPEED * distance / SHEEP_FORWARD_REACH;
        if (scaled < SHEEP_FORWARD_SPEED) {
            return SHEEP_FORWARD_SPEED;
        }
        return scaled > MAX_LAUNCH_FORWARD ? MAX_LAUNCH_FORWARD : scaled;
    }

    /** Vanilla {@code LivingEntity} gravity per tick, used only to size the air time. */
    private static final double GRAVITY = 0.08D;

    /**
     * Ticks the leap spends in the air, from the vertical impulse alone:
     * {@code ceil(2 * up / gravity)}, i.e. a projectile that returns to its launch level. Vanilla
     * 1.20.1 {@code jumpFromGround} applies 0.42, so 11 ticks; the reshape long-arms overrides it with
     * {@code +1.0}, so <b>25 ticks</b> - which is what makes an 8-block leap possible at all, because
     * the horizontal distance covered is {@code forward * (1 - 0.91^ticks) / 0.09} and 25 ticks of
     * 1.40 covers ~12.8 blocks against the 9.5 an 8-wide pit needs. Used to bound the mid-air
     * horizontal top-up; the impulse itself no longer solves for a flight time.
     */
    public static int airTicks(double up) {
        int ticks = (int) Math.ceil(2.0D * Math.max(0.0D, up) / GRAVITY);
        if (ticks < 10) {
            return 10;
        }
        return ticks > 40 ? 40 : ticks;
    }

    /**
     * True when the mob may consider a leap this tick: the feature is on for its group, it is not
     * swimming, and it is <b>not falling</b>.
     *
     * <p>User requirement: "只要不处于下落状态就可以跳跃" - support, ground contact and being pressed
     * against a block <b>no longer gate the leap at all</b>. The only remaining placement gate is that
     * the mob is not in a falling state. The signal used is {@link #FALL_EPSILON}: the mob may leap
     * while its vertical velocity is not downward ({@code deltaMovement.y > -FALL_EPSILON}), i.e. while
     * resting, rising or moving level. There is deliberately <b>no</b> {@code onGround()}, no
     * {@code horizontalCollision}, no {@code verticalCollisionBelow} and no block read.</p>
     *
     * <p><b>Why the epsilon and not {@code onGround()}:</b> velocity is the state the user described
     * ("下落状态"), it is already updated by the time the move control ticks, it costs nothing, and it
     * needs no collision flags or world access. {@code FALL_EPSILON} exists so that a mob resting on
     * the ground - whose gravity is cancelled by collision and whose vertical velocity jitters around
     * zero from -0.08 upward - is never mistaken for a falling mob; a genuine fall accelerates well
     * past it within a tick (vanilla gravity alone is 0.08/tick).</p>
     *
     * <p>Water and a width of 0 still reject. Airborne-but-not-falling mobs (mid-hop, knocked upward,
     * or on the first tick after walking off a ledge) are now allowed by design; the one-attempt-per-
     * window throttle in {@link ParasiteGapJump} is what keeps that from repeating.</p>
     */
    public static boolean canLeap(Mob mob, int maxGapJumpWidth) {
        if (maxGapJumpWidth <= 0 || mob.isInWater()) {
            return false;
        }
        return mob.getDeltaMovement().y > -FALL_EPSILON;
    }
}
