package org.tdddd.epca.impl.overworld.registry;

import java.util.function.Supplier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.fluid.AcidSolutionBlock;
import org.tdddd.epca.impl.fluid.ModFluids;
import org.tdddd.epca.impl.overworld.registry.blocks.block.*;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(epca.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    public static final DeferredBlock<Block> INFESTED_DIRT = BLOCKS.registerBlock(
            "infested_dirt",
            InfestedDirt::new,
            () -> Block.Properties.of().strength(0.7f, 0.7f).randomTicks().mapColor(DyeColor.BLACK)
    );

    public static final DeferredBlock<Block> INFESTED_SAND = BLOCKS.registerBlock(
            "infested_sand",
            InfestedSand::new,
            () -> Block.Properties.of().strength(0.7f, 0.7f).randomTicks().mapColor(DyeColor.WHITE)
    );

    public static final DeferredBlock<Block> INFESTED_LOG = BLOCKS.registerBlock(
            "infested_log",
            InfestedLog::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_WOOD = BLOCKS.registerBlock(
            "infested_wood",
            InfestedWood::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_STRIPPED_LOG = BLOCKS.registerBlock(
            "infested_stripped_log",
            InfestedStrippedLog::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_STRIPPED_WOOD = BLOCKS.registerBlock(
            "infested_stripped_wood",
            InfestedStrippedWood::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_PLANKS = BLOCKS.registerBlock(
            "infested_planks",
            InfestedPlanks::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_PLANKS_SLAB = BLOCKS.registerBlock(
            "infested_planks_slab",
            InfestedPlanksSlab::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).noOcclusion().randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_PLANKS_FENCE = BLOCKS.registerBlock(
            "infested_planks_fence",
            InfestedPlanksFence::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).noOcclusion().randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_PLANKS_STAIRS = BLOCKS.registerBlock(
            "infested_planks_stairs",
            InfestedPlanksStairs::new,
            () -> Block.Properties.of().strength(1.0f, 2.0f).noOcclusion().randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_STONE = BLOCKS.registerBlock(
            "infested_stone",
            InfestedStone::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_SLAB = BLOCKS.registerBlock(
            "infested_stone_slab",
            InfestedStoneSlab::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_STAIRS = BLOCKS.registerBlock(
            "infested_stone_stairs",
            InfestedStoneStairs::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_WALL = BLOCKS.registerBlock(
            "infested_stone_wall",
            InfestedStoneWall::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops().dynamicShape()
    );

    public static final DeferredBlock<Block> INFESTED_COBBLESTONE = BLOCKS.registerBlock(
            "infested_cobblestone",
            InfestedCobblestone::new,
            () -> Block.Properties.of().strength(2.0f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_COBBLESTONE_SLAB = BLOCKS.registerBlock(
            "infested_cobblestone_slab",
            InfestedCobblestoneSlab::new,
            () -> Block.Properties.of().strength(2.0f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_COBBLESTONE_STAIRS = BLOCKS.registerBlock(
            "infested_cobblestone_stairs",
            InfestedCobblestoneStairs::new,
            () -> Block.Properties.of().strength(2.0f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_COBBLESTONE_WALL = BLOCKS.registerBlock(
            "infested_cobblestone_wall",
            InfestedCobblestoneWall::new,
            () -> Block.Properties.of().strength(2.0f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops().dynamicShape()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_BRICKS = BLOCKS.registerBlock(
            "infested_stone_bricks",
            InfestedStoneBricks::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_BRICKS_SLAB = BLOCKS.registerBlock(
            "infested_stone_bricks_slab",
            InfestedStoneBricksSlab::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_BRICKS_STAIRS = BLOCKS.registerBlock(
            "infested_stone_bricks_stairs",
            InfestedStoneBricksStairs::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_STONE_BRICKS_WALL = BLOCKS.registerBlock(
            "infested_stone_bricks_wall",
            InfestedStoneBricksWall::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops().dynamicShape()
    );

    public static final DeferredBlock<Block> INFESTED_CRACKED_STONE_BRICKS = BLOCKS.registerBlock(
            "infested_cracked_stone_bricks",
            InfestedCrackedStoneBricks::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_CHISELED_STONE_BRICKS = BLOCKS.registerBlock(
            "infested_chiseled_stone_bricks",
            InfestedChiseledStoneBricks::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_STONE = BLOCKS.registerBlock(
            "infested_polished_stone",
            InfestedPolishedStone::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_STONE_SLAB = BLOCKS.registerBlock(
            "infested_polished_stone_slab",
            InfestedPolishedStoneSlab::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_STONE_STAIRS = BLOCKS.registerBlock(
            "infested_polished_stone_stairs",
            InfestedPolishedStoneStairs::new,
            () -> Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SANDSTONE = BLOCKS.registerBlock(
            "infested_sandstone",
            InfestedSandstone::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SANDSTONE_SLAB = BLOCKS.registerBlock(
            "infested_sandstone_slab",
            InfestedSandstoneSlab::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SANDSTONE_STAIRS = BLOCKS.registerBlock(
            "infested_sandstone_stairs",
            InfestedSandstoneStairs::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SANDSTONE_WALL = BLOCKS.registerBlock(
            "infested_sandstone_wall",
            InfestedSandstoneWall::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops().dynamicShape()
    );

    public static final DeferredBlock<Block> INFESTED_CHISELED_RED_SANDSTONE = BLOCKS.registerBlock(
            "infested_chiseled_red_sandstone",
            InfestedChiseledRedSandstone::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_CHISELED_SANDSTONE = BLOCKS.registerBlock(
            "infested_chiseled_sandstone",
            InfestedChiseledSandstone::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SMOOTH_SANDSTONE = BLOCKS.registerBlock(
            "infested_smooth_sandstone",
            InfestedSmoothSandstone::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SMOOTH_SANDSTONE_SLAB = BLOCKS.registerBlock(
            "infested_smooth_sandstone_slab",
            InfestedSmoothSandstoneSlab::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SMOOTH_SANDSTONE_STAIRS = BLOCKS.registerBlock(
            "infested_smooth_sandstone_stairs",
            InfestedSmoothSandstoneStairs::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_CUT_SANDSTONE = BLOCKS.registerBlock(
            "infested_cut_sandstone",
            InfestedCutSandstone::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_CUT_SANDSTONE_SLAB = BLOCKS.registerBlock(
            "infested_cut_sandstone_slab",
            InfestedCutSandstoneSlab::new,
            () -> Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_COAL_ORE = BLOCKS.registerBlock(
            "infested_coal_ore",
            InfestedCoalOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.BLACK).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_COPPER_ORE = BLOCKS.registerBlock(
            "infested_copper_ore",
            InfestedCopperOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.ORANGE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_IRON_ORE = BLOCKS.registerBlock(
            "infested_iron_ore",
            InfestedIronOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_GOLD_ORE = BLOCKS.registerBlock(
            "infested_gold_ore",
            InfestedGoldOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.YELLOW).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_LAPIS_ORE = BLOCKS.registerBlock(
            "infested_lapis_ore",
            InfestedLapisOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.BLUE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_REDSTONE_ORE = BLOCKS.registerBlock(
            "infested_redstone_ore",
            InfestedRedstoneOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.RED).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_EMERALD_ORE = BLOCKS.registerBlock(
            "infested_emerald_ore",
            InfestedEmeraldOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.GREEN).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_DIAMOND_ORE = BLOCKS.registerBlock(
            "infested_diamond_ore",
            InfestedDiamondOre::new,
            () -> Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.CYAN).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SNOW = BLOCKS.registerBlock(
            "infested_snow",
            InfestedSnow::new,
            () -> Block.Properties.of().strength(0.1f, 0.1f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_SNOW_BLOCK = BLOCKS.registerBlock(
            "infested_snow_block",
            InfestedSnowBlock::new,
            () -> Block.Properties.of().strength(0.1f, 0.1f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_GRASS = BLOCKS.registerBlock(
            "infested_grass",
            InfestedGrass::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_FERN = BLOCKS.registerBlock(
            "infested_fern",
            InfestedFern::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.FERN).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_SWEET_BERRY_BUSH = BLOCKS.registerBlock(
            "infested_sweet_berry_bush",
            InfestedSweetBerryBush::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).ignitedByLava()
    );

    // 26.1.2: these six blocks must be registered through BLOCKS.registerBlock(name, Function<Properties,B>),
    // which is the only overload that calls Properties#setId(...). Registering them through the inherited
    // register(String, Supplier) overload compiles fine and then aborts the whole block registry event at
    // runtime with "NullPointerException: Block id not set" (observed for mod eej), cascading into
    // "Trying to access unbound value: ResourceKey[.../block/...]" for every later registry.
    // The Properties bodies reproduce what each class's own no-arg constructor used to build.
    public static final DeferredBlock<Block> INFESTED_REMAINS_SMALL = BLOCKS.registerBlock(
            "infested_remains_small",
            InfestedRemainsSmall::new,
            () -> BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.0f)
                    .sound(SoundType.NETHER_WART)
                    .isViewBlocking((state, world, pos) -> false)
                    .isSuffocating((state, world, pos) -> false)
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final DeferredBlock<Block> INFESTED_REMAINS_MEDIUM = BLOCKS.registerBlock(
            "infested_remains_medium",
            InfestedRemainsMedium::new,
            () -> BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.0f)
                    .sound(SoundType.NETHER_WART)
                    .isViewBlocking((state, world, pos) -> false)
                    .isSuffocating((state, world, pos) -> false)
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final DeferredBlock<Block> INFESTED_REMAINS_LARGE = BLOCKS.registerBlock(
            "infested_remains_large",
            InfestedRemainsLarge::new,
            () -> BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.0f)
                    .sound(SoundType.NETHER_WART)
                    .isViewBlocking((state, world, pos) -> false)
                    .isSuffocating((state, world, pos) -> false)
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final DeferredBlock<Block> INFESTED_RESIDUE = BLOCKS.registerBlock(
            "infested_residue",
            InfestedResidue::new,
            () -> BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.0f)
                    .sound(SoundType.NETHER_WART)
                    .isViewBlocking((state, world, pos) -> false)
                    .isSuffocating((state, world, pos) -> false)
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
                    .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_VINE = BLOCKS.registerBlock(
            "infested_vine",
            InfestedVine::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .randomTicks()
                    .strength(0.2F)
                    .sound(SoundType.VINE)
                    .noOcclusion()
                    .ignitedByLava()
    );

    public static final DeferredBlock<Block> SWALLOW_CYST = BLOCKS.registerBlock(
            "swallow_cyst",
            SwallowCyst::new,
            () -> BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.9f, 0.9f)
                    .sound(SoundType.SLIME_BLOCK)
                    .isViewBlocking((state, world, pos) -> false)
                    .isSuffocating((state, world, pos) -> false)
                    .pushReaction(PushReaction.DESTROY)
                    .mapColor(DyeColor.RED)
                    .randomTicks()
    );

    public static final DeferredBlock<Block> INFESTED_LEAVES = BLOCKS.registerBlock(
            "infested_leaves",
            InfestedLeaves::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entity) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .mapColor(DyeColor.PINK)
                    .ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_FLOWERING_LEAVES = BLOCKS.registerBlock(
            "infested_flowering_leaves",
            InfestedFloweringLeaves::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entity) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .mapColor(DyeColor.PINK)
                    .ignitedByLava()
    );

    public static final DeferredBlock<LiquidBlock> ACID_SOLUTION_BLOCK = BLOCKS.registerBlock(
            "acid_solution",
            properties -> new AcidSolutionBlock(() -> ModFluids.ACID_SOLUTION.get(), properties),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_GREEN)
                    .lightLevel(state -> 0)
    );

    public static final DeferredBlock<Block> INFESTED_INFESTED_COBBLESTONE = BLOCKS.registerBlock(
            "infested_infested_cobblestone",
            InfestedInfestedCobblestone::new,
            () -> Block.Properties.of().strength(1.0f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_INFESTED_STONE = BLOCKS.registerBlock(
            "infested_infested_stone",
            InfestedInfestedStone::new,
            () -> Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_INFESTED_STONE_BRICKS = BLOCKS.registerBlock(
            "infested_infested_stone_bricks",
            InfestedInfestedStoneBricks::new,
            () -> Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_INFESTED_CRACKED_STONE_BRICKS = BLOCKS.registerBlock(
            "infested_infested_cracked_stone_bricks",
            InfestedInfestedCrackedStoneBricks::new,
            () -> Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_INFESTED_CHISELED_STONE_BRICKS = BLOCKS.registerBlock(
            "infested_infested_chiseled_stone_bricks",
            InfestedInfestedChiseledStoneBricks::new,
            () -> Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_NETHERSEA_BRAND_GROWN = BLOCKS.registerBlock(
            "infested_nethersea_brand_grown",
            InfestedNetherseaBrandGrown::new,
            () -> Block.Properties.of().strength(0.75f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).pushReaction(PushReaction.DESTROY).noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_NETHERSEA_BRAND_SOLID = BLOCKS.registerBlock(
            "infested_nethersea_brand_solid",
            InfestedNetherseaBrandSolid::new,
            () -> Block.Properties.of().strength(2.0f, 5.0f).randomTicks().mapColor(DyeColor.PURPLE)
    );

    public static final DeferredBlock<Block> INFESTED_POINTED_DRIPSTONE = BLOCKS.registerBlock(
            "infested_pointed_dripstone",
            InfestedPointedDripstone::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POINTED_DRIPSTONE).mapColor(DyeColor.LIGHT_GRAY)
    );

    // 祭坛方块（packed_mud_pedestal / packed_mud_altar_stone）已分离到前置模组 eej。

    public static final DeferredBlock<Block> BECKON_CORE = BLOCKS.registerBlock(
            "beckon_core",
            BeckonCore::new,
            () -> BlockBehaviour.Properties.of().strength(4.0F, 6.0F).sound(SoundType.MUDDY_MANGROVE_ROOTS).mapColor(DyeColor.GREEN)
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_STONE = BLOCKS.registerBlock(
            "infested_heavy_stone",
            InfestedHeavyStone::new,
            () -> Block.Properties.of().strength(3.0f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_INFESTED_HEAVY_STONE = BLOCKS.registerBlock(
            "infested_infested_heavy_stone",
            InfestedInfestedHeavyStone::new,
            () -> Block.Properties.of().strength(1.5f, 0.75f).randomTicks().mapColor(DyeColor.GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_COAL_ORE = BLOCKS.registerBlock(
            "infested_heavy_coal_ore",
            InfestedHeavyCoalOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.BLACK).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_COPPER_ORE = BLOCKS.registerBlock(
            "infested_heavy_copper_ore",
            InfestedHeavyCopperOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.ORANGE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_IRON_ORE = BLOCKS.registerBlock(
            "infested_heavy_iron_ore",
            InfestedHeavyIronOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_GOLD_ORE = BLOCKS.registerBlock(
            "infested_heavy_gold_ore",
            InfestedHeavyGoldOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.YELLOW).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_LAPIS_ORE = BLOCKS.registerBlock(
            "infested_heavy_lapis_ore",
            InfestedHeavyLapisOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.BLUE).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_REDSTONE_ORE = BLOCKS.registerBlock(
            "infested_heavy_redstone_ore",
            InfestedHeavyRedstoneOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.RED).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_EMERALD_ORE = BLOCKS.registerBlock(
            "infested_heavy_emerald_ore",
            InfestedHeavyEmeraldOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.GREEN).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_DIAMOND_ORE = BLOCKS.registerBlock(
            "infested_heavy_diamond_ore",
            InfestedHeavyDiamondOre::new,
            () -> Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.CYAN).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_DUSTLIKE = BLOCKS.registerBlock(
            "infested_dustlike",
            InfestedDustlike::new,
            () -> Block.Properties.of().strength(1.0f, 3.0f).randomTicks().mapColor(DyeColor.CYAN)
    );

    public static final DeferredBlock<Block> INFESTED_PLANKSLIKE = BLOCKS.registerBlock(
            "infested_plankslike",
            InfestedPlankslike::new,
            () -> Block.Properties.of().strength(2.0f, 4.0f).randomTicks().mapColor(DyeColor.GREEN)
    );

    public static final DeferredBlock<Block> INFESTED_ROCKLIKE = BLOCKS.registerBlock(
            "infested_rocklike",
            InfestedRocklike::new,
            () -> Block.Properties.of().strength(3.0f, 6.0f).randomTicks().mapColor(DyeColor.BLUE)
    );

    public static final DeferredBlock<Block> INFESTED_METALLIKE = BLOCKS.registerBlock(
            "infested_metallike",
            InfestedMetallike::new,
            () -> Block.Properties.of().strength(10.0f, 12.0f).randomTicks().mapColor(DyeColor.PURPLE)
    );

    public static final DeferredBlock<Block> INFESTED_HARDLIKE = BLOCKS.registerBlock(
            "infested_hardlike",
            InfestedHardlike::new,
            () -> Block.Properties.of().strength(60.0f, 1000.0f).randomTicks().mapColor(DyeColor.PURPLE)
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_COBBLESTONE = BLOCKS.registerBlock(
            "infested_heavy_cobblestone",
            InfestedHeavyCobblestone::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_COBBLESTONE_STAIRS = BLOCKS.registerBlock(
            "infested_heavy_cobblestone_stairs",
            InfestedHeavyCobblestoneStairs::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_COBBLESTONE_SLAB = BLOCKS.registerBlock(
            "infested_heavy_cobblestone_slab",
            InfestedHeavyCobblestoneSlab::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_COBBLESTONE_WALL = BLOCKS.registerBlock(
            "infested_heavy_cobblestone_wall",
            InfestedHeavyCobblestoneWall::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_CHISELED_DEEPSLATE = BLOCKS.registerBlock(
            "infested_chiseled_deepslate",
            InfestedChiseledDeepslate::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_HEAVY_STONE = BLOCKS.registerBlock(
            "infested_polished_heavy_stone",
            InfestedPolishedHeavyStone::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_HEAVY_STONE_STAIRS = BLOCKS.registerBlock(
            "infested_polished_heavy_stone_stairs",
            InfestedPolishedHeavyStoneStairs::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_HEAVY_STONE_SLAB = BLOCKS.registerBlock(
            "infested_polished_heavy_stone_slab",
            InfestedPolishedHeavyStoneSlab::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_POLISHED_HEAVY_STONE_WALL = BLOCKS.registerBlock(
            "infested_polished_heavy_stone_wall",
            InfestedPolishedHeavyStoneWall::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_LILY_PAD = BLOCKS.registerBlock(
            "infested_lily_pad",
            InfestedLilyPad::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).instabreak().pushReaction(PushReaction.DESTROY).randomTicks().noOcclusion().mapColor(DyeColor.LIGHT_GRAY)
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_BRICKS = BLOCKS.registerBlock(
            "infested_heavy_bricks",
            InfestedHeavyBricks::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_CRACKED_HEAVY_BRICKS = BLOCKS.registerBlock(
            "infested_cracked_heavy_bricks",
            InfestedCrackedHeavyBricks::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_BRICKS_STAIRS = BLOCKS.registerBlock(
            "infested_heavy_bricks_stairs",
            InfestedHeavyBricksStairs::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_BRICKS_SLAB = BLOCKS.registerBlock(
            "infested_heavy_bricks_slab",
            InfestedHeavyBricksSlab::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_BRICKS_WALL = BLOCKS.registerBlock(
            "infested_heavy_bricks_wall",
            InfestedHeavyBricksWall::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_CARVED_PUMPKIN = BLOCKS.registerBlock(
            "infested_carved_pumpkin",
            InfestedCarvedPumpkin::new,
            () -> Block.Properties.of().strength(1.0f, 1.0f).randomTicks().mapColor(DyeColor.BLUE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_PUMPKIN = BLOCKS.registerBlock(
            "infested_pumpkin",
            InfestedPumpkin::new,
            () -> Block.Properties.of().strength(1.0f, 1.0f).randomTicks().mapColor(DyeColor.BLUE).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_MUDDY_MANGROVE_ROOTS = BLOCKS.registerBlock(
            "infested_muddy_mangrove_roots",
            InfestedMuddyMangroveRoots::new,
            () -> Block.Properties.of().strength(0.7f, 0.7f).randomTicks().mapColor(DyeColor.PURPLE)
    );

    public static final DeferredBlock<Block> INFESTED_TALL_GRASS = BLOCKS.registerBlock(
            "infested_tall_grass",
            InfestedDoublePlantBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_FERN).sound(Blocks.SHORT_GRASS.defaultBlockState().getSoundType())
    );

    public static final DeferredBlock<Block> INFESTED_TALL_FERN = BLOCKS.registerBlock(
            "infested_tall_fern",
            InfestedDoublePlantBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_FERN).sound(Blocks.SHORT_GRASS.defaultBlockState().getSoundType())
    );

    public static final DeferredBlock<Block> INFESTED_SHORT_GRASS = BLOCKS.registerBlock(
            "infested_short_grass",
            InfestedShortGrass::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_CACTUS = BLOCKS.registerBlock(
            "infested_cactus",
            InfestedCactus::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CACTUS).strength(0.4f, 0.4f).randomTicks().mapColor(DyeColor.PURPLE).noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_SUGAR_CANE = BLOCKS.registerBlock(
            "infested_sugar_cane",
            InfestedSugarCane::new,
            () -> Block.Properties.of().instabreak().randomTicks().mapColor(DyeColor.PURPLE).noOcclusion().ignitedByLava()
    );

    public static final DeferredBlock<InfestedSpiderWeb> INFESTED_SPIDER_WEB = BLOCKS.registerBlock(
            "infested_spider_web",
            InfestedSpiderWeb::new,
            () -> BlockBehaviour.Properties.of().strength(4.0F, 0.0F).randomTicks().mapColor(DyeColor.GREEN).noOcclusion().noCollision().isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).ignitedByLava()
    );

    public static final DeferredBlock<InfestedSpiderWebBlood> INFESTED_SPIDER_WEB_BLOOD = BLOCKS.registerBlock(
            "infested_spider_web_blood",
            InfestedSpiderWebBlood::new,
            () -> BlockBehaviour.Properties.of().strength(4.0F, 0.0F).randomTicks().mapColor(DyeColor.RED).noOcclusion().noCollision().isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).ignitedByLava()
    );

    public static final DeferredBlock<InfestedCaveSpiderWeb> INFESTED_CAVE_SPIDER_WEB = BLOCKS.registerBlock(
            "infested_cave_spider_web",
            InfestedCaveSpiderWeb::new,
            () -> BlockBehaviour.Properties.of().strength(4.0F, 0.0F).randomTicks().mapColor(DyeColor.CYAN).noOcclusion().noCollision().isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).ignitedByLava()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_TILES = BLOCKS.registerBlock(
            "infested_heavy_tiles",
            InfestedHeavyTiles::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_CRACKED_HEAVY_TILES = BLOCKS.registerBlock(
            "infested_cracked_heavy_tiles",
            InfestedCrackedHeavyTiles::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_TILES_STAIRS = BLOCKS.registerBlock(
            "infested_heavy_tiles_stairs",
            InfestedHeavyTilesStairs::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_TILES_SLAB = BLOCKS.registerBlock(
            "infested_heavy_tiles_slab",
            InfestedHeavyTilesSlab::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_HEAVY_TILES_WALL = BLOCKS.registerBlock(
            "infested_heavy_tiles_wall",
            InfestedHeavyTilesWall::new,
            () -> Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_MANGROVE_ROOTS = BLOCKS.registerBlock(
            "infested_mangrove_roots",
            InfestedMangroveRoots::new,
            () -> Block.Properties.of().strength(0.7f, 0.7f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava().noOcclusion()
    );

    public static final DeferredBlock<Block> INFESTED_DEAD_BUSH = BLOCKS.registerBlock(
            "infested_dead_bush",
            InfestedDeadBush::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH).ignitedByLava()
    );
}