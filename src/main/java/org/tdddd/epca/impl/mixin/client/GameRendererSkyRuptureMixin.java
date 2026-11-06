package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererSkyRuptureMixin {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void epca$updateSkyRupture(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        SkyRuptureRenderer.update();
    }
}
