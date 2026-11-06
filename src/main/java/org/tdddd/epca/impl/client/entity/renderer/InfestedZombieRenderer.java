package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.model.InfestedZombieModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedZombie;

public class InfestedZombieRenderer extends EpcaGeoRenderer<InfestedZombie> {

    public InfestedZombieRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedZombieModel());
    }
}
