package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudLayer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;

/**
 * Renderer for the reshape yelloweye.
 *
 * <p>26.1.2 had no dedicated yelloweye renderer: the entity was auto-registered with a bare
 * {@code EpcaGeoRenderer}, which is exactly what this class specialises (same default
 * {@code EpcaGeoModel}, same resources resolved from {@code EpcaEntityManager} by entity type).
 * The only addition is the shader-rendered gas cloud layer, which needs a
 * {@code addLayerProvider} hook.</p>
 */
public class ReshapeYelloweyeRenderer extends EpcaGeoRenderer<ReshapeYelloweye> {

    public ReshapeYelloweyeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        addLayerProvider(GasCloudLayer.forYelloweye());
    }
}
