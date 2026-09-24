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
 *   <li>the <b>12 atlas rectangles</b> disappear because the twelve strips are packed into two
 *       generated textures bound once for the whole draw: {@link #SPRITE_SHEET} (the strips
 *       concatenated, sampled as {@code Sampler0}) and {@link #SPRITE_SCHEDULE_LUT} (the phase -&gt;
 *       band lookup table, sampled as {@code Sampler1}). The per-strip constants left over are the
 *       sheet band ranges and the cycle lengths, which are baked into the fragment stage as
 *       {@code COSMIC_SHEET_BASE} / {@code COSMIC_CYCLE} (see {@link #SPRITE_SHEET_BAND_BASE} and
 *       {@link #SPRITE_FRAME_SCHEDULES});</li>
 *   <li>the <b>rim/void/flash colours</b> are rebuilt in the fragment stage from {@code breakAmount}
 *       with the same lerp chain {@link SkyRuptureEffect} exposes;</li>
 *   <li>the 1.20.1 <b>{@code seed}</b> uniform is dropped: the fragment stage never consumed it (it
 *       only ever reached the diagnostic log line).</li>
 * </ul>
 *
 * <p>What remains is genuinely per-frame, and it fits the five writers exactly:</p>
 * <pre>
 *   loc attribute      element   writer          payload
 *   0   Position       POSITION  addVertex       xy = 0..1 screen quad, z unused
 *   1   Color          COLOR     setColor        breakAmount, fade, 1, 1
 *   2   TimeProgress   UV0       setUv           time (effect seconds), rupture progress 0..1
 *   3   SkyDark        UV1       setUv1          skyDarkProgress, skyDarkOpacity
 *   4   Pattern        UV2       setUv2          crack field offset X, Y
 * </pre>
 * <p>Eleven float slots, five attributes, and every declared element is written on every vertex,
 * which 26.1.2 requires (a short vertex is rejected). The two 16-bit slots carry <b>unsigned</b>
 * fixed point: {@code setUv1}/{@code setUv2} narrow their argument to {@code short} and the vertex
 * array binds the element with {@code glVertexAttribIFormat(GL_SHORT)}, so {@code sky_rupture.vsh}
 * masks both slots with {@code 0xFFFF} while decoding them.</p>
 *
 * <h2>One draw for all twelve shells</h2>
 * 1.20.1 drew a single quad whose fragment stage looped the twelve shells and summed them into one
 * source colour. An earlier revision of this port instead drew one quad per sprite; that recomputed
 * the crack field, the noise and the nebula once per shell (twelve times per pixel) and, because the
 * twelve draws composite into the framebuffer in sequence rather than summing them first, it also
 * changed the image. This class therefore exposes a single render type and
 * {@link SkyRuptureRenderer} submits a single quad, which is what makes the shared work run once per
 * pixel.
 *
 * <h2>Location binding</h2>
 * The attribute location is the position of the name in {@link #SKY_RUPTURE_VERTEX_FORMAT}:
 * {@code GlProgram.link} iterates {@code VertexFormat#getElementAttributeNames()} and calls
 * {@code glBindAttribLocation(program, i, name)} with a running {@code i}, which is exactly how the
 * shipped {@code GasCloudRenderType} feeds its {@code in} declarations (and why that format may
 * declare {@code Color} before {@code UV0}/{@code UV1} while their element ids are 1, 2 and 3).
 */
public final class SkyRuptureShaders {

    /** Star shell / sprite count. Matches {@code STAR_SHELLS} and {@code COSMIC_COUNT} in the GLSL. */
    public static final int SPRITE_COUNT = 12;

    /**
     * Frame counts of the 12 shipped sprite strips, derived from the asset heights
     * ({@code assets/epca/textures/shader/cosmic_N.png} is 16 px wide and 64, 64, 80, 80, 64, 64, 96,
     * 64, 112, 48, 112, 16 px tall respectively, i.e. 4, 4, 5, 5, 4, 4, 6, 4, 7, 3, 7 and 1 frames).
     *
     * <p>These are duplicated in {@code sky_rupture.fsh} as the differences of
     * {@code COSMIC_SHEET_BASE} because GLSL cannot read a Java array;
     * {@code build/javac-check/check-sky-glsl.py} and {@code check-sky-contract.py} cross-check them
     * against the real PNG headers so they cannot drift.</p>
     */
    public static final int[] SPRITE_FRAME_COUNTS = {4, 4, 5, 5, 4, 4, 6, 4, 7, 3, 7, 1};

    /**
     * The full animation timeline of every strip, derived from its {@code .mcmeta}.
     *
     * <p>Each entry is the ordered list of {@code (band index, hold ticks)} that vanilla's
     * {@code AnimationMetadataSection} produced, i.e. the list order with each entry's own
     * {@code time} (or the file's {@code frametime} when the entry has no explicit {@code time}).
     * Seven of the twelve files carry an explicit {@code frames} list whose band 0 is held for
     * several separate stretches, so a single ticks-per-frame number cannot reproduce them; the port
     * therefore expands this timeline into {@link #SPRITE_SCHEDULE_LUT} instead. This table exists for
     * the documentation of the parsed values and is cross-checked against the {@code .mcmeta} files
     * (and against the generated textures) by {@code build/javac-check/check-sky-parity.py} and
     * {@code check-sky-contract.py}; the shader never reads it through Java.</p>
     */
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

    /**
     * First band of every strip inside {@link #SPRITE_SHEET}, plus the sheet's total band count.
     *
     * <p>The sheet is 16 px wide with one 16 px band per animation frame, the strips concatenated in
     * order, so strip {@code i} owns the bands
     * {@code [SPRITE_SHEET_BAND_BASE[i], SPRITE_SHEET_BAND_BASE[i + 1])} and the last entry (54) is
     * the divisor of the shader's V coordinate. The values are the running sum of
     * {@link #SPRITE_FRAME_COUNTS} and are baked into the shader as {@code COSMIC_SHEET_BASE}.</p>
     */
    public static final int[] SPRITE_SHEET_BAND_BASE =
            {0, 4, 8, 13, 18, 22, 26, 32, 36, 43, 46, 53, 54};

    /**
     * The packed strip texture, {@code Sampler0}: {@code assets/epca/textures/sky/cosmic_sheet.png}
     * (16 x 864, 54 bands), generated from the twelve {@code textures/shader/cosmic_N.png} strips by
     * {@code build/javac-check/cosmic_sheet.py}. It replaces the 1.20.1 block-atlas lookup and is what
     * lets one draw sample any of the twelve sprites.
     *
     * <p>It deliberately does <b>not</b> live under {@code textures/shader/}: that directory is a
     * block-atlas source in {@code assets/minecraft/atlases/blocks.json}, and an atlas copy of a
     * texture the shader binds directly would be pure waste.</p>
     */
    public static final Identifier SPRITE_SHEET =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/sky/cosmic_sheet.png");

    /**
     * The phase -&gt; band lookup table, {@code Sampler1}:
     * {@code assets/epca/textures/sky/cosmic_schedule.png} (128 x 12). Row = strip index, column =
     * {@code tick mod cycle}, red = the band that strip shows at that tick; green carries the cycle and
     * blue the frame count for the checkers. It turns the fragment stage's frame lookup into one
     * {@code texelFetch} instead of a walk over up to 22 timeline entries per shell.
     */
    public static final Identifier SPRITE_SCHEDULE_LUT =
            Identifier.fromNamespaceAndPath(epca.MODID, "textures/sky/cosmic_schedule.png");

    /**
     * The twelve source strips the two generated textures are built from.
     *
     * <p>Nothing binds these any more (they are packed into {@link #SPRITE_SHEET} at build time), but
     * they are listed so the tree, the baker and
     * {@code build/javac-check/check-texture-bindings.py} agree on the same source files, and they must
     * keep their names: the atlas definition still stitches this directory.</p>
     */
    public static final Identifier[] SPRITE_TEXTURES = new Identifier[SPRITE_COUNT];

    static {
        for (int i = 0; i < SPRITE_COUNT; i++) {
            SPRITE_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    epca.MODID, "textures/shader/cosmic_" + i + ".png");
        }
    }

    /** Shader id; {@code FileToIdConverter("shaders", ".vsh"/".fsh")} maps it to the two assets. */
    public static final Identifier SKY_RUPTURE_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/sky_rupture");

    /**
     * Vertex layout, submitted as a 0..1 full-screen quad. See the class comment for the payload of
     * each slot. The format is unchanged at 32 bytes (12 + 4 + 8 + 4 + 4) and every attribute is
     * 4-byte aligned; what changed is how the two 16-bit slots are decoded, not the format.
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
     *   <li>two samplers: {@code Sampler0} (the packed strips) and {@code Sampler1} (the phase table).
     *       Both are plain 2D textures with no `texture` metadata, so they use the texture defaults
     *       (REPEAT wrap, NEAREST min/mag, no mip chain). That is harmless here: the lookup is a
     *       {@code texelFetch} (no filtering) and the star sprites are magnified by 2-5x, so the
     *       sampled LOD is negative and minification never engages - the same conclusion as for the
     *       earlier per-strip binding.</li>
     * </ul>
     */
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

    /** Name of the one render type, used for diagnostics. */
    public static final String RENDER_TYPE_NAME = "epca_sky_rupture";

    private static boolean pipelineRegistered;
    private static RenderType renderType;

    private SkyRuptureShaders() {
    }

    /**
     * Mod-bus handler for {@link RegisterRenderPipelinesEvent}. Until it has run the pipeline is not
     * part of the pipeline registry and {@link #getRenderType()} refuses to build.
     */
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

    /** True once {@link #registerPipeline} ran. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /**
     * The render type of the sky rupture draw, or {@code null} while the pipeline is not registered
     * yet.
     *
     * <p>There is exactly one, because all twelve shells are rendered by one draw (see the class
     * comment). It is created lazily: {@code RenderSetup} resolves its textures through a supplier at
     * first draw, while the pipeline object itself only becomes valid inside the registration
     * event.</p>
     */
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
