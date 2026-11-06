package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface IItemShaderLayer {

    String name();

    default Identifier id() {
        return Identifier.fromNamespaceAndPath("epca", "item_layer/" + name());
    }

    boolean isReady();

    default boolean supportsDeferredReplay() {
        return true;
    }

    Identifier maskTexture(ItemStack stack, ItemLayerConfig config);

    ItemLayerPayload prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx,
                             long gameTime);

    default boolean usesTwitchTransform(ItemStack stack, ItemLayerConfig config) {
        return config.twitch();
    }

    default void applyTwitch(com.mojang.blaze3d.vertex.PoseStack poseStack, ItemStack stack,
                             ItemLayerConfig config, long gameTime) {
    }
}
