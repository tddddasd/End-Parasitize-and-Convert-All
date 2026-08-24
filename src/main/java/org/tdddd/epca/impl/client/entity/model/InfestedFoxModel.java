package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedFox;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import software.bernie.geckolib.model.GeoModel;

public class InfestedFoxModel extends GeoModel<InfestedFox> {
    @Override
    public ResourceLocation getModelResource(InfestedFox entity) {
        return new ResourceLocation("epca", "geo/entity/infested_fox.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedFox entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedFox entity) {
        return new ResourceLocation("epca", "animations/infested_fox.animation.json");
    }
}