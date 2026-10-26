package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;

/**
 * Lets a like biome recolour the sky.
 *
 * <p>Verified signature (javap on the 1.20.1 mapped jar):
 * {@code public Vec3 getSkyColor(Vec3 cameraPos, float partialTick)}.</p>
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void epca$likeBiomeSkyColor(Vec3 cameraPos, float partialTicks,
                                        CallbackInfoReturnable<Vec3> cir) {
        Integer like = LikeBiomeClientEffects.skyColor(net.minecraft.core.BlockPos.containing(cameraPos));
        if (like == null) {
            return;
        }
        cir.setReturnValue(new Vec3(
                ((like >> 16) & 0xFF) / 255.0D,
                ((like >> 8) & 0xFF) / 255.0D,
                (like & 0xFF) / 255.0D));
    }
}
