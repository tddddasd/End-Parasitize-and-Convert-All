package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.tdddd.epca.impl.client.effect.ArayaSlashClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

public final class ArayaSlashRenderer {

    private ArayaSlashRenderer() {
    }

    public static void renderGeometry(RenderLevelStageEvent event) {
        if (!ArayaSlashClientCache.hasAny()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        ArayaSceneCopy.captureFrame(ArayaSlashClientCache.frameToken());

        float time = level.getGameTime() + (float) event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType refraction = ArayaSlashRenderType.refraction();
        RenderType glow = ArayaSlashRenderType.glow();
        try {

            VertexConsumer bandConsumer = buffers.getBuffer(refraction);
            for (ArayaSlashClientCache.Entry entry : ArayaSlashClientCache.entries()) {
                drawSlash(bandConsumer, matrix, entry, time, camera, true);
            }
            buffers.endBatch(refraction);

            VertexConsumer glowConsumer = buffers.getBuffer(glow);
            for (ArayaSlashClientCache.Entry entry : ArayaSlashClientCache.entries()) {
                drawSlash(glowConsumer, matrix, entry, time, camera, false);
            }
            buffers.endBatch(glow);
        } finally {

            buffers.endBatch();
            poseStack.popPose();
        }
    }

    private static void drawSlash(VertexConsumer consumer, Matrix4f matrix, ArayaSlashClientCache.Entry entry,
                                  float time, Vec3 camera, boolean refraction) {
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

        Vec3 centre = entry.position;
        double midX = centre.x - camera.x;
        double midY = centre.y - camera.y;
        double midZ = centre.z - camera.z;
        if (midX * midX + midY * midY + midZ * midZ
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
        Vec3 n = u.cross(w);
        if (n.lengthSqr() < 1.0E-6D) {
            n = new Vec3(0.0D, 1.0D, 0.0D);
        }
        n = n.normalize();

        double halfLength = ArayaConstants.SLASH_LENGTH * 0.5D;
        float coreAlpha = ArayaConstants.SLASH_CORE_ALPHA * fade;

        if (refraction) {
            double band = ArayaConstants.SLASH_BAND_WIDTH;
            float bandAlpha = ArayaConstants.SLASH_BAND_ALPHA * fade;
            double coreHalf = halfWidthForAge(age);

            for (int side = -1; side <= 1; side += 2) {
                double inner = side * coreHalf;
                double outer = inner + side * band;
                emitPrism(consumer, matrix, centre, u, w, n, halfLength, inner, outer, bandAlpha);
            }
            return;
        }

        double coreHalf = halfWidthForAge(age);
        emitPrism(consumer, matrix, centre, u, w, n, halfLength, -coreHalf, coreHalf,
                ArayaConstants.SLASH_CORE_ALPHA * fade);
        double haloHalf = coreHalf * ArayaConstants.SLASH_HALO_WIDTH_FACTOR;
        emitPrism(consumer, matrix, centre, u, w, n, halfLength, -haloHalf, haloHalf,
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
        double x = centre.x + u.x * alongLength + w.x * alongWidth + n.x * alongNormal;
        double y = centre.y + u.y * alongLength + w.y * alongWidth + n.y * alongNormal;
        double z = centre.z + u.z * alongLength + w.z * alongWidth + n.z * alongNormal;
        consumer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv((float) uvX, (float) halfWidth)
                .endVertex();
    }
}
