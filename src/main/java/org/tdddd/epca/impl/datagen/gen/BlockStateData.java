package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;

import java.util.Objects;
import java.util.Set;

/**
 * 数据生成器：自动为模组中所有方块生成 blockstates 和 models JSON。
 * 遍历 ForgeRegistries.BLOCKS 中属于本模组的方块，根据方块类型自动调用对应生成方法。
 * 特殊方块（自定义 BBmodel、多模型变体等）通过 MANUAL_BLOCKS 跳过。
 */
public class BlockStateData extends BlockStateProvider {

    /**
     * 需手动维护模型/blockstate 的方块：
     * - 多模型变体（dirt 随机纹理）
     * - 雪层（高度属性）
     * - 藤蔓（多方块面）
     * - 滴水石锥
     * - 方块实体渲染器（swallow_cyst, packed_mud_pedestal）
     * - 自定义 BBmodel 元素模型
     */
    private static final Set<String> MANUAL_BLOCKS = Set.of(
            "infested_dirt", "infested_log", "infested_stone", "infested_heavy_stone","infested_wood", "infested_stripped_wood",
            "infested_snow", "infested_spider_web", "infested_spider_web_blood", "infested_cave_spider_web",
            "infested_vine", "infested_sweet_berry_bush", "infested_cactus", "infested_sugar_cane",
            "infested_pointed_dripstone",
            "swallow_cyst",
            "infested_lily_pad", "infested_carved_pumpkin", "infested_pumpkin",
            "infested_remains_small", "infested_remains_medium", "infested_remains_large",
            "infested_residue",
            "infested_nethersea_brand_grown",
            "packed_mud_pedestal",
            "infested_sandstone", "infested_sandstone_slab", "infested_sandstone_stairs", "infested_chiseled_red_sandstone", "infested_chiseled_sandstone", "infested_cut_sandstone", "infested_cut_sandstone_slab",
            "infested_tall_grass", "infested_tall_fern"
    );

    public BlockStateData(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, epca.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ForgeRegistries.BLOCKS.getEntries().stream()
                .filter(e -> e.getKey().location().getNamespace().equals(epca.MODID))
                .filter(e -> !MANUAL_BLOCKS.contains(e.getKey().location().getPath()))
                .map(java.util.Map.Entry::getValue)
                .forEach(block -> {
                    try {
                        generateBlock(block);
                    } catch (Exception e) {
                        epca.LOGGER.warn("Skipping blockstate/model for {}: {}", name(block), e.getMessage());
                    }
                });
    }

    /**
     * 根据方块类型分发到对应的生成方法。
     */
    private void generateBlock(Block block) {
        // 最具体的类型优先匹配
        if (block instanceof SlabBlock slab) {
            slabBlockWithItem(slab);
        } else if (block instanceof StairBlock stair) {
            stairsBlockWithItem(stair);
        } else if (block instanceof WallBlock wall) {
            wallBlockWithItem(wall);
        } else if (block instanceof RotatedPillarBlock) {
            logBlockWithItem(block);
        } else if (block instanceof LeavesBlock) {
            leavesBlockWithItem(block);
        } else if (block instanceof FenceBlock fence) {
            fenceBlockWithItem(fence);
        } else if (block instanceof FenceGateBlock gate) {
            fenceGateBlockWithItem(gate);
        } else if (block instanceof DoorBlock door) {
            doorBlockWithItem(door);
        } else if (block instanceof TrapDoorBlock trapdoor) {
            trapdoorBlockWithItem(trapdoor);
        } else if (block instanceof ButtonBlock button) {
            buttonBlockWithItem(button);
        } else if (block instanceof PressurePlateBlock plate) {
            pressurePlateBlockWithItem(plate);
        } else if (block instanceof IronBarsBlock pane) {
            paneBlockWithItem(pane);
        } else if (block instanceof BushBlock) {
            crossBlockWithItem(block);
        } else {
            // 默认：普通完整方块
            simpleBlockWithItem(block);
        }
    }

    // ======================== 带默认纹理推断的辅助方法 ========================

    /**
     * 尝试从方块名推断配套的"完整方块"，用于 slab/stairs/wall 等派生方块的纹理。
     * 例如 infested_cobblestone_slab -> infested_cobblestone
     */
    private Block findParentBlock(Block child, String... suffixes) {
        String childName = name(child);
        for (String suffix : suffixes) {
            if (childName.endsWith(suffix)) {
                String parentName = childName.substring(0, childName.length() - suffix.length());
                ResourceLocation rl = new ResourceLocation(epca.MODID, parentName);
                if (ForgeRegistries.BLOCKS.containsKey(rl)) {
                    return ForgeRegistries.BLOCKS.getValue(rl);
                }
            }
        }
        // fallback: use the child's own texture
        return child;
    }

    // ======================== 具体类型的生成方法 ========================

    /** 普通完整方块（六面纹理相同） */
    protected void simpleBlockWithItem(Block block) {
        simpleBlock(block);
        simpleBlockItem(block, cubeAll(block));
    }

