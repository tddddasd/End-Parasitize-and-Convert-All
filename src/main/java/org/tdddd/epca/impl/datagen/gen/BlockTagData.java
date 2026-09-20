package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModTags;

import java.util.concurrent.CompletableFuture;


public class BlockTagData extends BlockTagsProvider {
    public BlockTagData(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, epca.MODID);
    }

    private boolean isModBlock(Block b) {
        var key = BuiltInRegistries.BLOCK.getKey(b);
        return key != null && key.getNamespace().equals(epca.MODID);
    }

    private static boolean isWoodLike(String name) {
        return name.contains("planks") || name.contains("log") || name.contains("wood");
    }
    private static boolean isOre(String name) {
        return name.contains("_ore");
    }
    private static boolean isStoneLike(String name) {
        return name.contains("stone") || name.contains("cobble") || name.contains("brick")
                || name.contains("sandstone") || name.contains("polished")
                || name.contains("rocklike") || name.contains("metallike")
                || name.contains("hardlike") || name.contains("pedestal") || name.contains("altar");
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Block block = entry.getValue();
            if (!isModBlock(block)) continue;
            String name = entry.getKey().identifier().getPath();

            
            if (block instanceof RotatedPillarBlock || block instanceof FenceBlock
                    || block instanceof FenceGateBlock || isWoodLike(name) ||
                    name.contains("pumpkin") || name.contains("carved_pumpkin") ||
                    name.contains("cactus") || name.contains("mangrove_roots")) {
                tag(BlockTags.MINEABLE_WITH_AXE).add(block);
            } else if (block instanceof SnowLayerBlock || block instanceof FallingBlock
                    || name.contains("sand") || name.contains("dirt")
                    || name.contains("dustlike") || name.contains("snow_block")
                    || name.contains("muddy_mangrove_roots")) {
                tag(BlockTags.MINEABLE_WITH_SHOVEL).add(block);
            } else if (block instanceof LeavesBlock || block instanceof BushBlock
                    || block instanceof MultifaceBlock || name.contains("nethersea_brand_grown")
                    || name.contains("residue")) {
                tag(BlockTags.MINEABLE_WITH_HOE).add(block);
            } else if (!(block instanceof LiquidBlock)) {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            }

            
            if (isOre(name) && block.defaultDestroyTime() >= 3.0F) {
                if (name.contains("iron") || name.contains("copper") || name.contains("lapis")) {
                    tag(BlockTags.NEEDS_STONE_TOOL).add(block);
                } else if (name.contains("gold") || name.contains("redstone")
                        || name.contains("emerald") || name.contains("diamond")) {
                    tag(BlockTags.NEEDS_IRON_TOOL).add(block);
                }
            }

            
            if (block instanceof WallBlock) tag(BlockTags.WALLS).add(block);
            if (block instanceof FenceBlock) {
                tag(BlockTags.FENCES).add(block);
                if (isWoodLike(name)) tag(BlockTags.WOODEN_FENCES).add(block);
            }
        }
    }
}
