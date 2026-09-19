package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.epca;

import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;

import com.geckolib.model.GeoModel;

public class ReshapeLongarmsModel extends GeoModel<ReshapeLongarms> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/reshape_longarms");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/reshape_longarms.png");
    }

    @Override
    public Identifier getAnimationResource(ReshapeLongarms entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "reshape_longarms");
    }
}