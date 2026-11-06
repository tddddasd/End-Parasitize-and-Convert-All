package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface IItemShaderLayer {

    String name();

    default ResourceLocation id() {
        return new ResourceLocation("epca", "item_layer/" + name());
    }

    ShaderInstance shader();

    ItemShaderRenderTypes renderTypes();

    default boolean supportsDeferredReplay() {
        return true;
    }

    ResourceLocation maskTexture(ItemStack stack, ItemLayerConfig config);

    boolean prepare(ItemStack stack, ItemLayerConfig config, ItemDisplayContext ctx, boolean lateRender);

    default boolean usesTwitchTransform(ItemStack stack, ItemLayerConfig config) {
        return config.twitch();
    }

    default void applyTwitch(PoseStack poseStack, ItemStack stack, ItemLayerConfig config, long gameTime) {
    }
}
