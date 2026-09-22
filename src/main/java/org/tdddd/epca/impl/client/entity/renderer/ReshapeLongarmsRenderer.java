package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudLayer;
import org.tdddd.epca.impl.client.entity.model.ReshapeLongarmsModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

public class ReshapeLongarmsRenderer extends EpcaGeoRenderer<ReshapeLongarms> {

    
    private final ReshapeLongarmsModel model;

    public ReshapeLongarmsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ReshapeLongarmsModel());
        this.model = (ReshapeLongarmsModel) getGeoModel();
        // Shader-rendered gas clouds for both the active jet skill and the passive back jet.
        addLayerProvider(GasCloudLayer.forLongarms());
    }

    @Override
    public void render(ReshapeLongarms entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        boolean hide = entity.isBackPartRemoved();
        setBoneHidden("pustule1", hide);
        setBoneHidden("pustule2", hide);
        setBoneHidden("pustule3", hide);
        setBoneHidden("streaks4", hide);
        setBoneHidden("streaks5", hide);
        setBoneHidden("streaks6", hide);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    
    private void setBoneHidden(String boneName, boolean hide) {
        CoreGeoBone bone = this.model.getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setHidden(hide);
        }
    }
}
