package org.tdddd.epca.impl.client.render.araya;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;

/**
 * When the Alayavijnana slash and the aura's fire blocks are drawn.
 *
 * <p>Both go into {@code AfterTranslucentParticles}: the terrain, the entities and the particles are
 * already in the frame, the world stage's pose stack is the camera-relative one, and the depth buffer is
 * complete, which is what the refraction needs (its frame copy must contain what is actually behind the
 * slash) and what the fire blocks need (they must be occluded by nearby terrain). Drawing them earlier -
 * for example in {@code AfterSky} - would put the fire behind the world instead.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>1.20.1 filtered the two stages out of the single Forge {@code RenderLevelStageEvent} with
 * {@code event.getStage() == Stage.AFTER_PARTICLES}. 26.1.2 replaced the {@code Stage} enum with one
 * subclass per stage, so this listener subscribes to
 * {@code RenderLevelStageEvent.AfterTranslucentParticles} directly, exactly like the tree's own
 * sacrifice-ritual and sky-rupture listeners do.</p>
 */
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
