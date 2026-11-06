package org.tdddd.epca.impl.client.render.ritual;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class RitualQuadRenderType extends RenderType {

    private static final String NAME = "epca_ritual_quads";

    private static final RenderStateShard.ShaderStateShard RITUAL_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader);

    private static final RenderType RITUAL_QUADS = new RitualQuadRenderType();

    private RitualQuadRenderType() {
        super(NAME, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                RenderType.TRANSIENT_BUFFER_SIZE, false, false,
                () -> {
                    RITUAL_SHADER_STATE.setupRenderState();
                    TRANSLUCENT_TRANSPARENCY.setupRenderState();
                    LEQUAL_DEPTH_TEST.setupRenderState();

                    COLOR_WRITE.setupRenderState();
                    NO_CULL.setupRenderState();
                },
                () -> {
                    NO_CULL.clearRenderState();
                    COLOR_WRITE.clearRenderState();
                    LEQUAL_DEPTH_TEST.clearRenderState();
                    TRANSLUCENT_TRANSPARENCY.clearRenderState();
                    RITUAL_SHADER_STATE.clearRenderState();
                });
    }

    public static RenderType get() {
        return RITUAL_QUADS;
    }
}
