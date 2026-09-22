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

/**
 * Custom render type and render pipeline used by the shader-rendered gas clouds.
 *
 * <h2>26.1.2 -- this is a real custom GLSL shader, not a fallback</h2>
 * <p>1.20.1 built the render type on a {@code ShaderInstance} loaded from
 * {@code assets/epca/shaders/core/gas_cloud.json} and drove it with per-draw uniforms
 * ({@code GasFade}, {@code GasSeed}, {@code GasTint}, {@code GasShape}). The 26.1.2 render system
 * replaced {@code ShaderInstance} with {@link RenderPipeline}: the whole render-state block
 * (blend, depth, cull, samplers, vertex format) now lives in the pipeline object, there is no
 * {@code shaders/core/*.json} file any more (verified: the patched jar contains only
 * {@code .vsh}/{@code .fsh} under {@code assets/minecraft/shaders/core}), and a mod supplies its
 * pipeline through {@link RegisterRenderPipelinesEvent}.</p>
 *
 * <p>Shader sources added by this change:</p>
 * <ul>
 *   <li>{@code assets/epca/shaders/core/gas_cloud.vsh}</li>
 *   <li>{@code assets/epca/shaders/core/gas_cloud.fsh}</li>
 * </ul>
 * <p>They are resolved by the pipeline's shader ids: the identifier
 * {@code epca:core/gas_cloud} maps through {@code ShaderType#idConverter()} (a
 * {@code FileToIdConverter("shaders", ".vsh"/".fsh")}) to exactly those two paths.</p>
 *
 * <h3>Per-draw values without uniforms</h3>
 * <p>{@code SubmitNodeCollector#submitCustomGeometry} hands the callback only a
 * {@code PoseStack.Pose} and a {@code VertexConsumer}, and {@code RenderType#draw} always writes
 * {@code ColorModulator = (1,1,1,1)} into the shared {@code DynamicTransforms} uniform, so a mod
 * cannot push a per-draw uniform through this path. The two per-cloud values are therefore carried
 * by the vertex data instead:</p>
 * <ul>
 *   <li>the fade ({@code GasFade} in 1.20.1) travels in the vertex colour alpha,</li>
 *   <li>the per-cloud noise seed ({@code GasSeed}) travels in the {@code UV1} vertex slot, which
 *       this pipeline does not use as an overlay coordinate,</li>
 *   <li>the tint ({@code GasTint}) and mask strength ({@code GasShape}) are compile-time constants
 *       inside the fragment shader.</li>
 * </ul>
 *
 * <h3>Render state</h3>
 * <ul>
 *   <li>blending: {@link BlendFunction#TRANSLUCENT} (SrcAlpha / OneMinusSrcAlpha). This also makes
 *       {@code RenderType#hasBlending()} true, which is what routes the submitted geometry into the
 *       translucent custom-geometry pass.</li>
 *   <li>depth: {@link CompareOp#LESS_THAN_OR_EQUAL} with {@code writeDepth = false}, so clouds are
 *       occluded by terrain but never occlude each other.</li>
 *   <li>culling: off, because a billboard's winding order flips with the viewing angle.</li>
 *   <li>lighting: none. The pipeline does not declare a lightmap sampler and the vertices carry no
 *       lightmap data, so the clouds render at full brightness (unlit / emissive).</li>
 * </ul>
 *
 * <h3>Vertex format</h3>
 * <p>The pipeline uses its own four-element format rather than {@code DefaultVertexFormat.ENTITY}:
 * the attribute names must match the {@code in} declarations of the vertex shader (26.1.2 binds
 * attribute locations from the format's element names), and this keeps the unused overlay/lightmap/
 * normal slots out of the buffer. The matching {@code in} declarations are
 * {@code Position}, {@code Color}, {@code UV0} and {@code UV1}.</p>
 */
public final class GasCloudRenderType {

