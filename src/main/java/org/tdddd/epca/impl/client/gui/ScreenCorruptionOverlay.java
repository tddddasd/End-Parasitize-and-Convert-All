package org.tdddd.epca.impl.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.client.effect.BioTortClientState;
import org.tdddd.epca.impl.client.render.araya.ArayaSceneCopy;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortSkillConstants;

/**
 *  2  2 "4 "
 *
 * <h2>""</h2>
 * <p> 1.20.1  grep </p>
 * <ul>
 *   <li><b></b> {@code shaders/post/*.json}  post chain
 *       {@code PostChain} / {@code PostPass} / {@code EffectInstance}
 *       1.20.1  {@code GameRenderer#postEffect}/ PostChain
 *       private  {@code shaders/post/*.json}
 *       {@code RegisterShadersEvent} {@code EpcaShaders} / {@code SkyRuptureShaders}
 *       / {@code ArayaSlashShaders} <b>core shader</b>
 *       ""</li>
 *   <li><b></b>
 *       <ol>
 *         <li>Forge  GUI {@code EnderErosionOverlay}
 *             {@code RegisterGuiOverlaysEvent#registerBelowAll} + {@link IGuiOverlay}
 *              {@code textures/gui/ender_erosion.png}
 *             ""</li>
 *         <li>{@link ArayaSceneCopy} {@code glCopyTexSubImage2D}
 *              {@code ArayaSlashRenderer}
 *             ""</li>
 *       </ol>
 *   </li>
 * </ul>
 * <p>"GUI  + "
 * {@link ArayaSceneCopy} //
 * ""GL Iris
 *  {@link ArayaSceneCopy#isReady()}  false
 * {@code fill} </p>
 *
 * <h2> {@link BioTortClientState} </h2>
 * <pre>
 *   [0, 3) s      +  +  +
 *   [3, 4) s
 *   [4, 5.5) s
 * </pre>
 *
 * <p> {@code registerBelowAll} HUD 1
 *  {@code EnderErosionOverlay} </p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScreenCorruptionOverlay implements IGuiOverlay {

    /**  */
    private static final int BAND_COUNT = 14;

    /** "" 90 / */
    private static final long STUTTER_MILLIS = 90L;

    /**  */
    private static final float BAND_MAX_SHIFT = 0.030F;

    /**  */
    private static final float BAND_MAX_VERTICAL_SHIFT = 3.0F;

    /**  */
    private static final float LINE_MAX_SHIFT = 0.060F;

    /**  */
    private static final int NOISE_MAX_COUNT = 150;

    /**  */
    private static final int BLUR_REGION_COUNT = 3;

    /**  */
    private static final int[][] BLUR_TAPS = {
            {0, 0}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}
    };

    /**  */
    private static final float RED_PEAK_ALPHA = 0.62F;

    /**  */
    private static final int RED_RGB = 0x8E0B0B;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!BioTortClientState.corruptionActive() || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        //  shader  EnderErosionOverlay
        // 0.9  alpha
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            renderCorruption(graphics, screenWidth, screenHeight);
        } catch (Throwable throwable) {
            //  "Rendering overlay"
            // GL
            // ""
            BioTortClientState.stopCorruption();
            epca.LOGGER.warn("[epca-bio-tort] 屏幕崩坏渲染失败，已本地关闭这次演出", throwable);
        }
    }

    /**  {@link #render}  */
    private static void renderCorruption(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float elapsed = BioTortClientState.corruptionElapsedSeconds();
        float decay = decayEnvelope(elapsed);
        if (decay <= 0.0F) {
            return;
        }

        float window = Math.max(0.05F, BioTortClientState.corruptionWindowSeconds());
        // 0 -> 1 0-3
        float strength = smooth01(elapsed / window) * decay;
        //  3  4
        float redStart = BioTortSkillConstants.CORRUPTION_RED_START_TICKS / 20.0F;
        float redRamp = Math.max(0.05F,
                (BioTortSkillConstants.CORRUPTION_TICKS - BioTortSkillConstants.CORRUPTION_RED_START_TICKS) / 20.0F);
        float red = Mth.clamp((elapsed - redStart) / redRamp, 0.0F, 1.0F) * RED_PEAK_ALPHA * decay;

        //  0 /
        if (strength <= 0.002F && red <= 0.01F) {
            return;
        }

        // //
        boolean haveFrame = captureScene(screenWidth, screenHeight);
        // "" step  step
        long step = (long) (elapsed * 1000.0F / STUTTER_MILLIS);

        if (haveFrame) {
            float scaleX = (float) ArayaSceneCopy.width() / screenWidth;
            float scaleY = (float) ArayaSceneCopy.height() / screenHeight;
            drawDisplacedBands(graphics, screenWidth, screenHeight, scaleY, strength, step);
            drawHorizontalLines(graphics, screenWidth, screenHeight, scaleY, strength, step);
            drawBlurRegions(graphics, screenWidth, screenHeight, scaleX, scaleY, strength, step);
        } else {
            // GL  /
            drawFallbackBands(graphics, screenWidth, screenHeight, strength, step);
        }

        drawNoise(graphics, screenWidth, screenHeight, strength, step);

        if (red > 0.01F) {
            int alpha = Mth.clamp(Math.round(red * 255.0F), 0, 255);
            int colour = (alpha << 24) | RED_RGB;
            graphics.fillGradient(0, 0, screenWidth, screenHeight, colour, colour);
        }
    }

    // ===============================================================================================
    // ===============================================================================================

    /**
     *  {@link ArayaSceneCopy}
     *
     * <p> {@code registerBelowAll}"
     * HUD "
     * </p>
     *
     * @return
     */
    private static boolean captureScene(int screenWidth, int screenHeight) {
        try {
            ArayaSceneCopy.captureFrame(BioTortClientState.nextFrameToken());
        } catch (Throwable throwable) {
            //  GL
            epca.LOGGER.debug("[epca-bio-tort] 场景拷贝失败，屏幕崩坏退化为覆盖层", throwable);
            return false;
        }
        return ArayaSceneCopy.isReady() && ArayaSceneCopy.width() > 0 && ArayaSceneCopy.height() > 0;
    }

    /**
     * " step /"
     *
     * <p>" +
     * " {@code strength}  0-3 </p>
     */
    private static void drawDisplacedBands(GuiGraphics graphics, int screenWidth, int screenHeight,
                                           float scaleY, float strength, long step) {
        float bandHeight = (float) screenHeight / BAND_COUNT;
        for (int i = 0; i < BAND_COUNT; i++) {
            int bandY = Math.round(i * bandHeight);
            int bandH = Math.max(1, Math.round(bandHeight));
            float shiftNoise = hash01(step * 31L + i * 7L) * 2.0F - 1.0F;
            float verticalNoise = hash01(step * 131L + i * 11L) * 2.0F - 1.0F;

            int destX = Math.round(shiftNoise * BAND_MAX_SHIFT * screenWidth * strength);
            int destY = bandY + Math.round(verticalNoise * BAND_MAX_VERTICAL_SHIFT * strength);
            if (strength <= 0.001F) {
                destY = bandY;
            }

            drawFrameSlice(graphics, destX, destY, bandY, screenWidth, bandH, scaleY);
        }
    }

    /** /"" */
    private static void drawHorizontalLines(GuiGraphics graphics, int screenWidth, int screenHeight,
                                            float scaleY, float strength, long step) {
        int lineCount = 2 + Math.round(strength * 4.0F);
        for (int i = 0; i < lineCount; i++) {
            int lineY = Mth.clamp(Math.round(hash01(step * 101L + i * 13L) * screenHeight), 0, screenHeight - 1);
            int lineHeight = 1 + Math.round(hash01(step * 211L + i * 5L) * 3.0F);
            float shiftNoise = hash01(step * 307L + i * 17L) * 2.0F - 1.0F;
            int destX = Math.round(shiftNoise * LINE_MAX_SHIFT * screenWidth * strength);
            drawFrameSlice(graphics, destX, lineY, lineY, screenWidth, lineHeight, scaleY);

            boolean bright = hash01(step * 401L + i * 23L) > 0.5F;
            int alpha = Mth.clamp(Math.round(strength * 0.45F * 255.0F), 0, 255);
            if (alpha > 0) {
                graphics.fill(0, lineY, screenWidth, lineY + 1, (alpha << 24) | (bright ? 0xFFFFFF : 0x101018));
            }
        }
    }

    /**
     *  5  0.3
     *  {@link GuiGraphics#setColor}
     */
    private static void drawBlurRegions(GuiGraphics graphics, int screenWidth, int screenHeight,
                                        float scaleX, float scaleY, float strength, long step) {
        float alpha = Mth.clamp(strength * 0.30F, 0.0F, 0.5F);
        if (alpha <= 0.02F) {
            return;
        }
        for (int r = 0; r < BLUR_REGION_COUNT; r++) {
            float nx = hash01(step * 53L + r * 19L);
            float ny = hash01(step * 71L + r * 23L);
            float nw = hash01(step * 89L + r * 29L);
            float nh = hash01(step * 97L + r * 37L);

            int width = Math.max(8, Math.round(screenWidth * (0.18F + 0.22F * nw)));
            int height = Math.max(8, Math.round(screenHeight * (0.10F + 0.15F * nh)));
            int baseX = Math.round((screenWidth - width) * nx);
            int baseY = Math.round((screenHeight - height) * ny);
            if (strength <= 0.001F) {
                continue;
            }

            graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            for (int[] tap : BLUR_TAPS) {
                int tapX = Mth.clamp(baseX + tap[0], 0, Math.max(0, screenWidth - width));
                int tapY = Mth.clamp(baseY + tap[1], 0, Math.max(0, screenHeight - height));
                graphics.blit(ArayaSceneCopy.TEXTURE_ID, tapX, tapY, baseX * scaleX, baseY * scaleY,
                        width, height, ArayaSceneCopy.width(), ArayaSceneCopy.height());
            }
            // setColor  shader /
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     *  {@code fill}  alpha
     *
     */
    private static void drawNoise(GuiGraphics graphics, int screenWidth, int screenHeight,
                                 float strength, long step) {
        int count = Math.round(NOISE_MAX_COUNT * Mth.clamp(strength, 0.0F, 1.0F));
        for (int i = 0; i < count; i++) {
            float n1 = hash01(step * 601L + i * 7L);
            float n2 = hash01(step * 607L + i * 11L);
            float n3 = hash01(step * 613L + i * 13L);
            float n4 = hash01(step * 617L + i * 17L);
            int x = Math.round(n1 * screenWidth);
            int y = Math.round(n2 * screenHeight);
            int w = 2 + Math.round(n3 * 4.0F);
            int h = 1 + Math.round(n4 * 3.0F);
            int alpha = Mth.clamp(Math.round((0.10F + 0.45F * n3) * strength * 255.0F), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            int rgb = n4 > 0.75F ? 0xFFFFFF : (n4 > 0.5F ? 0x909090 : (n4 > 0.25F ? 0x3A0A4A : 0x080808));
            graphics.fill(x, y, x + w, y + h, (alpha << 24) | rgb);
        }
    }

    /** "" */
    private static void drawFallbackBands(GuiGraphics graphics, int screenWidth, int screenHeight,
                                          float strength, long step) {
        float bandHeight = (float) screenHeight / BAND_COUNT;
        for (int i = 0; i < BAND_COUNT; i++) {
            float n = hash01(step * 37L + i * 7L);
            int bandY = Math.round(i * bandHeight);
            int bandH = Math.max(1, Math.round(bandHeight));
            int destX = Math.round((n * 2.0F - 1.0F) * BAND_MAX_SHIFT * screenWidth * strength);
            int alpha = Mth.clamp(Math.round(strength * (0.10F + 0.30F * n) * 255.0F), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            int rgb = n > 0.66F ? 0x2A0A38 : (n > 0.33F ? 0x120018 : 0x3A1030);
            graphics.fill(destX, bandY, destX + screenWidth, bandY + bandH, (alpha << 24) | rgb);
        }
    }

    /**  GUI */
    private static void drawFrameSlice(GuiGraphics graphics, int destX, int destY, int sourceY,
                                       int width, int height, float scaleY) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float v = sourceY * scaleY;
        graphics.blit(ArayaSceneCopy.TEXTURE_ID, destX, destY, 0.0F, v,
                width, height, ArayaSceneCopy.width(), ArayaSceneCopy.height());
    }

    // ===============================================================================================
    // ===============================================================================================

    /**  1 0"" */
    private static float decayEnvelope(float elapsed) {
        float window = BioTortClientState.corruptionWindowSeconds();
        float decay = Math.max(0.001F, BioTortClientState.corruptionDecaySeconds());
        if (elapsed <= window) {
            return 1.0F;
        }
        return 1.0F - smooth01((elapsed - window) / decay);
    }

    private static float smooth01(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /**
     *  {@code (step, index)}  {@code [0,1)}
     *
     * <p> {@code ArayaConstants#positionHash}
     * ""</p>
     */
    private static float hash01(long value) {
        long h = value * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (float) ((h >>> 40) & 0xFFFFFFL) / (float) 0x1000000L;
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        //  EnderErosionOverlay  HUD //
        event.registerBelowAll("bio_tort_screen_corruption", new ScreenCorruptionOverlay());
    }
}

