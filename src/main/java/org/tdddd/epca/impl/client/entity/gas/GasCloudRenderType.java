package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.tdddd.epca.impl.epca;

import java.util.Optional;

/**
 * Custom {@link RenderType} used by the shader-rendered gas clouds.
 *
 * <p>It is backed by a real custom core shader: {@code assets/epca/shaders/core/gas_cloud.json}
 * plus {@code gas_cloud.vsh} / {@code gas_cloud.fsh}. The {@link ShaderInstance} is created in
 * {@link #registerShader} from Forge's {@code RegisterShadersEvent} (a MOD-bus event, so that
 * handler lives in {@code ClientHandler}) and referenced here through a
 * {@link RenderStateShard.ShaderStateShard}, so the render type has no built-in shader
 * fallback.</p>
 *
 * <h2>No per-cloud uniforms</h2>
 * <p>{@code ShaderInstance.apply()} uploads uniforms once per RenderType flush, while the vertices
 * of every cloud in a pass are buffered beforehand. A per-cloud uniform would therefore give each
 * cloud the <em>last</em> cloud's value. Per-cloud data travels in vertex attributes instead:</p>
 * <ul>
 *   <li>per-cloud fade -&gt; {@code Color.a}, written by {@link GasCloudLayer};</li>
 *   <li>per-cloud seed -&gt; {@code UV2.x}, sub-quad index -&gt; {@code UV2.y} (see
 *       {@link #packSeed(float)}).</li>
 * </ul>
 * <p>The only uniforms are the automatically uploaded {@code ModelViewMat}, {@code ProjMat} and
 * {@code ColorModulator}. The tuning constants below are mirrored as GLSL {@code const}s with the
 * same meaning in {@code gas_cloud.fsh}; the two definitions are cross-checked by
 * {@code build/javac-check/check-glsl.py} so they cannot silently drift apart.</p>
 *
 * <h2>Render state</h2>
 * <ul>
 *   <li>transparency: {@code TRANSLUCENT_TRANSPARENCY} (SrcAlpha / OneMinusSrcAlpha);</li>
 *   <li>depth write: disabled, so clouds never occlude each other or later passes;</li>
 *   <li>depth test: {@code LEQUAL_DEPTH_TEST} so terrain still occludes clouds;</li>
 *   <li>culling: disabled, billboards are viewed from both sides;</li>
 *   <li>lighting: the clouds are unlit / emissive ({@code NO_LIGHTMAP}), which is also why the
 *       lightmap slot {@code UV2} is free to carry the packed seed.</li>
 * </ul>
 */
public final class GasCloudRenderType extends RenderType {

    /**
     * Shared texture: the exact same sprite file the INFESTIVE_GAS particle provider uses
     * ({@code addParticle(ModParticles.INFESTIVE_GAS, "infestive_gas")}).
     *
     * <p>The file is a 16x144 vertical animation strip holding 9 frames of 16x16. The render layer
     * emits V across the whole quad ({@code [0, 1]}) and the fragment shader divides V by
     * {@link #GAS_TEXTURE_FRAMES}, which maps the quad onto frame 0 (the top ninth) of the strip.
     * Using the particle texture directly (instead of a copy) keeps the 1.20.1 and 26.1.2
     * renderers visually identical.</p>
     */
    public static final ResourceLocation GAS_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/particle/infestive_gas.png");

    /**
     * Number of 16x16 frames packed vertically in {@link #GAS_TEXTURE}. Mirrors the GLSL
     * {@code GAS_TEXTURE_FRAMES}, which the fragment shader divides V by to select one band.
     */
    public static final float GAS_TEXTURE_FRAMES = 9.0F;

    /**
     * Which frame of {@link #GAS_TEXTURE} the fragment shader samples. Mirrors the GLSL
     * {@code GAS_FRAME_INDEX}.
     *
     * <p>Measured from {@code build/javac-check} with PIL: the strip's nine bands have mean alpha
     * 0.426, 0.355, 0.258, 0.188, 0.129, 0.090, 0.047, 0.020, 0.008 and in every band each covered
     * texel is fully opaque (255), i.e. band 0 (frame 0) is the most opaque one and is what the
     * shader must sample. Frame 0 is also the frame whose covered RGB average (137.7, 45.8, 45.8)
     * produced the red tint constants below.</p>
     */
    public static final float GAS_FRAME_INDEX = 0.0F;

    /** Red tint, normalised from the INFESTIVE_GAS texture (weighted average RGB 137,45,46). Mirrors {@code GAS_TINT_RGB}. */
    public static final float TINT_RED = 1.0F;
    public static final float TINT_GREEN = 0.33F;
    public static final float TINT_BLUE = 0.34F;

