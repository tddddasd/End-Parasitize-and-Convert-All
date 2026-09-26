package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.HashMap;
import java.util.Map;

/**
 * The render types the render-only fire blocks are drawn through.
 *
 * <h2>Why it is not simply {@code RenderType.cutout()}</h2>
 * <p>Two reasons, both about the fact that these fires do not exist in the world:</p>
 * <ul>
 *   <li><b>culling off</b>: the vanilla {@code minecraft:fire} model is two crossed planes seen from
 *       both sides. The chunk renderer gets away with culling because the blockstate's {@code multipart}
 *       only ever presents the quads facing the viewer; re-emitting the same model from a plain per-frame
 *       draw needs {@code NO_CULL}.</li>
 *   <li><b>alpha blending instead of cutout</b>: vanilla cutout discards fragments below an alpha
 *       threshold, which makes a partial fade impossible - half-transparent pixels would simply vanish.
 *       The fire sprite's alpha is binary, so blending shows exactly the same texture, and it also lets
 *       each fire carry its own opacity.</li>
 * </ul>
 *
 * <h2>Why there is a small family of them</h2>
 * <p>A {@code RenderType} carries one blend state, and the opacity has to be part of the blend state
 * because the vertex colour's alpha is multiplied by vanilla's own {@code ColorModulator}, which every
 * {@code RenderType#draw} resets to {@code (1,1,1,1)}. Rather than fight that, the fade is quantised
 * into {@link #ALPHA_STEPS} steps and one render type is created per step, lazily and at most once. A
 * fire therefore picks its render type from its own opacity, and the fade is drawn by the pipeline
 * instead of by a per-fire uniform. The step count is small enough that the batching cost is
 * irrelevant for the dozen fires a holder keeps alive.</p>
 *
 * <p>Like {@code SkyRuptureRenderType}, this class extends {@link RenderType} purely to reach the
 * {@code protected} state shards.</p>
 */
public abstract class ArayaFireRenderType extends RenderType {

    /** Number of discrete opacity levels the fade is drawn at; 1.0 is the top step. */
    public static final int ALPHA_STEPS = 8;

    private static final RenderType[] BY_ALPHA = new RenderType[ALPHA_STEPS + 1];

    /** This class is never instantiated; the constructor only satisfies the parent's signature. */
    private ArayaFireRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                boolean affectsCrumbling, boolean sortOnUpload,
                                Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /** The render type whose constant blend factor is closest to the given opacity. */
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
                        // Constant-alpha blending: the fragment keeps the fire texture's own alpha, and
                        // this factor is what fades the whole fire in and out.
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
