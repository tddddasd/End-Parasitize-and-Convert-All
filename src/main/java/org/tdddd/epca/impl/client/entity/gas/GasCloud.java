package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * One world-anchored, shader-rendered red gas cloud.
 *
 * <p>A cloud is a short-lived cluster of billboard sub-quads. Every cloud generates its own
 * fixed random sub-quad layout at creation time (random offset, rotation, per-quad scale and
 * spin phase) so the silhouette is irregular and never repeats between two clouds. The layout
 * is generated once and stored, so the shape stays stable while the cloud lives instead of
 * flickering every frame.</p>
 *
 * <p>This class is pure client-side visual state: it never talks to the server and holds no
 * reference to the owning entity beyond the UUID used for bookkeeping.</p>
 */
public final class GasCloud {

    /** How many sub-quads a cloud may be made of (spec: 5-9). */
    public static final int MIN_SUB_QUADS = 5;
    public static final int MAX_SUB_QUADS = 9;

    /** Per-quad scale range (spec: 0.6-1.6). */
    private static final float SUB_QUAD_SCALE_MIN = 0.6F;
    private static final float SUB_QUAD_SCALE_MAX = 1.6F;

    /** Per-quad offset magnitude, as a fraction of the cloud scale. */
    private static final float SUB_QUAD_OFFSET_MIN = 0.05F;
    private static final float SUB_QUAD_OFFSET_MAX = 0.35F;

    /** How far a spreading cloud pushes its sub-quads outward by the end of its life. */
    private static final float SPREAD_RADIUS_START = 1.0F;
    private static final float SPREAD_RADIUS_END = 2.4F;

    /** Vertical squash applied to the random sub-quad offsets so the puff reads as a cloud. */
    private static final float SUB_QUAD_VERTICAL_BIAS = 0.75F;

    /** Colour of a sub-quad that does not override it (the red tint lives in the shader). */
    public static final float DEFAULT_QUAD_RED = 1.0F;
    public static final float DEFAULT_QUAD_GREEN = 1.0F;
    public static final float DEFAULT_QUAD_BLUE = 1.0F;

    /** Rotation applied to each sub-quad at the moment it is drawn. */
    public static final class SubQuad {
        /** Offset from the cloud centre, already divided by the cloud scale. */
        public final float offsetX;
        public final float offsetY;
        public final float offsetZ;
        /** Fixed random orientation (degrees). */
        public final float yaw;
        public final float pitch;
        public final float roll;
        /** Individual billboard scale multiplier (0.6-1.6). */
        public final float scale;
        /** Random phase for the slow per-quad rotation. */
        public final float spinPhase;
        /** Random speed multiplier for the slow per-quad rotation. */
        public final float spinSpeed;
        /**
         * The quad's own colour, written into {@code Color.rgb}. A gas sub-quad leaves this at
         * white so the shader's red tint is the only colour; a water speck overrides it with its
         * dark red so the very same shader can emit a differently coloured hard rectangle.
         */
        public final float colorR;
        public final float colorG;
        public final float colorB;

        private SubQuad(float offsetX, float offsetY, float offsetZ,
                        float yaw, float pitch, float roll,
                        float scale, float spinPhase, float spinSpeed) {
            this(offsetX, offsetY, offsetZ, yaw, pitch, roll, scale, spinPhase, spinSpeed,
                    DEFAULT_QUAD_RED, DEFAULT_QUAD_GREEN, DEFAULT_QUAD_BLUE);
        }

