package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.ItemCorruptionRenderer;

/**
 * The two submit-time hooks of the item shader layers.
 *
 * <h2>Why {@code LayerRenderState#submit} and not {@code ItemStackRenderState#submit}</h2>
 * Verified with {@code javap -c} on the 26.1.2 patched jar:
 * <ul>
 *   <li>{@code ItemStackRenderState#submit(PoseStack, SubmitNodeCollector, int, int, int)} only loops
 *       over its layers and is <b>pose-balanced</b> at its TAIL, so the pose there is the caller's
 *       (world/entity or GUI) pose, not the item's model space;</li>
 *   <li>{@code ItemStackRenderState$LayerRenderState#submit(PoseStack, SubmitNodeCollector, int, int,
 *       int)} does {@code pushPose() -> applyTransform(last()) -> ... -> submitItem(...) -> popPose()},
 *       so the item's final model space exists <b>only just before that {@code popPose()}</b>.</li>
 * </ul>
 * The emit hook is therefore placed {@code BEFORE} the {@code popPose()} invocation. That single call
 * site covers both branches of the method (the special-model renderer path and the ordinary
 * {@code submitItem} path), because both converge on the same pop.
 *
 * <p>The HEAD hook on {@code ItemStackRenderState#submit} resets the per-render-state emit guard, so a
 * multi-layer item model draws the overlay exactly once per submit rather than once per layer.</p>
 *
 * <p>{@code this$0} is the synthetic outer reference of the inner class; it is shadowed here to key the
 * captured-stack lookup. Both descriptors were verified with {@code javap -s -p} on the patched
 * jar.</p>
 */
public abstract class ItemLayerEmitMixins {

    /** Emit hook: runs in the item's own model space, just before the layer's pose is popped. */
    @Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
    public abstract static class LayerEmit {

        @Shadow
        @Final
        ItemStackRenderState this$0;

        @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                at = @At(value = "INVOKE",
                        target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                        shift = At.Shift.BEFORE))
        private void epca$emitItemLayer(PoseStack poseStack, SubmitNodeCollector collector,
                                       int packedLight, int packedOverlay, int seed,
                                       CallbackInfo ci) {
            ItemCorruptionRenderer.emitForLayer(this$0, poseStack, collector, packedLight, packedOverlay);
        }
    }

    /** Reset hook: clears the once-per-submit guard at the start of each render state's submit. */
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
