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

/**
 * Pipeline and render types of the world barrier rupture shader.
 *
 * <h2>1.20.1 -&gt; 26.1.2: no ShaderInstance, no core shader JSON, no per-draw uniforms</h2>
 * The 1.20.1 twin loaded {@code shaders/core/sky_rupture.json} as a {@code ShaderInstance} through
 * {@code RegisterShadersEvent} and read 15 {@code Uniform} handles off it. 26.1.2 has none of that:
 * the shader is bound through a {@link RenderPipeline} registered with
 * {@link RegisterRenderPipelinesEvent}, the render state that used to live in the JSON now lives in
 * the pipeline, and {@code epca:core/sky_rupture} is resolved by {@code ShaderType#idConverter()} to
 * {@code assets/epca/shaders/core/sky_rupture.vsh/.fsh}.
 *
 * <p>Per-draw values are the interesting problem. {@code RenderType#draw} always writes
 * {@code ColorModulator = (1,1,1,1)} into the shared {@code DynamicTransforms} buffer, and
 * {@code SubmitNodeCollector#submitCustomGeometry} hands the callback only a pose and a
 * {@code VertexConsumer}, so a mod cannot reach a per-draw uniform on this path. The payload
 * therefore travels in the <b>vertex attributes</b>, which 26.1.2 caps hard: {@code BufferBuilder}
 * exposes only {@code addVertex}, {@code setColor}, {@code setUv}, {@code setUv1}, {@code setUv2},
 * {@code setNormal} and {@code setLineWidth} (verified with {@code javap -c} on the patched jar), and
 * a custom {@link VertexFormatElement} can be declared but never written, because nothing addresses
 * one.</p>
 *
 * <h2>How the payload fits in eleven floats</h2>
 * The 1.20.1 payload was 9 floats of camera basis + ~7 scalars + 24 floats of atlas rectangles. The
 * first and third groups are eliminated rather than compressed:
 * <ul>
 *   <li>the <b>camera basis</b> is reconstructed in {@code sky_rupture.vsh} from the pipeline's own
 *       {@code Projection} and {@code DynamicTransforms} UBOs, which
 *       {@link RenderPipelines#MATRICES_PROJECTION_SNIPPET} always binds. That needs zero slots and is
 *       more accurate than 1.20.1's {@code tan(fov/2)} reconstruction, since it uses the real
 *       projection matrix;</li>
 *   <li>the <b>12 atlas rectangles</b> disappear because each draw binds one sprite as its own direct
 *       texture, so a sprite's UV space is simply [0,1]^2. This is the same pattern the shipped
 *       {@code GasCloudRenderType} uses for the gas/puff texture, and it means the effect no longer
 *       depends on the block atlas layout at all. The only residual per-sprite constant, the strip's
 *       frame count, is baked into the fragment stage as {@code COSMIC_FRAMES} (see
 *       {@link #SPRITE_FRAME_COUNTS});</li>
 *   <li>the <b>rim/void/flash colours</b> are rebuilt in the fragment stage from {@code breakAmount}
 *       with the same lerp chain {@link SkyRuptureEffect} exposes;</li>
 *   <li>the 1.20.1 <b>{@code seed}</b> uniform is dropped: the fragment stage never consumed it (it
 *       only ever reached the diagnostic log line).</li>
 * </ul>
 *
 * <p>What remains is genuinely per-frame, and it fits the five writers exactly:</p>
 * <pre>
 *   loc attribute      element   writer          payload
 *   0   Position       POSITION  addVertex       xy = 0..1 screen quad, z = star shell index
 *   1   Color          COLOR     setColor        breakAmount, fade, 1, 1
 *   2   TimeProgress   UV0       setUv           time (seconds), rupture progress 0..1
 *   3   SkyDark        UV1       setUv1          skyDarkProgress, skyDarkOpacity
 *   4   Pattern        UV2       setUv2          crack field offset X, Y
 * </pre>
 * <p>Eleven float slots, five attributes, and every declared element is written on every vertex,
 * which 26.1.2 requires (a short vertex is rejected).</p>
 *
 * <h2>Location binding</h2>
 * The attribute location is the position of the name in {@link #SKY_RUPTURE_VERTEX_FORMAT}:
 * {@code GlProgram.link} iterates {@code VertexFormat#getElementAttributeNames()} and calls
 * {@code glBindAttribLocation(program, i, name)} with a running {@code i}, which is exactly how the
 * shipped {@code GasCloudRenderType} feeds its {@code in} declarations (and why that format may
 * declare {@code Color} before {@code UV0}/{@code UV1} while their element ids are 1, 2 and 3).</p>
 */
public final class SkyRuptureShaders {

    /** Star shell / sprite count. Matches {@code STAR_SHELLS} and {@code COSMIC_COUNT} in the GLSL. */
    public static final int SPRITE_COUNT = 12;

    /**
     * Frame counts of the 12 shipped sprite strips, derived from the asset heights
     * ({@code assets/epca/textures/shader/cosmic_N.png} is 16 px wide and 64, 64, 80, 80, 64, 64, 96,
     * 64, 112, 48, 112, 16 px tall respectively, i.e. 4, 4, 5, 5, 4, 4, 6, 4, 7, 3, 7 and 1 frames).
     *
     * <p>These are duplicated in {@code sky_rupture.fsh} as {@code COSMIC_FRAMES} because GLSL cannot
     * read a Java array; {@code build/javac-check/check-sky-glsl.py} cross-checks the two against the
     * real PNG headers so they cannot drift.</p>
     */
    public static final int[] SPRITE_FRAME_COUNTS = {4, 4, 5, 5, 4, 4, 6, 4, 7, 3, 7, 1};

