package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.organ.model.OrganFaceMask;

/**
 * 记录“当前正在渲染的人体部件”，让方块级 mixin 能区分头 / 躯干 / 帽子 / 外套 / 四肢。
 *
 * <p>26 的 {@code ModelPart#render} 只剩 4 参与 5 参两个重载（原来 8 个 float 的
 * {@code (red,green,blue,alpha)} 版本被合并成一个 {@code int color}），模型统一走
 * 5 参重载（{@code Model#renderToBuffer}），所以这里挂 5 参重载。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ModelPart.class)
public abstract class ModelPartOrganFaceMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"), require = 0)
    private void epca$organFacePushPart(PoseStack poseStack, VertexConsumer consumer, int lightCoords,
                                        int overlayCoords, int color, CallbackInfo ci) {
        OrganFaceMask.pushPart((ModelPart) (Object) this);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("RETURN"), require = 0)
    private void epca$organFacePopPart(PoseStack poseStack, VertexConsumer consumer, int lightCoords,
                                       int overlayCoords, int color, CallbackInfo ci) {
        OrganFaceMask.popPart();
    }
}
