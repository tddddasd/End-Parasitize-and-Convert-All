package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;

import org.tdddd.epca.impl.epca;

import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;

import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Ripper;

import com.geckolib.model.GeoModel;

public class RipperModel extends GeoModel<Ripper> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/ripper");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Ripper entity = EpcaGeoModel.entityOf(renderState, Ripper.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(Ripper entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "ripper");
    }
}