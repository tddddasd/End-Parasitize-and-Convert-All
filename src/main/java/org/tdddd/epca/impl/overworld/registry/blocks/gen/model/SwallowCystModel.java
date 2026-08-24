package org.tdddd.epca.impl.overworld.registry.blocks.gen.model;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.epca;
import software.bernie.geckolib.model.GeoModel;

public class SwallowCystModel extends GeoModel<SwallowCystBlockEntity> {
    @Override
    public ResourceLocation getModelResource(SwallowCystBlockEntity object) {
        return new ResourceLocation(epca.MODID, "geo/block/swallow_cyst.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwallowCystBlockEntity object) {
        return new ResourceLocation(epca.MODID, "textures/block/swallow_cyst.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwallowCystBlockEntity animatable) {
        return new ResourceLocation(epca.MODID, "animations/swallow_cyst.animation.json");
    }
}