package org.tdddd.epca.impl.client.render.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

/**
 * 延迟回放时的 GL 状态守卫，移植自 RottenRuinsSplendiding 的
 * {@code LateOutlineRenderState}。
 *
 * <p>光影激活时整个场景被写进 GBuffer，我们的自定义层必须绕过它、直写主
 * framebuffer。同时光影/原版管线可能残留了 scissor、深度掩码、颜色掩码、
 * blend 等状态，回放前后都要重置成已知良好值。</p>
 *
 * <pre>{@code
 * LateRenderState.prepareMainTargetPass();
 * try {
 *     // ... 回放所有延迟条目 ...
 * } finally {
 *     LateRenderState.finishMainTargetPass();
 * }
 * }</pre>
 */
public final class LateRenderState {

    private LateRenderState() {
    }

    /**
     * 回放前准备：绑主 framebuffer + 重置关键 GL 状态。
     * <ul>
     *   <li>绑定主 render target —— 这是“绕过光影 GBuffer”的关键一步</li>
     *   <li>关闭 scissor —— 光影常常留着过期的裁剪矩形</li>
     *   <li>开启深度测试与深度写入 —— LEQUAL 需要读到已有深度</li>
     *   <li>打开全部颜色通道、重置 shader color、恢复默认 blend</li>
     * </ul>
     */
    public static void prepareMainTargetPass() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        RenderSystem.disableScissor();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * 回放后清理：把状态复位，保证后续原版/光影 pass 从干净状态开始。
     */
    public static void finishMainTargetPass() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        RenderSystem.disableScissor();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }
}
