package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import org.tdddd.epca.impl.epca;

import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedZombie;

import com.geckolib.model.GeoModel;

public class InfestedZombieModel extends GeoModel<InfestedZombie> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/infested_zombie");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/infested_zombie.png");
    }

    @Override
    public Identifier getAnimationResource(InfestedZombie entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "infested_zombie");
    }
}