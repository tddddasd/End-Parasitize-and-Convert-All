package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Locale;

public final class GasCloudRenderer {

    public static final float QUAD_HALF_EXTENT = 0.5F;

    public static final float SPIN_DEGREES_PER_TICK = 1.4F;

    public static final float MIN_VISIBLE_ALPHA = 0.004F;

    private static final float SPECK_MIN_VISIBLE_ALPHA = 0.004F;

    private GasCloudRenderer() {
    }

    public static final class Submission {

        public final int cloudsDrawn;

        public final int cloudsTracked;

        public final int specksDrawn;

        public final float minAlpha;
        public final float maxAlpha;

        public final float poseBaseX;
        public final float poseBaseY;
        public final float poseBaseZ;

        public final Vec3 probeCloud;

        public final double probeAppliedX;
        public final double probeAppliedY;
        public final double probeAppliedZ;

        private Submission(int cloudsDrawn, int cloudsTracked, int specksDrawn,
                           float minAlpha, float maxAlpha,
                           float poseBaseX, float poseBaseY, float poseBaseZ,
                           Vec3 probeCloud,
                           double probeAppliedX, double probeAppliedY, double probeAppliedZ) {
            this.cloudsDrawn = cloudsDrawn;
            this.cloudsTracked = cloudsTracked;
            this.specksDrawn = specksDrawn;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.poseBaseX = poseBaseX;
            this.poseBaseY = poseBaseY;
            this.poseBaseZ = poseBaseZ;
            this.probeCloud = probeCloud;
            this.probeAppliedX = probeAppliedX;
            this.probeAppliedY = probeAppliedY;
            this.probeAppliedZ = probeAppliedZ;
        }

        public boolean isEmpty() {
            return this.cloudsDrawn == 0 && this.specksDrawn == 0;
        }
    }

    public static Submission submit(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                    float partialTick, List<GasCloud> clouds) {
        return submit(owner, poseStack, bufferSource, partialTick, clouds, null);
    }

    public static Submission submit(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                    float partialTick, List<GasCloud> clouds, List<WaterSpec> specks) {
        Minecraft minecraft = Minecraft.getInstance();
        float cameraYaw = minecraft.gameRenderer.getMainCamera().getYRot();
        float cameraPitch = minecraft.gameRenderer.getMainCamera().getXRot();

        Matrix4f pose = poseStack.last().pose();
        float poseBaseX = pose.m30();
        float poseBaseY = pose.m31();
        float poseBaseZ = pose.m32();

        VertexConsumer consumer = bufferSource.getBuffer(GasCloudRenderType.get());
        Vec3 origin = owner.getPosition(partialTick);

        int cloudsDrawn = 0;
        float minAlpha = Float.MAX_VALUE;
        float maxAlpha = 0.0F;
        Vec3 probeCloud = null;
        double probeAppliedX = 0.0D;
        double probeAppliedY = 0.0D;
        double probeAppliedZ = 0.0D;

        if (clouds != null) {
            for (GasCloud cloud : clouds) {
                float alpha = cloud.getAlpha(partialTick);
                if (alpha < MIN_VISIBLE_ALPHA) {
                    continue;
                }
                Vec3 position = cloud.getPosition(partialTick);
                if (cloudsDrawn == 0) {

                    probeCloud = position;
                    probeAppliedX = position.x - origin.x;
                    probeAppliedY = position.y - origin.y;
                    probeAppliedZ = position.z - origin.z;
                }

                int packedSeed = GasCloudRenderType.packSeed(cloud.getRenderSeed());
                renderCloud(consumer, poseStack, cloud, partialTick, alpha, packedSeed,
                        cameraYaw, cameraPitch, origin);
                cloudsDrawn++;
                minAlpha = Math.min(minAlpha, alpha);
                maxAlpha = Math.max(maxAlpha, alpha);
            }
        }

        int specksDrawn = 0;
        if (specks != null) {
            for (WaterSpec speck : specks) {

                float alpha = speck.getAlpha(partialTick);
                if (alpha < SPECK_MIN_VISIBLE_ALPHA) {
                    continue;
                }
                renderSpec(consumer, poseStack, speck, partialTick, alpha, cameraYaw, cameraPitch, origin);
                specksDrawn++;
            }
        }

        if (cloudsDrawn == 0) {
            minAlpha = 0.0F;
        }
        return new Submission(cloudsDrawn, clouds == null ? 0 : clouds.size(), specksDrawn,
                minAlpha, maxAlpha, poseBaseX, poseBaseY, poseBaseZ,
                probeCloud, probeAppliedX, probeAppliedY, probeAppliedZ);
    }

