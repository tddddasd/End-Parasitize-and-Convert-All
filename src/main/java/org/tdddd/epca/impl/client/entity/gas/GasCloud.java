package org.tdddd.epca.impl.client.entity.gas;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class GasCloud {

    public static final int MIN_SUB_QUADS = 5;
    public static final int MAX_SUB_QUADS = 9;

    private static final float SUB_QUAD_SCALE_MIN = 0.6F;
    private static final float SUB_QUAD_SCALE_MAX = 1.6F;

    private static final float SUB_QUAD_OFFSET_MIN = 0.05F;
    private static final float SUB_QUAD_OFFSET_MAX = 0.35F;

    private static final float SPREAD_RADIUS_START = 1.0F;
    private static final float SPREAD_RADIUS_END = 2.4F;

    private static final float SUB_QUAD_VERTICAL_BIAS = 0.75F;

    public static final class SubQuad {

        public final float offsetX;
        public final float offsetY;
        public final float offsetZ;

        public final float yaw;
        public final float pitch;
        public final float roll;

        public final float scale;

        public final float spinPhase;

        public final float spinSpeed;

        private SubQuad(float offsetX, float offsetY, float offsetZ,
                        float yaw, float pitch, float roll,
                        float scale, float spinPhase, float spinSpeed) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.scale = scale;
            this.spinPhase = spinPhase;
            this.spinSpeed = spinSpeed;
        }
    }

    private final int ownerEntityId;
    private final Vec3 position;
    private final int lifetime;
    private final float baseScale;
    private final float targetScale;

    private final float growFraction;
    private final float startAlpha;

    private final Vec3 drift;

    private final boolean anchored;

    private final boolean spreading;

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

    public boolean tick() {
        this.age++;
        return this.age < this.lifetime;
    }

    public float getAgeRatio(float partialTick) {
        if (this.lifetime <= 0) return 1.0F;
        return Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    public boolean isGrowing(float partialTick) {
        return getAgeRatio(partialTick) < this.growFraction;
    }

    public float getScale(float partialTick) {
        float ratio = getAgeRatio(partialTick);
        if (ratio >= this.growFraction) return this.targetScale;
        float t = ratio / this.growFraction;

        t = 1.0F - (1.0F - t) * (1.0F - t);
        return Mth.lerp(t, this.baseScale, this.targetScale);
    }

    public float getAlpha(float partialTick) {
        return this.startAlpha * (1.0F - getAgeRatio(partialTick));
    }

    public Vec3 getPosition(float partialTick) {
        if (this.anchored || this.drift.lengthSqr() < 1.0E-6) return this.position;
        return this.position.add(this.drift.scale(getAgeRatio(partialTick)));
    }

    public float getSpreadFactor(float partialTick) {
        if (!this.spreading) return 1.0F;
        float ratio = getAgeRatio(partialTick);
        return SPREAD_RADIUS_START + (SPREAD_RADIUS_END - SPREAD_RADIUS_START) * ratio;
    }

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

    public List<SubQuad> getSubQuads() {
        List<SubQuad> list = new ArrayList<>(this.subQuads.length);
        for (SubQuad quad : this.subQuads) {
            list.add(quad);
        }
        return list;
    }

    public Vector3f getPositionVector(float partialTick) {
        Vec3 pos = getPosition(partialTick);
        return new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
    }
}
