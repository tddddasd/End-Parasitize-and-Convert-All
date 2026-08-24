package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedFox;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingFoxHead;
import software.bernie.geckolib.model.GeoModel;

public class WalkingFoxHeadModel extends GeoModel<WalkingFoxHead> {
    @Override
    public ResourceLocation getModelResource(WalkingFoxHead entity) {
        return new ResourceLocation("epca", "geo/entity/walking_fox_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WalkingFoxHead entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(WalkingFoxHead entity) {
        return new ResourceLocation("epca", "animations/walking_fox_head.animation.json");
    }
}