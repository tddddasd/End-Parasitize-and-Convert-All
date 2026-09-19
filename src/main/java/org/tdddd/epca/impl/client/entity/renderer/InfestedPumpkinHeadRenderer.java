package org.tdddd.epca.impl.client.entity.renderer;

import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPumpkinHead;

public class InfestedPumpkinHeadRenderer extends EpcaGeoRenderer<InfestedPumpkinHead> {
    public InfestedPumpkinHeadRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 26.1.2: the GeckoLib 4 {@code render(entity, entityYaw, …)} override is gone (the pipeline
     * is extract-then-submit now). The roll-around-the-model-centre transform this class used to
     * apply to the pose stack before calling {@code super.render(…)} moves to
     * {@link EpcaGeoRenderer#adjustRenderPose}, the GeckoLib 5 hook that runs inside the render
     * pass with the same pose stack and before the model is submitted.
     *
     * <p>{@code RenderPassInfo} is raw for the reason documented on {@link EpcaGeoRenderer}.</p>
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void adjustRenderPose(RenderPassInfo passInfo) {
        InfestedPumpkinHead entity = EpcaGeoModel.entityOf(passInfo.renderState(), InfestedPumpkinHead.class);
        if (entity != null) {
            PoseStack poseStack = passInfo.poseStack();

            // 获取当前滚动角度
            float angle = entity.getRollAngle();

            // 获取模型高度一半（假设模型原点在底部，中心在高度的一半）
            float halfHeight = entity.getBbHeight() * 0.5f; // 碰撞箱高度为0.6，所以 half=0.3

            // 先平移使模型中心位于原点
            poseStack.translate(0, halfHeight, 0);
            // 绕 X 轴旋转（前后滚动）
            poseStack.mulPose(Axis.XP.rotation(angle));
            // 平移回原位
            poseStack.translate(0, -halfHeight, 0);
        }

        // 调用父类渲染（GeckoLib 会使用当前 poseStack 渲染模型）
        super.adjustRenderPose(passInfo);
    }
}