    /**
     * Dark-red colour of the water specks, written into the vertex colour together with the speck's
     * random alpha. It has no GLSL constant because {@code gas_cloud.fsh} never sees it: the speck
     * colour rides in the {@code Color.rgb} vertex attribute, which is exactly why the spec branch
     * of the shader can stay colour-agnostic.
     */
    public static final float SPEC_COLOR_RED = 0.42F;
    public static final float SPEC_COLOR_GREEN = 0.06F;
    public static final float SPEC_COLOR_BLUE = 0.06F;

    /**
     * Style channel value that turns a quad into a dark-red micro rectangle instead of a gas puff.
     *
     * <p>The render layer already has to write the sub-quad index into the {@code UV2.y} attribute
     * so the shader can offset its noise per sub-quad. Gas clouds use {@code 0..8}, so
     * {@code UV2.y == }{@value #SPEC_STYLE_CHANNEL} is unambiguous and {@code gas_cloud.fsh}'s
     * {@code GAS_SPEC_STYLE_CHANNEL} const uses it to select the hard-edged rectangle branch that
     * skips the texture, the radial falloff and the noise mask. The Java and GLSL values are
     * cross-checked by {@code build/javac-check/check-glsl.py} so they cannot drift apart.</p>
     */
    public static final int SPEC_STYLE_CHANNEL = 250;

    /**
     * Style channel value that turns a quad into the golden "Heart" of {@code epca:soul_protection}
     * instead of a gas puff or a water speck: the tall, irregular golden plasma/flame column of the
     * reference image.
     *
     * <p>Like {@link #SPEC_STYLE_CHANNEL} it rides in the {@code UV2.y} attribute, which already
     * carries the sub-quad index for gas clouds ({@code 0..8}); {@code 252} is therefore
     * unambiguous. {@code gas_cloud.fsh}'s {@code HEART_STYLE_CHANNEL} const selects the flame
     * branch, which draws the look procedurally and ignores the texture, the radial falloff and the
     * noise mask. The Java and GLSL values are cross-checked by
     * {@code build/javac-check/check-glsl.py} so they cannot drift apart.</p>
     *
     * <p>The name keeps the effect's "Heart" wording even though the look is a flame: the marker is
     * part of the 1.20.1 / 26.1.2 twin contract and both trees send this exact value. The quad itself
     * is built and submitted by {@code impl/client/entity/heart/SoulProtectionHeartRenderer}, which
     * reuses the shared camera-relative billboard path of {@link GasCloudRenderer}, writes this value
     * into the sub-quad channel and draws the column through the ordinary translucent variant (its
     * embers use {@link #getAdditive()}).</p>
     */
    public static final int HEART_STYLE_CHANNEL = 252;

    /**
     * Style channel value of the tiny golden embers drawn beside the {@link #HEART_STYLE_CHANNEL}
     * flame.
     *
     * <p>It is a branch of its own rather than a reuse of {@link #SPEC_STYLE_CHANNEL} because the
     * speck branch multiplies the vertex colour by the red {@code GAS_TINT_RGB}, which would turn a
     * gold ember red; the mote branch instead uses the shader's own {@code HEART_MOTE_COLOR}. A third
     * value in {@code UV2.y} keeps every existing gas and speck quad untouched.</p>
     */
    public static final int HEART_MOTE_STYLE_CHANNEL = 253;

    /**
     * Base factor at the head of the spec alpha chain. A hard-edged rectangle has no texture alpha to
     * reduce it, so the chain is just {@code Color.a * SPEC_BASE_ALPHA * ALPHA_BOOST * edgeFade}.
     * Mirrors the GLSL {@code GAS_SPEC_BASE_ALPHA} (cross-checked by {@code check-glsl.py}); it is
     * named rather than inlined so a future opacity tweak has one obvious place to happen, exactly
     * like the gas chain's {@code MIN_TEXTURE_ALPHA}.
     */
    public static final float SPEC_BASE_ALPHA = 1.0F;

