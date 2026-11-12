package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;

@Mixin(BiomeColors.class)
public abstract class BiomeColorsLikeBiomeMixin {

    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void epca$likeWater(BlockAndTintGetter level, BlockPos pos,
                                       CallbackInfoReturnable<Integer> cir) {
        Integer vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }
        // the vanilla colour doubles as the "no like biome" colour of the transition band
        Integer like = LikeBiomeClientEffects.waterColor(pos, vanilla);
        if (like != null) {
            cir.setReturnValue(like);
        }
    }

    @Inject(method = "getAverageGrassColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void epca$likeGrass(BlockAndTintGetter level, BlockPos pos,
                                       CallbackInfoReturnable<Integer> cir) {
        Integer vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }
        Integer like = LikeBiomeClientEffects.grassColor(pos, vanilla);
        if (like != null) {
            cir.setReturnValue(like);
        }
    }

    @Inject(method = "getAverageFoliageColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void epca$likeFoliage(BlockAndTintGetter level, BlockPos pos,
                                         CallbackInfoReturnable<Integer> cir) {
        Integer vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }
        Integer like = LikeBiomeClientEffects.foliageColor(pos, vanilla);
        if (like != null) {
            cir.setReturnValue(like);
        }
    }
}
