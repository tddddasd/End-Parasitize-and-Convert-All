package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class WaterSpec {

    public enum Kind {

        SURFACE,

        SUSPENDED
    }

    public static final float WATER_SPEC_HALF_EXTENT_MIN = 0.03F;

    public static final float WATER_SPEC_HALF_EXTENT_MAX = 0.09F;

    public static final float WATER_SPEC_ALPHA_MIN = 0.45F;

    public static final float WATER_SPEC_ALPHA_MAX = 0.75F;

    public static final float WATER_SPEC_COLOR_RED = 0.42F;
    public static final float WATER_SPEC_COLOR_GREEN = 0.06F;
    public static final float WATER_SPEC_COLOR_BLUE = 0.06F;

    public static final float WATER_SPEC_FADE_FRACTION = 0.20F;

    public static final double WATER_SPEC_DRIFT_SPEED = 0.01D;

    public static final int WATER_SPEC_DIRECTION_MIN_TICKS = 20;

    public static final int WATER_SPEC_DIRECTION_MAX_TICKS = 40;

    public static final float WATER_SPEC_SURFACE_BOB_AMPLITUDE = 0.03F;

    public static final float WATER_SPEC_SURFACE_BOB_PERIOD_TICKS = 60.0F;

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

    private Vec3 position;

    private Vec3 drift;

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

    private static double randomComponent(RandomSource random, double speed) {
        return (random.nextDouble() * 2.0D - 1.0D) * speed;
    }

    public static Vec3 randomSurfaceDrift(RandomSource random) {
        return new Vec3(randomComponent(random, WATER_SPEC_DRIFT_SPEED), 0.0D,
                randomComponent(random, WATER_SPEC_DRIFT_SPEED));
    }

    public static Vec3 randomSuspendedDrift(RandomSource random) {
        return new Vec3(randomComponent(random, WATER_SPEC_DRIFT_SPEED),
                randomComponent(random, WATER_SPEC_DRIFT_SPEED),
                randomComponent(random, WATER_SPEC_DRIFT_SPEED));
    }

    public static int randomDirectionTicks(RandomSource random) {
        return WATER_SPEC_DIRECTION_MIN_TICKS
                + random.nextInt(WATER_SPEC_DIRECTION_MAX_TICKS - WATER_SPEC_DIRECTION_MIN_TICKS + 1);
    }

    public boolean tick() {
        this.age++;
        if (this.kind == Kind.SURFACE) {

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

    public float getAgeRatio(float partialTick) {
        if (this.lifetime <= 0) {
            return 1.0F;
        }
        return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    public float getAlpha(float partialTick) {
        float ratio = getAgeRatio(partialTick);
        float fadeIn = Mth.clamp(ratio / WATER_SPEC_FADE_FRACTION, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - ratio) / WATER_SPEC_FADE_FRACTION, 0.0F, 1.0F);
        return this.startAlpha * Math.min(fadeIn, fadeOut);
    }

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