    private static void renderCloud(VertexConsumer consumer, PoseStack poseStack, GasCloud cloud,
                                    float partialTick, float alpha, int packedSeed,
                                    float cameraYaw, float cameraPitch, Vec3 origin) {
        Vec3 position = cloud.getPosition(partialTick);
        float scale = cloud.getScale(partialTick);
        float spread = cloud.getSpreadFactor(partialTick);
        int age = cloud.getAge();

        poseStack.pushPose();
        poseStack.translate(position.x - origin.x, position.y - origin.y, position.z - origin.z);

        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

        for (int i = 0; i < cloud.getSubQuadCount(); i++) {
            GasCloud.SubQuad quad = cloud.getSubQuad(i);
            poseStack.pushPose();
            poseStack.translate(quad.offsetX * scale * spread,
                    quad.offsetY * scale * spread,
                    quad.offsetZ * scale * spread);
            poseStack.mulPose(Axis.YP.rotationDegrees(quad.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(quad.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    quad.roll + (quad.spinPhase + age * quad.spinSpeed * SPIN_DEGREES_PER_TICK)));

            float half = QUAD_HALF_EXTENT * scale * quad.scale;
            drawBillboardQuad(consumer, poseStack.last().pose(), half,
                    quad.colorR, quad.colorG, quad.colorB, alpha, packedSeed, i);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderSpec(VertexConsumer consumer, PoseStack poseStack, WaterSpec speck,
                                   float partialTick, float alpha, float cameraYaw, float cameraPitch,
                                   Vec3 origin) {
        Vec3 position = speck.getPosition(partialTick);
        poseStack.pushPose();
        poseStack.translate(position.x - origin.x, position.y - origin.y, position.z - origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

        float half = speck.getHalfExtent();

        drawBillboardQuad(consumer, poseStack.last().pose(), half,
                speck.getColorRed(), speck.getColorGreen(), speck.getColorBlue(), alpha,
                0, GasCloudRenderType.SPEC_STYLE_CHANNEL);
        poseStack.popPose();
    }

    public static void submitBillboard(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                       float partialTick,
                                       double offsetX, double offsetY, double offsetZ,
                                       float halfWidth, float halfHeight,
                                       float red, float green, float blue, float alpha,
                                       int packedSeed, int subQuadChannel, RenderType renderType) {
        submitBillboard(owner, poseStack, bufferSource, partialTick,
                offsetX, offsetY, offsetZ, halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, subQuadChannel, renderType, true);
    }

    public static void submitVerticalBillboard(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                               float partialTick,
                                               double offsetX, double offsetY, double offsetZ,
                                               float halfWidth, float halfHeight,
                                               float red, float green, float blue, float alpha,
                                               int packedSeed, int subQuadChannel, RenderType renderType) {
        submitBillboard(owner, poseStack, bufferSource, partialTick,
                offsetX, offsetY, offsetZ, halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, subQuadChannel, renderType, false);
    }

    private static void submitBillboard(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                        float partialTick,
                                        double offsetX, double offsetY, double offsetZ,
                                        float halfWidth, float halfHeight,
                                        float red, float green, float blue, float alpha,
                                        int packedSeed, int subQuadChannel, RenderType renderType,
                                        boolean tiltWithCameraPitch) {
        Minecraft minecraft = Minecraft.getInstance();
        float cameraYaw = minecraft.gameRenderer.getMainCamera().getYRot();

        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);

        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        if (tiltWithCameraPitch) {
            poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.gameRenderer.getMainCamera().getXRot()));
        }
        drawBillboardQuad(consumer, poseStack.last().pose(), halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, subQuadChannel);
        poseStack.popPose();
    }

    private static void drawBillboardQuad(VertexConsumer consumer, Matrix4f matrix, float half,
                                          float red, float green, float blue, float alpha,
                                          int packedSeed, int subQuadIndex) {
        drawBillboardQuad(consumer, matrix, half, half, red, green, blue, alpha,
                packedSeed, subQuadIndex);
    }

    private static void drawBillboardQuad(VertexConsumer consumer, Matrix4f matrix,
                                          float halfWidth, float halfHeight,
                                          float red, float green, float blue, float alpha,
                                          int packedSeed, int subQuadIndex) {

        float vBottom = 1.0F;
        float vTop = 0.0F;

        consumer.vertex(matrix, -halfWidth, -halfHeight, 0.0F)
                .color(red, green, blue, alpha)
                .uv(0.0F, vBottom)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();

        consumer.vertex(matrix, halfWidth, -halfHeight, 0.0F)
                .color(red, green, blue, alpha)
                .uv(1.0F, vBottom)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();

        consumer.vertex(matrix, halfWidth, halfHeight, 0.0F)
                .color(red, green, blue, alpha)
                .uv(1.0F, vTop)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();

        consumer.vertex(matrix, -halfWidth, halfHeight, 0.0F)
                .color(red, green, blue, alpha)
                .uv(0.0F, vTop)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    public static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
