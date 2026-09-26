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
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ArayaConstants;

/**
 * Draws the Alayavijnana slash: a white glowing line 3 blocks long, 0.05 to 0.2 blocks wide, cut at 50
 * degrees through the victim, with a 0.05-wide transparent border on each side that refracts the frame.
 *
 * <h2>Geometry</h2>
 * <p>Everything is built in the blade's own frame, which is where the 50 degrees live:</p>
 * <pre>
 *   u = the horizontal direction of the cut (where the hit came from)
 *   w = u rotated 50 degrees up out of the horizontal plane  (the oblique rise)
 *   n = u x w                                                (the blade's face normal)
 * </pre>
 * <p>so the blade is the plane spanned by {@code u} and {@code w}: 3 blocks along {@code u}, and the
 * blade width runs along {@code w}. Five coplanar strips are emitted, all at the same surface:</p>
 * <ul>
 *   <li>the <b>core</b>, width {@code 0.05 -> 0.2} over the first ticks and then constant, drawn through
 *       the glow render type with a zero refraction offset;</li>
 *   <li>a wider, dimmer <b>halo</b> copy of the core, which is what turns a hard-edged strip into a
 *       glow;</li>
 *   <li>two <b>border</b> strips, {@code 0.05} wide, one on each side of the core, drawn through the
 *       refraction render type: their vertices carry the screen-space offset that makes the fragment
 *       stage read the frame copy a little to the side (see {@link ArayaSceneCopy}).</li>
 * </ul>
 *
 * <h2>Timing</h2>
 * <p>{@link ArayaConstants#SLASH_HOLD_TICKS} ticks at full strength, then
 * {@link ArayaConstants#SLASH_FADE_TICKS} of easing out, driven by the level's game time so the fade is
 * smooth and independent of the frame rate.</p>
 */
public final class ArayaSlashRenderer {

    /** TEMP DIAGNOSTIC frame counter (remove together with the diagnostic in {@link #renderGeometry}). */
    private static long diagnosticFrames;

    private ArayaSlashRenderer() {
    }

    /** Entry point, from the level-stage listener at {@code AFTER_PARTICLES}. */
    public static void renderGeometry(RenderLevelStageEvent event) {
        if (!ArayaSlashClientCache.hasAny()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        // One frame copy per frame, and only while at least one slash is visible: the copy is what the
        // border samples, and this is the earliest the renderer knows a slash is on screen.
        ArayaSceneCopy.captureFrame(ArayaSlashClientCache.frameToken());

        float time = level.getGameTime() + (float) event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        // A level stage's pose stack is world space (the camera offset is applied per object, exactly as
        // SacrificeRitualRenderer documents for the same event), so shift it to the camera-relative frame
        // the custom pipeline expects; the vertices below are absolute world coordinates. Without this
        // shift the blade is drawn one whole camera position away from the victim and is never on screen.
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();

        // TEMP DIAGNOSTIC (remove once the slash is confirmed on screen): reaches here only when the
        // client cache holds at least one slash, so a line proves the geometry is really submitted.
        if ((diagnosticFrames++ % 40L) == 0L) {
            epca.LOGGER.info("[araya] submitting {} slash(es); camera=({}, {}, {}) first={}",
                    ArayaSlashClientCache.entries().size(), camera.x, camera.y, camera.z,
                    ArayaSlashClientCache.entries().get(0).position);
        }

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType refraction = ArayaSlashRenderType.refraction();
        RenderType glow = ArayaSlashRenderType.glow();
        try {
            // Two batches, in this order: the refracting border first, the glowing line on top. Within
            // one batch the translucent sort keeps the quads in submission order, which is all that is
            // needed for a handful of coplanar strips.
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
            // endBatch is idempotent and safe on an empty builder, so a throw in the middle cannot leave
            // a half-filled buffer behind for the next stage.
            buffers.endBatch();
            poseStack.popPose();
        }
    }

    /**
     * Emits one slash: the border strips when {@code refraction} is true, the core and its halo
     * otherwise.
     */
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
            // Ease-out cubic: the line stays bright and then leaves quickly, instead of a flat linear
            // ramp that reads as a slow dissolve.
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

        // The blade frame. `u` is the horizontal cut direction, `w` the same direction lifted 50
        // degrees, `n` their cross product (the blade's normal).
        Vec3 horizontal = new Vec3(entry.direction.x, 0.0D, entry.direction.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        horizontal = horizontal.normalize();
        double angle = Math.toRadians(ArayaConstants.SLASH_ANGLE_DEGREES);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        Vec3 u = horizontal;
        Vec3 w = new Vec3(-horizontal.z * sin, cos, horizontal.x * sin);
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
            // One strip on each side of the line. `inner`/`outer` are measured from the blade's
            // centreline along w, so the core spans +-coreHalf and each border sits just outside it.
            // Their UV0 carries the distance from the centreline and the strip's own half width, which is
            // what the shader normalises the profile with and scales the frame-copy offset by.
            for (int side = -1; side <= 1; side += 2) {
                double inner = side * coreHalf;
                double outer = inner + side * band;
                emitStrip(consumer, matrix, centre, u, w, halfLength, inner, outer, bandAlpha, band * 0.5D);
            }
            return;
        }

        // The core and its halo. The halo is the same strip, wider than its own ribbon coordinate, so
        // the fragment profile stretches over it and the line gains a soft edge.
        double coreHalf = halfWidthForAge(age);
        emitStrip(consumer, matrix, centre, u, w, halfLength, -coreHalf, coreHalf,
                ArayaConstants.SLASH_CORE_ALPHA * fade, coreHalf);
        double haloHalf = coreHalf * ArayaConstants.SLASH_HALO_WIDTH_FACTOR;
        emitStrip(consumer, matrix, centre, u, w, halfLength, -haloHalf, haloHalf,
                ArayaConstants.SLASH_CORE_ALPHA * ArayaConstants.SLASH_HALO_ALPHA * fade, haloHalf);
    }