    /** How strongly the tint replaces the raw texture colour. Mirrors {@code GAS_TINT_STRENGTH}. */
    public static final float TINT_STRENGTH = 0.85F;
    /** Overall noise-mask strength. Mirrors {@code GAS_MASK_STRENGTH}. */
    public static final float MASK_STRENGTH = 1.0F;
    /** How strongly the noise cuts into the mask (gentler than before). Mirrors {@code GAS_MASK_DEPTH}. */
    public static final float MASK_DEPTH = 0.5F;
    /**
     * Floor applied to the texture alpha, i.e. {@code max(tex.a, GAS_MIN_TEXTURE_ALPHA)}. Mirrors
     * the GLSL {@code GAS_MIN_TEXTURE_ALPHA}: the sprite's alpha is binary (0 or 255), so without
     * this floor most of the quad multiplied to ~0.
     */
    public static final float MIN_TEXTURE_ALPHA = 0.65F;
    /** Floor applied to the procedural noise mask. Mirrors {@code GAS_MIN_NOISE_MASK}. */
    public static final float MIN_NOISE_MASK = 0.6F;
    /**
     * Overall scale of the alpha chain. Mirrors the GLSL {@code GAS_ALPHA_BOOST}:
     * {@code alpha = Color.a * GAS_ALPHA_BOOST * max(tex.a, MIN_TEXTURE_ALPHA)
     * * radial * max(mask, MIN_NOISE_MASK) * MASK_STRENGTH}, so with the radial plateau at 1.0 and
     * the mask at its upper end a fresh longarms active cloud and a fresh yelloweye cloud
     * ({@code Color.a = 0.90}, identical by spec) peak at 0.765 and the low-opacity longarms
     * passive cloud ({@code Color.a = 0.45}) peaks at 0.383; the guaranteed floor inside the
     * plateau is 0.298 / 0.149 respectively.
     */
    public static final float ALPHA_BOOST = 0.85F;
    /**
     * Radius (in normalised quad space, 0 at the centre and 1 at the edge midpoint) up to which
     * the radial falloff stays at 1.0. Mirrors {@code GAS_RADIAL_PLATEAU}; the cloud therefore
     * keeps full density over the inner ~55% of the quad and only eases out towards the rim.
     */
    public static final float RADIAL_PLATEAU = 0.55F;
    /** Noise frequency in quad space. Mirrors {@code GAS_NOISE_UV_SCALE}. */
    public static final float NOISE_UV_SCALE = 2.4F;
    /** Distance at which the far fade starts. Mirrors {@code GAS_DISTANCE_FADE_START}. */
    public static final float DISTANCE_FADE_START = 48.0F;
    /** Distance at which the far fade completes. Mirrors {@code GAS_DISTANCE_FADE_END}. */
    public static final float DISTANCE_FADE_END = 96.0F;
    /** Expands the packed seed to its shader-side range. Mirrors {@code GAS_SEED_SCALE}. */
    public static final float SEED_SCALE = 0.004F;
    /** Expands the sub-quad index to a per-sub-quad noise offset. Mirrors {@code GAS_SUBQUAD_SEED_SCALE}. */
    public static final float SUBQUAD_SEED_SCALE = 13.73F;

    /**
     * Fixed-point scale applied to the {@code [0,100)} cloud render seed before it is packed into
     * the {@code UV2.x} attribute.
     */
    public static final float SEED_PACK_SCALE = 250.0F;

    /**
     * Maximum value that may be written to {@code UV2}. {@code VertexFormatElement.ELEMENT_UV2} is
     * {@code Type.SHORT} x2 (signed 16-bit), so anything above this would wrap negative.
     */
    public static final int MAX_PACKED_UV2 = Short.MAX_VALUE;

    private static final int BUFFER_SIZE = RenderType.TRANSIENT_BUFFER_SIZE;
    private static final String RENDER_TYPE_NAME = "epca_gas_cloud";
    /** Name of the additive variant, so the two render types are distinguishable in diagnostics. */
    private static final String ADDITIVE_RENDER_TYPE_NAME = "epca_gas_cloud_additive";

    private static ShaderInstance gasCloudShader;
    /** Latches the single "this render type was flushed at least once" diagnostic line. */
    private static boolean debugFlushed;

    private static final RenderStateShard.ShaderStateShard GAS_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(GasCloudRenderType::getShader);

    /** Binds the shared gas texture so the shader's Sampler0 reads it. */
    private static final RenderStateShard.TextureStateShard GAS_TEXTURE_STATE =
            new RenderStateShard.TextureStateShard(GAS_TEXTURE, false, false);

    /** Ordinary translucent variant; used by the gas clouds and the water specks. */
    private static final RenderType GAS_CLOUD = new GasCloudRenderType(RENDER_TYPE_NAME, false);

    /**
     * Additive variant of the very same pipeline: same core shader, same vertex format, same texture
     * and depth state, only {@code ADDITIVE_TRANSPARENCY} instead of {@code TRANSLUCENT_TRANSPARENCY}.
     *
     * <p>It exists for the tiny golden embers that {@code SoulProtectionHeartRenderer} draws beside
     * the {@code epca:soul_protection} flame column: with {@code SRC_ALPHA / ONE} a few pixels of
     * near-white gold read as a glint. The column itself uses the ordinary translucent variant,
     * because additive blending saturates the whole quad against a lit world and turns the flame into
     * one flat golden block. Nothing else uses it, so the gas and speck quads keep their exact
     * previous blend.</p>
     */
    private static final RenderType GAS_CLOUD_ADDITIVE =
            new GasCloudRenderType(ADDITIVE_RENDER_TYPE_NAME, true);

