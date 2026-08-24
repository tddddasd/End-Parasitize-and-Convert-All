package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void onCanSurvive(BlockState blockState, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        Block belowBlock = belowState.getBlock();

        if (belowBlock == ModBlocks.INFESTED_SUGAR_CANE.get()) {
            cir.setReturnValue(true);
            return;
        }

        if (belowBlock == ModBlocks.INFESTED_DIRT.get() ||
                belowBlock == ModBlocks.INFESTED_SAND.get()) {

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockState sideState = level.getBlockState(below.relative(dir));
                if (sideState.is(Blocks.WATER)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        int age = state.getValue(SugarCaneBlock.AGE);
        if (age < 15) {
            if (random.nextInt(3) == 0) { // 1/3 概率增长年龄
                level.setBlock(pos, state.setValue(SugarCaneBlock.AGE, age + 1), 2);
            }
            ci.cancel();
            return;
        }

        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) {
            ci.cancel();
            return;
        }

        int totalCount = 0;
        BlockPos checkPos = pos;
        while (totalCount < 3 && (level.getBlockState(checkPos).getBlock() instanceof SugarCaneBlock || level.getBlockState(checkPos).getBlock() == ModBlocks.INFESTED_SUGAR_CANE.get())) {
            totalCount++;
            checkPos = checkPos.below();
        }
        if (totalCount >= 3) {
            ci.cancel();
            return;
        }

        level.setBlock(above, Blocks.SUGAR_CANE.defaultBlockState()
                .setValue(SugarCaneBlock.AGE, 0), 2);
        level.setBlock(pos, state.setValue(SugarCaneBlock.AGE, 0), 2);

        ci.cancel();
    }
}