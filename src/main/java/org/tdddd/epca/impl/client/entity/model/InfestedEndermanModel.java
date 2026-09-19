package org.tdddd.epca.impl.client.entity.model;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.onesent.Fins;
import com.geckolib.model.GeoModel;

public class InfestedEndermanModel extends GeoModel<InfestedEnderman> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/infested_enderman");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        InfestedEnderman entity = EpcaGeoModel.entityOf(renderState, InfestedEnderman.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(InfestedEnderman entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "infested_enderman");
    }
}