package org.tdddd.epca.impl.client.render.twitch;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * 标记接口：物品只要实现它就能获得“鬼畜抖动”，不需要挂崩坏 shader 层。
 *
 * <pre>{@code
 * public class CursedItem extends Item implements ITwitchItem {
 *     public CursedItem() { super(new Item.Properties()); }
 * }
 * }</pre>
 *
 * <p>驱动曲线与崩坏层相同（{@link org.tdddd.epca.impl.client.render.layer.CorruptionPulse}），
 * 强度同样可以用 NBT {@code epca_corruption} 常驻化。</p>
 */
public interface ITwitchItem {

    /**
     * 该上下文是否抖。默认除 GUI 之外都抖（背包里抖会很难用）。
     */
    default boolean twitchShouldRender(ItemDisplayContext ctx) {
        return ctx != ItemDisplayContext.GUI;
    }
}
