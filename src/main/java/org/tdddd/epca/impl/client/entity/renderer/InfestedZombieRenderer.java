package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.model.InfestedZombieModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedZombie;

/**
 * Renderer for the infested zombie.
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>The old {@code render(...)} override only assigned the (now removed)
 * {@code GeoRenderer#animatable} field so the inherited render path could see the entity; the glow
 * pass it fed was already commented out in the 1.20.1 sources. GeckoLib 5 passes the animatable to
 * render-state extraction itself, so the override has no replacement and was dropped.</p>
 */
public class InfestedZombieRenderer extends EpcaGeoRenderer<InfestedZombie> {

    public InfestedZombieRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedZombieModel());
    }
}
