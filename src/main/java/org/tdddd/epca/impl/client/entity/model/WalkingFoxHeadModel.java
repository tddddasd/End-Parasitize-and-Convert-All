package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;

import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedFox;

import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingFoxHead;

import com.geckolib.model.GeoModel;

public class WalkingFoxHeadModel extends GeoModel<WalkingFoxHead> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/walking_fox_head");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        WalkingFoxHead entity = EpcaGeoModel.entityOf(renderState, WalkingFoxHead.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(WalkingFoxHead entity) {
        return Identifier.fromNamespaceAndPath("epca", "walking_fox_head");
    }
}