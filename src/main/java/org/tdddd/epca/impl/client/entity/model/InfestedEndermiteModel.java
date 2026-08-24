package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import software.bernie.geckolib.model.GeoModel;

public class InfestedEndermiteModel extends GeoModel<InfestedEndermite> {
    @Override
    public ResourceLocation getModelResource(InfestedEndermite entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/infested_endermite.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InfestedEndermite entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(InfestedEndermite entity) {
        return new ResourceLocation(epca.MODID, "animations/infested_endermite.animation.json");
    }
}