package org.tdddd.epca.impl.client.entity.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingSpiderHead;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for both walking spider head variants.
 *
 * <p>Like {@link InfestedSpiderModel} the variants share the model and animation file and differ
 * only in the texture, resolved by
 * {@link WalkingSpiderHead#getTextureResource()}.</p>
 */
public class WalkingSpiderHeadModel extends GeoModel<WalkingSpiderHead> {

    @Override
    public ResourceLocation getModelResource(WalkingSpiderHead entity) {
        return new ResourceLocation("epca", "geo/entity/walking_spider_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WalkingSpiderHead entity) {
        return entity.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(WalkingSpiderHead entity) {
        return new ResourceLocation("epca", "animations/walking_spider_head.animation.json");
    }
}
