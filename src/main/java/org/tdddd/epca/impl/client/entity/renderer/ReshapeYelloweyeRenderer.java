package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudLayer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;

public class ReshapeYelloweyeRenderer extends EpcaGeoRenderer<ReshapeYelloweye> {

    public ReshapeYelloweyeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);

        addLayerProvider(GasCloudLayer.forYelloweye());
    }
}
