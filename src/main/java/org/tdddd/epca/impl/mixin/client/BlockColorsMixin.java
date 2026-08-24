package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.client.WaterColorEffectsManager;

@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {

    @Inject(
            method = "getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetBlockColor(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int tintIndex,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (tintIndex != 0 || state == null || pos == null) return;

        Block block = state.getBlock();
        if (block != Blocks.WATER) return;

        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() != Fluids.WATER && fluidState.getType() != Fluids.FLOWING_WATER) return;

        Integer originalColor = cir.getReturnValue();
        if (originalColor == null) {
            originalColor = 0x3F76E4;
        }

        int finalColor = WaterColorEffectsManager.getWaterColor(pos, originalColor);
        cir.setReturnValue(finalColor);
    }
}