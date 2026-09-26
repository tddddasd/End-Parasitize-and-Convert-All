package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.tdddd.epca.impl.epca;

/**
 * The render type the render-only fire blocks are drawn through.
 *
 * <p>It is the vanilla terrain pipeline with the two states these fires need and the chunk renderer does
 * not: <b>culling off</b> (the vanilla {@code minecraft:fire} model is two crossed planes seen from both
 * sides) and <b>translucent blending with depth writes off</b> instead of cutout, so a fire can fade and
 * the two planes blend against each other instead of cutting each other out. The lightmap sampler comes
 * with {@link RenderPipelines#TERRAIN_SNIPPET}, so unlit corners behave like vanilla; every quad is
 * emitted at full brightness anyway (see {@code ArayaFireRenderer}), because a fire is emissive.</p>
 *
 * <p>The vertex data is the vanilla block format, produced by
 * {@code VertexConsumer#putBakedQuad} from the very quads the vanilla fire model bakes - the same data the
 * chunk renderer would submit.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The 1.20.1 twin chose the {@code RENDERTYPE_CUTOUT_SHADER} plus a hand-written state block and had to
 * quantise the fade, because every {@code RenderType#draw} resets the shader colour to white. 26.1.2
 * carries the fade in the translucent blend itself, which is why this pipeline needs no alpha steps.</p>
 */
public final class ArayaFireRenderType {

    /** Render type and pipeline name. */
    public static final String NAME = "epca_araya_fire_blocks";

    /**
     * The fire pipeline: the immediate block-model state ({@code core/block}), translucent, no depth
     * writes, no culling.
     *
     * <p>Deliberately {@code BLOCK_SNIPPET} and NOT {@code TERRAIN_SNIPPET}. The terrain snippet runs
     * {@code core/terrain}, which takes its model-view matrix from the per-chunk-section {@code ChunkSection}
     * uniform buffer and has no {@code DynamicTransforms} at all; ours are immediate quads submitted outside
     * the chunk renderer, so that buffer is never filled for them and the geometry is transformed by
     * whatever the section buffer happened to hold - invisible, and with nothing in the log. The block
     * snippet is the one vanilla itself uses for immediate block-model draws ({@code SOLID_BLOCK} /
     * {@code CUTOUT_BLOCK}): {@code core/block} plus {@code MATRICES_PROJECTION_SNIPPET}, which is exactly
     * the {@code DynamicTransforms} + {@code Projection} pair {@code RenderType#draw} fills in.</p>
     */
    public static final RenderPipeline FIRE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/araya_fire"))
                    .withVertexFormat(net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT
                            .vertexFormat(), VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    private static boolean pipelineRegistered;
    private static RenderType fire;

    private ArayaFireRenderType() {
    }

    /** Mod-bus handler for {@link RegisterRenderPipelinesEvent}. */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FIRE_PIPELINE);
        pipelineRegistered = true;
    }

    /** True once {@link #registerPipeline} ran. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /** The shared fire render type, or {@code null} while the pipeline is not registered. */
    public static RenderType get() {
        if (!pipelineRegistered) {
            return null;
        }
        if (fire == null) {
            fire = RenderType.create(NAME,
                    RenderSetup.builder(FIRE_PIPELINE)
                            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                            .useLightmap()
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
        }
        return fire;
    }
}