    /** Shader id; {@code FileToIdConverter("shaders", ".vsh"/".fsh")} maps it to the two assets. */
    public static final Identifier SKY_RUPTURE_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/sky_rupture");

    /**
     * The 12 star sprite textures, bound one per draw.
     *
     * <p>1.20.1 sampled them out of the block atlas and carried their animated UV rectangles in a
     * {@code mat2 cosmicuvs[12]} uniform. Binding each strip directly removes the need for any
     * rectangle in the vertex stream, at the cost that a directly bound {@code SimpleTexture} uploads
     * the raw strip without applying its {@code .mcmeta} timings - which is why the fragment stage
     * animates the strips itself (see {@link #SPRITE_FRAME_COUNTS}).</p>
     */
    public static final Identifier[] SPRITE_TEXTURES = new Identifier[SPRITE_COUNT];

    static {
        for (int i = 0; i < SPRITE_COUNT; i++) {
            SPRITE_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    epca.MODID, "textures/shader/cosmic_" + i + ".png");
        }
    }

    /**
     * Vertex layout, submitted as a 0..1 full-screen quad. See the class comment for the payload of
     * each slot.
     */
    public static final VertexFormat SKY_RUPTURE_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("TimeProgress", VertexFormatElement.UV0)
            .add("SkyDark", VertexFormatElement.UV1)
            .add("Pattern", VertexFormatElement.UV2)
            .build();

    /**
     * The custom pipeline. {@link RenderPipelines#MATRICES_PROJECTION_SNIPPET} contributes the
     * {@code DynamicTransforms} and {@code Projection} uniform buffers, which are exactly the two
     * buffers {@code RenderType#draw} and {@code RenderSystem#bindDefaultUniforms} always bind - and
     * they are also what the vertex stage reconstructs the camera basis from.
     *
     * <p>Render state, mirroring the 1.20.1 {@code SkyRuptureRenderType} recipe:</p>
     * <ul>
     *   <li>depth: {@link CompareOp#LESS_THAN_OR_EQUAL} with {@code writeDepth = false}. The quad sits
     *       on the far plane ({@code z = 1}) and the depth test is what restricts it to sky pixels;
     *       writing depth is avoided so the first-person hand and the clouds are unaffected. This is
     *       the state the 1.20.1 {@code LEQUAL_DEPTH_TEST + COLOR_WRITE} pair expressed.</li>
     *   <li>blend: {@link BlendFunction#TRANSLUCENT} (SrcAlpha / OneMinusSrcAlpha), the 1.20.1
     *       {@code TRANSLUCENT_TRANSPARENCY}, so the intact parts of the barrier let the vanilla sky
     *       show through.</li>
     *   <li>culling off: a full-screen quad does not care about winding.</li>
     *   <li>no lightmap and no overlay sampler: the pipeline declares neither, so the effect is
     *       unlit.</li>
     * </ul>
     */
    public static final RenderPipeline SKY_RUPTURE_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/sky_rupture"))
                    .withVertexShader(SKY_RUPTURE_SHADER)
                    .withFragmentShader(SKY_RUPTURE_SHADER)
                    .withSampler("Sampler0")
                    .withVertexFormat(SKY_RUPTURE_VERTEX_FORMAT, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    /** Name of one per-sprite render type, used for diagnostics. */
    public static final String RENDER_TYPE_NAME_PREFIX = "epca_sky_rupture_";

    private static boolean pipelineRegistered;
    private static final RenderType[] RENDER_TYPES = new RenderType[SPRITE_COUNT];

    private SkyRuptureShaders() {
    }

    /**
     * Mod-bus handler for {@link RegisterRenderPipelinesEvent}. Until it has run the pipeline is not
     * part of the pipeline registry and {@link #getRenderType(int)} refuses to build.
     */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SKY_RUPTURE_PIPELINE);
        pipelineRegistered = true;
        epca.LOGGER.info("[epca-render] sky rupture pipeline registered: {} shaders={} format={} samplers={}",
                SKY_RUPTURE_PIPELINE.getLocation(), SKY_RUPTURE_SHADER,
                SKY_RUPTURE_VERTEX_FORMAT.getElementAttributeNames(),
                SKY_RUPTURE_PIPELINE.getSamplers());
        epca.LOGGER.info("[epca-render] sky rupture vertex size = {} bytes, {} attributes, {} sprites",
                SKY_RUPTURE_VERTEX_FORMAT.getVertexSize(),
                SKY_RUPTURE_VERTEX_FORMAT.getElements().size(), SPRITE_COUNT);
    }

    /** True once {@link #registerPipeline} ran. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /**
     * The render type of one star shell, or {@code null} while the pipeline is not registered yet.
     *
     * <p>One render type per sprite, because the sprite texture is part of the render setup. They are
     * created lazily: {@code RenderSetup} resolves its texture through a supplier at first draw, but
     * the pipeline object itself only becomes valid inside the registration event.</p>
     */
    public static RenderType getRenderType(int sprite) {
        if (!pipelineRegistered || sprite < 0 || sprite >= SPRITE_COUNT) {
            return null;
        }
        RenderType cached = RENDER_TYPES[sprite];
        if (cached == null) {
            cached = RenderType.create(RENDER_TYPE_NAME_PREFIX + sprite,
                    RenderSetup.builder(SKY_RUPTURE_PIPELINE)
                            .withTexture("Sampler0", SPRITE_TEXTURES[sprite])
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
            RENDER_TYPES[sprite] = cached;
        }
        return cached;
    }
}
