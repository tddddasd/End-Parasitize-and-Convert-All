package org.tdddd.epca.impl.client.entity.model;

import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingEndermanHead;
import com.geckolib.model.GeoModel;

public class WalkingEndermanHeadModel extends GeoModel<WalkingEndermanHead> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "entity/walking_enderman_head");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        WalkingEndermanHead entity = EpcaGeoModel.entityOf(renderState, WalkingEndermanHead.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(WalkingEndermanHead entity) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "walking_enderman_head");
    }
}