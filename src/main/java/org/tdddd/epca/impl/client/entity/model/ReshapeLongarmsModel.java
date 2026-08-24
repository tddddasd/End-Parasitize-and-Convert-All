package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;
import software.bernie.geckolib.model.GeoModel;

public class ReshapeLongarmsModel extends GeoModel<ReshapeLongarms> {
    @Override
    public ResourceLocation getModelResource(ReshapeLongarms entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/reshape_longarms.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ReshapeLongarms entity) {
        return new ResourceLocation(epca.MODID, "textures/entity/reshape_longarms.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ReshapeLongarms entity) {
        return new ResourceLocation(epca.MODID, "animations/reshape_longarms.animation.json");
    }
}