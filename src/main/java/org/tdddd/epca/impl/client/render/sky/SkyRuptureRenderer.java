package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;

/**
 * Draws the world barrier rupture onto the sky.
 *
 * <h2>Which layer it draws on (the key decision, changed once in the original commit)</h2>
 * 1.20.1 drew it in the Forge {@code RenderLevelStageEvent.Stage.AFTER_SKY} stage - the instant
 * {@code LevelRenderer.renderLevel()} returns from {@code renderSky()} and has not started the
 * terrain yet (verified on the 1.20.1 bytecode: {@code renderSky} at offset 438, the Forge AFTER_SKY
 * dispatch at 441, the first {@code renderChunkLayer} at 557). 26.1.2 exposes the same slot as the
 * dedicated {@code RenderLevelStageEvent.AfterSky} event class; see {@link SkyRuptureStageListener}.
 *
 * <pre>
 *   sky (renderSky) -> barrier rupture -> terrain/entities/clouds/rain -> hand -> HUD
 * </pre>
 *
 * <h2>Why it must not wait until the world is finished</h2>
 * The earliest implementation drew inside {@code GameRenderer.renderLevel()} after the whole world and
 * relied on "vertices on the far plane z = 1 + a {@code LEQUAL} depth test" to mask the effect onto
 * sky pixels. The idea is sound, but it tripped over a subtle detail:
 *
 * <blockquote>
 * <b>Vanilla clouds write depth over the whole cloud geometry, including fully transparent
 * texels.</b> The cloud render type is translucent with depth writes and no alpha test, so every quad
 * of the cloud plane writes depth. The cloud plane is a square area centred on the player at a
 * constant height, and <b>its outer boundary projects onto the screen as one straight horizontal
 * line</b>.
 * </blockquote>
 *
 * <p>Consequence: above that line every one of our quads was rejected by the depth test and below it
 * everything passed, so "a straight line sliced the picture in half" and there were inexplicable
 * occlusions all over the skybox. Terrain and entity silhouettes occluded it for the same reason.</p>
 *
 * <p><b>Fix: draw after the sky and before the terrain.</b> At that moment the depth buffer is still
 * all clear values ({@code renderSky} runs with {@code depthMask(false)} throughout and writes no
 * depth), so the layer still only lands on sky pixels, while terrain, entities and clouds are drawn
 * <b>on top of us</b> afterwards: the cloud plane can no longer cut it in half and the occlusion order
 * is naturally correct.</p>
 *
 * <h2>Why it sticks to the sky</h2>
 * <ol>
 *   <li>the vertices sit on the far NDC plane ({@code z = 1}) with a {@code LESS_THAN_OR_EQUAL} depth
 *       test,</li>
 *   <li>every pixel reconstructs its <b>world direction</b> (in the vertex stage, from the pipeline's
 *       own projection and model-view matrices) and computes the crack pattern from that direction,
 *       so turning the camera pans the cracks with the sky instead of sticking them to the screen.
 *   </li>
 * </ol>
 *
 * <h2>1.20.1 -&gt; 26.1.2: what changed in the draw call</h2>
 * <ul>
 *   <li>The per-draw {@code Uniform}s of the 1.20.1 {@code ShaderInstance} are gone. The payload is
 *       packed into eleven vertex floats; {@link #emitVertex} is the one place it is written, and
 *       {@link SkyRuptureShaders} documents the layout and why it is that shape.</li>
 *   <li>The camera basis vectors are <b>not</b> sent: the vertex stage rebuilds them from
 *       {@code ProjMat}/{@code ModelViewMat}. 1.20.1 derived {@code tan(fov/2)} from
 *       {@code RenderSystem.getProjectionMatrix()} and baked it into its ray vectors, so this port is
 *       equivalent but picks up the real FOV instead of a reconstruction.</li>
 *   <li>The 12 star shells are drawn by <b>one</b> quad that loops them in the fragment stage, exactly
 *       as 1.20.1 did. The earlier per-sprite draws recomputed the shared fields twelve times per pixel
 *       and composited twelve sources into the framebuffer in sequence instead of summing them, which
 *       is not the same image; the strips are packed into one sheet texture so a single Sampler0 can
 *       still reach all twelve (see {@link SkyRuptureShaders}).</li>
 *   <li>{@code mc.renderBuffers().bufferSource()} still exists and is used exactly as the 1.20.1
 *       version used {@code MultiBufferSource.BufferSource}.</li>
 * </ul>
 *
 * <h2>Shaders (Iris / Oculus)</h2>
 * The 1.20.1 version deferred the draw to the end of {@code renderLevel()} when a shader pack was
 * active, because the whole scene goes through a GBuffer there and writing the main framebuffer at
 * AFTER_SKY would be overwritten by the later composite. Neither Iris nor Oculus exists for 26.1.2 at
 * the time of this port, so {@link IrisShaderCompat} reports "inactive" unconditionally and the
 * deferred path is a documented no-op.
 */
