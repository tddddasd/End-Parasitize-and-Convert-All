package org.tdddd.epca.impl.client.render.ritual;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

/**
 * When the sacrifice-ritual aura is drawn.
 *
 * <p>{@code AFTER_PARTICLES} for the geometry: the terrain, entities and particles are already in the
 * frame, the world pose stack is still the camera-relative one, and the buffers of this pass are still
 * open - so the wavefield can lie on the block top surfaces and the pillar is still depth tested
 * against the terrain (a hill in front of the altar hides it, as it should). Drawing it earlier (for
 * example in {@code AFTER_SKY}) would put it behind the terrain instead.</p>
 *
 * <p>{@code AFTER_SKY} for the purple sky cover: it is the earliest stage of the level, so the
 * terrain, the entities and the aura itself are all drawn on top of it - which is exactly "the sky
 * turns purple" without covering anything that is not sky.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SacrificeRitualStageListener {

    private SacrificeRitualStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            SacrificeRitualRenderer.renderSky(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            SacrificeRitualRenderer.renderGeometry(event);
        }
    }
}
