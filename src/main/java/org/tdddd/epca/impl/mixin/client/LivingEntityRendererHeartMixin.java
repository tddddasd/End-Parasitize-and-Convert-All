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
import org.tdddd.epca.impl.client.entity.heart.SoulProtectionHeartRenderer;

/**
 * Client render hook for the golden "Heart" style of the {@code epca:soul_protection} effect: the
 * plasma/flame column and its little embers drawn by {@link SoulProtectionHeartRenderer}.
 *
 * <p>It follows the same pattern as the mod's existing living-entity render hook
 * {@link LivingEntityRendererTintMixin}: a client-only mixin on
 * {@link LivingEntityRenderer#render(LivingEntity, float, float, PoseStack, MultiBufferSource, int)}
 * rather than a Forge render event, so the hook fires for <em>every</em> living entity (vanilla
 * mobs, players, other mods' mobs) without registering a renderer for each type.</p>
 *
 * <h2>Why {@code TAIL} and not {@code RETURN}</h2>
 * <p>The method has an early {@code return} where a cancelled
 * {@code net.minecraftforge.client.event.RenderLivingEvent.Pre} bails out before anything is drawn,
 * so {@code RETURN} would inject twice. {@code TAIL} injects once, at the final return, where the
 * {@link PoseStack} has been popped back to the camera-relative entity translation that
 * {@link org.tdddd.epca.impl.client.entity.gas.GasCloudRenderer} requires (proved by the bytecode:
 * {@code pushPose} at offset 30, {@code popPose} at offset 662, the final {@code return} at offset
 * 703). The {@link MultiBufferSource} of the entity pass is still open there, so the flame is
 * buffered into the same pass as the gas clouds.</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererHeartMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void epca$renderSoulProtectionHeart(LivingEntity entity, float entityYaw, float partialTick,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, CallbackInfo ci) {
        SoulProtectionHeartRenderer.submit(entity, poseStack, bufferSource, partialTick);
    }
}
