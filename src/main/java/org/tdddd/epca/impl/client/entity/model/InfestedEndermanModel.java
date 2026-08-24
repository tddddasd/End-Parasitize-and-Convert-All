package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import software.bernie.geckolib.model.GeoModel;

public class InfestedEndermanModel extends GeoModel<InfestedEnderman> {
    @Override
    public ResourceLocation getModelResource(InfestedEnderman entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/infested_enderman.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedEnderman entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedEnderman entity) {
        return new ResourceLocation(epca.MODID, "animations/infested_enderman.animation.json");
    }
}