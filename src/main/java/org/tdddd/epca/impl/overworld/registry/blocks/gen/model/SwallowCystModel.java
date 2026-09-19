package org.tdddd.epca.impl.overworld.registry.blocks.gen.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;

import org.tdddd.epca.impl.epca;

import com.geckolib.model.GeoModel;

public class SwallowCystModel extends GeoModel<SwallowCystBlockEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "block/swallow_cyst");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/block/swallow_cyst.png");
    }

    @Override
    public Identifier getAnimationResource(SwallowCystBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "swallow_cyst");
    }
}