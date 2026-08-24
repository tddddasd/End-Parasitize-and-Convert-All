package org.tdddd.epca.impl.utils.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;


public class BillboardRenderHelper {

    
    public static void applyBillboardTransform(PoseStack poseStack, LivingEntity entity, float partialTick) {
        
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        Vec3 entityPos = entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2, 0);

        
        double dx = cameraPos.x - entityPos.x;
        double dz = cameraPos.z - entityPos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx));

        
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        
        
        poseStack.scale(1.0f, 1.0f, 0.01f);
    }
}