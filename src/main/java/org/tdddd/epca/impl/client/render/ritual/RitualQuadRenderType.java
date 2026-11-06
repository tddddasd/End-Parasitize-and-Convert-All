package org.tdddd.epca.impl.client.render.ritual;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.tdddd.epca.impl.epca;

public final class RitualQuadRenderType {

    public static final String RENDER_TYPE_NAME = "epca_ritual_quads";

    public static final Identifier RITUAL_SHADER =
            Identifier.parse("minecraft:core/position_color");

    public static final RenderPipeline RITUAL_QUADS_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/ritual_quads"))
                    .withVertexShader(RITUAL_SHADER)
                    .withFragmentShader(RITUAL_SHADER)

                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))

                    .withCull(false)
                    .build();

    private static final RenderType RITUAL_QUADS = RenderType.create(RENDER_TYPE_NAME,
            RenderSetup.builder(RITUAL_QUADS_PIPELINE)
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    private static boolean pipelineRegistered;

    private RitualQuadRenderType() {
    }

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(RITUAL_QUADS_PIPELINE);
        pipelineRegistered = true;
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    public static RenderType get() {
        return RITUAL_QUADS;
    }
}
