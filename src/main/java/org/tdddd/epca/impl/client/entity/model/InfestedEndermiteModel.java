package org.tdddd.epca.impl.client.entity.model;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import com.geckolib.model.GeoModel;

public class InfestedEndermiteModel extends GeoModel<InfestedEndermite> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/infested_endermite");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        InfestedEndermite entity = EpcaGeoModel.entityOf(renderState, InfestedEndermite.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(InfestedEndermite entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "infested_endermite");
    }
}