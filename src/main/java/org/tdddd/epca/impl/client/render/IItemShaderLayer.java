package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 *  shader
 *
 * <h3></h3>
 *  +  shader
 *  Mixin
 * RottenRuinsSplendiding  cosmic / corruption  RenderType
 *
 *
 * <h3></h3>
 * <ol>
 *   <li> shader{@code assets/epca/shaders/core/<name>.json|vsh|fsh}</li>
 *   <li> {@link org.tdddd.epca.impl.client.render.shader.EpcaShaders}
 *       {@link ShaderInstance}  uniform
 *        {@link ItemShaderRenderTypes#create}  RenderType
 *        {@link ItemShaderLayers}  {@code register(new MyLayer())}</li>
 *   <li> {@link #prepare}  uniform</li>
 *   <li> {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#attach} </li>
 * </ol>
 *
 * <h3></h3>
 * Mixin <b> flush</b>
 *  EQUAL
 *  {@code GameRenderer.renderLevel()}
 *  {@link org.tdddd.epca.impl.client.render.compat.ItemLayerLateRenderQueue}
 */
public interface IItemShaderLayer {

    /**  {@code "corruption"} */
    String name();

    /**  id{@code epca:item_layer/<name>} */
    default ResourceLocation id() {
        return new ResourceLocation("epca", "item_layer/" + name());
    }

    /**
     *  shader shader  {@code null}
     *
     */
    ShaderInstance shader();

    /**  RenderType  */
    ItemShaderRenderTypes renderTypes();

    /**
     *
     *  {@code false}  GBuffer
     */
    default boolean supportsDeferredReplay() {
        return true;
    }

    /**
     *
     *
     * <p>
     * {@code assets/<namespace>/textures/item/<path>.png}
     *  1:1 </p>
     *
     * @param config  {@link ItemLayerConfig#maskOverride()}
     */
    ResourceLocation maskTexture(ItemStack stack, ItemLayerConfig config);

    /**
     *  uniform
     *
     * @param stack
     * @param config     {@link ItemLayerConfig#strength()}
     * @param ctx
     * @param lateRender {@code true} /
     * @return {@code false}
     */
    boolean prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx, boolean lateRender);

    /**
     *  PoseStack
     *  {@link ItemLayerConfig#twitch()}
     */
    default boolean usesTwitchTransform(ItemStack stack, ItemLayerConfig config) {
        return config.twitch();
    }

    /**
     *  {@link #usesTwitchTransform}
     */
    default void applyTwitch(PoseStack poseStack, ItemStack stack, ItemLayerConfig config, long gameTime) {
    }
}

