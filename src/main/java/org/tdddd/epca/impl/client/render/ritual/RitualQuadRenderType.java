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

/**
 * The render type every sacrifice-ritual quad is drawn through: plain {@code POSITION_COLOR} quads with
 * the vanilla {@code core/position_color} program, translucent blending, depth testing on, depth writes
 * off and culling off.
 *
 * <h2>Why not the mod's custom {@code gas_cloud} pipeline</h2>
 * <p>The ritual aura was first drawn through {@code GasCloudRenderType.get()} together with a new
 * {@code RITUAL_STYLE_CHANNEL} branch in {@code gas_cloud.fsh}. In game that produced literally nothing,
 * while a positive-control quad drawn through the built-in {@code RenderType.debugQuads()} at the very
 * same pose, stage and buffer source was visible - so the level stage, the camera-relative pose space
 * and the render target were all correct and only the custom program or its state failed. This type
 * therefore reuses the vanilla program (exactly what the working control quad used) and adds the state
 * the ritual needs and that {@code debugQuads()} does not have: culling off, because the aura's quads
 * are seen from above and from below.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The 1.20.1 twin is a {@code RenderType} subclass whose constructor assembles
 * {@code RenderStateShard}s (shader state, transparency, depth test, colour write, cull). 26.1.2 has
 * no {@code RenderStateShard} and no {@code RenderType} subclassing any more: the whole render-state
 * block lives in a {@link RenderPipeline}, which a mod declares and registers through
 * {@link RegisterRenderPipelinesEvent} (the pattern {@code GasCloudRenderType} already uses), and a
 * {@code RenderType} is then a {@code RenderType.create(name, RenderSetup)} wrapper around that
 * pipeline. Every call used below was read from
 * {@code minecraft-patched-26.1.2.76.jar} with {@code javap}:</p>
 * <ul>
 *   <li>{@code RenderPipeline.builder(RenderPipeline.Snippet...)} and
 *       {@code RenderPipelines.MATRICES_PROJECTION_SNIPPET} - the snippet contributes the
 *       {@code DynamicTransforms} and {@code Projection} uniform buffers, which is exactly what the
 *       vanilla {@code position_color} program reads ({@code ProjMat * ModelViewMat * vec4(Position, 1)}
 *       and {@code ColorModulator});</li>
 *   <li>{@code Builder#withVertexShader(Identifier)} / {@code withFragmentShader(Identifier)} - both
 *       point at {@code core/position_color}, which resolves through the shader id converter to
 *       {@code assets/minecraft/shaders/core/position_color.vsh/.fsh};</li>
 *   <li>{@code Builder#withVertexFormat(VertexFormat, VertexFormat.Mode)},
 *       {@code withCull(boolean)}, {@code withColorTargetState(ColorTargetState)},
 *       {@code withDepthStencilState(DepthStencilState)};</li>
 *   <li>{@code RenderSetup.builder(RenderPipeline)} plus {@code setOutline},
 *       {@code createRenderSetup()} and {@code RenderType.create(String, RenderSetup)}.</li>
 * </ul>
 *
 * <p>The state mirrors the 1.20.1 twin one for one: {@link BlendFunction#TRANSLUCENT} is
 * {@code TRANSLUCENT_TRANSPARENCY}, {@code new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)}
 * is {@code LEQUAL_DEPTH_TEST} with depth writes off ({@code COLOR_WRITE}), and {@code withCull(false)}
 * is {@code NO_CULL}. It is deliberately the same state vanilla's own debug-fill pipeline uses - the
 * very pipeline the working control quad went through.</p>
 */
public final class RitualQuadRenderType {

    /** Render type name; the pipeline carries the same name under the {@code pipeline/} path. */
    public static final String RENDER_TYPE_NAME = "epca_ritual_quads";

    /**
     * The vanilla position-colour program the aura is drawn with. It is a core shader, not a mod
     * asset, and its vertex format is exactly {@link DefaultVertexFormat#POSITION_COLOR}, so the
     * renderer only ever writes a position and a colour.
     */
    public static final Identifier RITUAL_SHADER =
            Identifier.parse("minecraft:core/position_color");

    /**
     * The custom pipeline. {@link RenderPipelines#MATRICES_PROJECTION_SNIPPET} contributes the
     * {@code DynamicTransforms} and {@code Projection} uniform buffers, the two buffers
     * {@code RenderType#draw} and {@code RenderSystem#bindDefaultUniforms} always bind.
     */
    public static final RenderPipeline RITUAL_QUADS_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/ritual_quads"))
                    .withVertexShader(RITUAL_SHADER)
                    .withFragmentShader(RITUAL_SHADER)
                    // POSITION_COLOR only: no texture, no lightmap, no overlay, no normal.
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    // The aura's quads are seen from above and from below, so back-face culling is off.
                    .withCull(false)
                    .build();

    /** The shared render type; only valid to draw with while {@link #isPipelineRegistered()} is true. */
    private static final RenderType RITUAL_QUADS = RenderType.create(RENDER_TYPE_NAME,
            RenderSetup.builder(RITUAL_QUADS_PIPELINE)
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    private static boolean pipelineRegistered;

    private RitualQuadRenderType() {
    }

    /**
     * Mod-bus handler for {@link RegisterRenderPipelinesEvent}; call it from the mod's client event
     * subscriber next to {@code GasCloudRenderType.registerPipeline}. Until this has run the pipeline
     * is not part of the pipeline registry, so the renderer refuses to submit geometry.
     */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(RITUAL_QUADS_PIPELINE);
        pipelineRegistered = true;
        epca.LOGGER.info("[ritual] pipeline: REGISTERED {} shaders={} format={}",
                RITUAL_QUADS_PIPELINE.getLocation(), RITUAL_SHADER,
                DefaultVertexFormat.POSITION_COLOR.getElementAttributeNames());
    }

    /** True once {@link #registerPipeline} has registered the ritual pipeline. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /** The shared render type of every ritual quad. */
    public static RenderType get() {
        return RITUAL_QUADS;
    }
}
