package org.tdddd.epca.impl.client.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 让 <b>物品类自己</b> 声明要挂哪些 shader 层，无需外部注册。
 *
 * <p>与 RottenRuinsSplendiding 的 {@code ICosmicLayer} / {@code ICustomOutline}
 * 是同一个思路：接口优先级高于 {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} 的注册。</p>
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
     * 返回该物品要渲染的层。返回 {@code null} 或空列表时回退到
     * {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} 的注册。
     */
    List<ItemLayerBinding> shaderBindings(ItemStack stack);
}
