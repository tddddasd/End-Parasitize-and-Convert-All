package org.tdddd.epca.impl.client.render.sky;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.tdddd.epca.impl.epca;

/**
 *  RenderType
 *
 * <h3>""""</h3>
 *  NDC {@code z = 1}
 *  <b>{@code LEQUAL}</b>
 * <ul>
 *   <li>{@code 1.0}  ""  </li>
 *   <li>  </li>
 * </ul>
 *
 * <b></b>
 *
 * <h3></h3>
 * <table>
 *   <tr><th></th><th></th><th></th></tr>
 *   <tr><td></td><td>{@code LEQUAL_DEPTH_TEST}</td>
 *       <td>""<b></b>
 *            Oculus/Iris  composite  framebuffer </td></tr>
 *   <tr><td></td><td>{@code COLOR_WRITE}</td>
 *       <td> composite </td></tr>
 *   <tr><td></td><td>{@code MAIN_TARGET}</td>
 *       <td> framebuffer<b> GBuffer</b></td></tr>
 *   <tr><td></td><td>{@code NO_CULL}</td><td></td></tr>
 *   <tr><td></td><td>{@code TRANSLUCENT_TRANSPARENCY}</td>
 *       <td> alpha </td></tr>
 *   <tr><td></td><td> 12  sprite</td>
 *       <td> {@code epca:shader/cosmic_0..11}
 *           RottenRuinsSplendiding  mipmap</td></tr>
 *   <tr><td></td><td>{@code POSITION}</td><td> NDC UV/</td></tr>
 * </table>
 *
 * <p> {@link RenderType}
 * {@code protected}  {@code LEQUAL_DEPTH_TEST / MAIN_TARGET / COLOR_WRITE ...}
 *  ItemShaderRenderTypes </p>
 */
public abstract class SkyRuptureRenderType extends RenderType {

    /**  */
    public static final RenderType SKY_RUPTURE = RenderType.create(
            epca.MODID + ":sky_rupture",
            DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> SkyRuptureShaders.skyRuptureShader))
                    //  + 12  sprite
                    //  BLOCK_SHEET  mipmap
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, true, false))
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(MAIN_TARGET)
                    .createCompositeState(false)
    );

    /**  */
    private SkyRuptureRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload,
                                 Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}

