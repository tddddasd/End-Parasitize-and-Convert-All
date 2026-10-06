package org.tdddd.epca.impl.client.render.sky;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

/**
 * <b></b>
 *
 * <p> Forge  {@code RenderLevelStageEvent.Stage.AFTER_SKY}
 * {@code LevelRenderer.renderLevel()}
 * {@code renderSky}  438AFTER_SKY  441
 *  {@code renderChunkLayer}  557</p>
 *
 * <p> {@link SkyRuptureRenderer}
 *
 * <b></b>
 * </p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkyRuptureStageListener {

    private SkyRuptureStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            SkyRuptureRenderer.renderAfterSky();
        }
    }
}

