package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;

import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedFox;

import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;

import com.geckolib.model.GeoModel;

public class InfestedFoxModel extends GeoModel<InfestedFox> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/infested_fox");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        InfestedFox entity = EpcaGeoModel.entityOf(renderState, InfestedFox.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(InfestedFox entity) {
        return Identifier.fromNamespaceAndPath("epca", "infested_fox");
    }
}