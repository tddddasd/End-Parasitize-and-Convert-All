package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.tdddd.epca.impl.epca;

public final class EmissiveBlockRenderType {

    public static final String NAME = "epca_emissive_block";

    public static final Identifier SWEET_BERRY_FRUIT_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/block/infested_sweet_berry_bush_growed_e.png");

    public static final RenderPipeline SWEET_BERRY_FRUIT_PIPELINE =
            RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/emissive_block"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build();

    private static boolean pipelineRegistered;
    private static RenderType sweetBerryFruit;

    private EmissiveBlockRenderType() {
    }

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SWEET_BERRY_FRUIT_PIPELINE);
        pipelineRegistered = true;
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    public static RenderType sweetBerryFruit() {
        if (!pipelineRegistered) {
            return null;
        }
        if (sweetBerryFruit == null) {
            sweetBerryFruit = RenderType.create(NAME,
                    RenderSetup.builder(SWEET_BERRY_FRUIT_PIPELINE)
                            .withTexture("Sampler0", SWEET_BERRY_FRUIT_TEXTURE)
                            .useOverlay()
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
        }
        return sweetBerryFruit;
    }
}
