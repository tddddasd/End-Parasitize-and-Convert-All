package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.tdddd.epca.impl.epca;

import java.util.function.Supplier;

/**
 *  shader  <b> RenderType </b>
 * RottenRuinsSplendiding  {@code CosmicRenderType}
 *
 * <h3></h3>
 * <table>
 *   <tr><th></th><th></th><th></th><th></th><th></th></tr>
 *   <tr><td>{@link #immediate()}</td><td>EQUAL</td><td></td><td></td>
 *       <td> / GUI</td></tr>
 *   <tr><td>{@link #afterLevel()}</td><td>LEQUAL + polygonOffset</td><td>MAIN_TARGET</td><td>COLOR_WRITE</td>
 *       <td></td></tr>
 *   <tr><td>{@link #handAfterLevel()}</td><td>NO_DEPTH_TEST</td><td>MAIN_TARGET</td><td>COLOR_WRITE</td>
 *       <td></td></tr>
 * </table>
 *
 * <ul>
 *   <li><b> EQUAL</b> {@code =} </li>
 *   <li><b> LEQUAL</b> pass
 *        {@code <=}</li>
 *   <li><b> NO_DEPTH_TEST</b></li>
 *   <li><b>MAIN_TARGET + COLOR_WRITE</b> framebuffer  GBuffer
 *        composite pass</li>
 *   <li><b>polygonOffset</b> z-fighting</li>
 * </ul>
 *
 * <p> +  {@link RenderType}
 * {@link RenderStateShard}  {@code protected}
 * {@code EQUAL_DEPTH_TEST / MAIN_TARGET / COLOR_WRITE ...}
 *  {@code CosmicRenderType extends RenderType} </p>
 *
 * <p> shader
 * {@link #create(String, Supplier, boolean)}  RenderType
 *  RenderType </p>
 */
public abstract class ItemShaderRenderTypes extends RenderType {

    /**  z-fighting */
    private static final RenderStateShard.LayeringStateShard LAYER_DEPTH_BIAS =
            new RenderStateShard.LayeringStateShard(
                    "epca_item_layer_depth_bias",
                    () -> {
                        RenderSystem.polygonOffset(-1.0F, -32.0F);
                        RenderSystem.enablePolygonOffset();
                    },
                    () -> {
                        RenderSystem.polygonOffset(0.0F, 0.0F);
                        RenderSystem.disablePolygonOffset();
                    }
            );

    private static final int BUFFER_SIZE = 2097152;

    private final RenderType immediate;
    private final RenderType afterLevel;
    private final RenderType handAfterLevel;

    private ItemShaderRenderTypes(String name, Supplier<ShaderInstance> shader, boolean affectsCrumbling) {
        //  RenderStateShard  protected
        super(epca.MODID + ":item_layer/" + name + "_holder",
                DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, BUFFER_SIZE,
                false, false, () -> {
                }, () -> {
                });

        RenderStateShard.ShaderStateShard shaderState = new RenderStateShard.ShaderStateShard(shader);

        this.immediate = RenderType.create(
                epca.MODID + ":item_layer/" + name,
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                affectsCrumbling,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(EQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .createCompositeState(true)
        );

        this.afterLevel = RenderType.create(
                epca.MODID + ":item_layer/" + name + "_after_level",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .setLayeringState(LAYER_DEPTH_BIAS)
                        .setOutputState(MAIN_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );

        this.handAfterLevel = RenderType.create(
                epca.MODID + ":item_layer/" + name + "_hand_after_level",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                BUFFER_SIZE,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(shaderState)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(BLOCK_SHEET)
                        .setOutputState(MAIN_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }

    /**
     *  shader  RenderType
     *
     * @param name             RenderType  {@code "corruption"}
     * @param shader          shader  {@code null}
     * @param affectsCrumbling  {@code true}
     */
    public static ItemShaderRenderTypes create(String name, Supplier<ShaderInstance> shader, boolean affectsCrumbling) {
        return new ItemShaderRenderTypes(name, shader, affectsCrumbling) {
        };
    }

    /**  RenderTypeEQUAL  */
    public RenderType immediate() {
        return immediate;
    }

    /**  RenderTypeLEQUAL  +  framebuffer  */
    public RenderType afterLevel() {
        return afterLevel;
    }

    /**  RenderTypeNO_DEPTH_TEST +  framebuffer  */
    public RenderType handAfterLevel() {
        return handAfterLevel;
    }

    /**
     *
     */
    public RenderType lateFor(ItemDisplayContext context) {
        return isFirstPersonHand(context) ? handAfterLevel : afterLevel;
    }

    /**
     *
     */
    public static boolean isFirstPersonHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    /**  {@code name}  {@code epca:...}  ResourceLocation */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(epca.MODID, path);
    }
}

