package org.tdddd.epca.impl.client.render.twitch;

import net.minecraft.world.item.ItemDisplayContext;

public interface ITwitchItem {

    boolean twitchShouldRender(ItemDisplayContext ctx);
}
