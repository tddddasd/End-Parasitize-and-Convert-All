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
 *   w = u rotated 50 degrees up out of the horizontal plane   (the oblique rise)
 *   n = u x w                                                 (the blade's face normal)
 * </pre>
 * <p>so the blade is the plane spanned by {@code u} and {@code w}: 3 blocks along {@code u}, and the blade
 * width runs along {@code w}. Four coplanar strips are emitted, all at the same surface:</p>
 * <ul>
 *   <li>the <b>core</b>, width {@code 0.05 -> 0.2} over the first ticks and then constant;</li>
 *   <li>a wider, dimmer <b>halo</b> copy of the core, which is what turns a hard-edged strip into a
 *       glow;</li>
 *   <li>two <b>border</b> strips, {@code 0.05} wide, one on each side of the core, whose vertices carry
 *       the offset that makes the fragment stage read the frame copy a little to the side (see
 *       {@link ArayaSceneCopy}).</li>
 * </ul>
 *
 * <h2>Timing</h2>
 * <p>{@link ArayaConstants#SLASH_HOLD_TICKS} ticks at full strength ("持续存在3秒"), then
 * {@link ArayaConstants#SLASH_FADE_TICKS} of easing out, driven by the level's game time.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The 1.20.1 twin filtered the single {@code RenderLevelStageEvent} by stage and took the camera and
 * the partial tick off the event. 26.1.2 dispatches one event class per stage, so this is called from
 * {@code RenderLevelStageEvent.AfterTranslucentParticles}, which no longer carries either: the partial
 * tick comes from {@code minecraft.getDeltaTracker()} and the camera position from the level render state,
 * exactly like the tree's own sacrifice-ritual renderer does. The stage's pose is world space here too, so
 * translating it by the camera position gives the same camera-relative frame. {@code vertex(...)} becomes
 * {@code addVertex(...)}/{@code setColor}/{@code setUv}, and the render type lives in
 * {@code net.minecraft.client.renderer.rendertype}.</p>
 */
public final class ArayaSlashRenderer {

    /** TEMP DIAGNOSTIC frame counter (remove together with the diagnostic in {@link #renderGeometry}). */
    private static long diagnosticFrames;

    private ArayaSlashRenderer() {
    }

    /**
     * Entry point, from the level-stage listener at
     * {@code RenderLevelStageEvent.AfterTranslucentParticles}.
     */
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
            // The pipeline is not registered yet (early frames, or a failed registration): drawing with an
            // unregistered pipeline would be worse than one frame without a slash.
            return;
        }

        // One frame copy per frame, and only while at least one slash is visible: the copy is what the
        // border samples, and this is the earliest the renderer knows a slash is on screen.
        ArayaSceneCopy.captureFrame(ArayaSlashClientCache.frameToken());

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;

        // TEMP DIAGNOSTIC (remove once the slash is confirmed on screen): reaches here only when the
        // client cache holds at least one slash, so a line proves the geometry is really submitted.
        if ((diagnosticFrames++ % 40L) == 0L) {
            epca.LOGGER.info("[araya] submitting {} slash(es); camera=({}, {}, {}) first={}",
                    ArayaSlashClientCache.entries().size(), camera.x, camera.y, camera.z,
                    ArayaSlashClientCache.entries().get(0).position);
        }
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        // A level stage's pose stack is world space in 26.1.2 as well, so translating by the camera
        // position gives the camera-relative frame this renderer emits its vertices in.
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

    /** Emits one slash: the two border strips, then the core and its halo on top. */
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
            // Ease-out cubic: the line stays bright and then leaves quickly, instead of a flat linear ramp
            // that reads as a slow dissolve.
            float c = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
            fade = c * c * c;
        }
        if (fade <= 0.004F) {
            return;
        }

        // Absolute world coordinates: the stage's pose stack was already shifted to the camera-relative
        // frame in renderGeometry, so subtracting the camera position here as well would place the blade
        // one whole camera position away from the victim. The camera offset is only used for the distance
        // test, exactly like the 1.20.1 twin does.
        Vec3 world = entry.position;
        double toCameraX = world.x - camera.x;
        double toCameraY = world.y - camera.y;
        double toCameraZ = world.z - camera.z;
        if (toCameraX * toCameraX + toCameraY * toCameraY + toCameraZ * toCameraZ
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

        double halfLength = ArayaConstants.SLASH_LENGTH * 0.5D;
        double coreHalf = halfWidthForAge(age);

        // The refracting border first, the glowing line on top: translucency is order dependent, and the
        // border is meant to sit behind the line.
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
        float x = (float) (centre.x + u.x * alongLength + w.x * alongWidth + n.x * alongNormal);
        float y = (float) (centre.y + u.y * alongLength + w.y * alongWidth + n.y * alongNormal);
        float z = (float) (centre.z + u.z * alongLength + w.z * alongWidth + n.z * alongNormal);
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv((float) uvX, (float) halfWidth);
    }
}
