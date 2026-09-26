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

        Vec3 centre = new Vec3(entry.position.x - camera.x, entry.position.y - camera.y,
                entry.position.z - camera.z);
        if (centre.lengthSqr() > ArayaConstants.RENDER_DISTANCE * ArayaConstants.RENDER_DISTANCE) {
            return;
        }

        // The blade frame. `u` is the horizontal cut direction, `w` the same direction lifted 50 degrees,
        // `n` their cross product (the blade's normal).
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

        double halfLength = ArayaConstants.SLASH_LENGTH * 0.5D;
        double coreHalf = halfWidthForAge(age);

        // The refracting border first, the glowing line on top: translucency is order dependent, and the
        // border is meant to sit behind the line.
        double band = ArayaConstants.SLASH_BAND_WIDTH;
        for (int side = -1; side <= 1; side += 2) {
            double inner = side * coreHalf;
            double outer = inner + side * band;
            emitStrip(consumer, matrix, centre, u, w, halfLength, inner, outer,
                    ArayaConstants.SLASH_BAND_ALPHA * fade, band * 0.5D);
        }
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
     * width of the strip)}, both in blocks, which is all the shader needs for the profile and the
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
            float x = (float) (centre.x + u.x * alongLength + w.x * alongWidth);
            float y = (float) (centre.y + u.y * alongLength + w.y * alongWidth);
            float z = (float) (centre.z + u.z * alongLength + w.z * alongWidth);
            consumer.addVertex(matrix, x, y, z)
                    .setColor(1.0F, 1.0F, 1.0F, alpha)
                    .setUv((float) alongWidth, (float) halfWidth);
        }
    }
}