public final class SkyRuptureRenderer {

    private SkyRuptureRenderer() {
    }

    /**
     * Advances the effect progress once per frame.
     *
     * <p>Hung on the HEAD of {@code GameRenderer.render(DeltaTracker, boolean)}, which runs exactly
     * once per frame. It is deliberately <b>not</b> driven from the level-stage event: a shader pack's
     * shadow pass runs {@code LevelRenderer.renderLevel()} a second time, which would double the
     * progress. Inactive is a no-op.</p>
     */
    public static void update() {
        SkyRuptureEffect.update();
    }

    /**
     * Called once per frame from the AfterSky level stage, the draw slot between the sky and the
     * terrain. Draws here whenever no shader pack is active.
     */
    public static void renderAfterSky() {
        if (!IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    /**
     * Called once per frame after the world part of level rendering.
     *
     * <p>Only needed when a shader pack is active (see the class comment).</p>
     */
    public static void renderDeferred() {
        if (IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    /** Submits the one full-screen quad (progress has already been advanced by {@link #update()}). */
    private static void drawIfActive() {
        if (!SkyRuptureEffect.isActive() || !SkyRuptureShaders.isPipelineRegistered()) {
            return;
        }
        // Draw-level early-out: the shader multiplies its outgoing alpha by `fade`, so an envelope of
        // zero makes every pixel discard. Testing it here (and again first thing in the fragment stage)
        // skips the whole submission instead of shading a full screen of nothing.
        if (SkyRuptureEffect.fade() <= 0.002f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        RenderType renderType = SkyRuptureShaders.getRenderType();
        if (renderType == null) {
            // The pipeline is not registered (resource reload / very early frame): skip.
            return;
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // 1.20.1 pushed `SkyRuptureEffect.elapsedSeconds()` into the shader's `time` uniform, and every
        // time-driven term in the fragment stage is tuned for SECONDS: the nebula drift (t * 0.03,
        // t * 0.02, t * 0.04), the star field's scroll (t * 0.004) and its twinkle (sin(t * 1.6),
        // sin(t * 1.1)). Feeding world ticks here ran all of them 20x too fast, which is exactly the
        // "the star texture slides/twinkles far quicker than in 1.20.1" defect. The one term that is
        // NOT seconds-based is the star strip timeline: `.mcmeta` time/frametime count client ticks
        // (vanilla's atlas ticker advances one hold step per tick), so `sky_rupture.fsh` multiplies
        // this clock by TICKS_PER_SECOND right at the lookup and nowhere else.
        float time = SkyRuptureEffect.elapsedSeconds();
        float progress = SkyRuptureEffect.progress();
        float breakAmount = SkyRuptureEffect.breakAmount();
        float fade = SkyRuptureEffect.fade();
        float patternX = SkyRuptureEffect.patternOffsetX();
        float patternY = SkyRuptureEffect.patternOffsetY();
        float skyDarkProgress = DarknessDevourEffect.skyProgress();
        float skyDarkOpacity = DarknessDevourEffect.skyOpacity();

        // ONE quad, exactly as 1.20.1 had it: the fragment stage loops the twelve star shells itself,
        // so the crack field, the noise, the nebula and the composite are evaluated once per pixel
        // instead of once per shell. Vertices are the 0..1 screen coordinates; the vertex stage maps xy
        // to NDC and puts z on the far plane. The quad is emitted counter-clockwise as seen with +y up,
        // which does not matter because the pipeline disables culling.
        VertexConsumer consumer = buffers.getBuffer(renderType);
        emitVertex(consumer, 0.0f, 0.0f, breakAmount, fade, time, progress,
                skyDarkProgress, skyDarkOpacity, patternX, patternY);
        emitVertex(consumer, 1.0f, 0.0f, breakAmount, fade, time, progress,
                skyDarkProgress, skyDarkOpacity, patternX, patternY);
        emitVertex(consumer, 1.0f, 1.0f, breakAmount, fade, time, progress,
                skyDarkProgress, skyDarkOpacity, patternX, patternY);
        emitVertex(consumer, 0.0f, 1.0f, breakAmount, fade, time, progress,
                skyDarkProgress, skyDarkOpacity, patternX, patternY);
        buffers.endBatch(renderType);
    }

    /**
     * Writes one vertex. The element order must follow
     * {@link SkyRuptureShaders#SKY_RUPTURE_VERTEX_FORMAT}: Position, Color, UV0, UV1, UV2.
     *
     * <p>Every declared element is written, which 26.1.2 requires (a vertex that filled fewer elements
     * than the format declares is rejected). Note that {@code setUv1}/{@code setUv2} take
     * {@code (int, int)} rather than floats - verified with {@code javap} on the patched jar, only
     * {@code addVertex}, {@code setColor}, {@code setUv}, {@code setNormal} and {@code setLineWidth}
     * are float writers - so those two attributes carry 16-bit values that {@code sky_rupture.vsh}
     * decodes as <b>unsigned</b> (the writer narrows to {@code short}, the vertex array binds them as
     * {@code GL_SHORT}, and the shader masks with {@code 0xFFFF}).</p>
     */
    private static void emitVertex(VertexConsumer consumer, float screenX, float screenY,
                                   float breakAmount, float fade, float time, float progress,
                                   float skyDarkProgress, float skyDarkOpacity,
                                   float patternX, float patternY) {
        // Position.z is unused now that one draw covers every shell; it stays 0 so the 32-byte format
        // is still written in full.
        consumer.addVertex(screenX, screenY, 0.0f)
                // COLOR is four normalized bytes, so only the two 0..1 values ride here and the two
                // spare channels are pinned to 1.
                .setColor(breakAmount, fade, 1.0f, 1.0f)
                // UV0 = time (effect seconds) and the rupture progress, both full precision floats.
                .setUv(time, progress)
                // UV1 = the two sky-darkening values, both 0..1, as unsigned 16-bit fixed point.
                .setUv1(quantiseUnit(skyDarkProgress), quantiseUnit(skyDarkOpacity))
                // UV2 = the crack field offset. It covers 0..64, so it is quantised at 1/1023.
                .setUv2(quantisePattern(patternX), quantisePattern(patternY));
    }

    /**
     * Maps a 0..1 value onto the unsigned 16-bit scale {@code sky_rupture.vsh} divides by 65535.
     *
     * <p>The full 0..65535 range is used on purpose: {@code setUv1} narrows the argument to
     * {@code short} and the element is bound as {@code GL_SHORT}, and it is the shader's
     * {@code & 0xFFFF} mask that recovers the unsigned value. Values above 32767 are therefore expected
     * here and must not be clamped away - clamping them is exactly what shut the darkening layer off
     * halfway through its own phase (see {@code sky_rupture.vsh}).</p>
     */
    private static int quantiseUnit(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 65535.0f);
    }

    /** Maps a 0..64 crack-offset value onto the 1/1023 scale {@code sky_rupture.vsh} divides by. */
    private static int quantisePattern(float value) {
        return Math.round(Math.max(0.0f, Math.min(64.0f, value)) * 1023.0f);
    }
}
