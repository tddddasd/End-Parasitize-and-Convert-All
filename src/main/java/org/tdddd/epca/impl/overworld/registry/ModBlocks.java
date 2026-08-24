package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.fluid.AcidSolutionBlock;
import org.tdddd.epca.impl.fluid.ModFluids;
import org.tdddd.epca.impl.overworld.registry.blocks.block.*;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, epca.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    public static final RegistryObject<Block> INFESTED_DIRT = BLOCKS.register(
            "infested_dirt",
            () -> new InfestedDirt(Block.Properties.of().strength(0.7f, 0.7f).randomTicks().mapColor(DyeColor.BLACK))
    );

    public static final RegistryObject<Block> INFESTED_SAND = BLOCKS.register(
            "infested_sand",
            () -> new InfestedSand(Block.Properties.of().strength(0.7f, 0.7f).randomTicks().mapColor(DyeColor.WHITE))
    );

    public static final RegistryObject<Block> INFESTED_LOG = BLOCKS.register(
            "infested_log",
            () -> new InfestedLog(Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_WOOD = BLOCKS.register(
            "infested_wood",
            () -> new InfestedWood(Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_STRIPPED_LOG = BLOCKS.register(
            "infested_stripped_log",
            () -> new InfestedStrippedLog(Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_STRIPPED_WOOD = BLOCKS.register(
            "infested_stripped_wood",
            () -> new InfestedStrippedWood(Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_PLANKS = BLOCKS.register(
            "infested_planks",
            () -> new InfestedPlanks(Block.Properties.of().strength(1.0f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_PLANKS_SLAB = BLOCKS.register(
            "infested_planks_slab",
            () -> new InfestedPlanksSlab(Block.Properties.of().strength(1.0f, 2.0f).noOcclusion().randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_PLANKS_FENCE = BLOCKS.register(
            "infested_planks_fence",
            () -> new InfestedPlanksFence(Block.Properties.of().strength(1.0f, 2.0f).noOcclusion().randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_PLANKS_STAIRS = BLOCKS.register(
            "infested_planks_stairs",
            () -> new InfestedPlanksStairs(Block.Properties.of().strength(1.0f, 2.0f).noOcclusion().randomTicks().mapColor(DyeColor.PURPLE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_STONE = BLOCKS.register(
            "infested_stone",
            () -> new InfestedStone(Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_STONE_SLAB = BLOCKS.register(
            "infested_stone_slab",
            () -> new InfestedStoneSlab(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_STONE_STAIRS = BLOCKS.register(
            "infested_stone_stairs",
            () -> new InfestedStoneStairs(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_STONE_WALL = BLOCKS.register(
            "infested_stone_wall",
            () -> new InfestedStoneWall(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops().dynamicShape())
    );

    public static final RegistryObject<Block> INFESTED_COBBLESTONE = BLOCKS.register(
            "infested_cobblestone",
            () -> new InfestedCobblestone(Block.Properties.of().strength(2.0f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_COBBLESTONE_SLAB = BLOCKS.register(
            "infested_cobblestone_slab",
            () -> new InfestedCobblestoneSlab(Block.Properties.of().strength(2.0f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_COBBLESTONE_STAIRS = BLOCKS.register(
            "infested_cobblestone_stairs",
            () -> new InfestedCobblestoneStairs(Block.Properties.of().strength(2.0f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_COBBLESTONE_WALL = BLOCKS.register(
            "infested_cobblestone_wall",
            () -> new InfestedCobblestoneWall(Block.Properties.of().strength(2.0f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops().dynamicShape())
    );

    public static final RegistryObject<Block> INFESTED_STONE_BRICKS = BLOCKS.register(
            "infested_stone_bricks",
            () -> new InfestedStoneBricks(Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_STONE_BRICKS_SLAB = BLOCKS.register(
            "infested_stone_bricks_slab",
            () -> new InfestedStoneBricksSlab(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_STONE_BRICKS_STAIRS = BLOCKS.register(
            "infested_stone_bricks_stairs",
            () -> new InfestedStoneBricksStairs(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_STONE_BRICKS_WALL = BLOCKS.register(
            "infested_stone_bricks_wall",
            () -> new InfestedStoneBricksWall(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops().dynamicShape())
    );

    public static final RegistryObject<Block> INFESTED_CRACKED_STONE_BRICKS = BLOCKS.register(
            "infested_cracked_stone_bricks",
            () -> new InfestedCrackedStoneBricks(Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_CHISELED_STONE_BRICKS = BLOCKS.register(
            "infested_chiseled_stone_bricks",
            () -> new InfestedChiseledStoneBricks(Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_STONE = BLOCKS.register(
            "infested_polished_stone",
            () -> new InfestedPolishedStone(Block.Properties.of().strength(1.5f, 6.0f).randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_STONE_SLAB = BLOCKS.register(
            "infested_polished_stone_slab",
            () -> new InfestedPolishedStoneSlab(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_STONE_STAIRS = BLOCKS.register(
            "infested_polished_stone_stairs",
            () -> new InfestedPolishedStoneStairs(Block.Properties.of().strength(1.5f, 6.0f).noOcclusion().randomTicks().mapColor(DyeColor.LIGHT_GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SANDSTONE = BLOCKS.register(
            "infested_sandstone",
            () -> new InfestedSandstone(Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SANDSTONE_SLAB = BLOCKS.register(
            "infested_sandstone_slab",
            () -> new InfestedSandstoneSlab(Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SANDSTONE_STAIRS = BLOCKS.register(
            "infested_sandstone_stairs",
            () -> new InfestedSandstoneStairs(Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SANDSTONE_WALL = BLOCKS.register(
            "infested_sandstone_wall",
            () -> new InfestedSandstoneWall(Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops().dynamicShape())
    );

    public static final RegistryObject<Block> INFESTED_CHISELED_RED_SANDSTONE = BLOCKS.register(
            "infested_chiseled_red_sandstone",
            () -> new InfestedChiseledRedSandstone(Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_CHISELED_SANDSTONE = BLOCKS.register(
            "infested_chiseled_sandstone",
            () -> new InfestedChiseledSandstone(Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SMOOTH_SANDSTONE = BLOCKS.register(
            "infested_smooth_sandstone",
            () -> new InfestedSmoothSandstone(Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SMOOTH_SANDSTONE_SLAB = BLOCKS.register(
            "infested_smooth_sandstone_slab",
            () -> new InfestedSmoothSandstoneSlab(Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SMOOTH_SANDSTONE_STAIRS = BLOCKS.register(
            "infested_smooth_sandstone_stairs",
            () -> new InfestedSmoothSandstoneStairs(Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_CUT_SANDSTONE = BLOCKS.register(
            "infested_cut_sandstone",
            () -> new InfestedCutSandstone(Block.Properties.of().strength(0.8f, 0.8f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_CUT_SANDSTONE_SLAB = BLOCKS.register(
            "infested_cut_sandstone_slab",
            () -> new InfestedCutSandstoneSlab(Block.Properties.of().strength(0.8f, 0.8f).noOcclusion().randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_COAL_ORE = BLOCKS.register(
            "infested_coal_ore",
            () -> new InfestedCoalOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.BLACK).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_COPPER_ORE = BLOCKS.register(
            "infested_copper_ore",
            () -> new InfestedCopperOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.ORANGE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_IRON_ORE = BLOCKS.register(
            "infested_iron_ore",
            () -> new InfestedIronOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_GOLD_ORE = BLOCKS.register(
            "infested_gold_ore",
            () -> new InfestedGoldOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.YELLOW).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_LAPIS_ORE = BLOCKS.register(
            "infested_lapis_ore",
            () -> new InfestedLapisOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.BLUE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_REDSTONE_ORE = BLOCKS.register(
            "infested_redstone_ore",
            () -> new InfestedRedstoneOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.RED).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_EMERALD_ORE = BLOCKS.register(
            "infested_emerald_ore",
            () -> new InfestedEmeraldOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.GREEN).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_DIAMOND_ORE = BLOCKS.register(
            "infested_diamond_ore",
            () -> new InfestedDiamondOre(Block.Properties.of().strength(3.0f, 3.0f).randomTicks().mapColor(DyeColor.CYAN).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SNOW = BLOCKS.register(
            "infested_snow",
            () -> new InfestedSnow(Block.Properties.of().strength(0.1f, 0.1f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_SNOW_BLOCK = BLOCKS.register(
            "infested_snow_block",
            () -> new InfestedSnowBlock(Block.Properties.of().strength(0.1f, 0.1f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_GRASS = BLOCKS.register(
            "infested_grass",
            () -> new InfestedGrass(Block.Properties.copy(Blocks.GRASS).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_FERN = BLOCKS.register(
            "infested_fern",
            () -> new InfestedFern(Block.Properties.copy(Blocks.FERN).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_SWEET_BERRY_BUSH = BLOCKS.register(
            "infested_sweet_berry_bush",
            () -> new InfestedSweetBerryBush(Block.Properties.copy(Blocks.SWEET_BERRY_BUSH).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_REMAINS_SMALL = BLOCKS.register(
            "infested_remains_small",
            InfestedRemainsSmall::new
    );

    public static final RegistryObject<Block> INFESTED_REMAINS_MEDIUM = BLOCKS.register(
            "infested_remains_medium",
            InfestedRemainsMedium::new
    );

    public static final RegistryObject<Block> INFESTED_REMAINS_LARGE = BLOCKS.register(
            "infested_remains_large",
            InfestedRemainsLarge::new
    );

    public static final RegistryObject<Block> INFESTED_RESIDUE = BLOCKS.register(
            "infested_residue",
            InfestedResidue::new
    );

    public static final RegistryObject<Block> INFESTED_VINE = BLOCKS.register(
            "infested_vine",
            InfestedVine::new);

    public static final RegistryObject<Block> SWALLOW_CYST = BLOCKS.register(
            "swallow_cyst",
            SwallowCyst::new
    );

    public static final RegistryObject<Block> INFESTED_LEAVES = BLOCKS.register("infested_leaves",
            () -> new InfestedLeaves(Block.Properties.copy(Blocks.SPRUCE_LEAVES)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entity) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .mapColor(DyeColor.PINK)
                    .ignitedByLava()));

    public static final RegistryObject<Block> INFESTED_FLOWERING_LEAVES = BLOCKS.register("infested_flowering_leaves",
            () -> new InfestedFloweringLeaves(Block.Properties.copy(Blocks.SPRUCE_LEAVES)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entity) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .mapColor(DyeColor.PINK)
                    .ignitedByLava()));

    public static final RegistryObject<LiquidBlock> ACID_SOLUTION_BLOCK = BLOCKS.register(
            "acid_solution",
            () -> new AcidSolutionBlock(
                    ModFluids.ACID_SOLUTION,
                    BlockBehaviour.Properties.copy(Blocks.LAVA)
                            .mapColor(net.minecraft.world.level.material.MapColor.COLOR_GREEN)
                            .lightLevel(state -> 0)
            )
    );

    public static final RegistryObject<Block> INFESTED_INFESTED_COBBLESTONE = BLOCKS.register(
            "infested_infested_cobblestone",
            () -> new InfestedInfestedCobblestone(Block.Properties.of().strength(1.0f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY))
    );

    public static final RegistryObject<Block> INFESTED_INFESTED_STONE = BLOCKS.register(
            "infested_infested_stone",
            () -> new InfestedInfestedStone(Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY))
    );

    public static final RegistryObject<Block> INFESTED_INFESTED_STONE_BRICKS = BLOCKS.register(
            "infested_infested_stone_bricks",
            () -> new InfestedInfestedStoneBricks(Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY))
    );

    public static final RegistryObject<Block> INFESTED_INFESTED_CRACKED_STONE_BRICKS = BLOCKS.register(
            "infested_infested_cracked_stone_bricks",
            () -> new InfestedInfestedCrackedStoneBricks(Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY))
    );

    public static final RegistryObject<Block> INFESTED_INFESTED_CHISELED_STONE_BRICKS = BLOCKS.register(
            "infested_infested_chiseled_stone_bricks",
            () -> new InfestedInfestedChiseledStoneBricks(Block.Properties.of().strength(0.75f, 0.75f).randomTicks().mapColor(DyeColor.LIGHT_GRAY))
    );

    public static final RegistryObject<Block> INFESTED_NETHERSEA_BRAND_GROWN = BLOCKS.register(
            "infested_nethersea_brand_grown",
            () -> new InfestedNetherseaBrandGrown(Block.Properties.of().strength(0.75f, 2.0f).randomTicks().mapColor(DyeColor.PURPLE).pushReaction(PushReaction.DESTROY).noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_NETHERSEA_BRAND_SOLID = BLOCKS.register(
            "infested_nethersea_brand_solid",
            () -> new InfestedNetherseaBrandSolid(Block.Properties.of().strength(2.0f, 5.0f).randomTicks().mapColor(DyeColor.PURPLE))
    );

    public static final RegistryObject<Block> INFESTED_POINTED_DRIPSTONE = BLOCKS.register(
            "infested_pointed_dripstone",
            () -> new InfestedPointedDripstone(BlockBehaviour.Properties.copy(Blocks.POINTED_DRIPSTONE).mapColor(DyeColor.LIGHT_GRAY))
    );

    public static final RegistryObject<Block> PACKED_MUD_PEDESTAL = BLOCKS.register(
            "packed_mud_pedestal",
            PackedMudPedestal::new
    );

    public static final RegistryObject<Block> PACKED_MUD_ALTAR_STONE = BLOCKS.register(
            "packed_mud_altar_stone",
            () -> new PackedMudAltarStone(Block.Properties.of().strength(1.0f, 1.0f).randomTicks().sound(SoundType.PACKED_MUD).mapColor(DyeColor.ORANGE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> BECKON_CORE = BLOCKS.register(
            "beckon_core",
            () -> new BeckonCore(BlockBehaviour.Properties.of().strength(4.0F, 6.0F).sound(SoundType.MUDDY_MANGROVE_ROOTS).mapColor(DyeColor.GREEN)));

    public static final RegistryObject<Block> INFESTED_HEAVY_STONE = BLOCKS.register(
            "infested_heavy_stone",
            () -> new InfestedHeavyStone(Block.Properties.of().strength(3.0f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_INFESTED_HEAVY_STONE = BLOCKS.register(
            "infested_infested_heavy_stone",
            () -> new InfestedInfestedHeavyStone(Block.Properties.of().strength(1.5f, 0.75f).randomTicks().mapColor(DyeColor.GRAY))
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_COAL_ORE = BLOCKS.register(
            "infested_heavy_coal_ore",
            () -> new InfestedHeavyCoalOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.BLACK).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_COPPER_ORE = BLOCKS.register(
            "infested_heavy_copper_ore",
            () -> new InfestedHeavyCopperOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.ORANGE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_IRON_ORE = BLOCKS.register(
            "infested_heavy_iron_ore",
            () -> new InfestedHeavyIronOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.WHITE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_GOLD_ORE = BLOCKS.register(
            "infested_heavy_gold_ore",
            () -> new InfestedHeavyGoldOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.YELLOW).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_LAPIS_ORE = BLOCKS.register(
            "infested_heavy_lapis_ore",
            () -> new InfestedHeavyLapisOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.BLUE).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_REDSTONE_ORE = BLOCKS.register(
            "infested_heavy_redstone_ore",
            () -> new InfestedHeavyRedstoneOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.RED).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_EMERALD_ORE = BLOCKS.register(
            "infested_heavy_emerald_ore",
            () -> new InfestedHeavyEmeraldOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.GREEN).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_DIAMOND_ORE = BLOCKS.register(
            "infested_heavy_diamond_ore",
            () -> new InfestedHeavyDiamondOre(Block.Properties.of().strength(4.5f, 3.0f).randomTicks().mapColor(DyeColor.CYAN).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_DUSTLIKE = BLOCKS.register(
            "infested_dustlike",
            () -> new InfestedDustlike(Block.Properties.of().strength(1.0f, 3.0f).randomTicks().mapColor(DyeColor.CYAN))
    );

    public static final RegistryObject<Block> INFESTED_PLANKSLIKE = BLOCKS.register(
            "infested_plankslike",
            () -> new InfestedPlankslike(Block.Properties.of().strength(2.0f, 4.0f).randomTicks().mapColor(DyeColor.GREEN))
    );

    public static final RegistryObject<Block> INFESTED_ROCKLIKE = BLOCKS.register(
            "infested_rocklike",
            () -> new InfestedRocklike(Block.Properties.of().strength(3.0f, 6.0f).randomTicks().mapColor(DyeColor.BLUE))
    );

    public static final RegistryObject<Block> INFESTED_METALLIKE = BLOCKS.register(
            "infested_metallike",
            () -> new InfestedMetallike(Block.Properties.of().strength(10.0f, 12.0f).randomTicks().mapColor(DyeColor.PURPLE))
    );

    public static final RegistryObject<Block> INFESTED_HARDLIKE = BLOCKS.register(
            "infested_hardlike",
            () -> new InfestedHardlike(Block.Properties.of().strength(60.0f, 1000.0f).randomTicks().mapColor(DyeColor.PURPLE))
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_COBBLESTONE = BLOCKS.register(
            "infested_heavy_cobblestone",
            () -> new InfestedHeavyCobblestone(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_COBBLESTONE_STAIRS = BLOCKS.register(
            "infested_heavy_cobblestone_stairs",
            () -> new InfestedHeavyCobblestoneStairs(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_COBBLESTONE_SLAB = BLOCKS.register(
            "infested_heavy_cobblestone_slab",
            () -> new InfestedHeavyCobblestoneSlab(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_COBBLESTONE_WALL = BLOCKS.register(
            "infested_heavy_cobblestone_wall",
            () -> new InfestedHeavyCobblestoneWall(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_CHISELED_DEEPSLATE = BLOCKS.register(
            "infested_chiseled_deepslate",
            () -> new InfestedChiseledDeepslate(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_HEAVY_STONE = BLOCKS.register(
            "infested_polished_heavy_stone",
            () -> new InfestedPolishedHeavyStone(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_HEAVY_STONE_STAIRS = BLOCKS.register(
            "infested_polished_heavy_stone_stairs",
            () -> new InfestedPolishedHeavyStoneStairs(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_HEAVY_STONE_SLAB = BLOCKS.register(
            "infested_polished_heavy_stone_slab",
            () -> new InfestedPolishedHeavyStoneSlab(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_POLISHED_HEAVY_STONE_WALL = BLOCKS.register(
            "infested_polished_heavy_stone_wall",
            () -> new InfestedPolishedHeavyStoneWall(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_LILY_PAD = BLOCKS.register("infested_lily_pad",
            () -> new InfestedLilyPad(BlockBehaviour.Properties.copy(Blocks.LILY_PAD).instabreak().pushReaction(PushReaction.DESTROY).randomTicks().noOcclusion().mapColor(DyeColor.LIGHT_GRAY)));

    public static final RegistryObject<Block> INFESTED_HEAVY_BRICKS = BLOCKS.register(
            "infested_heavy_bricks",
            () -> new InfestedHeavyBricks(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_CRACKED_HEAVY_BRICKS = BLOCKS.register(
            "infested_cracked_heavy_bricks",
            () -> new InfestedCrackedHeavyBricks(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_BRICKS_STAIRS = BLOCKS.register(
            "infested_heavy_bricks_stairs",
            () -> new InfestedPolishedHeavyStoneStairs(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_BRICKS_SLAB = BLOCKS.register(
            "infested_heavy_bricks_slab",
            () -> new InfestedPolishedHeavyStoneSlab(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_HEAVY_BRICKS_WALL = BLOCKS.register(
            "infested_heavy_bricks_wall",
            () -> new InfestedPolishedHeavyStoneWall(Block.Properties.of().strength(3.5f, 6.0f).randomTicks().mapColor(DyeColor.GRAY).requiresCorrectToolForDrops().noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_CARVED_PUMPKIN = BLOCKS.register(
            "infested_carved_pumpkin",
            () -> new InfestedCarvedPumpkin(Block.Properties.of().strength(1.0f, 1.0f).randomTicks().mapColor(DyeColor.BLUE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_PUMPKIN = BLOCKS.register(
            "infested_pumpkin",
            () -> new InfestedPumpkin(Block.Properties.of().strength(1.0f, 1.0f).randomTicks().mapColor(DyeColor.BLUE).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_TALL_GRASS = BLOCKS.register("infested_tall_grass", () -> new InfestedDoublePlantBlock(BlockBehaviour.Properties.copy(Blocks.LARGE_FERN).sound(Blocks.GRASS.defaultBlockState().getSoundType())));

    public static final RegistryObject<Block> INFESTED_TALL_FERN = BLOCKS.register("infested_tall_fern", () -> new InfestedDoublePlantBlock(BlockBehaviour.Properties.copy(Blocks.LARGE_FERN).sound(Blocks.GRASS.defaultBlockState().getSoundType())));

    public static final RegistryObject<Block> INFESTED_SHORT_GRASS = BLOCKS.register(
            "infested_short_grass",
            () -> new InfestedShortGrass(Block.Properties.copy(Blocks.GRASS).ignitedByLava())
    );

    public static final RegistryObject<Block> INFESTED_CACTUS = BLOCKS.register(
            "infested_cactus",
            () -> new InfestedCactus(Block.Properties.copy(Blocks.CACTUS).strength(0.4f, 0.4f).randomTicks().mapColor(DyeColor.PURPLE).noOcclusion())
    );

    public static final RegistryObject<Block> INFESTED_SUGAR_CANE = BLOCKS.register(
            "infested_sugar_cane",
            () -> new InfestedSugarCane(Block.Properties.of().instabreak().randomTicks().mapColor(DyeColor.PURPLE).noOcclusion().ignitedByLava())
    );
}