package org.tdddd.epca.impl.client.render.araya;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class ArayaSlashRenderType extends RenderType {

    private static final String NAME_GLOW = "epca_araya_slash_glow";
    private static final String NAME_REFRACTION = "epca_araya_slash_refraction";

    private static final RenderStateShard.ShaderStateShard SLASH_SHADER =
            new RenderStateShard.ShaderStateShard(ArayaSlashShaders::arayaSlashShader);

    private static RenderType glow;
    private static RenderType refraction;

    private ArayaSlashRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType glow() {
        if (glow == null) {
            glow = create(NAME_GLOW);
        }
        return glow;
    }

    public static RenderType refraction() {
        if (refraction == null) {
            refraction = create(NAME_REFRACTION);
        }
        return refraction;
    }

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
