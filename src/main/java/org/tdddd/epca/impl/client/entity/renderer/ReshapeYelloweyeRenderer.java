package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudLayer;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;

/**
 * Renderer for {@code epca:reshape_yelloweye}.
 *
 * <p>Until this class existed the mob used the shared auto-registered {@link EpcaGeoRenderer},
 * which offers no hook for extra GeckoLib render layers. It now has a dedicated renderer so the
 * shader-rendered gas clouds of its jet skill can be attached as a layer.</p>
 */
public class ReshapeYelloweyeRenderer extends EpcaGeoRenderer<ReshapeYelloweye> {

    public ReshapeYelloweyeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        // Shader-rendered gas clouds for the yelloweye jet skill.
        addLayerProvider(GasCloudLayer.forYelloweye());
    }
}
