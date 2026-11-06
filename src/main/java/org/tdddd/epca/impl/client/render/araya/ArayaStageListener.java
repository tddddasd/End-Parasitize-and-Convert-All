package org.tdddd.epca.impl.client.render.araya;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class ArayaStageListener {

    private ArayaStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(
            RenderLevelStageEvent.AfterTranslucentParticles event) {
        ArayaSlashRenderer.renderGeometry(event);
        ArayaFireRenderer.renderGeometry(event);
    }
}
