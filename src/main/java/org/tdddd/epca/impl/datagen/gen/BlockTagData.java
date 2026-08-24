package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModTags;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class BlockTagData extends BlockTagsProvider {
    public BlockTagData(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                        @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, epca.MODID, existingFileHelper);
    }

    private boolean isModBlock(Block b) {
        var key = ForgeRegistries.BLOCKS.getKey(b);
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
        for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
            Block block = entry.getValue();
            if (!isModBlock(block)) continue;
            String name = entry.getKey().location().getPath();

            // ── 可挖掘工具标签 ──
            if (block instanceof RotatedPillarBlock || block instanceof FenceBlock
                    || block instanceof FenceGateBlock || isWoodLike(name) ||
                    name.contains("pumpkin") || name.contains("carved_pumpkin") ||
                    name.contains("cactus")) {
                tag(BlockTags.MINEABLE_WITH_AXE).add(block);
            } else if (block instanceof SnowLayerBlock || block instanceof FallingBlock
                    || name.contains("sand") || name.contains("dirt")
                    || name.contains("dustlike") || name.contains("snow_block")) {
                tag(BlockTags.MINEABLE_WITH_SHOVEL).add(block);
            } else if (block instanceof LeavesBlock || block instanceof BushBlock
                    || block instanceof MultifaceBlock || name.contains("nethersea_brand_grown")
                    || name.contains("residue")) {
                tag(BlockTags.MINEABLE_WITH_HOE).add(block);
            } else if (!(block instanceof LiquidBlock)) {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            }

            // ── 工具等级标签 ──
            if (isOre(name) && block.defaultDestroyTime() >= 3.0F) {
                if (name.contains("iron") || name.contains("copper") || name.contains("lapis")) {
                    tag(BlockTags.NEEDS_STONE_TOOL).add(block);
                } else if (name.contains("gold") || name.contains("redstone")
                        || name.contains("emerald") || name.contains("diamond")) {
                    tag(BlockTags.NEEDS_IRON_TOOL).add(block);
                }
            }

            // ── 结构标签 ──
            if (block instanceof WallBlock) tag(BlockTags.WALLS).add(block);
            if (block instanceof FenceBlock) {
                tag(BlockTags.FENCES).add(block);
                if (isWoodLike(name)) tag(BlockTags.WOODEN_FENCES).add(block);
            }
        }

        // ── 自定义标签 ──
        blockTag("altar_stones", "packed_mud_altar_stone");
        blockTag("pedestals",    "packed_mud_pedestal");
    }

    private void blockTag(String tagName, String blockName) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(epca.MODID, blockName));
        if (block != null) {
            tag(TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(),
                    new ResourceLocation(epca.MODID, tagName))).add(block);
        }
    }
}
