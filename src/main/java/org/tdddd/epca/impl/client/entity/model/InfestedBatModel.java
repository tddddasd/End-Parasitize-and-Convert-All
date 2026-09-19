package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.epca;

import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedBat;

import com.geckolib.model.GeoModel;

public class InfestedBatModel extends GeoModel<InfestedBat> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/infested_bat");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/infested_bat.png");
    }

    @Override
    public Identifier getAnimationResource(InfestedBat entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "infested_bat");
    }
}