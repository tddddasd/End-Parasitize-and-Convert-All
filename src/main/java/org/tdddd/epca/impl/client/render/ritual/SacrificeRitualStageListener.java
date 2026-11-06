package org.tdddd.epca.impl.client.render.ritual;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class SacrificeRitualStageListener {

    private SacrificeRitualStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStageAfterSky(RenderLevelStageEvent.AfterSky event) {
        SacrificeRitualRenderer.renderSky(event);
    }

    @SubscribeEvent
    public static void onRenderLevelStageAfterTranslucentParticles(
            RenderLevelStageEvent.AfterTranslucentParticles event) {
        SacrificeRitualRenderer.renderGeometry(event);
    }
}
