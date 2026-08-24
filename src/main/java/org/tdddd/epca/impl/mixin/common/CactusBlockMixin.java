package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void onCanSurvive(BlockState blockState, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.getBlock() == ModBlocks.INFESTED_CACTUS.get() || belowState.getBlock() == ModBlocks.INFESTED_SAND.get()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {

        int age = state.getValue(CactusBlock.AGE);
        if (age < 15) {
            if (random.nextInt(4) == 0) {
                level.setBlock(pos, state.setValue(CactusBlock.AGE, age + 1), 2);
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
        while (totalCount < 3 && (level.getBlockState(checkPos).getBlock() instanceof CactusBlock || level.getBlockState(checkPos).getBlock() == ModBlocks.INFESTED_CACTUS.get())) {
            totalCount++;
            checkPos = checkPos.below();
        }
        if (totalCount >= 3) {
            ci.cancel();
            return;
        }

        level.setBlock(above, Blocks.CACTUS.defaultBlockState()
                .setValue(CactusBlock.AGE, 0), 2);
        level.setBlock(pos, state.setValue(CactusBlock.AGE, 0), 2);

        ci.cancel();
    }
}