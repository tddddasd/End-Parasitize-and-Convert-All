package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 *
 *
 * <h3></h3>
 * <ol>
 *   <li><b></b><b></b> {@code dir.y}
 *       <b></b>
 *        0  </li>
 *   <li><b></b> /  /  /
 *         ""</li>
 *   <li><b></b></li>
 * </ol>
 *
 * <h3></h3>
 * ""
 * ""
 * {@code dir.y}
 *
 * <h3></h3>
 * <pre>
 *   [0, D)                 darkProgress 01 0
 *   [D, D+R)                01 0.280.72
 *   D = 1  2.5s +0.25s 5s
 *   R = 1  5s   +2.5s  25s
 *    = D + R
 * </pre>
 */
public final class DarknessDevourEffect {

    /** **** */
    private static final float GROUND_RISE_START = 0.28f;
    private static final float GROUND_RISE_END = 0.72f;
    private static final float GROUND_FALL_START = 0.84f;
    private static final float GROUND_FALL_END = 1.00f;

    private DarknessDevourEffect() {
    }


    /**
     * 0 = 1 =
     *
     * <p> {@link SkyRuptureEffect#darkProgress()}****
     * </p>
     */
    public static float skyProgress() {
        return SkyRuptureEffect.darkProgress();
    }

    /**
     * ""
     *  1
     *  {@code fade()}
     */
    public static float skyOpacity() {
        return Mth.clamp(skyProgress() * 1.15f, 0f, 1f);
    }


    /**
     * 0 = 1 =
     */
    public static float devour() {
        return window(GROUND_RISE_START, GROUND_RISE_END, GROUND_FALL_START, GROUND_FALL_END);
    }

    /**
     *  {@code FogRendererDevourMixin}  {@code FogRenderer.setupFog}
     *
     *
     * @param renderDistance
     */
    public static void applyFog(float renderDistance) {
        float d = devour();
        if (d <= 0.002f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.isUnderWater() || mc.player.isInLava())) {
            // /
            return;
        }

        float far = Math.max(renderDistance, 24.0f);
        // (far) ****
        //  fogStart ""
        // ""
        float front = Mth.lerp(d, far * 1.05f, -8.0f);
        float softness = Mth.lerp(d, far * 0.30f, 10.0f);

        float start = front;
        float end = start + Math.max(softness, 2.5f);

        RenderSystem.setShaderFogStart(start);
        RenderSystem.setShaderFogEnd(end);
        // """"
        RenderSystem.setShaderFogColor(
                SkyRuptureEffect.voidRed(), SkyRuptureEffect.voidGreen(), SkyRuptureEffect.voidBlue());
    }

    /** rise  1fall  0 */
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