    private final int bufferSize;
    private final String renderTypeName;

    private GasCloudRenderType(String renderTypeName, boolean additive) {
        super(renderTypeName, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, BUFFER_SIZE, false, false,
                () -> {
                    if (GasCloudManager.DEBUG && !debugFlushed) {
                        debugFlushed = true;
                        epca.LOGGER.info("[gascloud] render type: first flush of {} - setup state ran "
                                        + "while shaderReady={}", renderTypeName, isShaderReady());
                    }
                    GAS_TEXTURE_STATE.setupRenderState();
                    GAS_SHADER_STATE.setupRenderState();
                    // The one difference between the two variants.
                    if (additive) {
                        ADDITIVE_TRANSPARENCY.setupRenderState();
                    } else {
                        TRANSLUCENT_TRANSPARENCY.setupRenderState();
                    }
                    LEQUAL_DEPTH_TEST.setupRenderState();
                    // Colour writes on, depth writes off.
                    COLOR_WRITE.setupRenderState();
                    NO_CULL.setupRenderState();
                    NO_LIGHTMAP.setupRenderState();
                    NO_OVERLAY.setupRenderState();
                },
                () -> {
                    NO_OVERLAY.clearRenderState();
                    NO_LIGHTMAP.clearRenderState();
                    NO_CULL.clearRenderState();
                    COLOR_WRITE.clearRenderState();
                    LEQUAL_DEPTH_TEST.clearRenderState();
                    if (additive) {
                        ADDITIVE_TRANSPARENCY.clearRenderState();
                    } else {
                        TRANSLUCENT_TRANSPARENCY.clearRenderState();
                    }
                    GAS_SHADER_STATE.clearRenderState();
                    GAS_TEXTURE_STATE.clearRenderState();
                });
        this.bufferSize = BUFFER_SIZE;
        this.renderTypeName = renderTypeName;
    }

    /**
     * Called from the mod-bus {@code RegisterShadersEvent} handler with the freshly built shader
     * instance. Passing {@code null} clears the reference.
     */
    public static void registerShader(ShaderInstance shader) {
        gasCloudShader = shader;
        if (GasCloudManager.DEBUG) {
            epca.LOGGER.info("[gascloud] shader: RegisterShadersEvent consumer ran, epca:gas_cloud ready={}",
                    shader != null);
        }
    }

    /** True once the custom core shader finished compiling and linking. */
    public static boolean isShaderReady() {
        return gasCloudShader != null;
    }

    /** The shared render type; only valid to draw with while {@link #isShaderReady()} is true. */
    public static RenderType get() {
        return GAS_CLOUD;
    }

    /**
     * The additive variant of the same pipeline, for the soul-protection embers; only valid to draw
     * with while {@link #isShaderReady()} is true.
     */
    public static RenderType getAdditive() {
        return GAS_CLOUD_ADDITIVE;
    }

    /**
     * {@code 1 / }{@link #GAS_TEXTURE_FRAMES}: the Java mirror of the divisor the fragment shader
     * applies to the quad's V coordinate to select frame 0 of the animation strip.
     *
     * <p>The render layer emits V over the whole quad and leaves the division to the shader, so
     * nothing calls this helper; it documents the exact mapping for callers that want to emit a
     * frame-0-only V range directly.</p>
     */
    public static float getFrameZeroVScale() {
        return 1.0F / GAS_TEXTURE_FRAMES;
    }

    /**
     * Packs a cloud's render seed into the value written to the {@code UV2.x} vertex attribute.
     *
     * <p>Clamped to {@link #MAX_PACKED_UV2} because the lightmap attribute element is a signed
     * short.</p>
     *
     * @param renderSeed the cloud's render seed, expected in {@code [0,100)}
     * @return a value in {@code [0, Short.MAX_VALUE]}
     */
    public static int packSeed(float renderSeed) {
        int packed = (int) (renderSeed * SEED_PACK_SCALE);
        return Mth.clamp(packed, 0, MAX_PACKED_UV2);
    }

    private static ShaderInstance getShader() {
        return gasCloudShader;
    }

    @Override
    public int bufferSize() {
        return this.bufferSize;
    }

    @Override
    public VertexFormat format() {
        return DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
    }

    @Override
    public VertexFormat.Mode mode() {
        return VertexFormat.Mode.QUADS;
    }

    @Override
    public boolean canConsolidateConsecutiveGeometry() {
        return false;
    }

    @Override
    public Optional<RenderType> outline() {
        return Optional.empty();
    }

    @Override
    public boolean isOutline() {
        return false;
    }

    @Override
    public boolean affectsCrumbling() {
        return false;
    }

    @Override
    public String toString() {
        return this.renderTypeName;
    }
}
