package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    // The model draw call is redirected instead of modifying each colour argument by index:
    // a redirect handler is matched against the invocation itself (the instance plus its own
    // arguments), so the tint cannot break when an argument index shifts, or when another
    // mixin also rewrites the same invocation.
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void epca$renderTintedModel(EntityModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay,
                                        float red, float green, float blue, float alpha) {
        ClientColorEffect.EffectData data = ClientColorEffect.getEffect(EPCA_CURRENT_ENTITY.get());
        if (data != null) {
            float[] rgb = data.getColorRGB();
            int argb = data.getColorARGB();
            float effectAlpha = ((argb >>> 24) & 0xFF) / 255.0F;
            red *= rgb[0];
            green *= rgb[1];
            blue *= rgb[2];
            alpha *= effectAlpha;
        }
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
