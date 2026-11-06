package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class WaterSpec {

    public enum Kind {

        SURFACE,

        SUSPENDED
    }

    public static final float MIN_HALF_EXTENT = 0.03F;
    public static final float MAX_HALF_EXTENT = 0.09F;

    public static final float MIN_ALPHA = 0.45F;
    public static final float MAX_ALPHA = 0.75F;

    public static final int MIN_LIFETIME = 30;
    public static final int MAX_LIFETIME = 80;

    public static final float FADE_FRACTION = 0.2F;

    public static final float COLOR_RED = 0.42F;
    public static final float COLOR_GREEN = 0.06F;
    public static final float COLOR_BLUE = 0.06F;

    public static final double DRIFT_SPEED = 0.01D;

    public static final double SURFACE_BOB_AMPLITUDE = 0.03D;

    public static final int SURFACE_BOB_PERIOD_TICKS = 60;

    public static final int SUSPENDED_REDIRECT_MIN_TICKS = 20;
    public static final int SUSPENDED_REDIRECT_MAX_TICKS = 40;

    public static final double WATER_SPEC_SURFACE_LIFT = 0.03D;

    private static final Vec3 ZERO_DRIFT = Vec3.ZERO;

    private final Kind kind;
    private final Vec3 position;
    private final int lifetime;
    private final float halfExtent;
    private final float startAlpha;
    private final float bobPhase;

    private Vec3 drift;

    private Vec3 velocity;

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

    public static WaterSpec spawn(List<Vec3> surfacePositions, List<Vec3> suspendedPositions,
                                  RandomSource random) {

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

    private static double randomSignedSpeed(RandomSource random) {
        return (random.nextDouble() * 2.0D - 1.0D) * DRIFT_SPEED;
    }

    public static int randomRedirectDelay(RandomSource random) {
        return SUSPENDED_REDIRECT_MIN_TICKS
                + random.nextInt(SUSPENDED_REDIRECT_MAX_TICKS - SUSPENDED_REDIRECT_MIN_TICKS + 1);
    }

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

    public boolean isSurface() {
        return this.kind == Kind.SURFACE;
    }

    public float getAgeRatio(float partialTick) {
        if (this.lifetime <= 0) return 1.0F;
        return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    public float getAlpha(float partialTick) {
        float ratio = getAgeRatio(partialTick);
        float fadeIn = Mth.clamp(ratio / FADE_FRACTION, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - ratio) / FADE_FRACTION, 0.0F, 1.0F);
        return this.startAlpha * Math.min(fadeIn, fadeOut);
    }

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
