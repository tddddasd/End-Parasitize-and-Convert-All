package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.ItemLayerBinding;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.client.render.ItemShaderRenderHelper;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.compat.ItemLayerLateRenderQueue;

import java.util.ArrayList;
import java.util.List;

/**
 *  shader    RottenRuinsSplendiding
 * {@code MixinItemRendererCosmic}
 *
 * <h3></h3>
 * {@code ItemRenderer.render()} <b></b> {@code popPose()}
 *
 * <ul>
 *   <li>   EQUAL </li>
 *   <li> popPose   PoseStack </li>
 * </ul>
 *
 * <p><b> {@code ordinal = 1}</b>
 * 1.20.1  {@code ItemRenderer.render} <b></b> {@code popPose()}
 * offset 385 /{@code hasAnimatedTexture && hasFoil}
 *  push/pop offset 475
 * Mixin  {@code @At(INVOKE)} <b></b>
 *  N+1
 *  2 </p>
 *
 * <h3></h3>
 * <ul>
 *   <li><b> / GUI</b>  EQUAL </li>
 *   <li><b></b>   {@code GameRenderer.renderLevel()}
 *        framebuffer {@link ItemLayerLateRenderQueue}</li>
 * </ul>
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererShaderLayerMixin {

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE))
    private void epca$renderItemShaderLayers(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                             PoseStack poseStack, MultiBufferSource buffer,
                                             int packedLight, int packedOverlay, BakedModel model,
                                             CallbackInfo ci) {
        if (stack.isEmpty()) {
            return;
        }

        List<ItemLayerBinding> bindings = ItemRenderRegistry.resolve(stack, context);
        if (bindings.isEmpty()) {
            return;
        }

        //  GeckoLib  quad
        // EQUAL    draw
        if (model.isCustomRenderer()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        //  flush  GPU
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }

        //    GBuffer
        if (IrisShaderCompat.shouldDeferItemLayer(context)) {
            List<ItemLayerBinding> deferrable = new ArrayList<>(bindings.size());
            for (ItemLayerBinding binding : bindings) {
                if (binding.layer().supportsDeferredReplay()) {
                    deferrable.add(binding);
                }
            }
            if (!deferrable.isEmpty()) {
                ItemLayerLateRenderQueue.enqueue(stack, context, deferrable, poseStack, packedLight, packedOverlay);
            }
            return;
        }

        for (ItemLayerBinding binding : bindings) {
            ItemShaderRenderHelper.drawBinding(binding, stack, context, poseStack, buffer,
                    packedLight, packedOverlay, false);
        }
    }
}