    /** Red tint RGB, taken from the INFESTIVE_GAS particle texture (weighted average 137,45,45 -> 0.54,0.18,0.18, lifted to a readable red). */
    public static final float TINT_RED = 1.0F;
    public static final float TINT_GREEN = 0.33F;
    public static final float TINT_BLUE = 0.34F;
    /** How strongly {@link #TINT_RED}/{@link #TINT_GREEN}/{@link #TINT_BLUE} replace the texture colour. */
    public static final float TINT_STRENGTH = 0.85F;
    /** Overall mask strength; the fragment shader multiplies the noise mask by this value. */
    public static final float MASK_STRENGTH = 1.0F;

    // =================================================================
    //  Alpha-chain mirrors (GAS_* in gas_cloud.fsh)
    // =================================================================

    /**
     * Scale of the procedural noise field, in quad UV space.
     * Mirrors {@code GAS_NOISE_SCALE}.
     */
    public static final float NOISE_SCALE = 2.4F;
    /**
     * How strongly the noise cuts into the mask (0 = flat disc, 1 = fully noise shaped).
     * Mirrors {@code GAS_MASK_DEPTH}; same name and value as the 1.20.1 twin.
     */
    public static final float MASK_DEPTH = 0.50F;
    /**
     * Floor for the noise mask, so the noise cannot multiply the alpha chain down to nothing.
     * Mirrors {@code GAS_MIN_NOISE_MASK}.
     */
    public static final float MIN_NOISE_MASK = 0.60F;
    /**
     * Floor for the sampled sprite alpha.
     *
     * <p>Frame 0 of the {@link #GAS_TEXTURE} strip is the most opaque band of the sheet, yet its
     * alpha <em>median</em> is 0.0 (measured: band means 0.426, 0.355, 0.258, 0.188, 0.129, 0.090,
     * 0.047, 0.020, 0.008 for frames 0..8, with 42.6% of frame 0's texels at alpha 255 and every
     * covered texel fully opaque). Multiplying {@code tex.a} straight in collapsed the finished
     * alpha to nearly nothing. Floored at 0.65 the sprite's irregular silhouette still shows
     * through while the cloud stays visible.
     * Mirrors {@code GAS_MIN_TEXTURE_ALPHA}.
     */
    public static final float MIN_TEXTURE_ALPHA = 0.65F;
    /**
     * Overall scale of the finished alpha chain. Mirrors {@code GAS_ALPHA_BOOST}.
     *
     * <p>Peak alpha of a freshly spawned cloud (quad centre, camera close, {@code base = 1},
     * {@code mask = 0.9375} - the maximum reachable, since three-octave fbm tops out at 0.875):
     * longarms active and yelloweye {@code 0.90 * 0.85 * 0.9375 = 0.717}, longarms passive
     * {@code 0.45 * 0.85 * 0.9375 = 0.359}. Floor with {@code base = 0.65}, {@code mask = 0.60}:
     * 0.298 and 0.149.</p>
     */
    public static final float ALPHA_BOOST = 0.85F;
    /**
     * Fraction of the quad's half-extent over which the radial term stays at full strength, so the
     * cloud keeps a solid core instead of falling off from the very centre.
     * Mirrors {@code GAS_RADIAL_PLATEAU}.
     */
    public static final float RADIAL_PLATEAU = 0.55F;
    /** Number of vertically packed 16x16 frames in {@link #GAS_TEXTURE}. Mirrors {@code GAS_FRAME_COUNT}. */
    public static final float TEXTURE_FRAMES = 9.0F;
    /** Camera distance at which the far fade starts, in blocks. Mirrors {@code GAS_DISTANCE_FADE_START}. */
    public static final float DISTANCE_FADE_START = 48.0F;
    /** Camera distance at which the far fade completes, in blocks. Mirrors {@code GAS_DISTANCE_FADE_END}. */
    public static final float DISTANCE_FADE_END = 96.0F;
    /** Spread applied to the sub-quad index so the two seed channels differ. Mirrors {@code GAS_SUB_QUAD_INDEX_SCALE}. */
    public static final float SUB_QUAD_INDEX_SCALE = 0.37F;

    // =================================================================
    //  Hard-edged "spec" style (contaminated water micro rectangles)
    // =================================================================

