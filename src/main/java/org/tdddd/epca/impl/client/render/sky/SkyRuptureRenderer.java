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
 * 把世界结界破损画到天空上。
 *
 * <h3>画在哪一层（★ 这里是关键，改过一次）</h3>
 * 在 Forge 的 {@code RenderLevelStageEvent.Stage.AFTER_SKY} 阶段绘制 ——
 * 也就是 {@code LevelRenderer.renderLevel()} 里 {@code renderSky()} 调用<b>刚返回</b>、
 * 地形还没开始画的那一刻（已核对 1.20.1 字节码：{@code renderSky} 在 438，
 * Forge 的 AFTER_SKY 派发在 441，第一个 {@code renderChunkLayer} 在 557）：
 *
 * <pre>
 *   天光（renderSky） → 结界破损 → 地形/实体/云/雨 → 手 → HUD
 * </pre>
 *
 * <h3>⚠️ 为什么不能等世界画完再画（直线的真正来源）</h3>
 * 最早的实现在 {@code GameRenderer.renderLevel()} 里、世界全部画完之后绘制，
 * 靠「顶点放在最远平面 z=1 + 深度测试 {@code LEQUAL}」把效果遮罩到天空像素。
 * 这个思路本身没错，但踩了一个隐蔽的坑：
 *
 * <blockquote>
 * <b>原版云会把深度写满整块云几何，包括完全透明的纹素。</b>
 * 云用的 {@code RenderType} 是 {@code TRANSLUCENT_TRANSPARENCY + COLOR_DEPTH_WRITE}，
 * 没有 alpha test —— 所以云平面的每一个四边形都写深度。
 * 而云平面是一块以玩家为中心的方形区域、高度恒定，
 * 它的<b>外边界投影到屏幕上就是一条笔直的水平线</b>。
 * </blockquote>
 *
 * <p>结果：那条线以上的天空我们的四边形全部被深度测试挡掉、线以下正常，
 * 于是「像有一根直线把画面切成两半」，天空盒上到处是莫名其妙的遮挡。
 * 地形、实体的轮廓遮挡也是同一个原因。</p>
 *
 * <p><b>修法：改成画在天空之后、地形之前。</b>此时深度缓冲还是全清空值
 * （{@code renderSky} 全程 {@code depthMask(false)}，不写深度），
 * 我们的层照样只落在天空像素上；而地形、实体、云随后画在<b>我们上面</b> ——
 * 既不会被云平面劈成两半，遮挡关系也天然正确。</p>
 *
 * <h3>为什么它贴在天空上</h3>
 * <ol>
 *   <li>顶点放在 NDC 最远平面（{@code z = 1}），深度测试 {@code LEQUAL}。</li>
 *   <li>每个像素先用相机基向量重建<b>世界方向</b>，再用世界方向算裂缝图案：
 *       转动视角时裂缝跟着天空平移，而不是黏在屏幕上。</li>
 * </ol>
 *
 * <h3>光影（Iris / Oculus）兼容</h3>
 * 无光影时按上面的逻辑在 AFTER_SKY 画。光影激活时整个场景被写进 GBuffer，
 * 在这个时刻直写主 framebuffer 会被后面的 composite 覆盖掉 —— 所以那种情况
 * 只推进进度，绘制推迟到 {@code renderLevel()} 末尾
 * （{@code renderDeferred()}，见 {@code GameRendererShaderLayerMixin}）：
 * 那时绑定 {@code mainRenderTarget} 直写主 framebuffer，绕过 GBuffer、
 * 也不被光影重新着色。
 */
public final class SkyRuptureRenderer {

    private SkyRuptureRenderer() {
    }

    /**
     * 每帧一次推进效果进度。
     *
     * <p>挂在 {@code GameRenderer.render(FJZ)} 的 HEAD 上（一次也没多），
     * <b>不能</b>挂在 {@code AFTER_SKY} 上：光影的 shadow pass 会把
     * {@code LevelRenderer.renderLevel()} 再跑一遍，那样进度会变成双倍速。
     * 未激活时是空操作。</p>
     */
    public static void update() {
        SkyRuptureEffect.update();
    }

    /**
     * 每帧一次，在 {@code RenderLevelStageEvent.Stage.AFTER_SKY} 调用：
     * 天光之后、地形之前的绘制时机。没有光影时在这里画。
     */
    public static void renderAfterSky() {
        if (!IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    /**
     * 每帧一次，在 {@code GameRenderer.renderLevel()} 的世界部分之后调用。
     *
     * <p>只有光影激活时才需要在这里补画（见类文档「光影兼容」）。</p>
     */
    public static void renderDeferred() {
        if (IrisShaderCompat.isShaderPackActive()) {
            drawIfActive();
        }
    }

    /** 把全屏四边形画出去（进度已经由 {@link #update()} 推进过）。 */
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

        // 星点 sprite 是通过 uniform 采样的，Embeddium 看不到 → 每帧手动标记活跃，
        // 否则它们的动画帧不会推进（与参考项目同样的处理）
        SkyRuptureShaders.markSpritesActive();

        applyCameraUniforms(mc.gameRenderer.getMainCamera());
        applyEffectUniforms();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        LateRenderState.prepareMainTargetPass();
        try {
            VertexConsumer consumer = buffers.getBuffer(SkyRuptureRenderType.SKY_RUPTURE);
            // 顶点就是 0..1 的屏幕坐标：顶点着色器把 xy 映射到 NDC，z 放到最远平面
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
     * 相机基向量 —— 着色器用它把每个像素换算成世界方向。
     *
     * <p>{@code rayRight / rayUp} 顺便把 {@code tan(fov/2)} 乘进去，
     * 于是着色器里一句 {@code normalize(forward + right * ndc.x + up * ndc.y)}
     * 就是该像素的方向，不需要任何逆矩阵。</p>
     */
    private static void applyCameraUniforms(Camera camera) {
        Vector3f forward = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        // 从投影矩阵反推 tan(fov/2)：perspective() 里 m11 = 1/tan(fovY/2)，m00 = 1/(tan(fovY/2)*aspect)
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
        // 万一是正交投影之类的异常情况，退回一个合理视角，别让整个天空糊掉
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

        // 裂纹场的随机偏移：每次触发的碎片布局都不一样
        setVec2(SkyRuptureShaders.uPatternOffset,
                SkyRuptureEffect.patternOffsetX(), SkyRuptureEffect.patternOffsetY());

        setVec3(SkyRuptureShaders.uRimColor,
                SkyRuptureEffect.rimRed(), SkyRuptureEffect.rimGreen(), SkyRuptureEffect.rimBlue());
        setVec3(SkyRuptureShaders.uVoidColor,
                SkyRuptureEffect.voidRed(), SkyRuptureEffect.voidGreen(), SkyRuptureEffect.voidBlue());
        setVec3(SkyRuptureShaders.uFlashColor,
                SkyRuptureEffect.flashRed(), SkyRuptureEffect.flashGreen(), SkyRuptureEffect.flashBlue());

        // 12 张星点 sprite 的 UV 矩形（图集缝合时解析好，这里每帧上传）
        setFloatArray(SkyRuptureShaders.uCosmicUvs, SkyRuptureShaders.COSMIC_UVS);

        // 黑暗吞噬（天空部分）：从顶部扩散下来的黑暗，画在最底层
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