        private SubQuad(float offsetX, float offsetY, float offsetZ,
                        float yaw, float pitch, float roll,
                        float scale, float spinPhase, float spinSpeed,
                        float colorR, float colorG, float colorB) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.scale = scale;
            this.spinPhase = spinPhase;
            this.spinSpeed = spinSpeed;
            this.colorR = colorR;
            this.colorG = colorG;
            this.colorB = colorB;
        }
    }

    private final int ownerEntityId;
    private final Vec3 position;
    private final int lifetime;
    private final float baseScale;
    private final float targetScale;
    /** Fraction of the lifetime at which {@link #targetScale} is reached. */
    private final float growFraction;
    private final float startAlpha;
    /** World-space drift applied to the cloud centre over its whole life. */
    private final Vec3 drift;
    /** True for clouds that stay anchored at their spawn point (longarms jets). */
    private final boolean anchored;
    /** True for clouds that dissipate by spreading their sub-quads outward. */
    private final boolean spreading;
    /** Seed handed to the fragment shader so silhouettes differ between clouds. */
    private final float renderSeed;

    private final SubQuad[] subQuads;

    private int age;

    public GasCloud(int ownerEntityId, Vec3 position, int lifetime,
                    float baseScale, float targetScale, float growFraction,
                    float startAlpha, Vec3 drift, boolean anchored, boolean spreading,
                    RandomSource random) {
        this.ownerEntityId = ownerEntityId;
        this.position = position;
        this.lifetime = Math.max(1, lifetime);
        this.baseScale = baseScale;
        this.targetScale = targetScale;
        this.growFraction = Mth.clamp(growFraction, 0.05F, 1.0F);
        this.startAlpha = startAlpha;
        this.drift = drift;
        this.anchored = anchored;
        this.spreading = spreading;
        this.renderSeed = random.nextFloat() * 100.0F;

        int quadCount = MIN_SUB_QUADS + random.nextInt(MAX_SUB_QUADS - MIN_SUB_QUADS + 1);
        this.subQuads = new SubQuad[quadCount];
        for (int i = 0; i < quadCount; i++) {
            this.subQuads[i] = randomSubQuad(random);
        }
    }

    private static SubQuad randomSubQuad(RandomSource random) {
        // Random direction on a sphere, then biased vertically so the puff is wider than tall.
        double yaw = random.nextDouble() * Math.PI * 2.0;
        double pitch = (random.nextDouble() - 0.5) * Math.PI;
        double radiusFactor = SUB_QUAD_OFFSET_MIN
                + random.nextDouble() * (SUB_QUAD_OFFSET_MAX - SUB_QUAD_OFFSET_MIN);
        double dirX = Math.cos(yaw) * Math.cos(pitch);
        double dirY = Math.sin(pitch) * SUB_QUAD_VERTICAL_BIAS;
        double dirZ = Math.sin(yaw) * Math.cos(pitch);

        float quadScale = SUB_QUAD_SCALE_MIN + random.nextFloat() * (SUB_QUAD_SCALE_MAX - SUB_QUAD_SCALE_MIN);
        return new SubQuad(
                (float) (dirX * radiusFactor),
                (float) (dirY * radiusFactor),
                (float) (dirZ * radiusFactor),
                random.nextFloat() * 360.0F,
                random.nextFloat() * 360.0F,
                random.nextFloat() * 360.0F,
                quadScale,
                random.nextFloat() * 360.0F,
                0.15F + random.nextFloat() * 0.35F);
    }

    // =================================================================
    //  Lifetime
    // =================================================================

    /** Advance one client tick. Returns false once the cloud is finished and must be removed. */
    public boolean tick() {
        this.age++;
        return this.age < this.lifetime;
    }

    /** Normalised age in {@code [0, 1]}. */
    public float getAgeRatio(float partialTick) {
        if (this.lifetime <= 0) return 1.0F;
        return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    /** True while the cloud is still expanding (spec: the big cloud grows until ~45% of its life). */
    public boolean isGrowing(float partialTick) {
        return getAgeRatio(partialTick) < this.growFraction;
    }

    /**
     * Current cloud scale: grows from {@link #baseScale} to {@link #targetScale} over
     * {@link #growFraction} of the lifetime, then stays at the target scale while it fades.
     */
    public float getScale(float partialTick) {
        float ratio = getAgeRatio(partialTick);
        if (ratio >= this.growFraction) return this.targetScale;
        float t = ratio / this.growFraction;
        // Ease-out so the first burst of expansion is fast and the tail is slow.
        t = 1.0F - (1.0F - t) * (1.0F - t);
        return Mth.lerp(t, this.baseScale, this.targetScale);
    }

    /** Alpha from {@link #startAlpha} down to 0 over the whole lifetime. */
    public float getAlpha(float partialTick) {
        return this.startAlpha * (1.0F - getAgeRatio(partialTick));
    }

    /** Current world position: spawn point plus the (slow) drift offset. */
    public Vec3 getPosition(float partialTick) {
        if (this.anchored || this.drift.lengthSqr() < 1.0E-6) return this.position;
        return this.position.add(this.drift.scale(getAgeRatio(partialTick)));
    }

    /**
     * How far the sub-quads are pushed outward from the cloud centre, as a fraction of the
     * cloud scale. 1.0 = generated layout, higher = spread out. Only spreading clouds move.
     */
    public float getSpreadFactor(float partialTick) {
        if (!this.spreading) return 1.0F;
        float ratio = getAgeRatio(partialTick);
        return SPREAD_RADIUS_START + (SPREAD_RADIUS_END - SPREAD_RADIUS_START) * ratio;
    }

    // =================================================================
    //  Accessors used by the render layer
    // =================================================================

    public int getOwnerEntityId() {
        return this.ownerEntityId;
    }

    public int getAge() {
        return this.age;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public float getRenderSeed() {
        return this.renderSeed;
    }

    public int getSubQuadCount() {
        return this.subQuads.length;
    }

    public SubQuad getSubQuad(int index) {
        return this.subQuads[index];
    }
}
