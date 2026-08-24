package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPumpkinHead;

public class InfestedPumpkinHeadRenderer extends EpcaGeoRenderer<InfestedPumpkinHead> {
    public InfestedPumpkinHeadRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(InfestedPumpkinHead entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
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

        // 调用父类渲染（GeckoLib 会使用当前 poseStack 渲染模型）
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}