package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;

/**
 * Lets a like biome recolour the sky.
 *
 * <p>1.20.1 has no viewport event for the sky colour, but {@code ClientLevel#getSkyColor} is what the
 * sky renderer asks, so overriding its result is the smallest reliable hook.</p>
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void epca$likeBiomeSkyColor(BlockPos pos, float partialTicks,
                                        CallbackInfoReturnable<Vec3> cir) {
        Integer like = LikeBiomeClientEffects.skyColor(pos);
        if (like == null) {
            return;
        }
        cir.setReturnValue(new Vec3(
                ((like >> 16) & 0xFF) / 255.0D,
                ((like >> 8) & 0xFF) / 255.0D,
                (like & 0xFF) / 255.0D));
    }
}
