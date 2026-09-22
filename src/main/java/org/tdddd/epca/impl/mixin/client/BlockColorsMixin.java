package org.tdddd.epca.impl.mixin.client;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
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

import java.util.Optional;

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
        if (block != Blocks.WATER) {
            applyBiomeFoliageColor(state, level, pos, cir);
            return;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() != Fluids.WATER && fluidState.getType() != Fluids.FLOWING_WATER) return;

        Integer originalColor = cir.getReturnValue();
        if (originalColor == null) {
            originalColor = 0x3F76E4;
        }

        int finalColor = WaterColorEffectsManager.getWaterColor(pos, originalColor);
        cir.setReturnValue(finalColor);
    }

    /**
     * Makes every leaf block take the foliage colour of the biome it stands in.
     *
     * <p>Vanilla registers the biome foliage {@code BlockColor} only for oak/jungle/acacia/dark oak/mangrove
     * leaves; spruce and birch (and the azalea leaves) use hardcoded grayscale tints that ignore the biome
     * altogether. The cursed world's parasite biome sets {@code effects.foliage_color} to the purple
     * {@code 0x8E44AD} ({@code data/epca/worldgen/biome/parasite_biome.json}), so once those leaves follow the
     * biome they turn purple as well, while every other world keeps rendering them exactly as vanilla does -
     * the value installed here <b>is</b> the biome's own foliage colour, and a biome that overrides nothing
     * leaves the vanilla result untouched.
     *
     * <p>The biome lookup only works when the tint getter really is a level, which is always the case for
     * in-world block rendering; anything else falls through with the vanilla colour.
     */
    private static void applyBiomeFoliageColor(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (level == null || !state.is(BlockTags.LEAVES)) return;
        if (!(level instanceof LevelReader reader)) return;

        Optional<Integer> foliageColor = reader.getBiome(pos).value()
                .getSpecialEffects()
                .getFoliageColorOverride();
        if (foliageColor.isEmpty()) return;
        cir.setReturnValue(foliageColor.get());
    }
}
