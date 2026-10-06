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

/**
 * ""emissive layer /
 *
 * <h2> JSON  {@code render_type}</h2>
 * <p> 1.20.1  26.1.226.1.2
 * {@code ChunkSectionLayer} / {@code RenderPipelines.*_TERRAIN}
 * {@code TERRAIN_SNIPPET}  {@code ChunkSection}
 * ""
 * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}
 * {@code InfestedSweetBerryBushGlowRenderer}</p>
 *
 * <h2>""</h2>
 * <p> {@link RenderPipelines#ENTITY_EMISSIVE_SNIPPET} vanilla  {@code core/entity}
 *  {@code EMISSIVE} 1.20.1  vanilla
 * {@code rendertype_entity_translucent_emissive}<b></b> {@code LIGHTMAP}
 * 26.1.2  snippet UV1(overlay)/UV2(light)
 * {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#ENTITY}
 *  {@code EMISSIVE}
 * </p>
 *
 * <h2> 1.20.1  {@code EmissiveBlockRenderType} </h2>
 * <table>
 *   <tr><th>1.20.1 </th><th>26.1.2 </th><th></th></tr>
 *   <tr><td>{@code RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER}</td>
 *       <td>{@link RenderPipelines#ENTITY_EMISSIVE_SNIPPET}{@code core/entity} + {@code EMISSIVE}</td>
 *       <td>1.20.1  emissive  26.1.2 "entity  + EMISSIVE "</td></tr>
 *   <tr><td> {@code epca:block/infested_sweet_berry_bush_growed_e}</td>
 *       <td>{@code RenderSetup.withTexture("Sampler0", ...)}</td>
 *       <td> 16x16 <b></b>
 *           /
 *           {@code RenderTypes.entityTranslucentEmissive(glowTex)}</td></tr>
 *   <tr><td>{@code TRANSLUCENT_TRANSPARENCY}</td>
 *       <td>{@code new ColorTargetState(BlendFunction.TRANSLUCENT)}</td>
 *       <td> alpha </td></tr>
 *   <tr><td>{@code COLOR_WRITE}</td>
 *       <td>{@code new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)} = </td>
 *       <td>/</td></tr>
 *   <tr><td>{@code LEQUAL_DEPTH_TEST}</td>
 *       <td> {@code CompareOp.LESS_THAN_OR_EQUAL}</td>
 *       <td></td></tr>
 *   <tr><td>{@code CULL}</td>
 *       <td> {@code withCull(false)}{@code RenderPipeline} </td>
 *       <td> {@code cutout} {@code minecraft:block/cross}
 *           {@code north}+{@code south}{@code west}+{@code east}uv
 *            {@code NO_CULL}
 *           <b></b>u
 *           ""bug {@code CULL} </td></tr>
 *   <tr><td>{@code OVERLAY}</td><td>{@code RenderSetup.useOverlay()}</td>
 *       <td> vanilla  {@code entity_translucent_emissive} </td></tr>
 *   <tr><td>{@code VIEW_OFFSET_Z_LAYERING}</td>
 *       <td>{@code RenderSetup.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)}</td>
 *       <td><b></b>
 *            z-fighting </td></tr>
 * </table>
 *
 * <h2>1.20.1 -&gt; 26.1.2 </h2>
 * <p>26.1.2  {@code RenderStateShard} {@code RenderType}
 * uniform
 * {@link RenderPipeline} {@link RegisterRenderPipelinesEvent} {@code RenderType}
 *  {@code RenderType.create(name, RenderSetup)}  vanilla
 * {@code RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE} </p>
 */
public final class EmissiveBlockRenderType {

    /**  /  */
    public static final String NAME = "epca_emissive_block";

    /**
     * {@code age=3}
     *
     * <p> {@code epca:block/infested_sweet_berry_bush_growed}  16x16
     * </p>
     */
    public static final Identifier SWEET_BERRY_FRUIT_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/block/infested_sweet_berry_bush_growed_e.png");

    /**
     *  {@code cutout}
     *
     * <p> snippet {@code DefaultVertexFormat.ENTITY}
     * {@code addVertex  setColor  setUv  setOverlay  setLight  setNormal}
     *  {@code VertexConsumer#putBakedQuad} </p>
     */
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

    /**
     *  {@link RegisterRenderPipelinesEvent}  {@code GasCloudRenderType}
     * {@code RitualQuadRenderType}  {@code ClientHandler#onRegisterRenderPipelines}
     *
     */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SWEET_BERRY_FRUIT_PIPELINE);
        pipelineRegistered = true;
    }

    /** {@link #registerPipeline}  */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /**  {@code null} */
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

