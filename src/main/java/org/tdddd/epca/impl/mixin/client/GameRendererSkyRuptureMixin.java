package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureRenderer;

/**
 * Advances the world barrier rupture progress exactly once per rendered frame.
 *
 * <h2>Why {@code GameRenderer.render} and not the level-stage event</h2>
 * A shader pack's shadow pass runs {@code LevelRenderer.renderLevel()} a second time, so driving the
 * timer from the AfterSky stage would advance it twice per frame and halve the effect's duration.
 * {@code GameRenderer.render} runs strictly once per frame. This mirrors the 1.20.1
 * {@code GameRendererShaderLayerMixin}, which injected on the same method for the same reason.
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * The 1.20.1 target was {@code render(FJZ)V}. 26.1.2 replaced the raw partial-tick float and the
 * nanosecond timestamp with a single {@link DeltaTracker}, so the descriptor is now
 * {@code (Lnet/minecraft/client/DeltaTracker;Z)V} - verified with {@code javap -s} on the 26.1.2
 * patched jar:
 * {@code public void render(net.minecraft.client.DeltaTracker, boolean)}. The handler therefore takes
 * {@code (DeltaTracker, boolean, CallbackInfo)}; the timing information it needs is read from the
 * effect's own {@code System.nanoTime()} deltas, exactly as 1.20.1 did.
 *
 * <p>Priority is left at the default because nothing else in this mod touches the same method and the
 * injection is a plain HEAD insert.</p>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererSkyRuptureMixin {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void epca$updateSkyRupture(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        SkyRuptureRenderer.update();
    }
}
