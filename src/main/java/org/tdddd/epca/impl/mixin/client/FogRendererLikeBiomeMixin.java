package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;

/**
 * Applies the like biome fog colour.
 *
 * <p>Verified against the 26.1.2 client jar: {@code FogRenderer.setupFog(Camera, int, DeltaTracker, float,
 * ClientLevel)} returns a freshly allocated {@code FogData} whose colour field is an
 * {@code org.joml.Vector4f}; the method is an instance method.</p>
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererLikeBiomeMixin {

    @Inject(method = "setupFog", at = @At("RETURN"), require = 0)
    private void epca$likeBiomeFog(Camera camera, int renderDistance, DeltaTracker deltaTracker,
                                   float partialTick, ClientLevel level,
                                   CallbackInfoReturnable<FogData> cir) {
        FogData fog = cir.getReturnValue();
        if (fog == null || camera == null || fog.color == null) {
            return;
        }
        Vector4f color = LikeBiomeClientEffects.fogColor(camera.blockPosition());
        if (color != null) {
            fog.color.set(color);
        }
    }
}
