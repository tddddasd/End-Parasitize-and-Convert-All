package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.ItemCorruptionRenderer;

@Mixin(ItemModelResolver.class)
public abstract class ItemLayerCaptureMixin {

    @Inject(method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
            + "Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemDisplayContext;"
            + "Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("TAIL"))
    private void epca$captureItemLayerStack(ItemStackRenderState renderState, ItemStack stack,
                                            ItemDisplayContext context, Level level,
                                            ItemOwner owner, int seed, CallbackInfo ci) {
        ItemCorruptionRenderer.capture(renderState, stack, context);
    }
}
