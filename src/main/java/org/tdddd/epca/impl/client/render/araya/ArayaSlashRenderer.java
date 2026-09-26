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

        // The blade frame. The cut lies in the plane that FACES the hit - the plane perpendicular to the
        // horizontal direction the hit came from - because a plane that contains that direction is exactly
        // edge-on to both the attacker and the victim (the camera looks along it) and therefore projects to
        // nothing at all: that is why the first build of this effect was invisible even though the geometry
        // was submitted every frame. Inside that facing plane the cut rises by SLASH_ANGLE_DEGREES from the
        // horizontal, which is what makes it an oblique slash as seen by the two players involved.
        Vec3 hit = new Vec3(entry.direction.x, 0.0D, entry.direction.z);
        if (hit.lengthSqr() < 1.0E-6D) {
            hit = new Vec3(1.0D, 0.0D, 0.0D);
        }
        hit = hit.normalize();
        double angle = Math.toRadians(ArayaConstants.SLASH_ANGLE_DEGREES);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        // Horizontal in-plane axis, then the cut direction lifted out of the horizontal inside the plane.
        Vec3 right = new Vec3(-hit.z, 0.0D, hit.x);
        Vec3 u = new Vec3(right.x * cos, sin, right.z * cos).normalize();
        // The width axis: still inside the facing plane and perpendicular to the cut.
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
            // One strip on each side of the line. `inner`/`outer` are measured from the blade's
            // centreline along w, so the core spans +-coreHalf and each border sits just outside it.
            // Their UV0 carries the distance from the centreline and the strip's own half width, which is
            // what the shader normalises the profile with and scales the frame-copy offset by.
            for (int side = -1; side <= 1; side += 2) {
                double inner = side * coreHalf;
                double outer = inner + side * band;
                emitPrism(consumer, matrix, centre, u, w, n, halfLength, inner, outer, bandAlpha);
            }
            return;
        }

        // The core and its halo. The halo is the same strip, wider than its own ribbon coordinate, so
        // the fragment profile stretches over it and the line gains a soft edge.
        double coreHalf = halfWidthForAge(age);
        emitPrism(consumer, matrix, centre, u, w, n, halfLength, -coreHalf, coreHalf,
                ArayaConstants.SLASH_CORE_ALPHA * fade);
        double haloHalf = coreHalf * ArayaConstants.SLASH_HALO_WIDTH_FACTOR;
        emitPrism(consumer, matrix, centre, u, w, n, halfLength, -haloHalf, haloHalf,
                ArayaConstants.SLASH_CORE_ALPHA * ArayaConstants.SLASH_HALO_ALPHA * fade);
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
     * One strip as a triangular prism, so the blade has volume instead of being a zero-thickness plane.
     *
     * <p>The cross-section is a triangle: its base runs along {@code w} from {@code innerDistance} to
     * {@code outerDistance} on the blade's plane, and its apex stands {@code halfWidth} off that plane
     * along the normal {@code n}. That triangle is extruded along {@code u} over the whole blade length,
     * which makes each strip a wedge with two faces (inner base edge to apex, apex to outer base edge). No
     * end caps are emitted: the silhouette seen down the blade's axis is the same triangle from either
     * side, so the blade already reads as solid from every direction, and a cap would be a few centimetres
     * across.</p>
     *
     * <p>UV0 carries {@code (signed distance from THIS strip's centreline, half width of the strip)}, both
     * in blocks and measured inside the strip: the fragment stage shapes the glow profile with it (full in
     * the middle of the strip, zero at its edges) and scales the frame-copy offset with it.</p>
     */
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

    /**
     * One quad of a prism: two cross-section points, each extruded from {@code -halfLength} to
     * {@code +halfLength} along {@code u}. A cross-section point is
     * {@code (alongWidth, alongNormal, uvX)} - its offsets along {@code w} and {@code n}, and the UV0.x it
     * carries.
     */
    private static void emitPrismFace(VertexConsumer consumer, Matrix4f matrix, Vec3 centre, Vec3 u, Vec3 w, Vec3 n,
                                      double halfLength, double halfWidth, float alpha,
                                      double widthA, double normalA, double uvA,
                                      double widthB, double normalB, double uvB) {
        emitPrismVertex(consumer, matrix, centre, u, w, n, -halfLength, widthA, normalA, uvA, halfWidth, alpha);
        emitPrismVertex(consumer, matrix, centre, u, w, n, -halfLength, widthB, normalB, uvB, halfWidth, alpha);
        emitPrismVertex(consumer, matrix, centre, u, w, n, halfLength, widthB, normalB, uvB, halfWidth, alpha);
        emitPrismVertex(consumer, matrix, centre, u, w, n, halfLength, widthA, normalA, uvA, halfWidth, alpha);
    }

    /** One prism vertex: {@code centre + u * alongLength + w * alongWidth + n * alongNormal}. */
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
