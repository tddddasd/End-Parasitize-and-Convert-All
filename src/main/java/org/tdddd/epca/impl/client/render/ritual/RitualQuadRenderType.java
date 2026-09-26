package org.tdddd.epca.impl.client.render.ritual;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * The render type every sacrifice-ritual quad is drawn through: plain {@code POSITION_COLOR} quads with
 * the vanilla {@code position_color} program, translucent blending, depth testing on, depth writes off
 * and culling off.
 *
 * <h2>Why not the mod's custom {@code gas_cloud} pipeline</h2>
 * <p>The ritual aura was first drawn through {@code GasCloudRenderType.get()} together with a
 * dedicated style branch in {@code gas_cloud.fsh} (removed again once this type worked). In game that
 * produced literally nothing, while a positive-control quad drawn through the built-in
 * {@code RenderType.debugQuads()} at the very same pose, stage and buffer source was visible - so the
 * level stage, the camera-relative pose space and the render target were all correct and only the
 * custom program or its state failed. This type therefore reuses the vanilla program (exactly what the
 * working control quad used) and adds the two states the ritual needs and that {@code debugQuads()}
 * does not have: {@code NO_CULL} (the aura's quads are seen from above and from below) and depth
 * writes off.</p>
 */
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
                    // Colour writes on, depth writes off.
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

    /** The shared render type of every ritual quad. */
    public static RenderType get() {
        return RITUAL_QUADS;
    }
}
