package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedBat;
import software.bernie.geckolib.model.GeoModel;

public class InfestedBatModel extends GeoModel<InfestedBat> {
    @Override
    public ResourceLocation getModelResource(InfestedBat entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/infested_bat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedBat entity) {
        return new ResourceLocation(epca.MODID, "textures/entity/infested_bat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedBat entity) {
        return new ResourceLocation(epca.MODID, "animations/infested_bat.animation.json");
    }
}