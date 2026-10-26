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

/** Lets a like biome override rain. Verified: {@code Level.isRainingAt(BlockPos) : boolean}. */
@Mixin(Level.class)
public abstract class LevelRainLikeBiomeMixin {

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true, require = 0)
    private void epca$likeBiomeRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (!(self instanceof ServerLevel serverLevel)) {
            return;
        }
        LikeBiome like = LikeBiomeManager.activeAt(serverLevel, pos);
        if (like == null || !like.overrideRain()) {
            return;
        }
        var key = LikeBiomeManager.resolve(serverLevel, like.biomeId());
        if (key == null) {
            return;
        }
        var biome = serverLevel.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).get(key);
        if (biome.isEmpty()) {
            return;
        }
        cir.setReturnValue(biome.get().value().hasPrecipitation() && serverLevel.canSeeSky(pos));
    }
}
