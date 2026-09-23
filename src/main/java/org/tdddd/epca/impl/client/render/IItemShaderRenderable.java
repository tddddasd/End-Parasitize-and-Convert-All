package org.tdddd.epca.impl.client.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * An item that describes its own shader layers.
 *
 * <p>Highest priority in the resolution order of
 * {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry#resolveAll}: an item implementing this
 * interface does not need to be registered at all.</p>
 *
 * <pre>{@code
 * public class CursedItem extends Item implements IItemShaderRenderable {
 *     @Override
 *     public List<ItemLayerBinding> shaderBindings(ItemStack stack) {
 *         return List.of(ItemLayerBinding.of(ItemShaderLayers.CORRUPTION,
 *                 ItemLayerConfig.builder().strength(0.9f).build()));
 *     }
 * }
 * }</pre>
 *
 * <p>Ported unchanged from 1.20.1 apart from {@code ResourceLocation} -&gt; {@link net.minecraft.resources.Identifier}
 * in the referenced types.</p>
 */
public interface IItemShaderRenderable {

    /**
     * Layers this item wants, or an empty list to fall through to the registry.
     */
    List<ItemLayerBinding> shaderBindings(ItemStack stack);
}
