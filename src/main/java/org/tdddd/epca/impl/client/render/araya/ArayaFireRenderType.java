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

public final class ArayaFireRenderType {

    public static final String NAME = "epca_araya_fire_blocks";

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

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FIRE_PIPELINE);
        pipelineRegistered = true;
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

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
