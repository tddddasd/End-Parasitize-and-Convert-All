package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 render hook for the golden "Heart" style of the {@code epca:soul_protection} effect.
 *
 * <h2>DISABLED (2026): the render effect was removed on request</h2>
 * <p>The user asked for the render effect of the 1.20.1 {@code epca:soul_protection} buff to be
 * removed while the buff itself keeps working. The effect was removed at both of its layers, so no
 * path can draw it any more:</p>
 * <ol>
 *   <li>this mixin is <b>no longer registered</b> in {@code epca.mixins.json} (the
 *       {@code client.LivingEntityRendererHeartMixin} entry was deleted), so the injection below is
 *       never applied; and</li>
 *   <li>the injected body no longer calls
 *       {@code org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer#submit}, so even
 *       if the entry is ever restored the hook draws nothing.</li>
 * </ol>
 * <p>{@code SoulProtectionHeartRenderer} itself is intentionally kept in the tree (with its whole
 * constant block) because those constants mirror the {@code HEART_*} block of
 * {@code assets/epca/shaders/core/gas_cloud.fsh}; it is simply never called. The buff's mechanics,
 * damage handling, data and packets are untouched - see {@code SoulProtectionEffect},
 * {@code ShieldProtectionHelper} and {@code SoulProtectionSyncHandler}.</p>
 *
 * <p>For the record, the hook used to run at the end of
 * {@link LivingEntityRenderer#render(LivingEntity, float, float, PoseStack, MultiBufferSource, int)}
 * with {@code @At("TAIL")} (not {@code RETURN}: the method has an early return where a cancelled
 * {@code RenderLivingEvent.Pre} bails out, and {@code TAIL} fires once at the final return, where the
 * {@link PoseStack} is back at the camera-relative entity translation that the gas-cloud submission
 * path requires).</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererHeartMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void epca$renderSoulProtectionHeart(LivingEntity entity, float entityYaw, float partialTick,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, CallbackInfo ci) {
        // Removed on request: the soul-protection render effect is gone in 1.20.1. Do not call
        // SoulProtectionHeartRenderer#submit here again.
    }
}
