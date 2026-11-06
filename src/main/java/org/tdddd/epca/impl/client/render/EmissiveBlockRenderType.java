package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;

public final class EmissiveBlockRenderType extends RenderType {

    public static final ResourceLocation SWEET_BERRY_FRUIT_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/block/infested_sweet_berry_bush_growed_e.png");

    public static final RenderType SWEET_BERRY_FRUIT = RenderType.create(
            epca.MODID + ":emissive_block",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(SWEET_BERRY_FRUIT_TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(CULL)
                    .setOverlayState(OVERLAY)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false)
    );

    private EmissiveBlockRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                    boolean affectsCrumbling, boolean sortOnUpload,
                                    Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
