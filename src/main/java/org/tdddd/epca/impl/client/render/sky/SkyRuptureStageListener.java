package org.tdddd.epca.impl.client.render.sky;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;

/**
 * Draw timing of the world barrier rupture.
 *
 * <p>1.20.1 subscribed to Forge's {@code RenderLevelStageEvent} and filtered on
 * {@code Stage.AFTER_SKY} - the instant {@code LevelRenderer.renderLevel()} has just finished
 * {@code renderSky()} and has not started the terrain yet (verified on the 1.20.1 bytecode:
 * {@code renderSky} at 438, the Forge AFTER_SKY dispatch at 441, the first {@code renderChunkLayer}
 * at 557).</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * 26.1.2 replaced the {@code Stage} enum with one subclass per stage, so this listener subscribes to
 * {@code RenderLevelStageEvent.AfterSky} directly instead of filtering a stage value. Verified with
 * {@code javap -s} on {@code neoforge-26.1.2.76-universal.jar}: the class exists as
 * {@code net.neoforged.neoforge.client.event.RenderLevelStageEvent$AfterSky extends
 * RenderLevelStageEvent}. Subscribing to the concrete class is correct whether the event bus
 * dispatches exactly or also walks superclasses, which is why the concrete type is used rather than
 * the abstract base plus an {@code instanceof} test.
 *
 * <p>Why it must be this early in the frame is explained in {@link SkyRuptureRenderer}: drawing after
 * the world lets the vanilla cloud plane's straight projected edge slice the effect in half.</p>
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class SkyRuptureStageListener {

    private SkyRuptureStageListener() {
    }

    @SubscribeEvent
    public static void onRenderLevelStageAfterSky(RenderLevelStageEvent.AfterSky event) {
        SkyRuptureRenderer.renderAfterSky();
    }
}
