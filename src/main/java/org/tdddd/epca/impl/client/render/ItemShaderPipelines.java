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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;

/**
 * Pipeline, vertex format and render type of the item shader layers.
 *
 * <h2>1.20.1 -&gt; 26.1.2: one render type instead of three</h2>
 * The 1.20.1 {@code ItemShaderRenderTypes} built THREE render types per layer, because the render
 * state had to be selected at draw time from depth/target/write-mask shards:
 * <table>
 *   <tr><th>1.20.1 variant</th><th>depth</th><th>why</th></tr>
 *   <tr><td>{@code immediate()}</td><td>{@code EQUAL}</td>
 *       <td>the item had already written depth, so an exact match clips the layer to the item's
 *           opaque texels</td></tr>
 *   <tr><td>{@code afterLevel()}</td><td>{@code LEQUAL} + polygon offset, main target</td>
 *       <td>shader-pack replay after the GBuffer composite</td></tr>
 *   <tr><td>{@code handAfterLevel()}</td><td>{@code NO_DEPTH_TEST}, main target</td>
 *       <td>shader-pack replay of the first-person hand</td></tr>
 * </table>
 *
 * <p>Only the first survives, and its depth test changes:</p>
 * <ul>
 *   <li>26.1.2 renders submitted custom geometry in its own pass rather than inline after the item, so
 *       the item's depth writes are not guaranteed to have happened yet. An {@code EQUAL} test would
 *       therefore be unreliable, and the layer uses <b>{@code LESS_THAN_OR_EQUAL} with
 *       {@code writeDepth = false}</b> - the same state the sky rupture uses. Silhouette clipping is
 *       instead done by the shader, which multiplies by the mask sprite's alpha; for the flat items
 *       this layer is used on the result is the same. <b>Documented approximation.</b></li>
 *   <li>the two replay variants are dropped: they existed only for Iris/Oculus, and
 *       {@link org.tdddd.epca.impl.client.render.compat.IrisShaderCompat} reports "inactive"
 *       unconditionally because no Iris port exists for 26.1.2.</li>
 * </ul>
 *
 * <h2>Vertex format</h2>
 * The geometry is emitted by {@code VertexConsumer#putBakedQuad(PoseStack.Pose, BakedQuad,
 * QuadInstance)}, whose body calls the 11-argument
 * {@code addVertex(float,float,float,int,float,float,int,int,float,float,float)}. That helper writes
 * exactly six elements - verified with {@code javap -c} on the patched jar:
 * {@code addVertex}-&gt;POSITION, {@code setColor}-&gt;COLOR, {@code setUv}-&gt;UV0,
 * {@code setOverlay}-&gt;UV1, {@code setLight}-&gt;UV2, {@code setNormal}-&gt;NORMAL - so the format must
 * declare exactly those six and no more (a declared element that is never written would leave the
 * vertex short and 26.1.2 rejects it; writing an element that is not declared is a silent no-op).
 *
 * <pre>
 *   loc attribute  element  writer        payload
 *   0   Position   POSITION addVertex     item-model space quad
 *   1   Color      COLOR    setColor      tint red, green, blue, 1
 *   2   Uv         UV0      setUv         mask sprite UV
 *   3   TimeData   UV1      setOverlay    the 32-bit tick clock, split into two 16-bit halves
 *   4   Params     UV2      setLight      intensity and split strength, 16-bit fixed point
 *   5   Normal     NORMAL   setNormal     quad normal (unused by the shader)
 * </pre>
 *
 * <p>{@code UV1} and {@code UV2} are read by the shader as {@code ivec2}: 26.1.2's
 * {@code setUv1(int,int)}/{@code setUv2(int,int)} store raw shorts, which is what makes them usable
 * carriers for arbitrary integer data. The pipeline deliberately declares no lightmap and no overlay
 * sampler, so those two attributes never reach one.</p>
 *
 * <h2>Texture</h2>
 * {@code Sampler0} is the block atlas, because item textures are stitched into it; the mask is
 * selected per draw through the quad's UVs (from the sprite's {@code getU0()/getV0()/getU1()/getV1()}),
 * so <b>one</b> render type serves every corrupted item - unlike 1.20.1, which needed a render type
 * per mask texture.
 */
public final class ItemShaderPipelines {

    /** Shader id; {@code FileToIdConverter("shaders", ".vsh"/".fsh")} maps it to the two assets. */
    public static final Identifier CORRUPTION_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/corruption");

    /** The block atlas, which holds {@code epca:item/<item>} sprites. */
    public static final Identifier ATLAS = TextureAtlas.LOCATION_BLOCKS;

    /** Vertex layout: exactly the six elements {@code putBakedQuad} writes. See the class comment. */
    public static final VertexFormat ITEM_LAYER_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("Uv", VertexFormatElement.UV0)
            .add("TimeData", VertexFormatElement.UV1)
            .add("Params", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();

    /**
     * The corruption pipeline.
     *
     * <p>{@link RenderPipelines#MATRICES_PROJECTION_SNIPPET} supplies the {@code DynamicTransforms}
     * and {@code Projection} UBOs. Depth and blend mirror the 1.20.1 {@code immediate()} variant as
     * closely as the deferred submit allows (see the class comment): translucent blend, {@code LEQUAL}
     * with no depth write, no culling.</p>
     */
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

    /** Render type / diagnostics name. */
    public static final String RENDER_TYPE_NAME = "epca_item_corruption";

    private static boolean pipelineRegistered;
    private static RenderType corruptionRenderType;

    private ItemShaderPipelines() {
    }

    /** Mod-bus handler for {@code RegisterRenderPipelinesEvent}. */
    public static void registerPipeline(
            net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CORRUPTION_PIPELINE);
        pipelineRegistered = true;
        epca.LOGGER.info("[epca-render] item corruption pipeline registered: {} shader={} format={} samplers={}",
                CORRUPTION_PIPELINE.getLocation(), CORRUPTION_SHADER,
                ITEM_LAYER_VERTEX_FORMAT.getElementAttributeNames(),
                CORRUPTION_PIPELINE.getSamplers());
    }

    /** True once the pipeline has been registered. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /**
     * The corruption render type, or {@code null} while the pipeline is not registered yet. Created
     * lazily for the same reason as the sky render type: the pipeline object only becomes valid
     * inside the registration event.
     */
    public static RenderType corruptionRenderType() {
        if (!pipelineRegistered) {
            return null;
        }
        if (corruptionRenderType == null) {
            corruptionRenderType = RenderType.create(RENDER_TYPE_NAME,
                    RenderSetup.builder(CORRUPTION_PIPELINE)
                            .withTexture("Sampler0", ATLAS)
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
        }
        return corruptionRenderType;
    }
}
