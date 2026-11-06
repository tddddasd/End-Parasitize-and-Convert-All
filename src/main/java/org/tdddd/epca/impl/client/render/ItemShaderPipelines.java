package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemShaderPipelines {

    public static final Identifier CORRUPTION_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/corruption");

    public static final int BYTES_POSITION = 12;
    public static final int BYTES_COLOR = 4;
    public static final int BYTES_UV0 = 8;
    public static final int BYTES_UV1 = 4;
    public static final int BYTES_LINE_WIDTH = 4;

    public static final int EXPECTED_VERTEX_SIZE =
            BYTES_POSITION + BYTES_COLOR + BYTES_UV0 + BYTES_UV1 + BYTES_LINE_WIDTH;

    public static final VertexFormat ITEM_LAYER_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("Uv", VertexFormatElement.UV0)
            .add("Params", VertexFormatElement.UV1)
            .add("AnimClock", VertexFormatElement.LINE_WIDTH)
            .build();

    public static final RenderPipeline CORRUPTION_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/item_corruption"))
                    .withVertexShader(CORRUPTION_SHADER)
                    .withFragmentShader(CORRUPTION_SHADER)
                    .withSampler("Sampler0")
                    .withVertexFormat(ITEM_LAYER_VERTEX_FORMAT, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    public static final String RENDER_TYPE_NAME_PREFIX = "epca_item_corruption/";

    private static boolean pipelineRegistered;

    private static final Map<Identifier, RenderType> RENDER_TYPES = new ConcurrentHashMap<>();

    private ItemShaderPipelines() {
    }

    public static void registerPipeline(
            net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CORRUPTION_PIPELINE);
        pipelineRegistered = true;
        epca.LOGGER.info(
                "[epca-render] item corruption pipeline registered: {} shader={} format={} vertexSize={} samplers={}",
                CORRUPTION_PIPELINE.getLocation(), CORRUPTION_SHADER,
                ITEM_LAYER_VERTEX_FORMAT.getElementAttributeNames(),
                ITEM_LAYER_VERTEX_FORMAT.getVertexSize(),
                CORRUPTION_PIPELINE.getSamplers());
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    public static RenderType renderTypeFor(Identifier maskTexture) {
        if (!pipelineRegistered || maskTexture == null) {
            return null;
        }
        RenderType cached = RENDER_TYPES.get(maskTexture);
        if (cached != null) {
            return cached;
        }
        RenderType created = RenderType.create(RENDER_TYPE_NAME_PREFIX + maskTexture,
                RenderSetup.builder(CORRUPTION_PIPELINE)
                        .withTexture("Sampler0", maskTexture)
                        .setOutline(RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup());
        RENDER_TYPES.put(maskTexture, created);
        return created;
    }

    public static void invalidateRenderTypes() {
        RENDER_TYPES.clear();
    }

    public static int renderTypeCount() {
        return RENDER_TYPES.size();
    }
}