    /**
     * Style marker written into the second channel of the {@code UV1} vertex slot (the slot the gas
     * style uses for the sub-quad index).
     *
     * <p>{@code gas_cloud.fsh} compares the flat {@code gasSeed.y} varying against
     * {@code GAS_SPEC_STYLE_CHANNEL} and, on a match, draws a solid hard-edged quad: no texture
     * sampling, no radial falloff, no noise mask. Every real gas sub-quad index is far below this
     * value, so the two styles can never collide. Mirrors {@code GAS_SPEC_STYLE_CHANNEL} in the
     * fragment shader (checked by {@code check-glsl-26.py}).</p>
     */
    public static final int SPEC_STYLE_CHANNEL = 250;

    /**
     * The {@code base} factor of the spec style's alpha chain
     * ({@code base * GAS_ALPHA_BOOST * Color.a * edgeFade}). A hard-edged rectangle has no texture
     * alpha to reduce it, so it is 1.0. Mirrors {@code GAS_SPEC_BASE_ALPHA}.
     */
    public static final float SPEC_BASE_ALPHA = 1.0F;

    /**
     * Style channel value that turns a quad into the golden "Heart" of {@code epca:soul_protection}
     * instead of a gas puff or a water speck: the tall, irregular golden plasma/flame column of the
     * reference image.
     *
     * <p>Like {@link #SPEC_STYLE_CHANNEL} it rides in the second channel of the {@code UV1}
     * attribute, which already carries the sub-quad index for gas clouds ({@code 0..8}); {@code 252}
     * is therefore unambiguous. {@code gas_cloud.fsh}'s {@code HEART_STYLE_CHANNEL} const selects the
     * flame branch, which draws the look procedurally and ignores the texture, the radial falloff and
     * the noise mask. The Java and GLSL values are cross-checked by
     * {@code build/javac-check/check-glsl-26.py} so they cannot drift apart.</p>
     *
     * <p>The name keeps the effect's "Heart" wording even though the look is a flame: the marker is
     * part of the 1.20.1 / 26.1.2 twin contract and both trees send this exact value. The quad itself
     * is built and submitted by {@code impl/client/entity/heart/SoulProtectionHeartRenderer}, which
     * reuses the shared camera-relative billboard path of {@link GasCloudRenderer}, writes this value
     * into the style channel and draws through {@link #getAdditive()} so the flame is emissive.</p>
     */
    public static final int HEART_STYLE_CHANNEL = 252;

    /**
     * Style channel value of the tiny golden embers drawn beside the {@link #HEART_STYLE_CHANNEL}
     * flame.
     *
     * <p>It is a branch of its own rather than a reuse of {@link #SPEC_STYLE_CHANNEL} because the
     * speck branch multiplies the vertex colour by the red {@link #TINT_RED}/{@link #TINT_GREEN}/
     * {@link #TINT_BLUE}, which would turn a gold ember red; the mote branch instead uses the
     * shader's own {@code HEART_MOTE_COLOR}. A third value in the style channel keeps every existing
     * gas and speck quad untouched.</p>
     */
    public static final int HEART_MOTE_STYLE_CHANNEL = 253;

    /**
     * The same texture the INFESTIVE_GAS particle uses, so the clouds visually match the particles.
     * It is a 16x144 sheet of nine 16x16 animation frames; the raw 16x144 image is what
     * {@code SimpleTexture} uploads, so the fragment shader samples only the first frame band
     * ({@code v} in {@code [0, 1/9]}).
     */
    public static final Identifier GAS_TEXTURE =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/particle/infestive_gas.png");

    /** Shader id; {@code FileToIdConverter("shaders", ".vsh"/".fsh")} maps it to the two assets above. */
    public static final Identifier GAS_CLOUD_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/gas_cloud");

    /** Render type / pipeline name, used by {@code RenderType.create} and by diagnostics. */
    public static final String RENDER_TYPE_NAME = "epca_gas_cloud";

