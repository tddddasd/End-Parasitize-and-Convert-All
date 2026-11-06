package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;

public class InfestedSpiderModel extends GeoModel<InfestedSpider> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/infested_spider");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        InfestedSpider entity = EpcaGeoModel.entityOf(renderState, InfestedSpider.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(InfestedSpider entity) {
        return Identifier.fromNamespaceAndPath("epca", "infested_spider");
    }
}
