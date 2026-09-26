package org.tdddd.epca.impl.client.render.araya;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

/**
 * When the Alayavijnana slash and the aura's fire blocks are drawn.
 *
 * <p>Both go into {@code AFTER_PARTICLES}: the terrain, the entities and the particles are already in the
 * frame, the world pose stack is still the camera-relative one, and the depth buffer is complete, which
 * is what the refraction needs (its frame copy must contain what is actually behind the slash) and what
 * the fire blocks need (they must be occluded by nearby terrain). Drawing them earlier - for example in
 * {@code AFTER_SKY} - would put the fire behind the world instead.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArayaStageListener {

    private ArayaStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        ArayaSlashRenderer.renderGeometry(event);
        ArayaFireRenderer.renderGeometry(event);
    }
}
