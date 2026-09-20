package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.ClientColorEffect;


@OnlyIn(Dist.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererTintMixin {

    @Unique
    private static final ThreadLocal<LivingEntity> EPCA_CURRENT_ENTITY = new ThreadLocal<>();

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void epca$captureEntity(LivingEntity entity, float entityYaw, float partialTick,
                                    PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                    CallbackInfo ci) {
        EPCA_CURRENT_ENTITY.set(entity);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void epca$releaseEntity(LivingEntity entity, float entityYaw, float partialTick,
                                    PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                    CallbackInfo ci) {
        EPCA_CURRENT_ENTITY.remove();
    }

    @Unique
    private static float[] epca$currentRgb() {
        ClientColorEffect.EffectData data = ClientColorEffect.getEffect(EPCA_CURRENT_ENTITY.get());
        if (data == null) {
            return null;
        }
        return data.getColorRGB();
    }

    @Unique
    private static float epca$rgbChannel(int index) {
        float[] rgb = epca$currentRgb();
        return rgb == null ? 1.0F : rgb[index];
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 4)
    private float epca$tintRed(float original) {
        return original * epca$rgbChannel(0);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 5)
    private float epca$tintGreen(float original) {
        return original * epca$rgbChannel(1);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 6)
    private float epca$tintBlue(float original) {
        return original * epca$rgbChannel(2);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7)
    private float epca$tintAlpha(float original) {
        ClientColorEffect.EffectData data = ClientColorEffect.getEffect(EPCA_CURRENT_ENTITY.get());
        if (data == null) {
            return original;
        }
        int argb = data.getColorARGB();
        float effectAlpha = ((argb >>> 24) & 0xFF) / 255.0F;
        return original * effectAlpha;
    }
}
