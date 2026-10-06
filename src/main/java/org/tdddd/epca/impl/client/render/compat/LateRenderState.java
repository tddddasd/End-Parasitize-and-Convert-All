package org.tdddd.epca.impl.client.render.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

/**
 *  GL  RottenRuinsSplendiding
 * {@code LateOutlineRenderState}
 *
 * <p> GBuffer
 * framebuffer/ scissor
 * blend </p>
 *
 * <pre>{@code
 * LateRenderState.prepareMainTargetPass();
 * try {
 *     // ...  ...
 * } finally {
 *     LateRenderState.finishMainTargetPass();
 * }
 * }</pre>
 */
public final class LateRenderState {

    private LateRenderState() {
    }

    /**
     *  framebuffer +  GL
     * <ul>
     *   <li> render target   GBuffer</li>
     *   <li> scissor  </li>
     *   <li>  LEQUAL </li>
     *   <li> shader color blend</li>
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
     * / pass
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

