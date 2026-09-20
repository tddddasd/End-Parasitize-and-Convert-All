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
        
        float angle = entity.getRollAngle();

        
        float halfHeight = entity.getBbHeight() * 0.5f; 

        
        poseStack.translate(0, halfHeight, 0);
        
        poseStack.mulPose(Axis.XP.rotation(angle));
        
        poseStack.translate(0, -halfHeight, 0);

        
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}