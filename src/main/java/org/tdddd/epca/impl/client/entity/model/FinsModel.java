package org.tdddd.epca.impl.client.entity.model;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import com.geckolib.model.GeoModel;

public class FinsModel extends GeoModel<Fins> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/fins");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Fins entity = EpcaGeoModel.entityOf(renderState, Fins.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(Fins entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "fins");
    }
}