    /**
     * Fixed point scale used to pack the per-cloud float seed into the 16 bit {@code UV1} slot.
     * {@code GasCloud#getRenderSeed()} returns a value in {@code [0, 100)}, so
     * {@code (int) (seed * 100)} always fits in 16 bits and the fragment shader recovers it exactly
     * with {@code float(UV1.x) / 100.0}.
     */
    public static final float SEED_FIXED_POINT_SCALE = 100.0F;

    /**
     * Vertex layout of the billboard quads. {@code UV1} is documented in the shader as the per-cloud
     * noise seed slot ({@code x} = seed, {@code y} = sub-quad index).
     */
    public static final VertexFormat GAS_CLOUD_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .build();

    /**
     * The custom pipeline. {@link RenderPipelines#MATRICES_PROJECTION_SNIPPET} contributes the
     * {@code DynamicTransforms} and {@code Projection} uniform buffers, which are exactly the two
     * buffers {@code RenderType#draw} and {@code RenderSystem#bindDefaultUniforms} always bind.
     */
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

    /** The shared render type; only valid to draw with while {@link #isPipelineRegistered()} is true. */
    private static final RenderType GAS_CLOUD = RenderType.create(RENDER_TYPE_NAME,
            RenderSetup.builder(GAS_CLOUD_PIPELINE)
                    .withTexture("Sampler0", GAS_TEXTURE)
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    /** Name of the additive variant, so the two render types are distinguishable in diagnostics. */
    public static final String ADDITIVE_RENDER_TYPE_NAME = "epca_gas_cloud_additive";

    /**
     * Additive variant of the very same pipeline: same core shader, same vertex format, same texture
     * and depth state, only the blend function differs ({@code SRC_ALPHA / ONE} instead of
     * {@code TRANSLUCENT}'s {@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA}).
     *
     * <p>It exists for the emissive {@code epca:soul_protection} flame, whose reference look is a
     * glowing plasma column over a dark background: with {@code SRC_ALPHA / ONE} the near-opaque core
     * adds up to a bright near-white glow instead of merely blending towards the background. Nothing
     * else uses it, so the gas and speck quads keep their exact previous blend. This mirrors the
     * 1.20.1 twin, which builds the same variant on {@code ADDITIVE_TRANSPARENCY}.</p>
     */
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

    /** The additive render type; only valid to draw with while {@link #isPipelineRegistered()}. */
    private static final RenderType GAS_CLOUD_ADDITIVE = RenderType.create(ADDITIVE_RENDER_TYPE_NAME,
            RenderSetup.builder(GAS_CLOUD_ADDITIVE_PIPELINE)
                    .withTexture("Sampler0", GAS_TEXTURE)
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    private static boolean pipelineRegistered;

    private GasCloudRenderType() {
    }

    /**
     * Mod-bus handler for {@link RegisterRenderPipelinesEvent}; call it from the mod's client
     * event subscriber. Until this has run the pipelines are not part of the pipeline registry and
     * the callers refuse to submit geometry. Both variants are registered here, exactly like the
     * 1.20.1 twin builds both render types from the one shader.
     */
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

    /** True once {@link #registerPipeline} has registered the custom pipelines. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /** The shared render type; only valid to draw with while {@link #isPipelineRegistered()} is true. */
    public static RenderType get() {
        return GAS_CLOUD;
    }

    /**
     * The additive variant of the same pipeline, for the emissive soul-protection flame; only valid
     * to draw with while {@link #isPipelineRegistered()} is true.
     */
    public static RenderType getAdditive() {
        return GAS_CLOUD_ADDITIVE;
    }

    /** The red tint used by every gas cloud, normalised, for callers that need to mirror it. */
    public static float[] getTint() {
        return new float[]{TINT_RED, TINT_GREEN, TINT_BLUE, TINT_STRENGTH};
    }

    /** Packs a per-cloud seed into the 16 bit integer range carried by the {@code UV1} slot. */
    public static int packSeed(float seed) {
        int packed = (int) (seed * SEED_FIXED_POINT_SCALE);
        return packed < 0 ? 0 : (packed & 0xFFFF);
    }

    @Override
    public String toString() {
        return RENDER_TYPE_NAME;
    }
}
