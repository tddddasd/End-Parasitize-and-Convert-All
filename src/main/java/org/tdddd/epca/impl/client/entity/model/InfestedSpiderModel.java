package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class InfestedSpiderModel extends GeoModel<InfestedSpider> {

    private static final String LEG_BONE = "legs";

    private static final float CAVE_SHOOT_LEG_OFFSET_Y = -1.0F;

    @Override
    public ResourceLocation getModelResource(InfestedSpider entity) {
        return new ResourceLocation("epca", "geo/entity/infested_spider.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedSpider entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedSpider entity) {
        return new ResourceLocation("epca", "animations/infested_spider.animation.json");
    }

    @Override
    public void setCustomAnimations(InfestedSpider animatable, long instanceId,
                                    AnimationState<InfestedSpider> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (animatable.getVariant() != InfestedSpider.Variant.CAVE || !animatable.isShooting()) {
            return;
        }
        CoreGeoBone legs = this.getAnimationProcessor().getBone(LEG_BONE);
        if (legs != null) {
            legs.setPosY(CAVE_SHOOT_LEG_OFFSET_Y);
        }
    }
}
