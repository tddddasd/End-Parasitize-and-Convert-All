package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The two render types the Alayavijnana slash is drawn through.
 *
 * <h2>Why there are two and what tells them apart</h2>
 * <p>Both draw the same program over the same vertex data; the difference is <b>what is bound to
 * Sampler0</b>. {@link #glow()} is there for the state (blending, depth, culling) and reads the frame
 * copy at its own pixel, {@link #refraction()} is the same state with the copy the border actually
 * samples. The refraction itself is in the vertex data - the offset grows with the distance from the
 * blade's centreline - so neither type needs a "refraction mode" or a second shader.</p>
 *
 * <h2>The vertex format</h2>
 * <p>Vanilla {@link DefaultVertexFormat#POSITION_COLOR_TEX}: position, colour and one UV. That single UV
 * carries two numbers (distance from the centreline and the strip's half width, both in blocks), which is
 * enough for the whole effect and is why this class does not need a custom vertex format - 1.20.1 keeps
 * {@code VertexFormatElement}'s shared instances private, so a custom format would have to build the
 * elements by hand for no gain.</p>
 */
public final class ArayaSlashRenderType extends RenderType {

    private static final String NAME_GLOW = "epca_araya_slash_glow";
    private static final String NAME_REFRACTION = "epca_araya_slash_refraction";

    /**
     * The slash program, bound lazily through the shader instance the mod registers, so the two render
     * types can be built before the shader finishes loading.
     */
    private static final RenderStateShard.ShaderStateShard SLASH_SHADER =
            new RenderStateShard.ShaderStateShard(ArayaSlashShaders::arayaSlashShader);

    private static RenderType glow;
    private static RenderType refraction;

    private ArayaSlashRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /** The glowing line and its halo, drawn with the scene copy bound. */
    public static RenderType glow() {
        if (glow == null) {
            glow = create(NAME_GLOW);
        }
        return glow;
    }

    /** The transparent refracting border: Sampler0 is the per-frame copy of the frame behind the slash. */
    public static RenderType refraction() {
        if (refraction == null) {
            refraction = create(NAME_REFRACTION);
        }
        return refraction;
    }

    /**
     * One slash render type: translucent blending, depth writes off, culling off and a {@code LEQUAL}
     * depth test - the same recipe the ritual aura uses - plus the texture state that binds the scene
     * copy. The buffers are transient-sized because one slash is a handful of quads.
     */
    private static RenderType create(String name) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(SLASH_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(ArayaSceneCopy.TEXTURE_ID, false,
                        false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .createCompositeState(false);
        return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS,
                1536, false, true, state);
    }
}
