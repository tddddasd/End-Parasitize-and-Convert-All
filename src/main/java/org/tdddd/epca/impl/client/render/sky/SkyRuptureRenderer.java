package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;

public final class SkyRuptureRenderer {

    private SkyRuptureRenderer() {
    }

    public static void update() {
        SkyRuptureEffect.update();
    }

    public static void renderAfterSky() {
        if (!IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    public static void renderDeferred() {
        if (IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    private static void drawIfActive() {
        if (!SkyRuptureEffect.isActive() || !SkyRuptureShaders.isPipelineRegistered()) {
            return;
        }

        if (SkyRuptureEffect.fade() <= 0.002f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        RenderType renderType = SkyRuptureShaders.getRenderType();
        if (renderType == null) {

            return;
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        float time = SkyRuptureEffect.elapsedSeconds();
        float progress = SkyRuptureEffect.progress();
        float breakAmount = SkyRuptureEffect.breakAmount();
        float fade = SkyRuptureEffect.fade();
        float patternX = SkyRuptureEffect.patternOffsetX();
        float patternY = SkyRuptureEffect.patternOffsetY();
        float skyDarkProgress = DarknessDevourEffect.skyProgress();
        float skyDarkOpacity = DarknessDevourEffect.skyOpacity();

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

    private static void emitVertex(VertexConsumer consumer, float screenX, float screenY,
                                   float breakAmount, float fade, float time, float progress,
                                   float skyDarkProgress, float skyDarkOpacity,
                                   float patternX, float patternY) {

        consumer.addVertex(screenX, screenY, 0.0f)

                .setColor(breakAmount, fade, 1.0f, 1.0f)

                .setUv(time, progress)

                .setUv1(quantiseUnit(skyDarkProgress), quantiseUnit(skyDarkOpacity))

                .setUv2(quantisePattern(patternX), quantisePattern(patternY));
    }

    private static int quantiseUnit(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 65535.0f);
    }

    private static int quantisePattern(float value) {
        return Math.round(Math.max(0.0f, Math.min(64.0f, value)) * 1023.0f);
    }
}
