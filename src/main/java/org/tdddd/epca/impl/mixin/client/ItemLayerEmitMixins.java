package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.ItemCorruptionRenderer;

import java.util.List;

public abstract class ItemLayerEmitMixins {

    @Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
    public abstract static class LayerEmit {

        @Shadow
        @Final
        ItemStackRenderState this$0;

        @Redirect(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem("
                                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                                + "Lnet/minecraft/world/item/ItemDisplayContext;"
                                + "III[ILjava/util/List;"
                                + "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"))
        private void epca$submitItemOrCorruptionOverlay(SubmitNodeCollector collector, PoseStack poseStack,
                                                       net.minecraft.world.item.ItemDisplayContext context,
                                                       int light, int overlay, int seed, int[] tints,
                                                       List<BakedQuad> quads,
                                                       ItemStackRenderState.FoilType foilType) {

            List<BakedQuad> itemQuads =
                    ((ItemStackRenderState.LayerRenderState) (Object) this).prepareQuadList();

            if (ItemCorruptionRenderer.suppressAndOverlay(this$0, itemQuads, poseStack, collector)) {

                return;
            }

            collector.submitItem(poseStack, context, light, overlay, seed, tints, quads, foilType);
        }
    }

    @Mixin(ItemStackRenderState.class)
    public abstract static class ResetGuard {

        @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                at = @At("HEAD"))
        private void epca$beginItemLayerSubmit(PoseStack poseStack, SubmitNodeCollector collector,
                                              int packedLight, int packedOverlay, int seed,
                                              CallbackInfo ci) {
            ItemCorruptionRenderer.beginSubmit((ItemStackRenderState) (Object) this);
        }
    }
}
