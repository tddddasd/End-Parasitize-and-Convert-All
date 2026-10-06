package org.tdddd.epca.impl.client.render.twitch;

import net.minecraft.world.item.ItemDisplayContext;

/**
 *  shader
 *
 * <pre>{@code
 * public class CursedItem extends Item implements ITwitchItem {
 *     public CursedItem() { super(new Item.Properties()); }
 * }
 * }</pre>
 *
 * <p>{@link org.tdddd.epca.impl.client.render.layer.CorruptionPulse}
 *  NBT {@code epca_corruption} </p>
 */
public interface ITwitchItem {

    /**
     *  GUI
     */
    default boolean twitchShouldRender(ItemDisplayContext ctx) {
        return ctx != ItemDisplayContext.GUI;
    }
}

