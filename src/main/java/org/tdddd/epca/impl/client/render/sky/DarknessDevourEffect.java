package org.tdddd.epca.impl.client.render.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.Mth;

/**
 * Darkness devour: the sky darkens first, then the barrier cracks, and finally the ground is eaten
 * as well.
 *
 * <h2>Three phases</h2>
 * <ol>
 *   <li><b>Darkening (a separate leading phase)</b>: darkness spreads down from the <b>zenith</b>
 *       along {@code dir.y} until even below the horizon is black. This phase is <b>appended in front
 *       of</b> the performance and the rupture progress is identically 0 throughout, so no crack can
 *       appear early. It is drawn by the sky shader, not by fog.</li>
 *   <li><b>Rupture</b>: only starts once the sky is fully dark. Crack void, rims, bright lines and
 *       the shock wave all composite on top of the darkened sky inside the sky shader.</li>
 *   <li><b>Ground (takes over the fog)</b>: in the middle of the rupture phase the fog closes in from
 *       the world edge to under the player's feet, eating the blocks too.</li>
 * </ol>
 *
 * <h2>Why the sky cannot use fog</h2>
 * Fog distance is measured from the camera and the world edge is exactly the horizon ring, so "the
 * edge went black but the sky overhead is still bright" looks wrong. The sky therefore uses a
 * direction threshold ({@code dir.y}) in the shader, which reads as darkness dropping from overhead.
 *
 * <h2>Timeline</h2>
 * <pre>
 *   [0, D)                darkening: darkProgress 0->1, rupture progress identically 0
 *   [D, D+R)              rupture: rupture progress 0->1 (ground darkness sweeps in over
 *                         0.28 -> 0.72 of that phase, and back out over 0.84 -> 1.0)
 *   D = darkening duration (2.5s at stage 1, +0.25s per stage, at most 5s)
 *   R = rupture duration   (5s at stage 1,   +2.5s per stage,  at most 25s)
 *   total = D + R (the darkening phase is additional)
 * </pre>
 *
 * <h2>1.20.1 -&gt; 26.1.2: how the fog is applied</h2>
 * The 1.20.1 twin called {@code RenderSystem.setShaderFogStart/End/Color} inside
 * {@code FogRenderer.setupFog}. Neither those setters nor a fog uniform in {@code RenderSystem} exists
 * any more. 26.1.2 builds a {@link FogData} value object in
 * {@code FogRenderer#setupFog(Camera, int, DeltaTracker, float, ClientLevel)}, packs it into a UBO
 * with {@code FogRenderer#updateBuffer(FogData)}, and the chunk/entity shaders read the {@code Fog}
 * std140 block (see {@code assets/minecraft/shaders/include/fog.glsl}, which has
 * {@code FogEnvironmentalStart/End}, {@code FogRenderDistanceStart/End}, {@code FogSkyEnd},
 * {@code FogCloudsEnd} and {@code FogColor}).
 *
 * <p>{@code FogRendererDevourMixin} therefore injects at the RETURN of {@code setupFog} and writes
 * the same numbers into the returned {@link FogData}: the 1.20.1 "start" becomes
 * {@code environmentalStart}, the 1.20.1 "end" becomes {@code environmentalEnd}, and the colour goes
 * to {@code color}. The values themselves - including the deliberately negative front, which is what
 * eats the blocks underfoot and the hand - are unchanged.</p>
 *
 * <p>Both the environmental band and the render-distance band are overwritten, because 26.1.2's
 * {@code total_fog_value} takes the <em>max</em> of the two.</p>
 */
public final class DarknessDevourEffect {

    /** Time window of the ground darkness (fog) inside the <b>rupture</b> phase. */
    private static final float GROUND_RISE_START = 0.28f;
    private static final float GROUND_RISE_END = 0.72f;
    private static final float GROUND_FALL_START = 0.84f;
    private static final float GROUND_FALL_END = 1.00f;

    private DarknessDevourEffect() {
    }

    // -- sky: spreads down from the top (a separate leading phase) -------------

    /**
     * Advance of the sky darkness front (0 = not started, 1 = even below the horizon is black).
     *
     * <p>It is exactly {@link SkyRuptureEffect#darkProgress()}: the darkening is a <b>separate leading
     * phase</b> whose duration is added in front of the rupture, so the sky goes fully dark first and
     * only then starts to crack.</p>
     */
    public static float skyProgress() {
        return SkyRuptureEffect.darkProgress();
    }

    /**
     * Opacity of the sky darkness: it lags the front slightly so "going dark" is a gradual thickening
     * rather than an instant black wall once the front arrives. After the darkening phase it is
     * constantly 1 (the sky stays dark) until the whole performance fades out through
     * {@link SkyRuptureEffect#fade()}.
     */
    public static float skyOpacity() {
        return Mth.clamp(skyProgress() * 1.15f, 0f, 1f);
    }

    // -- ground: takes over the fog -------------------------------------------

    /** How far the ground has been devoured: 0 = not at all, 1 = the darkness has reached the player. */
    public static float devour() {
        return window(GROUND_RISE_START, GROUND_RISE_END, GROUND_FALL_START, GROUND_FALL_END);
    }

    /**
     * Takes over the terrain fog. Called by {@code FogRendererDevourMixin} from the RETURN of
     * {@code FogRenderer#setupFog}. That method runs once per frame and is idempotent.
     *
     * @param fog            the fog values vanilla just computed for this frame
     * @param renderDistance the render distance vanilla used (the position of the world edge)
     */
    public static void applyFog(FogData fog, float renderDistance) {
        float d = devour();
        if (d <= 0.002f) {
            // Not taking over: keep the vanilla fog (nether, underwater and potion fog untouched).
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isUnderWater() || mc.player.isInLava())) {
            // Underwater / lava have their own fog; do not fight them.
            return;
        }

        float far = Math.max(renderDistance, 24.0f);
        // Front: closes in from the world edge (~far) all the way to a NEGATIVE value.
        // A negative fog start is essential: vanilla fog is "the closer, the less affected", and only
        // pushing the start below zero also eats the blocks underfoot and even the hand; otherwise a
        // ring of ground always stays lit and "the ground turns black too" is never reached.
        float front = Mth.lerp(d, far * 1.05f, -8.0f);
        // Soft edge: wide at first (the distance darkens naturally), narrow once fully closed
        // (everything in front of the eyes goes black at once).
        float softness = Mth.lerp(d, far * 0.30f, 10.0f);

        float start = front;
        float end = start + Math.max(softness, 2.5f);

        fog.environmentalStart = start;
        fog.environmentalEnd = end;
        fog.renderDistanceStart = start;
        fog.renderDistanceEnd = end;
        // Same colour as the sky darkness and the crack void, so "the darkness outside" and "the void
        // inside the cracks" are visibly the same thing. The alpha is left as vanilla computed it.
        fog.color.set(SkyRuptureEffect.voidRed(), SkyRuptureEffect.voidGreen(),
                SkyRuptureEffect.voidBlue(), fog.color.w);
    }

    /** Time window: rises to 1 over the rise segment, holds, then falls back to 0. */
    private static float window(float riseStart, float riseEnd, float fallStart, float fallEnd) {
        if (!SkyRuptureEffect.isActive()) {
            return 0f;
        }
        float u = SkyRuptureEffect.progress();
        float rise = smooth01((u - riseStart) / (riseEnd - riseStart));
        float fall = smooth01((u - fallStart) / (fallEnd - fallStart));
        return rise * (1.0f - fall);
    }

    private static float smooth01(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }
}
