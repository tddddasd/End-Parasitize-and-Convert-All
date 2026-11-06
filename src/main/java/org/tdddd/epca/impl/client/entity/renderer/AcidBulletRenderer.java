package org.tdddd.epca.impl.client.entity.renderer;

import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.AcidBullet;

public class AcidBulletRenderer extends EpcaGeoRenderer<AcidBullet> {

    public AcidBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void adjustRenderPose(RenderPassInfo passInfo) {
        AcidBullet entity = EpcaGeoModel.entityOf(passInfo.renderState(), AcidBullet.class);
        if (entity != null) {
            PoseStack poseStack = passInfo.poseStack();
            float partialTicks = passInfo.renderState().getPartialTick();

            float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        }

        super.adjustRenderPose(passInfo);
    }
}
