package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.BiomassEgg;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.BoneArrow;

public class BiomassEggRenderer extends EpcaGeoRenderer<BiomassEgg> {

    public BiomassEggRenderer(EntityRendererProvider.Context context) {
        super(context); // 使用默认模型（由 EpcaEntityManager 自动生成）
        // 若需指定特定模型，可传入 GeoModel，但这里用默认
    }

    @Override
    public void render(BiomassEgg entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // 绕 Y 轴旋转 180°（弧度 PI）
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}