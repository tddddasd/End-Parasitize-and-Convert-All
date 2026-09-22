package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One dark-red micro rectangle ("speck") drawn for a water-touching {@code epca:contaminated_water}
 * entity.
 *
 * <p>A speck is the second cloud style of the gas cloud pipeline: the exact same camera-relative
 * billboard frame and the exact same {@link GasCloudRenderType} as {@link GasCloud}, but only one
 * quad per speck and with the sub-quad channel set to
 * {@link GasCloudRenderType#SPEC_STYLE_CHANNEL} so {@code gas_cloud.fsh} skips the texture, the
 * radial falloff and the noise mask and emits a hard-edged rectangle.</p>
 *
 * <p>Pure client-side visual state: it never talks to the server and holds no reference to its
 * owning entity. All of its movement state (accumulated drift plus the current velocity) lives here,
 * so the manager only has to tick it and the renderer only has to read it.</p>
 *
 * <h2>Two kinds</h2>
 * <ul>
 *   <li>{@link Kind#SURFACE} - floats on the water surface: its Y is locked to the top of the water
 *       block it spawned on, it only drifts horizontally and it bobs up and down on a slow sine.</li>
 *   <li>{@link Kind#SUSPENDED} - hangs in the water: it drifts slowly on all three axes and
 *       re-randomises its drift direction every 20-40 ticks.</li>
 * </ul>
 */
public final class WaterSpec {

    /** The two kinds of speck the spec asks for, roughly half of the population each. */
    public enum Kind {
        /** Floats on the top face of a water block. */
        SURFACE,
        /** Hangs inside water that still has water above it. */
        SUSPENDED
    }

    // =================================================================
    //  Spawn ranges (spec)
    // =================================================================

    /** Spec: half extent 0.03-0.09 blocks. */
    public static final float MIN_HALF_EXTENT = 0.03F;
    public static final float MAX_HALF_EXTENT = 0.09F;

    /** Spec: alpha 0.45-0.75, random per speck. */
    public static final float MIN_ALPHA = 0.45F;
    public static final float MAX_ALPHA = 0.75F;

    /** Spec: random lifetime 30-80 ticks. */
    public static final int MIN_LIFETIME = 30;
    public static final int MAX_LIFETIME = 80;

    /**
     * Spec: quick fade in over the first 20% of the life and quick fade out over the last 20%.
     * The alpha carried by {@code Color.a} is already {@code startAlpha * fade}, so the shader value
     * stays a plain per-quad fade and the shared renderer needs no new shader constant for it.
     */
    public static final float FADE_FRACTION = 0.2F;

    /** Spec: dark red delivered through the vertex colour (WATER_SPEC_COLOR). */
    public static final float COLOR_RED = 0.42F;
    public static final float COLOR_GREEN = 0.06F;
    public static final float COLOR_BLUE = 0.06F;

    /** Spec: drift is +/- 0.01 blocks/tick (horizontal for SURFACE, all three axes for SUSPENDED). */
    public static final double DRIFT_SPEED = 0.01D;
    /** Spec: SURFACE gets a tiny vertical sine bob of +/- 0.03 blocks. */
    public static final double SURFACE_BOB_AMPLITUDE = 0.03D;
    /** Bob period in ticks; the phase is random per speck. */
    public static final int SURFACE_BOB_PERIOD_TICKS = 60;

    /** Spec: SUSPENDED re-randomises its drift direction every ~20-40 ticks. */
    public static final int SUSPENDED_REDIRECT_MIN_TICKS = 20;
    public static final int SUSPENDED_REDIRECT_MAX_TICKS = 40;

    /**
     * Small lift above the fluid surface. A SURFACE speck is locked to the top face of its water
     * block and this lift places it just above the water plane, so it reads as floating on the
     * surface instead of being coplanar with it (a coplanar quad z-fights with the water plane).
     * Applied by {@link org.tdddd.epca.impl.client.entity.gas.GasCloudManager} when it builds the
     * surface position list. Mirrors the 26.1.2 twin's constant of the same name.
     */
    public static final double WATER_SPEC_SURFACE_LIFT = 0.03D;

    private static final Vec3 ZERO_DRIFT = Vec3.ZERO;

    private final Kind kind;
    private final Vec3 position;
    private final int lifetime;
    private final float halfExtent;
    private final float startAlpha;
    private final float bobPhase;

    /** Accumulated drift from {@link #position}; a SURFACE speck only ever changes x/z here. */
    private Vec3 drift;
    /** Current velocity in blocks per tick. */
    private Vec3 velocity;
    /** Tick at which a SUSPENDED speck draws a new random velocity. */
    private int redirectAtTick;

    private int age;

    private WaterSpec(Kind kind, Vec3 position, int lifetime, float halfExtent, float startAlpha,
                      Vec3 velocity, float bobPhase) {
        this.kind = kind;
        this.position = position;
        this.lifetime = Math.max(1, lifetime);
        this.halfExtent = halfExtent;
        this.startAlpha = startAlpha;
        this.bobPhase = bobPhase;
        this.drift = ZERO_DRIFT;
        this.velocity = velocity;
    }

    /**
     * Creates a speck at a random water position of one water entity.
     *
     * @param surfacePositions   top faces of the water blocks inside the entity volume
     * @param suspendedPositions water blocks inside the entity volume that still have water above
     * @param random             the owner entity's random source
     * @return the new speck, or {@code null} when the requested kind has no valid position
     */
    public static WaterSpec spawn(List<Vec3> surfacePositions, List<Vec3> suspendedPositions,
                                  RandomSource random) {
        // Roughly half of the population on the surface, half suspended: pick the kind first and
        // fall back to the other list when the requested one is empty (e.g. a fully submerged box).
        boolean wantSurface = random.nextBoolean();
        Kind kind = wantSurface ? Kind.SURFACE : Kind.SUSPENDED;
        List<Vec3> positions = wantSurface ? surfacePositions : suspendedPositions;
        if (positions.isEmpty()) {
            kind = wantSurface ? Kind.SUSPENDED : Kind.SURFACE;
            positions = wantSurface ? suspendedPositions : surfacePositions;
        }
        if (positions.isEmpty()) {
            return null;
        }

        Vec3 base = positions.get(random.nextInt(positions.size()));
        Vec3 origin;
        Vec3 velocity;
        if (kind == Kind.SURFACE) {
            // Y is already lifted above the water top face by the manager (see
            // WATER_SPEC_SURFACE_LIFT), and only the horizontal drift is random.
            origin = base;
            velocity = new Vec3(randomSignedSpeed(random), 0.0D, randomSignedSpeed(random));
        } else {
            origin = base;
            velocity = new Vec3(randomSignedSpeed(random), randomSignedSpeed(random), randomSignedSpeed(random));
        }

        int lifetime = MIN_LIFETIME + random.nextInt(MAX_LIFETIME - MIN_LIFETIME + 1);
        float halfExtent = MIN_HALF_EXTENT + random.nextFloat() * (MAX_HALF_EXTENT - MIN_HALF_EXTENT);
        float alpha = MIN_ALPHA + random.nextFloat() * (MAX_ALPHA - MIN_ALPHA);
        WaterSpec speck = new WaterSpec(kind, origin, lifetime, halfExtent, alpha, velocity,
                random.nextFloat() * 360.0F);
        speck.redirectAtTick = randomRedirectDelay(random);
        return speck;
    }

    /** One velocity component: a random value in {@code [-DRIFT_SPEED, +DRIFT_SPEED]}. */
    private static double randomSignedSpeed(RandomSource random) {
        return (random.nextDouble() * 2.0D - 1.0D) * DRIFT_SPEED;
    }

    /** Ticks until the next drift re-randomisation, drawn from the spec's 20-40 tick window. */
    public static int randomRedirectDelay(RandomSource random) {
        return SUSPENDED_REDIRECT_MIN_TICKS
                + random.nextInt(SUSPENDED_REDIRECT_MAX_TICKS - SUSPENDED_REDIRECT_MIN_TICKS + 1);
    }

    /**
     * Advances the speck by one client tick.
     *
     * <p>A SURFACE speck moves only horizontally (its drift Y stays 0) and gets a new horizontal
     * direction every 20-40 ticks; a SUSPENDED speck moves on all three axes and re-randomises the
     * whole velocity when its redirect window expires. Both stay within the spec's
     * +/- 0.01 blocks/tick.</p>
     *
     * @return false once the speck is finished and must be removed
     */
    public boolean tick(RandomSource random) {
        this.age++;
        if (this.kind == Kind.SURFACE) {
            if (this.age >= this.redirectAtTick) {
                this.velocity = new Vec3(randomSignedSpeed(random), 0.0D, randomSignedSpeed(random));
                this.redirectAtTick = this.age + randomRedirectDelay(random);
            }
        } else if (this.age >= this.redirectAtTick) {
            this.velocity = new Vec3(randomSignedSpeed(random), randomSignedSpeed(random),
                    randomSignedSpeed(random));
            this.redirectAtTick = this.age + randomRedirectDelay(random);
        }
        this.drift = this.drift.add(this.velocity);
        return this.age < this.lifetime;
    }

    // =================================================================
    //  Queries used by the shared renderer
    // =================================================================

    /** True for a speck that floats on the water surface instead of drifting freely. */
    public boolean isSurface() {
        return this.kind == Kind.SURFACE;
    }

    /** Normalised age in {@code [0, 1]}. */
    public float getAgeRatio(float partialTick) {
        if (this.lifetime <= 0) return 1.0F;
        return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    /**
     * Current alpha: {@link #startAlpha} faded in over the first {@link #FADE_FRACTION} of the life
     * and out over the last {@link #FADE_FRACTION}, so a speck appears and disappears quickly
     * instead of popping.
     */
    public float getAlpha(float partialTick) {
        float ratio = getAgeRatio(partialTick);
        float fadeIn = Mth.clamp(ratio / FADE_FRACTION, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - ratio) / FADE_FRACTION, 0.0F, 1.0F);
        return this.startAlpha * Math.min(fadeIn, fadeOut);
    }

    /**
     * Current world position: the spawn point plus the accumulated drift, plus the sine bob for a
     * SURFACE speck (whose Y therefore stays at the top face of its water block plus
     * {@link #WATER_SPEC_SURFACE_LIFT}, bobbing up and down around it).
     */
    public Vec3 getPosition(float partialTick) {
        double bob = 0.0D;
        if (this.kind == Kind.SURFACE) {
            double phase = Math.toRadians(this.bobPhase)
                    + (this.age + partialTick) * (2.0D * Math.PI / SURFACE_BOB_PERIOD_TICKS);
            bob = Math.sin(phase) * SURFACE_BOB_AMPLITUDE;
        }
        return new Vec3(this.position.x + this.drift.x,
                this.position.y + this.drift.y + bob,
                this.position.z + this.drift.z);
    }

    // =================================================================
    //  Accessors used by the manager and the render helper
    // =================================================================

    public Kind getKind() {
        return this.kind;
    }

    public int getAge() {
        return this.age;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public float getHalfExtent() {
        return this.halfExtent;
    }

    public float getColorRed() {
        return COLOR_RED;
    }

    public float getColorGreen() {
        return COLOR_GREEN;
    }

    public float getColorBlue() {
        return COLOR_BLUE;
    }
}
