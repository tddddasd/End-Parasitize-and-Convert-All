package org.tdddd.epca.impl.client.entity.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;

/**
 * Model for all three infested spider variants.
 *
 * <p>GeckoLib 5 asset convention: the model / animation identifier is the bare name, and the loader
 * derives the file from it ({@code geckolib/models/epca/entity/infested_spider.geo.json},
 * {@code geckolib/animations/epca/infested_spider.animation.json}). The texture stays a full
 * {@code textures/...png} path because it goes through the vanilla texture pipeline.</p>
 *
 * <p>The variants share one model and one animation file and differ only in the texture, which the
 * entity itself resolves. This is the same shape as {@code InfestedFoxModel} / {@code RipperModel}.</p>
 */
public class InfestedSpiderModel extends GeoModel<InfestedSpider> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("epca", "entity/infested_spider");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        InfestedSpider entity = EpcaGeoModel.entityOf(renderState, InfestedSpider.class);
        return entity == null ? null : entity.getTextureResource();
    }

    @Override
    public Identifier getAnimationResource(InfestedSpider entity) {
        return Identifier.fromNamespaceAndPath("epca", "infested_spider");
    }
}
