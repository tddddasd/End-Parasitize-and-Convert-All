package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.compat.LateRenderState;

/**
 *
 *
 * <h3> </h3>
 *  Forge  {@code RenderLevelStageEvent.Stage.AFTER_SKY}
 *  {@code LevelRenderer.renderLevel()}  {@code renderSky()} <b></b>
 *  1.20.1 {@code renderSky}  438
 * Forge  AFTER_SKY  441 {@code renderChunkLayer}  557
 *
 * <pre>
 *   renderSky    ///    HUD
 * </pre>
 *
 * <h3> </h3>
 *  {@code GameRenderer.renderLevel()}
 *  z=1 +  {@code LEQUAL}
 *
 *
 * <blockquote>
 * <b></b>
 *  {@code RenderType}  {@code TRANSLUCENT_TRANSPARENCY + COLOR_DEPTH_WRITE}
 *  alpha test
 *
 * <b></b>
 * </blockquote>
 *
 * <p>
 *
 * </p>
 *
 * <p><b></b>
 * {@code renderSky}  {@code depthMask(false)}
 * <b></b>
 * </p>
 *
 * <h3></h3>
 * <ol>
 *   <li> NDC {@code z = 1} {@code LEQUAL}</li>
 *   <li><b></b>
 *       </li>
 * </ol>
 *
 * <h3>Iris / Oculus</h3>
 *  AFTER_SKY  GBuffer
 *  framebuffer  composite
 *  {@code renderLevel()}
 * {@code renderDeferred()} {@code GameRendererShaderLayerMixin}
 *  {@code mainRenderTarget}  framebuffer GBuffer
 *
 */
public final class SkyRuptureRenderer {

    private SkyRuptureRenderer() {
    }

    /**
     *
     *
     * <p> {@code GameRenderer.render(FJZ)}  HEAD
     * <b></b> {@code AFTER_SKY}  shadow pass
     * {@code LevelRenderer.renderLevel()}
     * </p>
     */
    public static void update() {
        SkyRuptureEffect.update();
    }

    /**
     *  {@code RenderLevelStageEvent.Stage.AFTER_SKY}
     *
     */
    public static void renderAfterSky() {
        if (!IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    /**
     *  {@code GameRenderer.renderLevel()}
     *
     * <p></p>
     */
    public static void renderDeferred() {
        if (IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    /**  {@link #update()}  */
    private static void drawIfActive() {
        if (!SkyRuptureEffect.isActive()) {
            return;
        }

        ShaderInstance shader = SkyRuptureShaders.skyRuptureShader;
        if (shader == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        //  sprite  uniform Embeddium
        SkyRuptureShaders.markSpritesActive();

        applyCameraUniforms(mc.gameRenderer.getMainCamera());
        applyEffectUniforms();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        LateRenderState.prepareMainTargetPass();
        try {
            VertexConsumer consumer = buffers.getBuffer(SkyRuptureRenderType.SKY_RUPTURE);
            //  0..1  xy  NDCz
            consumer.vertex(0.0f, 0.0f, 0.0f).endVertex();
            consumer.vertex(1.0f, 0.0f, 0.0f).endVertex();
            consumer.vertex(1.0f, 1.0f, 0.0f).endVertex();
            consumer.vertex(0.0f, 1.0f, 0.0f).endVertex();
            buffers.endBatch(SkyRuptureRenderType.SKY_RUPTURE);
        } finally {
            LateRenderState.finishMainTargetPass();
        }
    }

    /**
     *
     *
     * <p>{@code rayRight / rayUp}  {@code tan(fov/2)}
     *  {@code normalize(forward + right * ndc.x + up * ndc.y)}
     * </p>
     */
    private static void applyCameraUniforms(Camera camera) {
        Vector3f forward = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        //  tan(fov/2)perspective()  m11 = 1/tan(fovY/2)m00 = 1/(tan(fovY/2)*aspect)
        float tanY = 1.0f;
        float tanX = 1.7778f;
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        if (projection != null) {
            float m11 = projection.m11();
            float m00 = projection.m00();
            if (Math.abs(m11) > 1.0e-4f) {
                tanY = 1.0f / m11;
            }
            if (Math.abs(m00) > 1.0e-4f) {
                tanX = 1.0f / m00;
            }
        }
        tanY = Mth.clamp(tanY, 0.05f, 10.0f);
        tanX = Mth.clamp(tanX, 0.05f, 10.0f);

        setVec3(SkyRuptureShaders.uRayForward, forward.x(), forward.y(), forward.z());
        setVec3(SkyRuptureShaders.uRayRight, -left.x() * tanX, -left.y() * tanX, -left.z() * tanX);
        setVec3(SkyRuptureShaders.uRayUp, up.x() * tanY, up.y() * tanY, up.z() * tanY);
    }

    private static void applyEffectUniforms() {
        setFloat(SkyRuptureShaders.uTime, SkyRuptureEffect.elapsedSeconds());
        setFloat(SkyRuptureShaders.uProgress, SkyRuptureEffect.progress());
        setFloat(SkyRuptureShaders.uBreakAmount, SkyRuptureEffect.breakAmount());
        setFloat(SkyRuptureShaders.uFade, SkyRuptureEffect.fade());
        setFloat(SkyRuptureShaders.uSeed, SkyRuptureEffect.seed());

        setVec2(SkyRuptureShaders.uPatternOffset,
                SkyRuptureEffect.patternOffsetX(), SkyRuptureEffect.patternOffsetY());

        setVec3(SkyRuptureShaders.uRimColor,
                SkyRuptureEffect.rimRed(), SkyRuptureEffect.rimGreen(), SkyRuptureEffect.rimBlue());
        setVec3(SkyRuptureShaders.uVoidColor,
                SkyRuptureEffect.voidRed(), SkyRuptureEffect.voidGreen(), SkyRuptureEffect.voidBlue());
        setVec3(SkyRuptureShaders.uFlashColor,
                SkyRuptureEffect.flashRed(), SkyRuptureEffect.flashGreen(), SkyRuptureEffect.flashBlue());

        // 12  sprite  UV
        setFloatArray(SkyRuptureShaders.uCosmicUvs, SkyRuptureShaders.COSMIC_UVS);

        setFloat(SkyRuptureShaders.uSkyDarkProgress, DarknessDevourEffect.skyProgress());
        setFloat(SkyRuptureShaders.uSkyDarkOpacity, DarknessDevourEffect.skyOpacity());
    }

    private static void setFloatArray(Uniform uniform, float[] values) {
        if (uniform != null) {
            uniform.set(values);
        }
    }

    private static void setFloat(Uniform uniform, float value) {
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setVec2(Uniform uniform, float x, float y) {
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private static void setVec3(Uniform uniform, float x, float y, float z) {
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }
}

