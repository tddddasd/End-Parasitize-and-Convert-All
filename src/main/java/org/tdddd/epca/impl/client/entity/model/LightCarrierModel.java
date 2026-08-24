package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.LightCarrier;
import software.bernie.geckolib.model.GeoModel;

public class LightCarrierModel extends GeoModel<LightCarrier> {
    @Override
    public ResourceLocation getModelResource(LightCarrier entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/light_carrier.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LightCarrier entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(LightCarrier entity) {
        return new ResourceLocation(epca.MODID, "animations/light_carrier.animation.json");
    }
}