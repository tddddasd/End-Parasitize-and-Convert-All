package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.HashMap;
import java.util.Map;

public abstract class ArayaFireRenderType extends RenderType {

    public static final int ALPHA_STEPS = 8;

    private static final RenderType[] BY_ALPHA = new RenderType[ALPHA_STEPS + 1];

    private ArayaFireRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                boolean affectsCrumbling, boolean sortOnUpload,
                                Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType forOpacity(float opacity) {
        int step = Math.round(Math.max(0.0F, Math.min(1.0F, opacity)) * ALPHA_STEPS);
        if (step <= 0) {
            step = 1;
        }
        RenderType existing = BY_ALPHA[step];
        if (existing != null) {
            return existing;
        }
        float alpha = step / (float) ALPHA_STEPS;
        RenderType created = RenderType.create(
                "epca_araya_fire_blocks_" + step,
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                TextureAtlas.LOCATION_BLOCKS, false, false))

                        .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                                "araya_fire_fade_" + step, () -> {
                                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                                    com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                                            com.mojang.blaze3d.platform.GlStateManager.SourceFactor
                                                    .SRC_ALPHA,
                                            com.mojang.blaze3d.platform.GlStateManager.DestFactor
                                                    .ONE_MINUS_SRC_ALPHA,
                                            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                                            com.mojang.blaze3d.platform.GlStateManager.DestFactor
                                                    .ONE_MINUS_SRC_ALPHA);
                                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F,
                                            1.0F, alpha);
                                }, () -> {
                                    com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F,
                                            1.0F, 1.0F);
                                }))
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP)
                        .createCompositeState(true));
        BY_ALPHA[step] = created;
        return created;
    }
}
