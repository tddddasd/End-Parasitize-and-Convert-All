package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;

/**
 * Lets a like biome override rain at a position.
 *
 * <p>Only the server can answer this, because the like biome table is server saved data; the client keeps
 * rendering whatever the server told it, which is the usual precipitation sync.</p>
 */
@Mixin(Level.class)
public abstract class LevelRainMixin {

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
    private void epca$likeBiomeRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (!(self instanceof ServerLevel serverLevel)) {
            return;
        }
        LikeBiome like = LikeBiomeManager.activeAt(serverLevel, pos);
        if (like == null || !like.overrideRain()) {
            return;
        }
        var biome = serverLevel.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .get(LikeBiomeManager.resolve(serverLevel, like.biomeId()));
        if (biome == null) {
            return;
        }
        // rain in the like biome requires that biome to have precipitation and no sky obstruction
        if (!biome.hasPrecipitation()) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(serverLevel.canSeeSky(pos) && serverLevel.getBiome(pos).value().hasPrecipitation());
    }
}
