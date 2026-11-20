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
import org.tdddd.epca.impl.client.organ.model.OrganFaceMask;

/**
 * 渲染玩家前把「当前实体 + 人体模型」交给 {@link OrganFaceMask}，
 * 供后两个 mixin 判断是否要挖掉头部/躯干第一层皮肤的正面。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererOrganFaceMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void epca$organFaceBegin(LivingEntity entity, float entityYaw, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, CallbackInfo ci) {
        LivingEntityRenderer<?, ?> renderer = (LivingEntityRenderer<?, ?>) (Object) this;
        OrganFaceMask.beginRender(entity, renderer.getModel());
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void epca$organFaceEnd(LivingEntity entity, float entityYaw, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, CallbackInfo ci) {
        OrganFaceMask.endRender();
    }
}
