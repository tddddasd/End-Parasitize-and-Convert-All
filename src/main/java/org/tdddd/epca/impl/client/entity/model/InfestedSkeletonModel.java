package org.tdddd.epca.impl.client.entity.model;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSkeleton;
import com.geckolib.model.GeoModel;

public class InfestedSkeletonModel extends GeoModel<InfestedSkeleton> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/infested_skeleton");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        InfestedSkeleton entity = EpcaGeoModel.entityOf(renderState, InfestedSkeleton.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(InfestedSkeleton entity) {
        return Identifier.fromNamespaceAndPath("epca", "infested_skeleton");
    }
}
