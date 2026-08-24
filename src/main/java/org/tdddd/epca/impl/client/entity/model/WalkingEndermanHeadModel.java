package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingEndermanHead;
import software.bernie.geckolib.model.GeoModel;

public class WalkingEndermanHeadModel extends GeoModel<WalkingEndermanHead> {
    @Override
    public ResourceLocation getModelResource(WalkingEndermanHead entity) {
        return new ResourceLocation(epca.MODID, "geo/entity/walking_enderman_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WalkingEndermanHead entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(WalkingEndermanHead entity) {
        return new ResourceLocation(epca.MODID, "animations/walking_enderman_head.animation.json");
    }
}