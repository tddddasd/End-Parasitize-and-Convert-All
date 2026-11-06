package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class GasCloudRenderer {

    public static final float QUAD_HALF_EXTENT = 0.5F;

    public static final float SPIN_DEGREES_PER_TICK = 1.4F;

    public static final float MIN_VISIBLE_ALPHA = 0.004F;

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private GasCloudRenderer() {
    }

    public static final class Stats {

        public int submitted;

        public int quads;

        public float minAlpha = Float.MAX_VALUE;

        public float maxAlpha;
    }

    @FunctionalInterface
    private interface QuadEmitter {
        void emit(VertexConsumer consumer, PoseStack.Pose pose, float partialTick, float alpha);
    }

    public enum BillboardMode {

        CAMERA_FACING,

        YAW_ONLY
    }

    public static Stats submit(SubmitNodeCollector collector, PoseStack poseStack, PoseStack.Pose basis,
                               double anchorX, double anchorY, double anchorZ,
                               float partialTick, List<GasCloud> clouds) {
        Stats stats = new Stats();
        if (clouds.isEmpty()) {
            return stats;
        }
        RenderContext context = RenderContext.of();
        for (GasCloud cloud : clouds) {
            float alpha = cloud.getAlpha(partialTick);
            if (alpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            Vec3 position = cloud.getPosition(partialTick);
            submitBillboardAt(collector, poseStack, basis, anchorX, anchorY, anchorZ, context, position,
                    partialTick, alpha,
                    (consumer, pose, tick, fade) -> emitCloud(consumer, pose, cloud, tick, fade));
            stats.submitted++;
            stats.quads += cloud.getSubQuadCount();
            stats.minAlpha = Math.min(stats.minAlpha, alpha);
            stats.maxAlpha = Math.max(stats.maxAlpha, alpha);
        }
        return stats;
    }

    public static Stats submitSpecs(SubmitNodeCollector collector, PoseStack poseStack, PoseStack.Pose basis,
                                    double anchorX, double anchorY, double anchorZ,
                                    float partialTick, List<WaterSpec> specs) {
        Stats stats = new Stats();
        if (specs.isEmpty()) {
            return stats;
        }
        RenderContext context = RenderContext.of();
        for (WaterSpec spec : specs) {
            float alpha = spec.getAlpha(partialTick);
            if (alpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            Vec3 position = spec.getPosition(partialTick);
            submitBillboardAt(collector, poseStack, basis, anchorX, anchorY, anchorZ, context, position,
                    partialTick, alpha,
                    (consumer, pose, tick, fade) -> emitSpec(consumer, pose, spec, fade));
            stats.submitted++;
            stats.quads += 1;
            stats.minAlpha = Math.min(stats.minAlpha, alpha);
            stats.maxAlpha = Math.max(stats.maxAlpha, alpha);
        }
        return stats;
    }

    public static void submitBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                       PoseStack.Pose basis,
                                       double anchorX, double anchorY, double anchorZ,
                                       double offsetX, double offsetY, double offsetZ,
                                       float halfWidth, float halfHeight,
                                       float red, float green, float blue, float alpha,
                                       int packedSeed, int styleChannel, RenderType renderType) {
        submitBillboard(collector, poseStack, basis, anchorX, anchorY, anchorZ,
                offsetX, offsetY, offsetZ, halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, styleChannel, renderType,
                BillboardMode.CAMERA_FACING);
    }

    public static void submitBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                       PoseStack.Pose basis,
                                       double anchorX, double anchorY, double anchorZ,
                                       double offsetX, double offsetY, double offsetZ,
                                       float halfWidth, float halfHeight,
                                       float red, float green, float blue, float alpha,
                                       int packedSeed, int styleChannel, RenderType renderType,
                                       BillboardMode mode) {
        Vec3 position = new Vec3(anchorX + offsetX, anchorY + offsetY, anchorZ + offsetZ);
        submitBillboardAt(collector, poseStack, basis, anchorX, anchorY, anchorZ,
                RenderContext.of(renderType, mode), position, 0.0F, alpha,
                (consumer, pose, tick, fade) -> emitQuad(consumer, pose, 0.0F, 0.0F, 0.0F,
                        new Vector3f(halfWidth, 0.0F, 0.0F), new Vector3f(0.0F, halfHeight, 0.0F),
                        red, green, blue, alpha, packedSeed, styleChannel));
    }

    public static void submitVerticalBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                               PoseStack.Pose basis,
                                               double anchorX, double anchorY, double anchorZ,
                                               double offsetX, double offsetY, double offsetZ,
                                               float halfWidth, float halfHeight,
                                               float red, float green, float blue, float alpha,
                                               int packedSeed, int styleChannel, RenderType renderType) {
        submitBillboard(collector, poseStack, basis, anchorX, anchorY, anchorZ,
                offsetX, offsetY, offsetZ, halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, styleChannel, renderType,
                BillboardMode.YAW_ONLY);
    }

    private static final class RenderContext {
        private final RenderType renderType;
        private final Quaternionf cameraRotation;

        private RenderContext(RenderType renderType, Quaternionf cameraRotation) {
            this.renderType = renderType;
            this.cameraRotation = cameraRotation;
        }

        private static RenderContext of() {
            return of(GasCloudRenderType.get(), BillboardMode.CAMERA_FACING);
        }

        private static RenderContext of(RenderType renderType, BillboardMode mode) {

            Quaternionf rotation =
                    new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            if (mode == BillboardMode.YAW_ONLY) {

                rotation = Mth.rotationAroundAxis(Mth.Y_AXIS, rotation, new Quaternionf());
            }
            return new RenderContext(renderType, rotation);
        }
    }

    private static void submitBillboardAt(SubmitNodeCollector collector, PoseStack poseStack,
                                          PoseStack.Pose basis,
                                          double anchorX, double anchorY, double anchorZ,
                                          RenderContext context, Vec3 position,
                                          float partialTick, float alpha, QuadEmitter emitter) {
        poseStack.pushPose();

        poseStack.last().set(basis);
        poseStack.translate(position.x - anchorX, position.y - anchorY, position.z - anchorZ);
        poseStack.mulPose(context.cameraRotation);

        collector.submitCustomGeometry(poseStack, context.renderType,
                (pose, consumer) -> emitter.emit(consumer, pose, partialTick, alpha));

        poseStack.popPose();
    }

    public static void emitCloud(VertexConsumer consumer, PoseStack.Pose pose, GasCloud cloud,
                                 float partialTick, float alpha) {
        float scale = cloud.getScale(partialTick);
        float spread = cloud.getSpreadFactor(partialTick);
        int age = cloud.getAge();
        int seedChannel = GasCloudRenderType.packSeed(cloud.getRenderSeed());

        for (int index = 0; index < cloud.getSubQuadCount(); index++) {
            GasCloud.SubQuad quad = cloud.getSubQuad(index);
            Quaternionf rotation = new Quaternionf()
                    .rotateY(quad.yaw * DEG_TO_RAD)
                    .rotateX(quad.pitch * DEG_TO_RAD)
                    .rotateZ((quad.roll + quad.spinPhase + age * quad.spinSpeed * SPIN_DEGREES_PER_TICK)
                            * DEG_TO_RAD);
            float half = QUAD_HALF_EXTENT * scale * quad.scale;
            Vector3f right = rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F)).mul(half);
            Vector3f up = rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F)).mul(half);

            emitQuad(consumer, pose,
                    quad.offsetX * scale * spread,
                    quad.offsetY * scale * spread,
                    quad.offsetZ * scale * spread,
                    right, up,
                    1.0F, 1.0F, 1.0F, alpha,
                    seedChannel, index);
        }
    }

    public static void emitSpec(VertexConsumer consumer, PoseStack.Pose pose, WaterSpec spec, float alpha) {
        float half = spec.getHalfExtent();
        Vector3f right = new Vector3f(half, 0.0F, 0.0F);
        Vector3f up = new Vector3f(0.0F, half, 0.0F);
        emitQuad(consumer, pose, 0.0F, 0.0F, 0.0F, right, up,
                WaterSpec.WATER_SPEC_COLOR_RED, WaterSpec.WATER_SPEC_COLOR_GREEN,
                WaterSpec.WATER_SPEC_COLOR_BLUE, alpha,
                0, GasCloudRenderType.SPEC_STYLE_CHANNEL);
    }

    public static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                float centerX, float centerY, float centerZ,
                                Vector3f right, Vector3f up,
                                float red, float green, float blue, float alpha,
                                int seedChannel, int styleChannel) {

        emitVertex(consumer, pose, centerX - right.x - up.x, centerY - right.y - up.y,
                centerZ - right.z - up.z, 0.0F, 1.0F, red, green, blue, alpha, seedChannel, styleChannel);

        emitVertex(consumer, pose, centerX + right.x - up.x, centerY + right.y - up.y,
                centerZ + right.z - up.z, 1.0F, 1.0F, red, green, blue, alpha, seedChannel, styleChannel);

        emitVertex(consumer, pose, centerX + right.x + up.x, centerY + right.y + up.y,
                centerZ + right.z + up.z, 1.0F, 0.0F, red, green, blue, alpha, seedChannel, styleChannel);

        emitVertex(consumer, pose, centerX - right.x + up.x, centerY - right.y + up.y,
                centerZ - right.z + up.z, 0.0F, 0.0F, red, green, blue, alpha, seedChannel, styleChannel);
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   float x, float y, float z, float u, float v,
                                   float red, float green, float blue, float alpha,
                                   int seedChannel, int styleChannel) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setUv1(seedChannel, styleChannel);
    }
}
