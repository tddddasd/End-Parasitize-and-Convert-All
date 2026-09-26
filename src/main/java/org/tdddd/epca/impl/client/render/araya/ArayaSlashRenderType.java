package org.tdddd.epca.impl.client.render.araya;

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
 * The render type the Alayavijnana slash is drawn through.
 *
 * <h2>One type, two draws</h2>
 * <p>The glowing line and its halo are drawn first and the refracting border second, but both go through
 * this single type and the same program: there is no "refraction mode", because the offset is already in
 * the vertex data (it grows with the distance from the blade's centreline, see {@code araya_slash.vsh}).
 * The only thing sampled is the per-frame scene copy {@link ArayaSceneCopy} publishes, so the border
 * really refracts the finished frame.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>The 1.20.1 twin is a {@code RenderType} subclass assembling {@code RenderStateShard}s and binding a
 * {@code ShaderInstance} registered through {@code RegisterShadersEvent}. 26.1.2 has neither: the whole
 * render-state block is a {@link RenderPipeline} declared here and registered through
 * {@link RegisterRenderPipelinesEvent} (the pattern {@code RitualQuadRenderType} already uses), the
 * shader is a pair of mod core-shader assets resolved from the pipeline's location, and the sampler is
 * declared with {@code withSampler} and bound by name in the {@link RenderSetup}.</p>
 */
public final class ArayaSlashRenderType {

    /** Render type and pipeline name. */
    public static final String NAME = "epca_araya_slash";

    /** Shader id; resolves to {@code assets/epca/shaders/core/araya_slash.vsh/.fsh}. */
    public static final Identifier SLASH_SHADER =
            Identifier.fromNamespaceAndPath(epca.MODID, "core/araya_slash");

    /**
     * The slash pipeline: translucent blending, depth test on with depth writes off, culling off (the
     * blade is seen from both faces) and one sampler for the frame copy. It mirrors the 1.20.1 recipe one
     * for one.
     */
    public static final RenderPipeline SLASH_PIPELINE =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(epca.MODID, "pipeline/araya_slash"))
                    .withVertexShader(SLASH_SHADER)
                    .withFragmentShader(SLASH_SHADER)
                    .withSampler("Sampler0")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

    private static boolean pipelineRegistered;
    private static RenderType slash;

    private ArayaSlashRenderType() {
    }

    /** Mod-bus handler for {@link RegisterRenderPipelinesEvent}. */
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SLASH_PIPELINE);
        pipelineRegistered = true;
    }

    /** True once {@link #registerPipeline} ran. */
    public static boolean isPipelineRegistered() {
        return pipelineRegistered;
    }

    /**
     * The shared slash render type, or {@code null} while the pipeline is not registered.
     *
     * <p>Created lazily: {@code RenderSetup} resolves its texture through the texture manager at first
     * draw, while the pipeline object itself only becomes valid inside the registration event.</p>
     */
    public static RenderType get() {
        if (!pipelineRegistered) {
            return null;
        }
        if (slash == null) {
            slash = RenderType.create(NAME,
                    RenderSetup.builder(SLASH_PIPELINE)
                            .withTexture("Sampler0", ArayaSceneCopy.TEXTURE_ID)
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup());
        }
        return slash;
    }
}
