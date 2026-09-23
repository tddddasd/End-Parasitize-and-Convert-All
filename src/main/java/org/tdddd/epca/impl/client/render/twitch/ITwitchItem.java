package org.tdddd.epca.impl.client.render.twitch;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * An item that wants the geometric "twitch" (geometry-level decay) even without a registered layer.
 *
 * <pre>{@code
 * public class CursedItem extends Item implements ITwitchItem {
 *     @Override
 *     public boolean twitchShouldRender(ItemDisplayContext ctx) {
 *         return ctx != ItemDisplayContext.GUI;   // never twitch in the inventory
 *     }
 * }
 * }</pre>
 *
 * <p>Ported unchanged from 1.20.1. The 1.20.1 {@code ItemRendererTwitchMixin} drove this from the HEAD
 * of {@code ItemRenderer#render}, which no longer exists; on 26.1.2 the check lives in the same emit
 * path as the layers ({@code ItemCorruptionRenderer}), because the jitter is applied to the layer's own
 * pose. An item that only implements this interface (no layer) therefore does not get a twitch pass
 * yet - documented in the port report.</p>
 */
public interface ITwitchItem {

    /**
     * Whether to twitch in this display context. Returning {@code false} suppresses the jitter for
     * that context (the usual implementation returns {@code false} for {@code GUI}).
     */
    boolean twitchShouldRender(ItemDisplayContext ctx);
}