    /**
     * Half width of the line at the given age: it grows from {@code SLASH_MIN_WIDTH / 2} to
     * {@code SLASH_MAX_WIDTH / 2} over {@link ArayaConstants#SLASH_GROW_TICKS} ticks and then stays.
     */
    private static double halfWidthForAge(float age) {
        float t = Mth.clamp(age / (float) ArayaConstants.SLASH_GROW_TICKS, 0.0F, 1.0F);
        // Ease-out cubic again, so the line snaps to its width and then settles.
        float c = 1.0F - t;
        float eased = 1.0F - c * c * c;
        float width = ArayaConstants.SLASH_MIN_WIDTH
                + (ArayaConstants.SLASH_MAX_WIDTH - ArayaConstants.SLASH_MIN_WIDTH) * eased;
        return width * 0.5D;
    }

    /**
     * One strip: a quad spanning {@code innerDistance .. outerDistance} along {@code w} and
     * {@code +-halfLength} along {@code u}. Its UV carries {@code (distance from the centreline, half
     * width of the strip)}, both in blocks, which is all the shader needs for the profile and for the
     * frame-copy offset.
     */
    private static void emitStrip(VertexConsumer consumer, Matrix4f matrix, Vec3 centre, Vec3 u, Vec3 w,
                                  double halfLength, double innerDistance, double outerDistance, float alpha,
                                  double halfWidth) {
        double length = (outerDistance - innerDistance) * 0.5D;
        double midpoint = (outerDistance + innerDistance) * 0.5D;
        for (int i = 0; i < 4; i++) {
            boolean atOuterEnd = i == 2 || i == 3;
            boolean atOuterEdge = i == 1 || i == 2;
            double alongLength = atOuterEnd ? halfLength : -halfLength;
            double alongWidth = midpoint + (atOuterEdge ? length : -length);
            double x = centre.x + u.x * alongLength + w.x * alongWidth;
            double y = centre.y + u.y * alongLength + w.y * alongWidth;
            double z = centre.z + u.z * alongLength + w.z * alongWidth;
            consumer.vertex(matrix, (float) x, (float) y, (float) z)
                    .color(1.0F, 1.0F, 1.0F, alpha)
                    .uv((float) alongWidth, (float) halfWidth)
                    .endVertex();
        }
    }
}
