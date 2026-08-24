package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingSkeletonHead;
import software.bernie.geckolib.model.GeoModel;

public class WalkingSkeletonHeadModel extends GeoModel<WalkingSkeletonHead> {
    @Override
    public ResourceLocation getModelResource(WalkingSkeletonHead entity) {
        return new ResourceLocation("epca", "geo/entity/walking_skeleton_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WalkingSkeletonHead entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(WalkingSkeletonHead entity) {
        return new ResourceLocation("epca", "animations/walking_skeleton_head.animation.json");
    }
}
