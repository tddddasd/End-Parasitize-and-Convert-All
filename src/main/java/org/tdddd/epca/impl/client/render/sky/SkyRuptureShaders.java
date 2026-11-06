package org.tdddd.epca.impl.client.render.sky;

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
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.tdddd.epca.impl.epca;

public final class SkyRuptureShaders {

    public static final int SPRITE_COUNT = 12;

    public static final int[] SPRITE_FRAME_COUNTS = {4, 4, 5, 5, 4, 4, 6, 4, 7, 3, 7, 1};

    public static final int[][][] SPRITE_FRAME_SCHEDULES = {
            {{0, 7}, {1, 1}, {2, 1}, {3, 1}},
            {{0, 4}, {1, 1}, {0, 9}, {2, 1}, {0, 7}, {3, 1}},
            {{0, 16}, {1, 1}, {1, 1}, {1, 1}, {2, 1}, {2, 1}, {3, 1}, {4, 1}, {3, 1}, {4, 1}, {3, 1},
                    {2, 1}, {2, 1}, {1, 1}, {1, 1}, {1, 1}},
            {{0, 13}, {1, 1}, {0, 10}, {3, 1}, {0, 5}, {2, 1}, {0, 15}, {4, 1}},
            {{0, 34}, {1, 1}, {2, 1}, {3, 1}},
            {{0, 18}, {1, 1}, {0, 4}, {3, 1}, {0, 14}, {2, 1}},
            {{0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}},
            {{0, 2}, {1, 2}, {2, 2}, {3, 2}},
            {{1, 1}, {2, 1}, {3, 1}, {2, 1}, {3, 1}, {2, 1}, {1, 1}, {0, 22}, {4, 1}, {5, 1}, {6, 1},
                    {5, 1}, {6, 1}, {5, 1}, {4, 1}, {0, 31}, {1, 1}, {2, 1}, {3, 1}, {2, 1}, {1, 1},
                    {0, 12}},
            {{0, 2}, {1, 2}, {2, 2}},
            {{0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}},
            {{0, 1}},
    };

    public static final int[] SPRITE_SHEET_BAND_BASE =
            {0, 4, 8, 13, 18, 22, 26, 32, 36, 43, 46, 53, 54};

    public static final Identifier SPRITE_SHEET =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/sky/cosmic_sheet.png");

    public static final Identifier SPRITE_SCHEDULE_LUT =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/sky/cosmic_schedule.png");

    public static final Identifier[] SPRITE_TEXTURES = new Identifier[SPRITE_COUNT];

    static {
        for (int i = 0; i < SPRITE_COUNT; i++) {
            SPRITE_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    epca.MODID, "textures/shader/cosmic_" + i + ".png");
        }
    }

    public static final Identifier SKY_RUPTURE_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/sky_rupture");

    public static final VertexFormat SKY_RUPTURE_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("TimeProgress", VertexFormatElement.UV0)
            .add("SkyDark", VertexFormatElement.UV1)
            .add("Pattern", VertexFormatElement.UV2)
            .build();

    public static final RenderPipeline SKY_RUPTURE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/sky_rupture"))
                    .withVertexShader(SKY_RUPTURE_SHADER)
                    .withFragmentShader(SKY_RUPTURE_SHADER)
                    .withSampler("Sampler0")
                    .withSampler("Sampler1")
                    .withVertexFormat(SKY_RUPTURE_VERTEX_FORMAT, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    public static final String RENDER_TYPE_NAME = "epca_sky_rupture";

    private static boolean pipelineRegistered;
    private static RenderType renderType;

    private SkyRuptureShaders() {
    }

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SKY_RUPTURE_PIPELINE);
        pipelineRegistered = true;
        epca.LOGGER.info("[epca-render] sky rupture pipeline registered: {} shaders={} format={} samplers={}",
                SKY_RUPTURE_PIPELINE.getLocation(), SKY_RUPTURE_SHADER,
                SKY_RUPTURE_VERTEX_FORMAT.getElementAttributeNames(),
                SKY_RUPTURE_PIPELINE.getSamplers());
        epca.LOGGER.info("[epca-render] sky rupture vertex size = {} bytes, {} attributes, 12 sprites "
                        + "packed into {} ({} bands) + {}",
                SKY_RUPTURE_VERTEX_FORMAT.getVertexSize(),
                SKY_RUPTURE_VERTEX_FORMAT.getElements().size(), SPRITE_SHEET,
                SPRITE_SHEET_BAND_BASE[SPRITE_COUNT], SPRITE_SCHEDULE_LUT);
    }

    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    public static RenderType getRenderType() {
        if (!pipelineRegistered) {
            return null;
        }
        if (renderType == null) {
            renderType = RenderType.create(RENDER_TYPE_NAME,
                    RenderSetup.builder(SKY_RUPTURE_PIPELINE)
                            .withTexture("Sampler0", SPRITE_SHEET)
                            .withTexture("Sampler1", SPRITE_SCHEDULE_LUT)
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
        }
        return renderType;
    }
}
