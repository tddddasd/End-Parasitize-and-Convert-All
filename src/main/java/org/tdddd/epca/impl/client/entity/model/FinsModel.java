package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import software.bernie.geckolib.model.GeoModel;

public class FinsModel extends GeoModel<Fins> {
    @Override
    public ResourceLocation getModelResource(Fins entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/fins.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Fins entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(Fins entity) {
        return new ResourceLocation(epca.MODID, "animations/fins.animation.json");
    }
}