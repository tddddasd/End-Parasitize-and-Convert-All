package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingSpiderHead;

/**
 * Model for the two walking spider head variants.
 *
 * <p>Like {@link InfestedSpiderModel} the variants share one model and one animation file and
 * differ only in the texture, which the entity resolves itself.</p>
 */
public class WalkingSpiderHeadModel extends GeoModel<WalkingSpiderHead> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/walking_spider_head");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        WalkingSpiderHead entity = EpcaGeoModel.entityOf(renderState, WalkingSpiderHead.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(WalkingSpiderHead entity) {
        return Identifier.fromNamespaceAndPath("epca", "walking_spider_head");
    }
}
