package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.LikeBiomeClientEffects;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;

@Mixin(BiomeColors.class)
public abstract class BiomeColorsMixin {

    @Inject(
            method = "getAverageWaterColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetAverageWaterColor(
            BlockAndTintGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (level == null || pos == null) return;

        // a like biome recolours the water outright when its colour toggle is on
        Integer likeWater = LikeBiomeClientEffects.waterColor(pos);
        if (likeWater != null) {
            cir.setReturnValue(likeWater);
            return;
        }

        Integer originalColor = cir.getReturnValue();
        if (originalColor == null) {
            originalColor = 0x3F76E4; 
        }

        int finalColor = WaterColorEffectsManager.getWaterColor(pos, originalColor);
        cir.setReturnValue(finalColor);
    }

    /** Grass colour follows the like biome when the layer overrides colours. */
    @Inject(
            method = "getAverageGrassColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetAverageGrassColor(BlockAndTintGetter level, BlockPos pos,
                                               CallbackInfoReturnable<Integer> cir) {
        Integer like = LikeBiomeClientEffects.grassColor(pos);
        if (like != null) {
            cir.setReturnValue(like);
        }
    }

    /** Foliage colour follows the like biome when the layer overrides colours. */
    @Inject(
            method = "getAverageFoliageColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetAverageFoliageColor(BlockAndTintGetter level, BlockPos pos,
                                                 CallbackInfoReturnable<Integer> cir) {
        Integer like = LikeBiomeClientEffects.foliageColor(pos);
        if (like != null) {
            cir.setReturnValue(like);
        }
    }
}
