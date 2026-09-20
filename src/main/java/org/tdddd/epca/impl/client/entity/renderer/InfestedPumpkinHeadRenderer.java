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

            
            float angle = entity.getRollAngle();

            
            float halfHeight = entity.getBbHeight() * 0.5f; 

            
            poseStack.translate(0, halfHeight, 0);
            
            poseStack.mulPose(Axis.XP.rotation(angle));
            
            poseStack.translate(0, -halfHeight, 0);
        }

        
        super.adjustRenderPose(passInfo);
    }
}