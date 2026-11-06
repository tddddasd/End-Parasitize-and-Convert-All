package org.tdddd.epca.impl.client.render.araya;

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

public final class ArayaSlashRenderType {

    public static final String NAME = "epca_araya_slash";

    public static final Identifier SLASH_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/araya_slash");

    public static final RenderPipeline SLASH_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/araya_slash"))
                    .withVertexShader(SLASH_SHADER)
                    .withFragmentShader(SLASH_SHADER)
                    .withSampler("Sampler0")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    private static boolean pipelineRegistered;
    private static RenderType slash;

    private ArayaSlashRenderType() {
    }

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SLASH_PIPELINE);
        pipelineRegistered = true;
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    public static RenderType get() {
        if (!pipelineRegistered) {
            return null;
        }
        if (slash == null) {
            slash = RenderType.create(NAME,
                    RenderSetup.builder(SLASH_PIPELINE)
                            .withTexture("Sampler0", ArayaSceneCopy.TEXTURE_ID)
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
        }
        return slash;
    }
}
