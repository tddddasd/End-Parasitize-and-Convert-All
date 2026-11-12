package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

/**
 * Central block-change hook of the like biome system.
 *
 * <p>Every block change of an already loaded chunk passes through here, no matter who caused it
 * (players, explosions, pistons, growth of infested dirt/sand, /setblock, world edit...). This is
 * what drives the generation/removal rule of the 4x4x4 like biome units:</p>
 * <ul>
 *     <li>new state implements {@link InfestedBlockInterface} &rarr; the unit gets the parasite
 *     like biome immediately (no scan needed, the block itself proves it);</li>
 *     <li>an infested block was replaced by something else &rarr; the unit is re-checked at the end
 *     of the tick (the check must run after the world settled).</li>
 * </ul>
 *
 * <p>26.1.2 notes: {@code setBlockState} takes the update flags as an {@code int} (1.20.1 used a
 * boolean) and may return {@code null} when the state did not change; mixins run on official names
 * so no refmap entry is required.</p>
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkLikeBiomeMixin {

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void epca$likeBiomeUnitTracking(BlockPos pos, BlockState newState, int flags,
                                            CallbackInfoReturnable<BlockState> cir) {
        BlockState replaced = cir.getReturnValue();
        LevelChunk chunk = (LevelChunk) (Object) this;
        Level level = chunk.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean placedInfested = newState.getBlock() instanceof InfestedBlockInterface;
        boolean removedInfested = replaced != null && replaced.getBlock() instanceof InfestedBlockInterface;
        if (!placedInfested && !removedInfested) {
            return;
        }
        if (placedInfested) {
            LikeBiomeManager.markUnitInfested(serverLevel, pos);
        } else {
            LikeBiomeManager.scheduleRefresh(serverLevel, pos);
        }
    }
}
