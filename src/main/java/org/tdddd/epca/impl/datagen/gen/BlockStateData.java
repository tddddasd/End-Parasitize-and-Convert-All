package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import org.tdddd.epca.impl.epca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;


public class BlockStateData extends ModelProvider {

    
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
            "infested_tall_grass", "infested_tall_fern",
            "acid_solution"
    );

    public BlockStateData(PackOutput output) {
        super(output, epca.MODID);
    }

    

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> isGenerated(holder.value()));
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        
        
        return BuiltInRegistries.ITEM.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> holder.value() instanceof BlockItem blockItem && isGenerated(blockItem.getBlock()));
    }

    private static boolean isGenerated(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null || !key.getNamespace().equals(epca.MODID)) return false;
        if (MANUAL_BLOCKS.contains(key.getPath())) return false;
        return block instanceof SlabBlock
                || block instanceof StairBlock
                || block instanceof WallBlock
                || block instanceof RotatedPillarBlock
                || block instanceof LeavesBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof ButtonBlock
                || block instanceof PressurePlateBlock
                || block instanceof IronBarsBlock
                || block instanceof BushBlock
                || true; 
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> isGenerated(holder.value()))
                .map(Holder::value)
                .forEach(block -> {
                    try {
                        generateBlock(blockModels, block);
                    } catch (Exception e) {
                        epca.LOGGER.warn("Skipping blockstate/model for {}: {}", name(block), e.getMessage());
                    }
                });
    }

    
    private void generateBlock(BlockModelGenerators g, Block block) {
        if (block instanceof SlabBlock slab) {
            slabBlockWithItem(g, slab);
        } else if (block instanceof StairBlock stair) {
            stairsBlockWithItem(g, stair);
        } else if (block instanceof WallBlock wall) {
            wallBlockWithItem(g, wall);
        } else if (block instanceof RotatedPillarBlock) {
            logBlockWithItem(g, block);
        } else if (block instanceof LeavesBlock) {
            leavesBlockWithItem(g, block);
        } else if (block instanceof FenceBlock fence) {
            fenceBlockWithItem(g, fence);
        } else if (block instanceof FenceGateBlock gate) {
            fenceGateBlockWithItem(g, gate);
        } else if (block instanceof DoorBlock door) {
            doorBlockWithItem(g, door);
        } else if (block instanceof TrapDoorBlock trapdoor) {
            trapdoorBlockWithItem(g, trapdoor);
        } else if (block instanceof ButtonBlock button) {
            buttonBlockWithItem(g, button);
        } else if (block instanceof PressurePlateBlock plate) {
            pressurePlateBlockWithItem(g, plate);
        } else if (block instanceof IronBarsBlock pane) {
            paneBlockWithItem(g, pane);
        } else if (block instanceof BushBlock) {
            crossBlockWithItem(g, block);
        } else {
            
            simpleBlockWithItem(g, block);
        }
    }

    

    
    private Block findParentBlock(Block child, String... suffixes) {
        String childName = name(child);
        for (String suffix : suffixes) {
            if (childName.endsWith(suffix)) {
                String parentName = childName.substring(0, childName.length() - suffix.length());
                Identifier rl = Identifier.fromNamespaceAndPath(epca.MODID, parentName);
                if (BuiltInRegistries.BLOCK.containsKey(rl)) {
                    return BuiltInRegistries.BLOCK.getValue(rl);
                }
            }
        }
        // fallback: use the child's own texture
        return child;
    }

    

    
    protected void simpleBlockWithItem(BlockModelGenerators g, Block block) {
        blockTextureModel(g, block, ModelTemplates.CUBE_ALL);
        g.registerSimpleItemModel(block, modelId(block));
    }

    
    private void slabBlockWithItem(BlockModelGenerators g, SlabBlock slab) {
        Block fullBlock = findParentBlock(slab, "_slab");
        TextureMapping tex = TextureMapping.cube(blockTexture(fullBlock));
        Identifier bottom = ModelTemplates.SLAB_BOTTOM.create(slab, tex, g.modelOutput);
        Identifier top = ModelTemplates.SLAB_TOP.create(slab, tex, g.modelOutput);
        Identifier full = modelId(fullBlock);
        g.blockStateOutput.accept(BlockModelGenerators.createSlab(slab,
                BlockModelGenerators.plainVariant(bottom),
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(full)));
        g.registerSimpleItemModel(slab, bottom);
    }

    
    private void stairsBlockWithItem(BlockModelGenerators g, StairBlock stair) {
        Block fullBlock = findParentBlock(stair, "_stairs");
        TextureMapping tex = TextureMapping.cube(blockTexture(fullBlock));
        Identifier inner = ModelTemplates.STAIRS_INNER.create(stair, tex, g.modelOutput);
        Identifier straight = ModelTemplates.STAIRS_STRAIGHT.create(stair, tex, g.modelOutput);
        Identifier outer = ModelTemplates.STAIRS_OUTER.create(stair, tex, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createStairs(stair,
                BlockModelGenerators.plainVariant(inner),
                BlockModelGenerators.plainVariant(straight),
                BlockModelGenerators.plainVariant(outer)));
        g.registerSimpleItemModel(stair, straight);
    }

    
    private void wallBlockWithItem(BlockModelGenerators g, WallBlock wall) {
        Block fullBlock = findParentBlock(wall, "_wall");
        Material tex = blockTexture(fullBlock);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.WALL, tex);
        Identifier post = ModelTemplates.WALL_POST.create(wall, mapping, g.modelOutput);
        Identifier lowSide = ModelTemplates.WALL_LOW_SIDE.create(wall, mapping, g.modelOutput);
        Identifier tallSide = ModelTemplates.WALL_TALL_SIDE.create(wall, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createWall(wall,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(lowSide),
                BlockModelGenerators.plainVariant(tallSide)));
        Identifier inventory = ModelTemplates.WALL_INVENTORY.create(wall, mapping, g.modelOutput);
        g.registerSimpleItemModel(wall, inventory);
    }

    
    private void fenceBlockWithItem(BlockModelGenerators g, FenceBlock fence) {
        Block plank = findParentBlock(fence, "_fence");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier post = ModelTemplates.FENCE_POST.create(fence, mapping, g.modelOutput);
        Identifier side = ModelTemplates.FENCE_SIDE.create(fence, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createFence(fence,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(side)));
        Identifier inventory = ModelTemplates.FENCE_INVENTORY.create(fence, mapping, g.modelOutput);
        g.registerSimpleItemModel(fence, inventory);
    }

    
    protected void logBlockWithItem(BlockModelGenerators g, Block block) {
        Material side = blockTexture(block);
        Material end = blockTexture(block, "_top");
        TextureMapping mapping = TextureMapping.column(side, end);
        Identifier model = ModelTemplates.CUBE_COLUMN.create(block, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(block,
                BlockModelGenerators.plainVariant(model)));
        g.registerSimpleItemModel(block, model);
    }

    
    private void leavesBlockWithItem(BlockModelGenerators g, Block block) {
        blockTextureModel(g, block, ModelTemplates.LEAVES);
        g.registerSimpleItemModel(block, modelId(block));
    }

    
    protected void crossBlockWithItem(BlockModelGenerators g, Block block) {
        TextureMapping mapping = TextureMapping.cross(blockTexture(block));
        Identifier model = ModelTemplates.CROSS.create(block, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
                BlockModelGenerators.plainVariant(model)));
        
        
        g.registerSimpleFlatItemModel(block);
    }

    
    private void fenceGateBlockWithItem(BlockModelGenerators g, FenceGateBlock gate) {
        Block plank = findParentBlock(gate, "_fence_gate");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier closed = ModelTemplates.FENCE_GATE_CLOSED.create(gate, mapping, g.modelOutput);
        Identifier open = ModelTemplates.FENCE_GATE_OPEN.create(gate, mapping, g.modelOutput);
        Identifier wallClosed = ModelTemplates.FENCE_GATE_WALL_CLOSED.create(gate, mapping, g.modelOutput);
        Identifier wallOpen = ModelTemplates.FENCE_GATE_WALL_OPEN.create(gate, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createFenceGate(gate,
                BlockModelGenerators.plainVariant(open),
                BlockModelGenerators.plainVariant(closed),
                BlockModelGenerators.plainVariant(wallOpen),
                BlockModelGenerators.plainVariant(wallClosed),
                true));
        g.registerSimpleItemModel(gate, closed);
    }

    
    private void doorBlockWithItem(BlockModelGenerators g, DoorBlock door) {
        g.createDoor(door);
    }

    
    private void trapdoorBlockWithItem(BlockModelGenerators g, TrapDoorBlock trapdoor) {
        Block plank = findParentBlock(trapdoor, "_trapdoor");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier top = ModelTemplates.TRAPDOOR_TOP.create(trapdoor, mapping, g.modelOutput);
        Identifier bottom = ModelTemplates.TRAPDOOR_BOTTOM.create(trapdoor, mapping, g.modelOutput);
        Identifier open = ModelTemplates.TRAPDOOR_OPEN.create(trapdoor, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createTrapdoor(trapdoor,
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(bottom),
                BlockModelGenerators.plainVariant(open)));
        g.registerSimpleItemModel(trapdoor, bottom);
    }

    
    private void buttonBlockWithItem(BlockModelGenerators g, ButtonBlock button) {
        Block plank = findParentBlock(button, "_button");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier normal = ModelTemplates.BUTTON.create(button, mapping, g.modelOutput);
        Identifier pressed = ModelTemplates.BUTTON_PRESSED.create(button, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createButton(button,
                BlockModelGenerators.plainVariant(normal),
                BlockModelGenerators.plainVariant(pressed)));
        Identifier inventory = ModelTemplates.BUTTON_INVENTORY.create(button, mapping, g.modelOutput);
        g.registerSimpleItemModel(button, inventory);
    }

    
    private void pressurePlateBlockWithItem(BlockModelGenerators g, PressurePlateBlock plate) {
        Block plank = findParentBlock(plate, "_pressure_plate");
        Material tex = blockTexture(plank);
        TextureMapping mapping = TextureMapping.singleSlot(net.minecraft.client.data.models.model.TextureSlot.TEXTURE, tex);
        Identifier off = ModelTemplates.PRESSURE_PLATE_UP.create(plate, mapping, g.modelOutput);
        Identifier on = ModelTemplates.PRESSURE_PLATE_DOWN.create(plate, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(plate,
                BlockModelGenerators.plainVariant(off),
                BlockModelGenerators.plainVariant(on)));
        g.registerSimpleItemModel(plate, off);
    }

    
    private void paneBlockWithItem(BlockModelGenerators g, IronBarsBlock pane) {
        g.createBarsAndItem(pane);
    }

    

    
    private void blockTextureModel(BlockModelGenerators g, Block block, ModelTemplate template) {
        TextureMapping mapping = TextureMapping.cube(blockTexture(block));
        template.create(block, mapping, g.modelOutput);
        g.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
                BlockModelGenerators.plainVariant(modelId(block))));
    }

    protected String name(Block block) {
        return Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block)).getPath();
    }

    
    protected Material blockTexture(Block block) {
        return new Material(Identifier.fromNamespaceAndPath(epca.MODID, "block/" + name(block)));
    }

    
    protected Material blockTexture(Block block, String suffix) {
        return new Material(Identifier.fromNamespaceAndPath(epca.MODID, "block/" + name(block) + suffix));
    }

    protected Identifier modelId(Block block) {
        return Identifier.fromNamespaceAndPath(epca.MODID, "block/" + name(block));
    }

    
    public static List<Block> generatedBlocks() {
        List<Block> result = new ArrayList<>();
        BuiltInRegistries.BLOCK.listElements()
                .filter(holder -> holder.getKey().identifier().getNamespace().equals(epca.MODID))
                .filter(holder -> isGenerated(holder.value()))
                .forEach(holder -> result.add(holder.value()));
        return result;
    }
}
