package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * One dark-red micro rectangle ("speck") of the contaminated water effect.
 *
 * <p>A speck is a single flat, hard-edged, camera-facing quad drawn by
 * {@link GasCloudRenderer#submitSpecs}. It has no texture, no radial falloff and no noise mask:
 * the fragment shader selects the solid style through
 * {@link GasCloudRenderType#SPEC_STYLE_CHANNEL}, and the dark red travels in the vertex colour.</p>
 *
 * <p>There are two kinds, as asked for by the spec:</p>
 * <ul>
 *   <li>{@link Kind#SURFACE}: the y is locked to the top of a water block that has no water above
 *       it, so the speck floats on the water surface. It only drifts horizontally
 *       ({@link #WATER_SPEC_DRIFT_SPEED} blocks per tick, direction re-randomised every
 *       {@link #WATER_SPEC_DIRECTION_MIN_TICKS}..{@link #WATER_SPEC_DIRECTION_MAX_TICKS} ticks)
 *       and adds a tiny vertical sine bob of +/-{@link #WATER_SPEC_SURFACE_BOB_AMPLITUDE} blocks
 *       that is applied at draw time, so the locked y is never violated.</li>
 *   <li>{@link Kind#SUSPENDED}: at a water position that has water above it, drifting slowly on all
 *       three axes (each component within +/-{@link #WATER_SPEC_DRIFT_SPEED} blocks per tick,
 *       direction re-randomised on the same cadence).</li>
 * </ul>
 *
 * <p>This class is pure client-side visual state: it never talks to the server and holds no
 * reference to the owning entity beyond the entity id used for bookkeeping. It is version
 * independent (only {@code Mth}, {@code RandomSource} and {@code Vec3}), so the 1.20.1 twin and
 * this tree can share one source of truth for the speck constants and the motion curves.</p>
 */
public final class WaterSpec {

    /** Which of the two speck kinds this instance is. */
    public enum Kind {
        /** Locked to the water surface: horizontal drift plus a tiny vertical bob. */
        SURFACE,
        /** Suspended inside the water: slow drift on all three axes. */
        SUSPENDED
    }

    /** Half extent (blocks) of a speck quad, lower bound. Spec: 0.03. */
    public static final float WATER_SPEC_HALF_EXTENT_MIN = 0.03F;
    /** Half extent (blocks) of a speck quad, upper bound. Spec: 0.09. */
    public static final float WATER_SPEC_HALF_EXTENT_MAX = 0.09F;

    /** Start alpha of a speck, lower bound. Spec: 0.45. */
    public static final float WATER_SPEC_ALPHA_MIN = 0.45F;
    /** Start alpha of a speck, upper bound. Spec: 0.75. */
    public static final float WATER_SPEC_ALPHA_MAX = 0.75F;

    /**
     * Dark red speck colour, carried as the vertex colour. The fragment shader multiplies it by
     * {@code GAS_TINT} (1.00, 0.33, 0.34), so the finished colour is a deep dark red.
     */
    public static final float WATER_SPEC_COLOR_RED = 0.42F;
    public static final float WATER_SPEC_COLOR_GREEN = 0.06F;
    public static final float WATER_SPEC_COLOR_BLUE = 0.06F;

    /**
     * Fraction of the lifetime used to fade a speck in at the start and out at the end (spec:
     * a quick fade over the first/last 20% of its life).
     */
    public static final float WATER_SPEC_FADE_FRACTION = 0.20F;

    /** Maximum drift speed per axis, in blocks per tick. Spec: +/-0.01. */
    public static final double WATER_SPEC_DRIFT_SPEED = 0.01D;

    /** Shortest interval between two drift direction re-randomisations, in ticks. Spec: ~20. */
    public static final int WATER_SPEC_DIRECTION_MIN_TICKS = 20;
    /** Longest interval between two drift direction re-randomisations, in ticks. Spec: ~40. */
    public static final int WATER_SPEC_DIRECTION_MAX_TICKS = 40;

    /** Amplitude of the surface bob, in blocks. Spec: +/-0.03. */
    public static final float WATER_SPEC_SURFACE_BOB_AMPLITUDE = 0.03F;
    /** Period of the surface bob, in ticks. Slow enough to read as "floating", not as jitter. */
    public static final float WATER_SPEC_SURFACE_BOB_PERIOD_TICKS = 60.0F;

    /**
     * Small lift above the fluid surface so a surface speck cannot z-fight with the water plane it
     * is locked to. The surface position is built as
     * {@code blockY + FluidState#getOwnHeight() + WATER_SPEC_SURFACE_LIFT} and only the sine bob
     * ever moves it afterwards. Same value as the 1.20.1 twin.
     */
    public static final double WATER_SPEC_SURFACE_LIFT = 0.03D;

    private static final double BOB_ANGULAR_SPEED =
            2.0D * Math.PI / (double) WATER_SPEC_SURFACE_BOB_PERIOD_TICKS;

    private final int ownerEntityId;
    private final Kind kind;
    private final RandomSource random;
    private final int lifetime;
    private final float halfExtent;
    private final float startAlpha;
    private final float bobPhase;

    /** Tick position (the partial-tick offset is applied in {@link #getPosition(float)}). */
    private Vec3 position;
    /** Current drift velocity in blocks per tick; y is always 0 for {@link Kind#SURFACE}. */
    private Vec3 drift;
    /** Ticks left before the drift direction is re-randomised. */
    private int directionTicksLeft;

    private int age;

    public WaterSpec(int ownerEntityId, Kind kind, Vec3 position, Vec3 drift, int lifetime,
                     float halfExtent, float startAlpha, float bobPhase, RandomSource random) {
        this.ownerEntityId = ownerEntityId;
        this.kind = kind;
        this.position = position;
        this.drift = drift;
        this.lifetime = Math.max(1, lifetime);
        this.halfExtent = halfExtent;
        this.startAlpha = startAlpha;
        this.bobPhase = bobPhase;
        this.random = random;
        this.directionTicksLeft = randomDirectionTicks(random);
    }

    // =================================================================
    //  Random motion helpers
    // =================================================================

    /** One random component in {@code [-speed, +speed]}. */
    private static double randomComponent(RandomSource random, double speed) {
        return (random.nextDouble() * 2.0D - 1.0D) * speed;
    }

    /** Horizontal-only drift for a surface speck (y stays 0 so the lock is never broken). */
    public static Vec3 randomSurfaceDrift(RandomSource random) {
        return new Vec3(randomComponent(random, WATER_SPEC_DRIFT_SPEED), 0.0D,
                randomComponent(random, WATER_SPEC_DRIFT_SPEED));
    }

    /** Slow three-axis drift for a suspended speck. */
    public static Vec3 randomSuspendedDrift(RandomSource random) {
        return new Vec3(randomComponent(random, WATER_SPEC_DRIFT_SPEED),
                randomComponent(random, WATER_SPEC_DRIFT_SPEED),
                randomComponent(random, WATER_SPEC_DRIFT_SPEED));
    }

    /** Ticks until the next direction re-randomisation. */
    public static int randomDirectionTicks(RandomSource random) {
        return WATER_SPEC_DIRECTION_MIN_TICKS
                + random.nextInt(WATER_SPEC_DIRECTION_MAX_TICKS - WATER_SPEC_DIRECTION_MIN_TICKS + 1);
    }

    // =================================================================
    //  Lifetime
    // =================================================================

    /** Advance one client tick. Returns false once the speck is finished and must be removed. */
    public boolean tick() {
        this.age++;
        if (this.kind == Kind.SURFACE) {
            // The y is locked to the water surface, so only x/z may move.
            this.position = this.position.add(this.drift.x, 0.0D, this.drift.z);
            if (--this.directionTicksLeft <= 0) {
                this.directionTicksLeft = randomDirectionTicks(this.random);
                this.drift = randomSurfaceDrift(this.random);
            }
        } else {
            this.position = this.position.add(this.drift);
            if (--this.directionTicksLeft <= 0) {
                this.directionTicksLeft = randomDirectionTicks(this.random);
                this.drift = randomSuspendedDrift(this.random);
            }
        }
        return this.age < this.lifetime;
    }

    /** Normalised age in {@code [0, 1]}. */
    public float getAgeRatio(float partialTick) {
        if (this.lifetime <= 0) {
            return 1.0F;
        }
        return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    /**
     * Quick fade in over the first {@link #WATER_SPEC_FADE_FRACTION} of the lifetime and out over
     * the last {@link #WATER_SPEC_FADE_FRACTION}, so a speck pops in and out instead of growing.
     */
    public float getAlpha(float partialTick) {
        float ratio = getAgeRatio(partialTick);
        float fadeIn = Mth.clamp(ratio / WATER_SPEC_FADE_FRACTION, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - ratio) / WATER_SPEC_FADE_FRACTION, 0.0F, 1.0F);
        return this.startAlpha * Math.min(fadeIn, fadeOut);
    }

    /**
     * Current world position: the tick position plus the partial-tick drift. A surface speck also
     * gets its vertical sine bob here, so its stored y stays exactly on the fluid surface.
     */
    public Vec3 getPosition(float partialTick) {
        double x = this.position.x + this.drift.x * partialTick;
        double z = this.position.z + this.drift.z * partialTick;
        if (this.kind == Kind.SURFACE) {
            double bob = WATER_SPEC_SURFACE_BOB_AMPLITUDE
                    * Math.sin((this.age + partialTick) * BOB_ANGULAR_SPEED + this.bobPhase);
            return new Vec3(x, this.position.y + bob, z);
        }
        return new Vec3(x, this.position.y + this.drift.y * partialTick, z);
    }

    // =================================================================
    //  Accessors
    // =================================================================

    public int getOwnerEntityId() {
        return this.ownerEntityId;
    }

    public Kind getKind() {
        return this.kind;
    }

    public float getHalfExtent() {
        return this.halfExtent;
    }

    public int getAge() {
        return this.age;
    }

    public int getLifetime() {
        return this.lifetime;
    }
}
