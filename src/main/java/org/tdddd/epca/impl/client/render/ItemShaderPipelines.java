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
 * <h2>Vertex format: 32 bytes, and why the alignment matters</h2>
 * {@code VertexFormat.Builder#build()} rejects any layout whose total size is not a multiple of 4
 * (verified with {@code javap -c}: it calls {@code Mth.isMultipleOf(offset, 4)} and throws
 * {@code IllegalStateException("Vertex size must be a multiple of 4, was N")}). The element sizes are
 * POSITION 12, COLOR 4, UV0 8, UV1 4, UV2 4, NORMAL 3 and LINE_WIDTH 4.
 *
 * <p>An earlier revision of this class declared the six elements that
 * {@code VertexConsumer#putBakedQuad} writes - POSITION, COLOR, UV0, UV1, UV2, NORMAL - which sums to
 * <b>35 bytes</b> because {@code NORMAL} is three normalised bytes. That threw during class
 * initialisation and, because it happened inside the {@code RegisterRenderPipelines} event, it took the
 * whole event down with it. The geometry is therefore emitted <b>manually</b> now, and the layout is
 * built only from elements whose sizes keep the total 4-byte aligned:</p>
 *
 * <pre>
 *   loc attribute  element     bytes  writer                     payload
 *   0   Position   POSITION     12    addVertex(pose, x, y, z)   quad corner, pose-transformed
 *   1   Color      COLOR         4    setColor(r, g, b, a)       tint rgb, alpha 1
 *   2   Uv         UV0           8    setUv(u, v)                mask sprite UV
 *   3   Params     UV1           4    setUv1(int, int)           intensity, split strength
 *   4   AnimClock  LINE_WIDTH    4    setLineWidth(timeTicks)    the tick clock, as a float
 *                                 --
 *                                 32   (32 % 4 == 0)
 * </pre>
 *
 * <p>Every declared element is written on every vertex. That is required: 26.1.2 counts the elements a
 * vertex filled, and a vertex that filled fewer than the format declares is rejected - so declaring an
 * element purely "for alignment" would be wrong in both directions.
 * {@code build/javac-check/check-item-corruption.py} recomputes this sum from the declared element ids
 * and asserts the multiple-of-4 rule, so this bug class fails the checks rather than the game.</p>
 *
 * <p>{@code UV1} is read by the shader as an {@code ivec2}: 26.1.2's {@code setUv1(int,int)} stores raw
 * shorts rather than normalised floats, which is what makes it a usable carrier for 16-bit fixed point
 * data. {@code LINE_WIDTH} is an ordinary 4-byte float slot and is not consumed by anything, because the
 * pipeline's mode is QUADS; it carries the animation clock at full float precision, which is exactly what
 * the 1.20.1 {@code time} uniform was ({@code (float) (gameTime % Integer.MAX_VALUE)}).</p>
 *
 * <p>Because {@code UV2} is gone, the payload was re-assigned: {@code UV1.x} = intensity (16-bit),
 * {@code UV1.y} = split strength (16-bit over 0..4), and the clock moved to {@code LINE_WIDTH} (float).
 * The clock actually <em>gained</em> precision relative to the earlier 16-bit packing, and intensity and
 * split strength stay at 16 bits, so nothing regressed against 1.20.1's floats for any value the eye can
 * resolve.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2: one render type instead of three</h2>
 * The 1.20.1 {@code ItemShaderRenderTypes} built three render types per layer, because the render state
 * had to be picked at draw time from depth/target/write-mask shards: {@code immediate()} ({@code EQUAL}
 * depth, clipping the layer to the item's opaque texels), {@code afterLevel()} and
 * {@code handAfterLevel()} (main target, for shader-pack replay). Only the first survives, and its depth
 * test changes:
 * <ul>
 *   <li>26.1.2 renders submitted custom geometry in its own pass rather than inline after the item, so
 *       the item's depth writes are not guaranteed to have happened. An {@code EQUAL} test would be
 *       unreliable, so the layer uses <b>{@code LESS_THAN_OR_EQUAL} with {@code writeDepth = false}</b>.
 *       Silhouette clipping is done by the shader, which multiplies by the mask sprite's alpha.
 *       <b>Documented approximation.</b></li>
 *   <li>the two replay variants are dropped: they existed only for Iris/Oculus, and
 *       {@link org.tdddd.epca.impl.client.render.compat.IrisShaderCompat} reports "inactive"
 *       unconditionally because no Iris port exists for 26.1.2.</li>
 * </ul>
 *
 * <h2>Mask texture</h2>
 * {@code Sampler0} is bound to {@link TextureAtlas#LOCATION_BLOCKS}, which is the <b>direct texture
 * path</b> {@code minecraft:textures/atlas/blocks.png} (verified with {@code javap -c} on the
 * {@code TextureAtlas} static initialiser, which loads the literal {@code "textures/atlas/blocks.png"}).
 * Item textures are stitched into that atlas in vanilla, so the mask is selected per draw through the
 * quad's UVs (taken from the sprite's {@code getU0()/getV0()/getU1()/getV1()}) and <b>one</b> render type
 * serves every corrupted item - unlike 1.20.1, which needed a render type per mask texture. This is the
 * same shape as the shipped {@code GasCloudRenderType}, which binds
 * {@code epca:textures/particle/infestive_gas.png} directly.
 */
public final class ItemShaderPipelines {

    /** Shader id; {@code FileToIdConverter("shaders", ".vsh"/".fsh")} maps it to the two assets. */
    public static final Identifier CORRUPTION_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/corruption");

    /** The block atlas texture, which holds {@code epca:item/<item>} sprites. */
    public static final Identifier ATLAS = TextureAtlas.LOCATION_BLOCKS;

    /** Byte size each element contributes; the checks recompute the sum from these. */
    public static final int BYTES_POSITION = 12;
    public static final int BYTES_COLOR = 4;
    public static final int BYTES_UV0 = 8;
    public static final int BYTES_UV1 = 4;
    public static final int BYTES_LINE_WIDTH = 4;

    /** The expected total, so the checks can assert it and the alignment rule. */
    public static final int EXPECTED_VERTEX_SIZE =
            BYTES_POSITION + BYTES_COLOR + BYTES_UV0 + BYTES_UV1 + BYTES_LINE_WIDTH;

    /**
     * Vertex layout. See the class comment for the payload each slot carries. The total is
     * {@link #EXPECTED_VERTEX_SIZE} bytes (a multiple of 4), and every declared element is written by
     * {@link ItemCorruptionRenderer}.
     */
    public static final VertexFormat ITEM_LAYER_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("Uv", VertexFormatElement.UV0)
            .add("Params", VertexFormatElement.UV1)
            .add("AnimClock", VertexFormatElement.LINE_WIDTH)
            .build();

    /**
     * The corruption pipeline.
     *
     * <p>{@link RenderPipelines#MATRICES_PROJECTION_SNIPPET} supplies the {@code DynamicTransforms} and
     * {@code Projection} UBOs. Depth and blend mirror the 1.20.1 {@code immediate()} variant as closely as
     * the deferred submit allows (see the class comment): translucent blend, {@code LEQUAL} with no depth
     * write, no culling.</p>
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
        epca.LOGGER.info(
                "[epca-render] item corruption pipeline registered: {} shader={} format={} vertexSize={} samplers={}",
                CORRUPTION_PIPELINE.getLocation(), CORRUPTION_SHADER,
                ITEM_LAYER_VERTEX_FORMAT.getElementAttributeNames(),
                ITEM_LAYER_VERTEX_FORMAT.getVertexSize(),
                CORRUPTION_PIPELINE.getSamplers());
    }

    /** True once the pipeline has been registered. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /**
     * The corruption render type, or {@code null} while the pipeline is not registered yet. Created
     * lazily because the pipeline object only becomes valid inside the registration event.
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
