package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;

import org.tdddd.epca.impl.epca;

import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;

import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.LightCarrier;

import com.geckolib.model.GeoModel;

public class LightCarrierModel extends GeoModel<LightCarrier> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/light_carrier");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        LightCarrier entity = EpcaGeoModel.entityOf(renderState, LightCarrier.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(LightCarrier entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "light_carrier");
    }
}