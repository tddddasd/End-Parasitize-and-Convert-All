package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

public final class ArayaSlashRenderer {

    private ArayaSlashRenderer() {
    }

    public static void renderGeometry(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (!ArayaSlashClientCache.hasAny()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RenderType renderType = ArayaSlashRenderType.get();
        if (renderType == null) {

            return;
        }

        ArayaSceneCopy.captureFrame(ArayaSlashClientCache.frameToken());

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(renderType);
        try {
            for (ArayaSlashClientCache.Entry entry : ArayaSlashClientCache.entries()) {
                drawSlash(consumer, matrix, entry, time, camera);
            }
        } finally {
            buffers.endBatch(renderType);
            poseStack.popPose();
        }
    }

    private static void drawSlash(VertexConsumer consumer, Matrix4f matrix, ArayaSlashClientCache.Entry entry,
                                  float time, Vec3 camera) {
        float age = time - entry.startTime;
        if (age < 0.0F || age > ArayaConstants.SLASH_HOLD_TICKS + ArayaConstants.SLASH_FADE_TICKS) {
            return;
        }
        float fade;
        if (age <= ArayaConstants.SLASH_HOLD_TICKS) {
            fade = 1.0F;
        } else {
            float t = (age - ArayaConstants.SLASH_HOLD_TICKS) / (float) ArayaConstants.SLASH_FADE_TICKS;

            float c = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
            fade = c * c * c;
        }
        if (fade <= 0.004F) {
            return;
        }

        Vec3 world = entry.position;
        double toCameraX = world.x - camera.x;
        double toCameraY = world.y - camera.y;
        double toCameraZ = world.z - camera.z;
        if (toCameraX * toCameraX + toCameraY * toCameraY + toCameraZ * toCameraZ
                > ArayaConstants.RENDER_DISTANCE * ArayaConstants.RENDER_DISTANCE) {
            return;
        }

        Vec3 hit = new Vec3(entry.direction.x, 0.0D, entry.direction.z);
        if (hit.lengthSqr() < 1.0E-6D) {
            hit = new Vec3(1.0D, 0.0D, 0.0D);
        }
        hit = hit.normalize();
        double angle = Math.toRadians(ArayaConstants.SLASH_ANGLE_DEGREES);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        Vec3 right = new Vec3(-hit.z, 0.0D, hit.x);
        Vec3 u = new Vec3(right.x * cos, sin, right.z * cos).normalize();

        Vec3 w = u.cross(hit).normalize();

        double halfLength = ArayaConstants.SLASH_LENGTH * 0.5D;
        double coreHalf = halfWidthForAge(age);

        double band = ArayaConstants.SLASH_BAND_WIDTH;
        for (int side = -1; side <= 1; side += 2) {
            double inner = side * coreHalf;
            double outer = inner + side * band;
            emitPrism(consumer, matrix, world, u, w, hit, halfLength, inner, outer,
                    ArayaConstants.SLASH_BAND_ALPHA * fade);
        }
        emitPrism(consumer, matrix, world, u, w, hit, halfLength, -coreHalf, coreHalf,
                ArayaConstants.SLASH_CORE_ALPHA * fade);
        double haloHalf = coreHalf * ArayaConstants.SLASH_HALO_WIDTH_FACTOR;
        emitPrism(consumer, matrix, world, u, w, hit, halfLength, -haloHalf, haloHalf,
                ArayaConstants.SLASH_CORE_ALPHA * ArayaConstants.SLASH_HALO_ALPHA * fade);
    }

    private static double halfWidthForAge(float age) {
        float t = Mth.clamp(age / (float) ArayaConstants.SLASH_GROW_TICKS, 0.0F, 1.0F);

        float c = 1.0F - t;
        float eased = 1.0F - c * c * c;
        float width = ArayaConstants.SLASH_MIN_WIDTH
                + (ArayaConstants.SLASH_MAX_WIDTH - ArayaConstants.SLASH_MIN_WIDTH) * eased;
        return width * 0.5D;
    }

    private static void emitPrism(VertexConsumer consumer, Matrix4f matrix, Vec3 centre, Vec3 u, Vec3 w, Vec3 n,
                                  double halfLength, double innerDistance, double outerDistance, float alpha) {
        double midpoint = (outerDistance + innerDistance) * 0.5D;
        double halfWidth = Math.abs(outerDistance - innerDistance) * 0.5D;
        double height = halfWidth;
        emitPrismFace(consumer, matrix, centre, u, w, n, halfLength, halfWidth, alpha,
                innerDistance, 0.0D, -halfWidth,
                midpoint, height, 0.0D);
        emitPrismFace(consumer, matrix, centre, u, w, n, halfLength, halfWidth, alpha,
                midpoint, height, 0.0D,
                outerDistance, 0.0D, halfWidth);
    }

    private static void emitPrismFace(VertexConsumer consumer, Matrix4f matrix, Vec3 centre, Vec3 u, Vec3 w, Vec3 n,
                                      double halfLength, double halfWidth, float alpha,
                                      double widthA, double normalA, double uvA,
                                      double widthB, double normalB, double uvB) {
        emitPrismVertex(consumer, matrix, centre, u, w, n, -halfLength, widthA, normalA, uvA, halfWidth, alpha);
        emitPrismVertex(consumer, matrix, centre, u, w, n, -halfLength, widthB, normalB, uvB, halfWidth, alpha);
        emitPrismVertex(consumer, matrix, centre, u, w, n, halfLength, widthB, normalB, uvB, halfWidth, alpha);
        emitPrismVertex(consumer, matrix, centre, u, w, n, halfLength, widthA, normalA, uvA, halfWidth, alpha);
    }

    private static void emitPrismVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 centre, Vec3 u, Vec3 w, Vec3 n,
                                        double alongLength, double alongWidth, double alongNormal, double uvX,
                                        double halfWidth, float alpha) {
        float x = (float) (centre.x + u.x * alongLength + w.x * alongWidth + n.x * alongNormal);
        float y = (float) (centre.y + u.y * alongLength + w.y * alongWidth + n.y * alongNormal);
        float z = (float) (centre.z + u.z * alongLength + w.z * alongWidth + n.z * alongNormal);
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv((float) uvX, (float) halfWidth);
    }
}