    /** Slab：自动查找配套 fullBlock 获取纹理 */
    private void slabBlockWithItem(SlabBlock slab) {
        Block fullBlock = findParentBlock(slab, "_slab");
        ResourceLocation tex = blockTexture(fullBlock);
        slabBlock(slab, tex, tex);
        simpleBlockItem(slab, models().slab(name(slab), tex, tex, tex));
    }

    /** Stairs：自动查找配套 fullBlock 获取纹理 */
    private void stairsBlockWithItem(StairBlock stair) {
        Block fullBlock = findParentBlock(stair, "_stairs");
        ResourceLocation tex = blockTexture(fullBlock);
        stairsBlock(stair, tex);
        simpleBlockItem(stair, models().stairs(name(stair), tex, tex, tex));
    }

    /** Wall：自动查找配套 fullBlock 获取纹理 */
    private void wallBlockWithItem(WallBlock wall) {
        Block fullBlock = findParentBlock(wall, "_wall");
        ResourceLocation tex = blockTexture(fullBlock);
        wallBlock(wall, tex);
        simpleBlockItem(wall, models().wallInventory(name(wall) + "_inventory", tex));
    }

    /** Fence：自动查找配套 plank 获取纹理 */
    private void fenceBlockWithItem(FenceBlock fence) {
        Block plank = findParentBlock(fence, "_fence");
        ResourceLocation tex = blockTexture(plank);
        fenceBlock(fence, tex);
        simpleBlockItem(fence, models().fenceInventory(name(fence) + "_inventory", tex));
    }

    /** RotatedPillarBlock 原木/柱子类 */
    protected void logBlockWithItem(Block block) {
        logBlock((RotatedPillarBlock) block);
        simpleBlockItem(block, models().cubeColumn(
                name(block),
                blockTexture(block),
                extend(blockTexture(block), "_top")
        ));
    }

    /** 树叶方块 — 使用 cutout 渲染 */
    private void leavesBlockWithItem(Block block) {
        ModelFile leavesModel = models().cubeAll(name(block), blockTexture(block))
                .renderType("cutout");
        simpleBlock(block, leavesModel);
        simpleBlockItem(block, leavesModel);
    }

    /** 交叉植物（花草）— 使用 cutout 渲染 */
    protected void crossBlockWithItem(Block block) {
        simpleBlock(block, models().cross(name(block), blockTexture(block)).renderType("cutout"));
        itemModels().withExistingParent(name(block), "item/generated")
                .texture("layer0", blockTexture(block));
    }

    /** 栅栏门 */
    private void fenceGateBlockWithItem(FenceGateBlock gate) {
        Block plank = findParentBlock(gate, "_fence_gate");
        ResourceLocation tex = blockTexture(plank);
        fenceGateBlock(gate, tex);
        simpleBlockItem(gate, models().fenceGate(name(gate), tex));
    }

    /** 门 */
    private void doorBlockWithItem(DoorBlock door) {
        Block plank = findParentBlock(door, "_door");
        ResourceLocation tex = blockTexture(plank);
        doorBlockWithRenderType(door, tex, tex, "cutout");
        itemModels().withExistingParent(name(door), "item/generated")
                .texture("layer0", new ResourceLocation(epca.MODID, "item/" + name(door)));
    }

    /** 活板门 */
    private void trapdoorBlockWithItem(TrapDoorBlock trapdoor) {
        Block plank = findParentBlock(trapdoor, "_trapdoor");
        ResourceLocation tex = blockTexture(plank);
        trapdoorBlockWithRenderType(trapdoor, tex, true, "cutout");
        simpleBlockItem(trapdoor, models().trapdoorBottom(name(trapdoor) + "_bottom", tex));
    }

    /** 按钮 */
    private void buttonBlockWithItem(ButtonBlock button) {
        Block plank = findParentBlock(button, "_button");
        ResourceLocation tex = blockTexture(plank);
        ModelFile buttonModel = models().button(name(button), tex);
        ModelFile buttonPressedModel = models().buttonPressed(name(button) + "_pressed", tex);
        buttonBlock(button, buttonModel, buttonPressedModel);
        simpleBlockItem(button, models().buttonInventory(name(button) + "_inventory", tex));
    }

    /** 压力板 */
    private void pressurePlateBlockWithItem(PressurePlateBlock plate) {
        Block plank = findParentBlock(plate, "_pressure_plate");
        ResourceLocation tex = blockTexture(plank);
        ModelFile plateModel = models().pressurePlate(name(plate), tex);
        ModelFile plateDownModel = models().pressurePlateDown(name(plate) + "_down", tex);
        pressurePlateBlock(plate, plateModel, plateDownModel);
        simpleBlockItem(plate, plateModel);
    }

    /** 玻璃板/铁栏杆 */
    private void paneBlockWithItem(IronBarsBlock pane) {
        Block glass = findParentBlock(pane, "_pane");
        ResourceLocation tex = blockTexture(glass);
        paneBlock(pane, tex, extend(tex, "_pane_top"));
        itemModels().withExistingParent(name(pane), "item/generated")
                .texture("layer0", tex);
    }

    // ======================== 工具方法 ========================

    protected String name(Block block) {
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).getPath();
    }

    protected ResourceLocation extend(ResourceLocation rl, String suffix) {
        return new ResourceLocation(rl.getNamespace(), rl.getPath() + suffix);
    }
}
