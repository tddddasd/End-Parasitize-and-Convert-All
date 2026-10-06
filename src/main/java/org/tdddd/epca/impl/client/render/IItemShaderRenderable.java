package org.tdddd.epca.impl.client.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 *  <b></b>  shader
 *
 * <p> RottenRuinsSplendiding  {@code ICosmicLayer} / {@code ICustomOutline}
 *  {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} </p>
 *
 * <pre>{@code
 * public class EnderBlade extends SwordItem implements IItemShaderRenderable {
 *     @Override
 *     public List<ItemLayerBinding> shaderBindings(ItemStack stack) {
 *         return List.of(ItemLayerBinding.of(ItemShaderLayers.CORRUPTION,
 *                 ItemLayerConfig.builder().strength(0.9f).twitch(true).build()));
 *     }
 * }
 * }</pre>
 */
public interface IItemShaderRenderable {

    /**
     *  {@code null}
     * {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry}
     */
    List<ItemLayerBinding> shaderBindings(ItemStack stack);
}

