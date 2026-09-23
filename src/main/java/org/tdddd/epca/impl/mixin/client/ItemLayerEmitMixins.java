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

/**
 * The submit-time hooks of the item shader layers.
 *
 * <h2>The suppression redirect</h2>
 * While the corruption layer is active on an item, the item's own model must not be drawn - only the overlay,
 * so the item reads as dissolving. The vanilla geometry is submitted by
 * {@code SubmitNodeCollector#submitItem(...)} from inside {@code LayerRenderState#submit}, and that single
 * call is what this redirects:
 *
 * <ul>
 *   <li><b>effect active</b> - the overlay is emitted from the item's own quads
 *       ({@code prepareQuadList()}, same pose) and the original call is <b>not</b> made, so the vanilla item
 *       disappears;</li>
 *   <li><b>effect inactive</b> - the original call is made unchanged, so the item renders exactly normally.</li>
 * </ul>
 *
 * <h2>Why the redirect sits here and not at the previous pre-popPose hook</h2>
 * This invocation happens after {@code pushPose() -> applyTransform(...)}, so the pose is the item's own model
 * space, and the pose, the space and the quads are exactly the ones the item itself used. Redirecting the
 * submission is also the only way to <em>suppress</em> the vanilla geometry: an {@code @Inject} can observe
 * the call but cannot stop it.
 *
 * <p>The earlier revision had a second, separate emission path (an {@code @Inject} before the method's
 * {@code popPose()}), which would now draw the overlay twice. It has been <b>removed</b>; this redirect is the
 * only emission path, and {@code build/javac-check/check-item-corruption.py} asserts that.</p>
 *
 * <h2>Call sites and descriptor (javap -p -c -s on the patched jar)</h2>
 * {@code ItemStackRenderState$LayerRenderState#submit(PoseStack, SubmitNodeCollector, int, int, int)V}
 * contains <b>exactly one</b> {@code submitItem} invocation, at bytecode offset 108, so a single redirect
 * covers every item path. Its exact target:
 * <pre>
 *   Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(
 *     Lcom/mojang/blaze3d/vertex/PoseStack;
 *     Lnet/minecraft/world/item/ItemDisplayContext;
 *     III[ILjava/util/List;
 *     Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V
 * </pre>
 *
 * <p>The method's other branch (a special-model renderer, invoked around offset 49) never reaches
 * {@code submitItem} at all, so this redirect does not touch it - and {@code suppressAndOverlay} returns
 * {@code false} when the quad list is empty, which keeps such items visible instead of making them vanish.</p>
 *
 * <h2>The guard reset</h2>
 * The HEAD hook on {@code ItemStackRenderState#submit} clears the once-per-submit emit guard. The redirect
 * fires once per <em>layer</em>, and an item model can have several; without the guard the tint would be
 * applied once per layer. With it, every layer's vanilla submission is suppressed while the effect is active,
 * and the overlay itself is emitted exactly once.
 *
 * <p>{@code this$0} is the synthetic outer reference of the inner class, shadowed to key the captured-stack
 * lookup. Both descriptors were verified with {@code javap -s -p}.</p>
 */
public abstract class ItemLayerEmitMixins {

    /**
     * Suppress-and-overlay redirect on the one {@code submitItem} call in
     * {@code LayerRenderState#submit}.
     */
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
            // prepareQuadList() is public on this very class and returns the list submit() reads at bytecode
            // offset 101 before handing it to submitItem() at 108 - i.e. exactly the quads the item would have
            // been drawn from.
            List<BakedQuad> itemQuads =
                    ((ItemStackRenderState.LayerRenderState) (Object) this).prepareQuadList();

            if (ItemCorruptionRenderer.suppressAndOverlay(this$0, itemQuads, poseStack, collector)) {
                // The effect is active: the vanilla submission is skipped, so only the corruption overlay
                // remains. That is the user-visible "dissolving" behaviour.
                return;
            }
            // Not active (or no overlay could be built): draw the item exactly as vanilla would.
            collector.submitItem(poseStack, context, light, overlay, seed, tints, quads, foilType);
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
