package org.tdddd.epca.impl.client.blackhole;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.tdddd.epca.impl.epca;

public abstract class BlackHoleLensRenderType extends RenderType {

    public static final RenderType LENS = RenderType.create(
            epca.MODID + ":blackhole_lens",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(BlackHoleShaders::lens))
                    .setTextureState(new RenderStateShard.TextureStateShard(BlackHoleSceneCopy.TEXTURE_ID, false, false))
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    private BlackHoleLensRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                    boolean affectsCrumbling, boolean sortOnUpload,
                                    Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}