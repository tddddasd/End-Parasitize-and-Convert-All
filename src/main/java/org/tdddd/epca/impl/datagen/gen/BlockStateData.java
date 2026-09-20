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


public class BlockStateData extends BlockStateProvider {

    
    private static final Set<String> MANUAL_BLOCKS = Set.of(
            "infested_dirt", "infested_log", "infested_stone", "infested_heavy_stone","infested_wood", "infested_stripped_wood",
            "infested_snow", "infested_spider_web", "infested_spider_web_blood", "infested_cave_spider_web",
            "infested_vine", "infested_sweet_berry_bush", "infested_cactus", "infested_sugar_cane",
            "infested_pointed_dripstone", "infested_mangrove_roots", "infested_muddy_mangrove_roots",
            "swallow_cyst",
            "infested_lily_pad", "infested_carved_pumpkin", "infested_pumpkin",
            "infested_remains_small", "infested_remains_medium", "infested_remains_large",
            "infested_residue",
            "infested_nethersea_brand_grown",
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

    
    private void generateBlock(Block block) {
        
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
            
            simpleBlockWithItem(block);
        }
    }

    

    
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

    

    
    protected void simpleBlockWithItem(Block block) {
        simpleBlock(block);
        simpleBlockItem(block, cubeAll(block));
    }

    
    private void slabBlockWithItem(SlabBlock slab) {
        Block fullBlock = findParentBlock(slab, "_slab");
        ResourceLocation tex = blockTexture(fullBlock);
        slabBlock(slab, tex, tex);
        simpleBlockItem(slab, models().slab(name(slab), tex, tex, tex));
    }

    
    private void stairsBlockWithItem(StairBlock stair) {
        Block fullBlock = findParentBlock(stair, "_stairs");
        ResourceLocation tex = blockTexture(fullBlock);
        stairsBlock(stair, tex);
        simpleBlockItem(stair, models().stairs(name(stair), tex, tex, tex));
    }

    
    private void wallBlockWithItem(WallBlock wall) {
        Block fullBlock = findParentBlock(wall, "_wall");
        ResourceLocation tex = blockTexture(fullBlock);
        wallBlock(wall, tex);
        simpleBlockItem(wall, models().wallInventory(name(wall) + "_inventory", tex));
    }

    
    private void fenceBlockWithItem(FenceBlock fence) {
        Block plank = findParentBlock(fence, "_fence");
        ResourceLocation tex = blockTexture(plank);
        fenceBlock(fence, tex);
        simpleBlockItem(fence, models().fenceInventory(name(fence) + "_inventory", tex));
    }

    
    protected void logBlockWithItem(Block block) {
        logBlock((RotatedPillarBlock) block);
        simpleBlockItem(block, models().cubeColumn(
                name(block),
                blockTexture(block),
                extend(blockTexture(block), "_top")
        ));
    }

    
    private void leavesBlockWithItem(Block block) {
        ModelFile leavesModel = models().cubeAll(name(block), blockTexture(block))
                .renderType("cutout");
        simpleBlock(block, leavesModel);
        simpleBlockItem(block, leavesModel);
    }

    
    protected void crossBlockWithItem(Block block) {
        simpleBlock(block, models().cross(name(block), blockTexture(block)).renderType("cutout"));
        itemModels().withExistingParent(name(block), "item/generated")
                .texture("layer0", blockTexture(block));
    }

    
    private void fenceGateBlockWithItem(FenceGateBlock gate) {
        Block plank = findParentBlock(gate, "_fence_gate");
        ResourceLocation tex = blockTexture(plank);
        fenceGateBlock(gate, tex);
        simpleBlockItem(gate, models().fenceGate(name(gate), tex));
    }

    
    private void doorBlockWithItem(DoorBlock door) {
        Block plank = findParentBlock(door, "_door");
        ResourceLocation tex = blockTexture(plank);
        doorBlockWithRenderType(door, tex, tex, "cutout");
        itemModels().withExistingParent(name(door), "item/generated")
                .texture("layer0", new ResourceLocation(epca.MODID, "item/" + name(door)));
    }

    
    private void trapdoorBlockWithItem(TrapDoorBlock trapdoor) {
        Block plank = findParentBlock(trapdoor, "_trapdoor");
        ResourceLocation tex = blockTexture(plank);
        trapdoorBlockWithRenderType(trapdoor, tex, true, "cutout");
        simpleBlockItem(trapdoor, models().trapdoorBottom(name(trapdoor) + "_bottom", tex));
    }

    
    private void buttonBlockWithItem(ButtonBlock button) {
        Block plank = findParentBlock(button, "_button");
        ResourceLocation tex = blockTexture(plank);
        ModelFile buttonModel = models().button(name(button), tex);
        ModelFile buttonPressedModel = models().buttonPressed(name(button) + "_pressed", tex);
        buttonBlock(button, buttonModel, buttonPressedModel);
        simpleBlockItem(button, models().buttonInventory(name(button) + "_inventory", tex));
    }

    
    private void pressurePlateBlockWithItem(PressurePlateBlock plate) {
        Block plank = findParentBlock(plate, "_pressure_plate");
        ResourceLocation tex = blockTexture(plank);
        ModelFile plateModel = models().pressurePlate(name(plate), tex);
        ModelFile plateDownModel = models().pressurePlateDown(name(plate) + "_down", tex);
        pressurePlateBlock(plate, plateModel, plateDownModel);
        simpleBlockItem(plate, plateModel);
    }

    
    private void paneBlockWithItem(IronBarsBlock pane) {
        Block glass = findParentBlock(pane, "_pane");
        ResourceLocation tex = blockTexture(glass);
        paneBlock(pane, tex, extend(tex, "_pane_top"));
        itemModels().withExistingParent(name(pane), "item/generated")
                .texture("layer0", tex);
    }

    

    protected String name(Block block) {
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).getPath();
    }

    protected ResourceLocation extend(ResourceLocation rl, String suffix) {
        return new ResourceLocation(rl.getNamespace(), rl.getPath() + suffix);
    }
}
