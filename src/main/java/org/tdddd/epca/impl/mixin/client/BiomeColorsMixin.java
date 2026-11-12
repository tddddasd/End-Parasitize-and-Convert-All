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

        Integer originalColor = cir.getReturnValue();
        if (originalColor == null) {
            originalColor = 0x3F76E4;
        }

        // the vanilla colour doubles as the "no like biome" colour of the transition band
        Integer likeWater = LikeBiomeClientEffects.waterColor(pos, originalColor);
        if (likeWater != null) {
            cir.setReturnValue(likeWater);
            return;
        }

        cir.setReturnValue(WaterColorEffectsManager.getWaterColor(pos, originalColor));
    }

    @Inject(
            method = "getAverageGrassColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetAverageGrassColor(BlockAndTintGetter level, BlockPos pos,
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

    @Inject(
            method = "getAverageFoliageColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetAverageFoliageColor(BlockAndTintGetter level, BlockPos pos,
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
