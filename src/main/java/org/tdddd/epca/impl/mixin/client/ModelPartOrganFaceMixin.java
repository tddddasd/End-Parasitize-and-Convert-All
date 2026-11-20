package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.organ.model.OrganFaceMask;

/**
 * 记录"当前正在渲染的人体部件"，让方块级 mixin 能区分头 / 躯干 / 帽子 / 外套 / 四肢。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ModelPart.class)
public abstract class ModelPartOrganFaceMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"))
    private void epca$organFacePushPart(PoseStack poseStack, VertexConsumer consumer, int light,
                                        int overlay, float red, float green, float blue, float alpha,
                                        CallbackInfo ci) {
        OrganFaceMask.pushPart((ModelPart) (Object) this);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("RETURN"))
    private void epca$organFacePopPart(PoseStack poseStack, VertexConsumer consumer, int light,
                                       int overlay, float red, float green, float blue, float alpha,
                                       CallbackInfo ci) {
        OrganFaceMask.popPart();
    }
}
