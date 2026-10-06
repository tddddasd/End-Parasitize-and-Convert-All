package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;

/**
 * ""emissive layer RenderType
 *
 * <h2> JSON  {@code render_type}</h2>
 * <p>1.20.1
 * {@code RenderType.chunkBufferLayers()}{@code solid / cutout_mipped / cutout / translucent / tripwire}
 * vanilla  {@code RenderType.java:148} + {@code :443} {@code LIGHTMAP}
 * ""
 * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}
 * {@code InfestedSweetBerryBushGlowRenderer}</p>
 *
 * <h2>""</h2>
 * <p> vanilla  {@code rendertype_entity_translucent_emissive}vanilla  state
 * <b></b> {@code LIGHTMAP}{@code RenderType.java:67-70}
 * </p>
 *
 * <h2></h2>
 * <table>
 *   <tr><th></th><th></th><th></th></tr>
 *   <tr><td></td><td>{@code epca:block/infested_sweet_berry_bush_growed_e}</td>
 *       <td> 16x16 <b></b>
 *           /
 *           {@code RenderType.eyes(glowTex)}</td></tr>
 *   <tr><td></td><td>{@code TRANSLUCENT_TRANSPARENCY}</td>
 *       <td> alpha </td></tr>
 *   <tr><td></td><td>{@code COLOR_WRITE}</td>
 *       <td>/</td></tr>
 *   <tr><td></td><td>{@code LEQUAL_DEPTH_TEST}</td>
 *       <td></td></tr>
 *   <tr><td></td><td>{@code CULL}</td>
 *       <td> {@code cutout} {@code RenderType.CUTOUT}
 *           {@code RenderType.java:31} {@code setCullState}
 *           {@code CompositeState}  {@code CULL}{@code RenderType.java:568}
 *           <b></b> {@code NO_CULL}{@code minecraft:block/cross}
 *           {@code north}+{@code south}{@code west}+{@code east}
 *            cullface
 *            {@code NO_CULL}  u
 *            {@code ItemModelGenerator.java:50-51} south
 *           {@code uv [0,0,16,16]}north  {@code uv [16,0,0,16]} ""
 *           </td></tr>
 *   <tr><td></td><td>{@code VIEW_OFFSET_Z_LAYERING}</td>
 *       <td><b></b>
 *            z-fighting </td></tr>
 * </table>
 *
 * <h2>""</h2>
 * <p> {@code NO_CULL}""
 * {@code minecraft:block/cross}
 * {@code north}+{@code south}{@code west}+{@code east}uv
 * {@code cutout} <b></b> {@code NO_CULL}
 *  u  uv
 * {@code FaceInfo.java:12-13}  NORTH/SOUTH  + {@code FaceBakery.fillVertex}
 * {@code :139-140}" + "
 *  {@code CULL} </p>
 *
 * <p> {@link RenderType}  {@code protected}
 * {@code RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER / TRANSLUCENT_TRANSPARENCY / ...}
 *  {@code SkyRuptureRenderType}{@code RitualQuadRenderType} </p>
 */
public final class EmissiveBlockRenderType extends RenderType {

    /**
     * {@code age=3}
     *
     * <p> {@code epca:block/infested_sweet_berry_bush_growed}  16x16
     * </p>
     */
    public static final ResourceLocation SWEET_BERRY_FRUIT_TEXTURE =
            new ResourceLocation(epca.MODID, "textures/block/infested_sweet_berry_bush_growed_e.png");

    /**  {@code cutout}  */
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

    /**  */
    private EmissiveBlockRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                    boolean affectsCrumbling, boolean sortOnUpload,
                                    Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}

