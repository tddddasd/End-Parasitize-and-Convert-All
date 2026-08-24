package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSkeleton;
import software.bernie.geckolib.model.GeoModel;

public class InfestedSkeletonModel extends GeoModel<InfestedSkeleton> {
    @Override
    public ResourceLocation getModelResource(InfestedSkeleton entity) {
        return new ResourceLocation("epca", "geo/entity/infested_skeleton.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedSkeleton entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedSkeleton entity) {
        return new ResourceLocation("epca", "animations/infested_skeleton.animation.json");
    }
}
