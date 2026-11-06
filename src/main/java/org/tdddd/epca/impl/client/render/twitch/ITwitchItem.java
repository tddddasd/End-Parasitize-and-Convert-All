package org.tdddd.epca.impl.client.render.twitch;

import net.minecraft.world.item.ItemDisplayContext;

public interface ITwitchItem {

    default boolean twitchShouldRender(ItemDisplayContext ctx) {
        return ctx != ItemDisplayContext.GUI;
    }
}
