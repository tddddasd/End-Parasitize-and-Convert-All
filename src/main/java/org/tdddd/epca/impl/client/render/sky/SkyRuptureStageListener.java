package org.tdddd.epca.impl.client.render.sky;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class SkyRuptureStageListener {

    private SkyRuptureStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStageAfterSky(RenderLevelStageEvent.AfterSky event) {
        SkyRuptureRenderer.renderAfterSky();
    }
}
