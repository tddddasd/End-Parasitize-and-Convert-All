package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.render.sky.DarknessDevourEffect;

@Mixin(FogRenderer.class)
public abstract class FogRendererDevourMixin {

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;F"
            + "Lnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
            at = @At("RETURN"))
    private void epca$applyDevourFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
                                     float partialTick, ClientLevel level,
                                     CallbackInfoReturnable<FogData> cir) {
        FogData fog = cir.getReturnValue();
        if (fog != null) {
            DarknessDevourEffect.applyFog(fog, (float) renderDistance);
        }
    }
}
