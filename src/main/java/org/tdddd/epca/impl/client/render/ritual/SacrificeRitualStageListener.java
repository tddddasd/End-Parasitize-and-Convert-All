package org.tdddd.epca.impl.client.render.ritual;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;

/**
 * When the sacrifice-ritual aura is drawn.
 *
 * <p>{@code RenderLevelStageEvent.AfterTranslucentParticles} for the geometry: the terrain, entities
 * and particles are already in the frame, the world stage's buffers are still open - so the wavefield
 * can lie on the block top surfaces and the pillar is still depth tested against the terrain (a hill
 * in front of the altar hides it, as it should). Drawing it earlier (for example at
 * {@code AfterSky}) would put it behind the terrain instead.</p>
 *
 * <p>{@code RenderLevelStageEvent.AfterSky} for the purple sky cover: it is the earliest stage of the
 * level, so the terrain, the entities and the aura itself are all drawn on top of it - which is
 * exactly "the sky turns purple" without covering anything that is not sky.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>1.20.1 filtered the two stages out of the single Forge {@code RenderLevelStageEvent} with
 * {@code event.getStage() == Stage.AFTER_SKY / AFTER_PARTICLES}. 26.1.2 replaced the {@code Stage}
 * enum with one subclass per stage, so this listener subscribes to
 * {@code RenderLevelStageEvent.AfterSky} and {@code RenderLevelStageEvent.AfterTranslucentParticles}
 * directly, exactly like the tree's own sky-rupture listener subscribes to {@code AfterSky}
 * ({@code net.neoforged.neoforge.client.event.RenderLevelStageEvent$AfterSky extends
 * RenderLevelStageEvent}, verified with {@code javap} on
 * {@code neoforge-26.1.2.76-universal.jar}; {@code LevelRenderer} posts
 * {@code AfterTranslucentParticles} right after {@code renderTranslucentParticles()}). Subscribing to
 * the concrete classes is correct whether the bus dispatches exactly or also walks superclasses.</p>
 */
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
