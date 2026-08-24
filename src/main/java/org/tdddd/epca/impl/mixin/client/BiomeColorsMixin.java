package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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

        int finalColor = WaterColorEffectsManager.getWaterColor(pos, originalColor);
        cir.setReturnValue(finalColor);
    }
}