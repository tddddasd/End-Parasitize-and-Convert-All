package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.tdddd.epca.impl.epca;

public final class GasCloudRenderType {

    public static final float TINT_RED = 1.0F;
    public static final float TINT_GREEN = 0.33F;
    public static final float TINT_BLUE = 0.34F;

    public static final float TINT_STRENGTH = 0.85F;

    public static final float MASK_STRENGTH = 1.0F;

    public static final float NOISE_SCALE = 2.4F;

    public static final float MASK_DEPTH = 0.50F;

    public static final float MIN_NOISE_MASK = 0.60F;

    public static final float MIN_TEXTURE_ALPHA = 0.65F;

    public static final float ALPHA_BOOST = 0.85F;

    public static final float RADIAL_PLATEAU = 0.55F;

    public static final float TEXTURE_FRAMES = 9.0F;

    public static final float DISTANCE_FADE_START = 48.0F;

    public static final float DISTANCE_FADE_END = 96.0F;

    public static final float SUB_QUAD_INDEX_SCALE = 0.37F;

    public static final int SPEC_STYLE_CHANNEL = 250;

    public static final float SPEC_BASE_ALPHA = 1.0F;

    public static final int HEART_STYLE_CHANNEL = 252;

    public static final int HEART_MOTE_STYLE_CHANNEL = 253;

    public static final Identifier GAS_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/particle/infestive_gas.png");

    public static final Identifier GAS_CLOUD_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/gas_cloud");

    public static final String RENDER_TYPE_NAME = "epca_gas_cloud";

    public static final float SEED_FIXED_POINT_SCALE = 100.0F;

    public static final VertexFormat GAS_CLOUD_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .build();

    public static final RenderPipeline GAS_CLOUD_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/gas_cloud"))
                    .withVertexShader(GAS_CLOUD_SHADER)
                    .withFragmentShader(GAS_CLOUD_SHADER)
                    .withSampler("Sampler0")
                    .withVertexFormat(GAS_CLOUD_VERTEX_FORMAT, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    private static final RenderType GAS_CLOUD = RenderType.create(RENDER_TYPE_NAME,
            RenderSetup.builder(GAS_CLOUD_PIPELINE)
                    .withTexture("Sampler0", GAS_TEXTURE)
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    public static final String ADDITIVE_RENDER_TYPE_NAME = "epca_gas_cloud_additive";

    public static final RenderPipeline GAS_CLOUD_ADDITIVE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/gas_cloud_additive"))
                    .withVertexShader(GAS_CLOUD_SHADER)
                    .withFragmentShader(GAS_CLOUD_SHADER)
                    .withSampler("Sampler0")
                    .withVertexFormat(GAS_CLOUD_VERTEX_FORMAT, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(
                            new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE)))
                    .withCull(false)
                    .build();

    private static final RenderType GAS_CLOUD_ADDITIVE = RenderType.create(ADDITIVE_RENDER_TYPE_NAME,
            RenderSetup.builder(GAS_CLOUD_ADDITIVE_PIPELINE)
                    .withTexture("Sampler0", GAS_TEXTURE)
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    private static boolean pipelineRegistered;

    private GasCloudRenderType() {
    }

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GAS_CLOUD_PIPELINE);
        event.registerPipeline(GAS_CLOUD_ADDITIVE_PIPELINE);
        pipelineRegistered = true;
        if (GasCloudManager.DEBUG) {
            epca.LOGGER.info("[gascloud] pipeline: REGISTERED {} shaders={} format={} samplers={}",
                    GAS_CLOUD_PIPELINE.getLocation(), GAS_CLOUD_SHADER,
                    GAS_CLOUD_VERTEX_FORMAT.getElementAttributeNames(),
                    GAS_CLOUD_PIPELINE.getSamplers());
            epca.LOGGER.info("[gascloud] pipeline: REGISTERED {} blend={}",
                    GAS_CLOUD_ADDITIVE_PIPELINE.getLocation(),
                    GAS_CLOUD_ADDITIVE_PIPELINE.getColorTargetState().blendFunction());
        }
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    public static RenderType get() {
        return GAS_CLOUD;
    }

    public static RenderType getAdditive() {
        return GAS_CLOUD_ADDITIVE;
    }

    public static float[] getTint() {
        return new float[]{TINT_RED, TINT_GREEN, TINT_BLUE, TINT_STRENGTH};
    }

    public static int packSeed(float seed) {
        int packed = (int) (seed * SEED_FIXED_POINT_SCALE);
        return packed < 0 ? 0 : (packed & 0xFFFF);
    }

    @Override
    public String toString() {
        return RENDER_TYPE_NAME;
    }
}
