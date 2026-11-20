package org.tdddd.epca.impl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.organ.model.OrganFaceMask;

/**
 * 渲染玩家前把「当前渲染态 + 人体模型」交给 {@link OrganFaceMask}。
 *
 * <p><b>26 API 差异</b>：1.20.1 直接在 {@code LivingEntityRenderer#render} 里挖洞没问题，因为那时
 * {@code ModelPart} 的顶点是在实体渲染调用栈里同步写进 {@code MultiBufferSource} 的。26 改成了
 * SubmitNodeCollector 延迟提交——实体 {@code submit} 只是把 {@code ModelSubmit} 排队，
 * 真正调用 {@code Model#renderToBuffer} 是在后面的 {@code ModelFeatureRenderer#renderModel} 里。
 * 所以挖洞状态必须挂在 {@code renderModel} 的头尾，否则 ThreadLocal 在写顶点时早就被清掉了。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererOrganFaceMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private void epca$organFaceBegin(SubmitNodeStorage.ModelSubmit<?> submit, RenderType renderType,
                                     VertexConsumer buffer, OutlineBufferSource outlineBufferSource,
                                     MultiBufferSource.BufferSource crumblingBufferSource,
                                     CallbackInfo ci) {
        OrganFaceMask.beginRender(submit.state(), submit.model());
    }

    @Inject(method = "renderModel", at = @At("RETURN"), require = 0)
    private void epca$organFaceEnd(SubmitNodeStorage.ModelSubmit<?> submit, RenderType renderType,
                                   VertexConsumer buffer, OutlineBufferSource outlineBufferSource,
                                   MultiBufferSource.BufferSource crumblingBufferSource,
                                   CallbackInfo ci) {
        OrganFaceMask.endRender();
    }
}
