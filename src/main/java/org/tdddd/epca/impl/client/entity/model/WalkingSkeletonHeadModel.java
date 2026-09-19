package org.tdddd.epca.impl.client.entity.model;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingSkeletonHead;
import com.geckolib.model.GeoModel;

public class WalkingSkeletonHeadModel extends GeoModel<WalkingSkeletonHead> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/walking_skeleton_head");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        WalkingSkeletonHead entity = EpcaGeoModel.entityOf(renderState, WalkingSkeletonHead.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(WalkingSkeletonHead entity) {
        return Identifier.fromNamespaceAndPath("epca", "walking_skeleton_head");
    }
}
