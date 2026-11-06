package org.tdddd.epca.impl.client.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IItemShaderRenderable {

    List<ItemLayerBinding> shaderBindings(ItemStack stack);
}
