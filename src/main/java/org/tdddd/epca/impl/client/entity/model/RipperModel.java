package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper;
import software.bernie.geckolib.model.GeoModel;

public class RipperModel extends GeoModel<Ripper> {
    @Override
    public ResourceLocation getModelResource(Ripper entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/ripper.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Ripper entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(Ripper entity) {
        return new ResourceLocation(epca.MODID, "animations/ripper.animation.json");
    }
}