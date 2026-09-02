package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.fluid.ModFluids;
import org.tdddd.epca.impl.overworld.registry.blocks.block.*;
import org.tdddd.epca.impl.overworld.registry.items.item.*;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedCarvedPumpkin;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, epca.MODID);

    // ==================== 便捷注册静态方法 ====================

    /** 注册最基础的 Item（无特殊属性，堆叠 64） */
    private static RegistryObject<Item> simpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /** 注册自定义 Item 子类（使用默认属性，堆叠 64） */
    private static <T extends Item> RegistryObject<T> customItem(String name, Supplier<T> supplier) {
        return ITEMS.register(name, supplier);
    }

    /** 注册刷怪蛋（背景色、斑点色均为 -1） */
    private static RegistryObject<Item> spawnEgg(String name, Supplier<? extends EntityType<? extends Mob>> entitySupplier) {
        return ITEMS.register(name + "_spawn_egg",
                () -> new ForgeSpawnEggItem(entitySupplier, -1, -1, new Item.Properties()));
    }

    /** 注册普通方块物品（使用默认属性） */
    private static RegistryObject<Item> blockItem(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    /** 注册特殊 BlockItem 子类（如 InfestedLogItem） */
    private static <T extends BlockItem> RegistryObject<T> customBlockItem(String name, Supplier<T> supplier) {
        return ITEMS.register(name, supplier);
    }

    // ==================== 物品注册 ====================

    // 特殊物品（自定义 Item 子类，无特殊属性）
    public static final RegistryObject<Item> CLUSTER = customItem("cluster", () -> new Cluster(new Item.Properties()));
    public static final RegistryObject<Item> PARASITE_VISCERA = customItem("parasite_viscera", () -> new ParasiteViscera(new Item.Properties()));
    public static final RegistryObject<Item> FINS_FIN = customItem("fins_fin", () -> new FinsFin(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_BONE = customItem("infested_bone", () -> new InfestedBone(new Item.Properties()));
    public static final RegistryObject<Item> WEIRD_MINCED_FLESH = customItem("weird_minced_flesh", () -> new WeirdMincedFlesh(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_FLESH = customItem("infested_flesh", () -> new InfestedFlesh(new Item.Properties()));
    public static final RegistryObject<Item> RESHAPE_FLESH = customItem("reshape_flesh", () -> new ReshapeFlesh(new Item.Properties()));
    public static final RegistryObject<Item> RESHAPE_SHELL = customItem("reshape_shell", () -> new ReshapeShell(new Item.Properties()));
    public static final RegistryObject<Item> TWISTED_BONE = customItem("twisted_bone", () -> new TwistedBone(new Item.Properties()));
    public static final RegistryObject<Item> TIGHT_TENDONS = customItem("tight_tendons", () -> new TightTendons(new Item.Properties()));
    public static final RegistryObject<Item> GASBAG_DEBRIS = customItem("gasbag_debris", () -> new GasbagDebris(new Item.Properties()));
    public static final RegistryObject<Item> BECKON_MEMBRANE = customItem("beckon_membrane", () -> new BeckonMembrane(new Item.Properties()));
    public static final RegistryObject<Item> DISEASED_HEART = customItem("diseased_heart", () -> new DiseasedHeart(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_COAL = customItem("infested_coal", () -> new InfestedCoal(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_RAW_COPPER = customItem("infested_raw_copper", () -> new InfestedRawCopper(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_RAW_IRON = customItem("infested_raw_iron", () -> new InfestedRawIron(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_RAW_GOLD = customItem("infested_raw_gold", () -> new InfestedRawGold(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_LAPIS_LAZULI = customItem("infested_lapis_lazuli", () -> new InfestedLapisLazuli(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_EMERALD = customItem("infested_emerald", () -> new InfestedEmerald(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_REDSTONE = customItem("infested_redstone", () -> new InfestedRedstone(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_DIAMOND = customItem("infested_diamond", () -> new InfestedDiamond(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_RUBBISH = customItem("infested_rubbish", () -> new InfestedRubbish(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_STICK = customItem("infested_stick", () -> new InfestedStick(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_SLIME_BALL = customItem("infested_slime_ball", () -> new InfestedSlimeBall(new Item.Properties()));
    public static final RegistryObject<Item> EPCA_ICON = customItem("epca_icon", () -> new EPCAIcon(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_NETHERSEA_BRAND_MOR = customItem("infested_nethersea_brand_mor", () -> new InfestedNetherseaBrandMor(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_NETHERSEA_ICECREAM = customItem("infested_nethersea_icecream", () -> new InfestedNetherseaIcecream(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON)));
    public static final RegistryObject<Item> SMALL_ITEM_FRAME = customItem("small_item_frame", () -> new SmallItemFrame(new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_SWEET_BERRIES = customItem("infested_sweet_berries", () -> new InfestedSweetBerries(new Item.Properties()));

    // 特殊物品（有特定堆叠或稀有度）
    public static final RegistryObject<Item> BLOODY_CLOCK = ITEMS.register("erosion_clock",
            () -> new BloodyClock(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> KILL_STICK = ITEMS.register("endless_wand",
            () -> new KillStick(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> INFESTED_ENDER_PEARL = ITEMS.register("infested_ender_pearl",
            () -> new InfestedEnderPearl(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FEEDING_MODULE_I = ITEMS.register("feeding_module_i",
            () -> new AutomaticFeedingModuleI(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> INSTINCT_MODULE_I = ITEMS.register("instinct_module_i",
            () -> new InstinctModuleI(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FLESH_ARMOR_MODULE_I = ITEMS.register("flesh_armor_module_i",
            () -> new FleshArmorModuleI(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NETHERITE_MODULE_I = ITEMS.register("netherite_module_i",
            () -> new NetheriteModuleI(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FLIGHT_MODULE_I = ITEMS.register("flight_module_i",
            () -> new FlightModuleI(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LIVING_ARMOR_BOX = ITEMS.register("living_armor_box",
            () -> new LivingArmorBox(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EPCA_NOTE = ITEMS.register("epca_note",
            () -> new EPCANote(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BIOMASS_COUNT_ICON = ITEMS.register("biomass_count_icon",
            () -> new BiomassCountIcon(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ENDER_BLADE_SCRAP = ITEMS.register("ender_blade_scrap",
            () -> new EnderBladeScrap(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // 工具 / 武器（耐久、堆叠 1）
    public static final RegistryObject<Item> WOODEN_SPEAR = ITEMS.register("wooden_spear",
            () -> new WoodenSpear(new Item.Properties().stacksTo(1).durability(64)));
    public static final RegistryObject<Item> STONE_SPEAR = ITEMS.register("stone_spear",
            () -> new StoneSpear(new Item.Properties().stacksTo(1).durability(131)));
    public static final RegistryObject<Item> FLINT_SPEAR = ITEMS.register("flint_spear",
            () -> new FlintSpear(new Item.Properties().stacksTo(1).durability(145)));
    public static final RegistryObject<Item> COPPER_SPEAR = ITEMS.register("copper_spear",
            () -> new CopperSpear(new Item.Properties().stacksTo(1).durability(190)));
    public static final RegistryObject<Item> IRON_SPEAR = ITEMS.register("iron_spear",
            () -> new IronSpear(new Item.Properties().stacksTo(1).durability(250)));
    public static final RegistryObject<Item> GOLDEN_SPEAR = ITEMS.register("golden_spear",
            () -> new GoldenSpear(new Item.Properties().stacksTo(1).durability(99)));
    public static final RegistryObject<Item> DIAMOND_SPEAR = ITEMS.register("diamond_spear",
            () -> new DiamondSpear(new Item.Properties().stacksTo(1).durability(1561)));
    public static final RegistryObject<Item> NETHERITE_SPEAR = ITEMS.register("netherite_spear",
            () -> new NetheriteSpear(new Item.Properties().stacksTo(1).durability(2031)));

    // 盔甲
    public static final RegistryObject<LivingArmorItem> LIVING_HELMET = ITEMS.register("living_helmet",
            () -> new LivingArmorItem(new LivingArmorMaterial(), ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<LivingArmorItem> LIVING_CHESTPLATE = ITEMS.register("living_chestplate",
            () -> new LivingArmorItem(new LivingArmorMaterial(), ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<LivingArmorItem> LIVING_LEGGINGS = ITEMS.register("living_leggings",
            () -> new LivingArmorItem(new LivingArmorMaterial(), ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<LivingArmorItem> LIVING_BOOTS = ITEMS.register("living_boots",
            () -> new LivingArmorItem(new LivingArmorMaterial(), ArmorItem.Type.BOOTS, new Item.Properties()));

    // 流体桶
    public static final RegistryObject<Item> ACID_SOLUTION_BUCKET = ITEMS.register("acid_bucket",
            () -> new BucketItem(ModFluids.ACID_SOLUTION, new Item.Properties().stacksTo(1)));

    // ==================== 刷怪蛋 ====================
    public static final RegistryObject<Item> BUGLIN_SPAWN_EGG = spawnEgg("curbug", ModEntities.CURBUG);
    public static final RegistryObject<Item> YAWNING_NYA_SPAWN_EGG = spawnEgg("yawning_nya", ModEntities.YAWNING_NYA);
    public static final RegistryObject<Item> RUPTER_SPAWN_EGG = spawnEgg("ripper", ModEntities.RIPPER);
    public static final RegistryObject<Item> SMALL_INCOMPLETE_FORM_SPAWN_EGG = spawnEgg("small_incomplete_form", ModEntities.SMALL_INCOMPLETE_FORM);
    public static final RegistryObject<Item> MEDIUM_INCOMPLETE_FORM_SPAWN_EGG = spawnEgg("medium_incomplete_form", ModEntities.MEDIUM_INCOMPLETE_FORM);
    public static final RegistryObject<Item> INFESTED_ZOMBIE_SPAWN_EGG = spawnEgg("infested_zombie", ModEntities.INFESTED_ZOMBIE);
    public static final RegistryObject<Item> WALKING_ZOMBIE_HEAD_SPAWN_EGG = spawnEgg("walking_zombie_head", ModEntities.WALKING_ZOMBIE_HEAD);
    public static final RegistryObject<Item> INFESTED_HUSK_SPAWN_EGG = spawnEgg("infested_husk", ModEntities.INFESTED_HUSK);
    public static final RegistryObject<Item> WALKING_HUSK_HEAD_SPAWN_EGG = spawnEgg("walking_husk_head", ModEntities.WALKING_HUSK_HEAD);
    public static final RegistryObject<Item> INFESTED_DROWNED_SPAWN_EGG = spawnEgg("infested_drowned", ModEntities.INFESTED_DROWNED);
    public static final RegistryObject<Item> WALKING_DROWNED_HEAD_SPAWN_EGG = spawnEgg("walking_drowned_head", ModEntities.WALKING_DROWNED_HEAD);
    public static final RegistryObject<Item> BIOMASS_SMALL_SPAWN_EGG = spawnEgg("biomass_small", ModEntities.BIOMASS_SMALL);
    public static final RegistryObject<Item> INFESTED_PILLAGER_SPAWN_EGG = spawnEgg("infested_pillager", ModEntities.INFESTED_PILLAGER);
    public static final RegistryObject<Item> WALKING_PILLAGER_HEAD_SPAWN_EGG = spawnEgg("walking_pillager_head", ModEntities.WALKING_PILLAGER_HEAD);
    public static final RegistryObject<Item> INFESTED_VINDICATOR_SPAWN_EGG = spawnEgg("infested_vindicator", ModEntities.INFESTED_VINDICATOR);
    public static final RegistryObject<Item> WALKING_VINDICATOR_HEAD_SPAWN_EGG = spawnEgg("walking_vindicator_head", ModEntities.WALKING_VINDICATOR_HEAD);
    public static final RegistryObject<Item> INFESTED_VILLAGER_SPAWN_EGG = spawnEgg("infested_villager", ModEntities.INFESTED_VILLAGER);
    public static final RegistryObject<Item> WALKING_VILLAGER_HEAD_SPAWN_EGG = spawnEgg("walking_villager_head", ModEntities.WALKING_VILLAGER_HEAD);
    public static final RegistryObject<Item> INFESTED_ZOMBIE_VILLAGER_SPAWN_EGG = spawnEgg("infested_zombie_villager", ModEntities.INFESTED_ZOMBIE_VILLAGER);
    public static final RegistryObject<Item> WALKING_ZOMBIE_VILLAGER_HEAD_SPAWN_EGG = spawnEgg("walking_zombie_villager_head", ModEntities.WALKING_ZOMBIE_VILLAGER_HEAD);
    public static final RegistryObject<Item> FINS_SPAWN_EGG = spawnEgg("fins", ModEntities.FINS);
    public static final RegistryObject<Item> INFESTED_PIG_SPAWN_EGG = spawnEgg("infested_pig", ModEntities.INFESTED_PIG);
    public static final RegistryObject<Item> WALKING_PIG_HEAD_SPAWN_EGG = spawnEgg("walking_pig_head", ModEntities.WALKING_PIG_HEAD);
    public static final RegistryObject<Item> INFESTED_SHEEP_SPAWN_EGG = spawnEgg("infested_sheep", ModEntities.INFESTED_SHEEP);
    public static final RegistryObject<Item> WALKING_SHEEP_HEAD_SPAWN_EGG = spawnEgg("walking_sheep_head", ModEntities.WALKING_SHEEP_HEAD);
    public static final RegistryObject<Item> LARGE_INCOMPLETE_FORM_SPAWN_EGG = spawnEgg("large_incomplete_form", ModEntities.LARGE_INCOMPLETE_FORM);
    public static final RegistryObject<Item> INFESTED_COW_SPAWN_EGG = spawnEgg("infested_cow", ModEntities.INFESTED_COW);
    public static final RegistryObject<Item> WALKING_COW_HEAD_SPAWN_EGG = spawnEgg("walking_cow_head", ModEntities.WALKING_COW_HEAD);
    public static final RegistryObject<Item> NULLTHING_SPAWN_EGG = spawnEgg("nullthing", ModEntities.NULLTHING);
    public static final RegistryObject<Item> MOZZIE_SPAWN_EGG = spawnEgg("mozzie", ModEntities.MOZZIE);
    public static final RegistryObject<Item> INFESTED_WOLF_SPAWN_EGG = spawnEgg("infested_wolf", ModEntities.INFESTED_WOLF);
    public static final RegistryObject<Item> WALKING_WOLF_HEAD_SPAWN_EGG = spawnEgg("walking_wolf_head", ModEntities.WALKING_WOLF_HEAD);
    public static final RegistryObject<Item> RESHAPE_LONGARMS_SPAWN_EGG = spawnEgg("reshape_longarms", ModEntities.RESHAPE_LONGARMS);
    public static final RegistryObject<Item> BIOMASS_MEDIUM_SPAWN_EGG = spawnEgg("biomass_medium", ModEntities.BIOMASS_MEDIUM);
    public static final RegistryObject<Item> WALKING_CHICKEN_HEAD_SPAWN_EGG = spawnEgg("walking_chicken_head", ModEntities.WALKING_CHICKEN_HEAD);
    public static final RegistryObject<Item> INFESTED_CHICKEN_SPAWN_EGG = spawnEgg("infested_chicken", ModEntities.INFESTED_CHICKEN);
    public static final RegistryObject<Item> FLYING_CARRIER_SPAWN_EGG = spawnEgg("flying_carrier", ModEntities.FLYING_CARRIER);
    public static final RegistryObject<Item> INFESTED_ENDERMAN_SPAWN_EGG = spawnEgg("infested_enderman", ModEntities.INFESTED_ENDERMAN);
    public static final RegistryObject<Item> WALKING_ENDERMAN_HEAD_SPAWN_EGG = spawnEgg("walking_enderman_head", ModEntities.WALKING_ENDERMAN_HEAD);
    public static final RegistryObject<Item> INFESTED_ENDERMITE_SPAWN_EGG = spawnEgg("infested_endermite", ModEntities.INFESTED_ENDERMITE);
    public static final RegistryObject<Item> INFESTED_SILVERFISH_SPAWN_EGG = spawnEgg("infested_silverfish", ModEntities.INFESTED_SILVERFISH);
    public static final RegistryObject<Item> LIGHT_CARRIER_SPAWN_EGG = spawnEgg("light_carrier", ModEntities.LIGHT_CARRIER);
    public static final RegistryObject<Item> INFESTED_SKELETON_SPAWN_EGG = spawnEgg("infested_skeleton", ModEntities.INFESTED_SKELETON);
    public static final RegistryObject<Item> WALKING_SKELETON_HEAD_SPAWN_EGG = spawnEgg("walking_skeleton_head", ModEntities.WALKING_SKELETON_HEAD);
    public static final RegistryObject<Item> INFESTED_PLAYER_SPAWN_EGG = spawnEgg("infested_player", ModEntities.INFESTED_PLAYER);
    public static final RegistryObject<Item> WALKING_FOX_HEAD_SPAWN_EGG = spawnEgg("walking_fox_head", ModEntities.WALKING_FOX_HEAD);
    public static final RegistryObject<Item> RESHAPE_YELLOWEYE_SPAWN_EGG = spawnEgg("reshape_yelloweye", ModEntities.RESHAPE_YELLOWEYE);
    public static final RegistryObject<Item> INFESTED_FOX_SPAWN_EGG = spawnEgg("infested_fox", ModEntities.INFESTED_FOX);
    public static final RegistryObject<Item> INFESTED_SLIME_SPAWN_EGG = ITEMS.register("infested_slime_spawn_egg", () -> new InfestedSlimeSpawnEgg(new Item.Properties()));
    public static final RegistryObject<Item> STAGE_I_BECKON_SPAWN_EGG = customItem("stage_i_beckon_spawn_egg", () -> new StageIBeckonSpawnEgg(new Item.Properties()));
    public static final RegistryObject<Item> STAGE_II_BECKON_SPAWN_EGG = customItem("stage_ii_beckon_spawn_egg", () -> new StageIIBeckonSpawnEgg(new Item.Properties()));
    public static final RegistryObject<Item> LIVING_FLESH_SPAWN_EGG =
            ITEMS.register("living_flesh_spawn_egg",
                    () -> new ForgeSpawnEggItem(() -> ModEntities.LIVING_FLESH_SIZE0.get(), -1, -1, new Item.Properties()));

    // ==================== 方块物品 ====================

    public static final RegistryObject<Item> INFESTED_REMAINS_SMALL = blockItem("infested_remains_small", ModBlocks.INFESTED_REMAINS_SMALL);
    public static final RegistryObject<Item> INFESTED_REMAINS_MEDIUM = blockItem("infested_remains_medium", ModBlocks.INFESTED_REMAINS_MEDIUM);
    public static final RegistryObject<Item> INFESTED_REMAINS_LARGE = blockItem("infested_remains_large", ModBlocks.INFESTED_REMAINS_LARGE);
    public static final RegistryObject<Item> INFESTED_DIRT = blockItem("infested_dirt", ModBlocks.INFESTED_DIRT);
    public static final RegistryObject<Item> INFESTED_SAND = blockItem("infested_sand", ModBlocks.INFESTED_SAND);
    public static final RegistryObject<Item> INFESTED_GRASS = blockItem("infested_grass", ModBlocks.INFESTED_GRASS);
    public static final RegistryObject<Item> INFESTED_FERN = blockItem("infested_fern", ModBlocks.INFESTED_FERN);
    public static final RegistryObject<Item> INFESTED_SWEET_BERRY_BUSH = blockItem("infested_sweet_berry_bush", ModBlocks.INFESTED_SWEET_BERRY_BUSH);
    public static final RegistryObject<Item> INFESTED_RESIDUE = blockItem("infested_residue", ModBlocks.INFESTED_RESIDUE);
    public static final RegistryObject<Item> INFESTED_LEAVES = blockItem("infested_leaves", ModBlocks.INFESTED_LEAVES);
    public static final RegistryObject<Item> INFESTED_FLOWERING_LEAVES = blockItem("infested_flowering_leaves", ModBlocks.INFESTED_FLOWERING_LEAVES);
    public static final RegistryObject<Item> INFESTED_VINE = blockItem("infested_vine", ModBlocks.INFESTED_VINE);
    public static final RegistryObject<Item> INFESTED_STONE = blockItem("infested_stone", ModBlocks.INFESTED_STONE);
    public static final RegistryObject<Item> INFESTED_STONE_SLAB = blockItem("infested_stone_slab", ModBlocks.INFESTED_STONE_SLAB);
    public static final RegistryObject<Item> INFESTED_STONE_STAIRS = blockItem("infested_stone_stairs", ModBlocks.INFESTED_STONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_STONE_WALL = blockItem("infested_stone_wall", ModBlocks.INFESTED_STONE_WALL);
    public static final RegistryObject<Item> INFESTED_COBBLESTONE = blockItem("infested_cobblestone", ModBlocks.INFESTED_COBBLESTONE);
    public static final RegistryObject<Item> INFESTED_COBBLESTONE_SLAB = blockItem("infested_cobblestone_slab", ModBlocks.INFESTED_COBBLESTONE_SLAB);
    public static final RegistryObject<Item> INFESTED_COBBLESTONE_STAIRS = blockItem("infested_cobblestone_stairs", ModBlocks.INFESTED_COBBLESTONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_COBBLESTONE_WALL = blockItem("infested_cobblestone_wall", ModBlocks.INFESTED_COBBLESTONE_WALL);
    public static final RegistryObject<Item> INFESTED_STONE_BRICKS = blockItem("infested_stone_bricks", ModBlocks.INFESTED_STONE_BRICKS);
    public static final RegistryObject<Item> INFESTED_STONE_BRICKS_SLAB = blockItem("infested_stone_bricks_slab", ModBlocks.INFESTED_STONE_BRICKS_SLAB);
    public static final RegistryObject<Item> INFESTED_STONE_BRICKS_STAIRS = blockItem("infested_stone_bricks_stairs", ModBlocks.INFESTED_STONE_BRICKS_STAIRS);
    public static final RegistryObject<Item> INFESTED_STONE_BRICKS_WALL = blockItem("infested_stone_bricks_wall", ModBlocks.INFESTED_STONE_BRICKS_WALL);
    public static final RegistryObject<Item> INFESTED_CRACKED_STONE_BRICKS = blockItem("infested_cracked_stone_bricks", ModBlocks.INFESTED_CRACKED_STONE_BRICKS);
    public static final RegistryObject<Item> INFESTED_CHISELED_STONE_BRICKS = blockItem("infested_chiseled_stone_bricks", ModBlocks.INFESTED_CHISELED_STONE_BRICKS);
    public static final RegistryObject<Item> INFESTED_POLISHED_STONE = blockItem("infested_polished_stone", ModBlocks.INFESTED_POLISHED_STONE);
    public static final RegistryObject<Item> INFESTED_POLISHED_STONE_SLAB = blockItem("infested_polished_stone_slab", ModBlocks.INFESTED_POLISHED_STONE_SLAB);
    public static final RegistryObject<Item> INFESTED_POLISHED_STONE_STAIRS = blockItem("infested_polished_stone_stairs", ModBlocks.INFESTED_POLISHED_STONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_SANDSTONE = blockItem("infested_sandstone", ModBlocks.INFESTED_SANDSTONE);
    public static final RegistryObject<Item> INFESTED_SANDSTONE_SLAB = blockItem("infested_sandstone_slab", ModBlocks.INFESTED_SANDSTONE_SLAB);
    public static final RegistryObject<Item> INFESTED_SANDSTONE_STAIRS = blockItem("infested_sandstone_stairs", ModBlocks.INFESTED_SANDSTONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_SANDSTONE_WALL = blockItem("infested_sandstone_wall", ModBlocks.INFESTED_SANDSTONE_WALL);
    public static final RegistryObject<Item> INFESTED_CHISELED_RED_SANDSTONE = blockItem("infested_chiseled_red_sandstone", ModBlocks.INFESTED_CHISELED_RED_SANDSTONE);
    public static final RegistryObject<Item> INFESTED_CHISELED_SANDSTONE = blockItem("infested_chiseled_sandstone", ModBlocks.INFESTED_CHISELED_SANDSTONE);
    public static final RegistryObject<Item> INFESTED_SMOOTH_SANDSTONE = blockItem("infested_smooth_sandstone", ModBlocks.INFESTED_SMOOTH_SANDSTONE);
    public static final RegistryObject<Item> INFESTED_SMOOTH_SANDSTONE_SLAB = blockItem("infested_smooth_sandstone_slab", ModBlocks.INFESTED_SMOOTH_SANDSTONE_SLAB);
    public static final RegistryObject<Item> INFESTED_SMOOTH_SANDSTONE_STAIRS = blockItem("infested_smooth_sandstone_stairs", ModBlocks.INFESTED_SMOOTH_SANDSTONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_CUT_SANDSTONE = blockItem("infested_cut_sandstone", ModBlocks.INFESTED_CUT_SANDSTONE);
    public static final RegistryObject<Item> INFESTED_CUT_SANDSTONE_SLAB = blockItem("infested_cut_sandstone_slab", ModBlocks.INFESTED_CUT_SANDSTONE_SLAB);
    public static final RegistryObject<Item> INFESTED_COAL_ORE = blockItem("infested_coal_ore", ModBlocks.INFESTED_COAL_ORE);
    public static final RegistryObject<Item> INFESTED_COPPER_ORE = blockItem("infested_copper_ore", ModBlocks.INFESTED_COPPER_ORE);
    public static final RegistryObject<Item> INFESTED_IRON_ORE = blockItem("infested_iron_ore", ModBlocks.INFESTED_IRON_ORE);
    public static final RegistryObject<Item> INFESTED_GOLD_ORE = blockItem("infested_gold_ore", ModBlocks.INFESTED_GOLD_ORE);
    public static final RegistryObject<Item> INFESTED_LAPIS_ORE = blockItem("infested_lapis_ore", ModBlocks.INFESTED_LAPIS_ORE);
    public static final RegistryObject<Item> INFESTED_REDSTONE_ORE = blockItem("infested_redstone_ore", ModBlocks.INFESTED_REDSTONE_ORE);
    public static final RegistryObject<Item> INFESTED_EMERALD_ORE = blockItem("infested_emerald_ore", ModBlocks.INFESTED_EMERALD_ORE);
    public static final RegistryObject<Item> INFESTED_DIAMOND_ORE = blockItem("infested_diamond_ore", ModBlocks.INFESTED_DIAMOND_ORE);
    public static final RegistryObject<Item> INFESTED_SNOW = blockItem("infested_snow", ModBlocks.INFESTED_SNOW);
    public static final RegistryObject<Item> INFESTED_SNOW_BLOCK = blockItem("infested_snow_block", ModBlocks.INFESTED_SNOW_BLOCK);
    public static final RegistryObject<Item> INFESTED_INFESTED_COBBLESTONE = blockItem("infested_infested_cobblestone", ModBlocks.INFESTED_INFESTED_COBBLESTONE);
    public static final RegistryObject<Item> INFESTED_INFESTED_STONE = blockItem("infested_infested_stone", ModBlocks.INFESTED_INFESTED_STONE);
    public static final RegistryObject<Item> INFESTED_INFESTED_STONE_BRICKS = blockItem("infested_infested_stone_bricks", ModBlocks.INFESTED_INFESTED_STONE_BRICKS);
    public static final RegistryObject<Item> INFESTED_INFESTED_CRACKED_STONE_BRICKS = blockItem("infested_infested_cracked_stone_bricks", ModBlocks.INFESTED_INFESTED_CRACKED_STONE_BRICKS);
    public static final RegistryObject<Item> INFESTED_INFESTED_CHISELED_STONE_BRICKS = blockItem("infested_infested_chiseled_stone_bricks", ModBlocks.INFESTED_INFESTED_CHISELED_STONE_BRICKS);
    public static final RegistryObject<Item> INFESTED_NETHERSEA_BRAND_GROWN = blockItem("infested_nethersea_brand_grown", ModBlocks.INFESTED_NETHERSEA_BRAND_GROWN);
    public static final RegistryObject<Item> INFESTED_NETHERSEA_BRAND_SOLID = blockItem("infested_nethersea_brand_solid", ModBlocks.INFESTED_NETHERSEA_BRAND_SOLID);
    public static final RegistryObject<Item> SWALLOW_CYST = blockItem("swallow_cyst", ModBlocks.SWALLOW_CYST);
    public static final RegistryObject<Item> PACKED_MUD_PEDESTAL = blockItem("packed_mud_pedestal", ModBlocks.PACKED_MUD_PEDESTAL);
    public static final RegistryObject<Item> PACKED_MUD_ALTAR_STONE = blockItem("packed_mud_altar_stone", ModBlocks.PACKED_MUD_ALTAR_STONE);
    public static final RegistryObject<Item> INFESTED_POINTED_DRIPSTONE = blockItem("infested_pointed_dripstone", ModBlocks.INFESTED_POINTED_DRIPSTONE);
    public static final RegistryObject<Item> BECKON_CORE = blockItem("beckon_core", ModBlocks.BECKON_CORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_STONE = blockItem("infested_heavy_stone", ModBlocks.INFESTED_HEAVY_STONE);
    public static final RegistryObject<Item> INFESTED_INFESTED_HEAVY_STONE = blockItem("infested_infested_heavy_stone", ModBlocks.INFESTED_INFESTED_HEAVY_STONE);
    public static final RegistryObject<Item> INFESTED_HEAVY_COAL_ORE = blockItem("infested_heavy_coal_ore", ModBlocks.INFESTED_HEAVY_COAL_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_COPPER_ORE = blockItem("infested_heavy_copper_ore", ModBlocks.INFESTED_HEAVY_COPPER_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_IRON_ORE = blockItem("infested_heavy_iron_ore", ModBlocks.INFESTED_HEAVY_IRON_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_GOLD_ORE = blockItem("infested_heavy_gold_ore", ModBlocks.INFESTED_HEAVY_GOLD_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_LAPIS_ORE = blockItem("infested_heavy_lapis_ore", ModBlocks.INFESTED_HEAVY_LAPIS_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_REDSTONE_ORE = blockItem("infested_heavy_redstone_ore", ModBlocks.INFESTED_HEAVY_REDSTONE_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_EMERALD_ORE = blockItem("infested_heavy_emerald_ore", ModBlocks.INFESTED_HEAVY_EMERALD_ORE);
    public static final RegistryObject<Item> INFESTED_HEAVY_DIAMOND_ORE = blockItem("infested_heavy_diamond_ore", ModBlocks.INFESTED_HEAVY_DIAMOND_ORE);
    public static final RegistryObject<Item> INFESTED_DUSTLIKE = blockItem("infested_dustlike", ModBlocks.INFESTED_DUSTLIKE);
    public static final RegistryObject<Item> INFESTED_PLANKSLIKE = blockItem("infested_plankslike", ModBlocks.INFESTED_PLANKSLIKE);
    public static final RegistryObject<Item> INFESTED_ROCKLIKE = blockItem("infested_rocklike", ModBlocks.INFESTED_ROCKLIKE);
    public static final RegistryObject<Item> INFESTED_METALLIKE = blockItem("infested_metallike", ModBlocks.INFESTED_METALLIKE);
    public static final RegistryObject<Item> INFESTED_HARDLIKE = blockItem("infested_hardlike", ModBlocks.INFESTED_HARDLIKE);
    public static final RegistryObject<Item> INFESTED_HEAVY_COBBLESTONE = blockItem("infested_heavy_cobblestone", ModBlocks.INFESTED_HEAVY_COBBLESTONE);
    public static final RegistryObject<Item> INFESTED_HEAVY_COBBLESTONE_STAIRS = blockItem("infested_heavy_cobblestone_stairs", ModBlocks.INFESTED_HEAVY_COBBLESTONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_HEAVY_COBBLESTONE_SLAB = blockItem("infested_heavy_cobblestone_slab", ModBlocks.INFESTED_HEAVY_COBBLESTONE_SLAB);
    public static final RegistryObject<Item> INFESTED_HEAVY_COBBLESTONE_WALL = blockItem("infested_heavy_cobblestone_wall", ModBlocks.INFESTED_HEAVY_COBBLESTONE_WALL);
    public static final RegistryObject<Item> INFESTED_CHISELED_DEEPSLATE = blockItem("infested_chiseled_deepslate", ModBlocks.INFESTED_CHISELED_DEEPSLATE);
    public static final RegistryObject<Item> INFESTED_POLISHED_HEAVY_STONE = blockItem("infested_polished_heavy_stone", ModBlocks.INFESTED_POLISHED_HEAVY_STONE);
    public static final RegistryObject<Item> INFESTED_POLISHED_HEAVY_STONE_STAIRS = blockItem("infested_polished_heavy_stone_stairs", ModBlocks.INFESTED_POLISHED_HEAVY_STONE_STAIRS);
    public static final RegistryObject<Item> INFESTED_POLISHED_HEAVY_STONE_SLAB = blockItem("infested_polished_heavy_stone_slab", ModBlocks.INFESTED_POLISHED_HEAVY_STONE_SLAB);
    public static final RegistryObject<Item> INFESTED_POLISHED_HEAVY_STONE_WALL = blockItem("infested_polished_heavy_stone_wall", ModBlocks.INFESTED_POLISHED_HEAVY_STONE_WALL);
    public static final RegistryObject<Item> INFESTED_LILY_PAD = ITEMS.register("infested_lily_pad", () -> new BlockItem(ModBlocks.INFESTED_LILY_PAD.get(), new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_CRACKED_HEAVY_BRICKS = blockItem("infested_cracked_heavy_bricks", ModBlocks.INFESTED_CRACKED_HEAVY_BRICKS);
    public static final RegistryObject<Item> INFESTED_HEAVY_BRICKS = blockItem("infested_heavy_bricks", ModBlocks.INFESTED_HEAVY_BRICKS);
    public static final RegistryObject<Item> INFESTED_HEAVY_BRICKS_STAIRS = blockItem("infested_heavy_bricks_stairs", ModBlocks.INFESTED_HEAVY_BRICKS_STAIRS);
    public static final RegistryObject<Item> INFESTED_HEAVY_BRICKS_SLAB = blockItem("infested_heavy_bricks_slab", ModBlocks.INFESTED_HEAVY_BRICKS_SLAB);
    public static final RegistryObject<Item> INFESTED_HEAVY_BRICKS_WALL = blockItem("infested_heavy_bricks_wall", ModBlocks.INFESTED_HEAVY_BRICKS_WALL);
    public static final RegistryObject<Item> INFESTED_CARVED_PUMPKIN = ITEMS.register("infested_carved_pumpkin", () -> new InfestedCarvedPumpkin(ModBlocks.INFESTED_CARVED_PUMPKIN.get(), new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_PUMPKIN = blockItem("infested_pumpkin", ModBlocks.INFESTED_PUMPKIN);
    public static final RegistryObject<Item> INFESTED_TALL_GRASS = ITEMS.register("infested_tall_grass", () -> new BlockItem(ModBlocks.INFESTED_TALL_GRASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_TALL_FERN = ITEMS.register("infested_tall_fern", () -> new BlockItem(ModBlocks.INFESTED_TALL_FERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> INFESTED_SHORT_GRASS = blockItem("infested_short_grass", ModBlocks.INFESTED_SHORT_GRASS);
    public static final RegistryObject<Item> INFESTED_CACTUS = blockItem("infested_cactus", ModBlocks.INFESTED_CACTUS);
    public static final RegistryObject<Item> INFESTED_SUGAR_CANE = blockItem("infested_sugar_cane", ModBlocks.INFESTED_SUGAR_CANE);
    public static final RegistryObject<Item> INFESTED_SPIDER_WEB = blockItem("infested_spider_web", ModBlocks.INFESTED_SPIDER_WEB);

    // 特殊 BlockItem 子类（使用自定义的 Item 内部类）
    public static final RegistryObject<InfestedLog.InfestedLogItem> INFESTED_LOG =
            customBlockItem("infested_log", () -> new InfestedLog.InfestedLogItem(ModBlocks.INFESTED_LOG.get(), new Item.Properties()));
    public static final RegistryObject<InfestedWood.InfestedWoodItem> INFESTED_WOOD =
            customBlockItem("infested_wood", () -> new InfestedWood.InfestedWoodItem(ModBlocks.INFESTED_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<InfestedStrippedLog.InfestedStrippedLogItem> INFESTED_STRIPPED_LOG =
            customBlockItem("infested_stripped_log", () -> new InfestedStrippedLog.InfestedStrippedLogItem(ModBlocks.INFESTED_STRIPPED_LOG.get(), new Item.Properties()));
    public static final RegistryObject<InfestedStrippedWood.InfestedStrippedWoodItem> INFESTED_STRIPPED_WOOD =
            customBlockItem("infested_stripped_wood", () -> new InfestedStrippedWood.InfestedStrippedWoodItem(ModBlocks.INFESTED_STRIPPED_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<InfestedPlanks.InfestedPlanksItem> INFESTED_PLANKS =
            customBlockItem("infested_planks", () -> new InfestedPlanks.InfestedPlanksItem(ModBlocks.INFESTED_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<InfestedPlanksSlab.InfestedPlanksSlabItem> INFESTED_PLANKS_SLAB =
            customBlockItem("infested_planks_slab", () -> new InfestedPlanksSlab.InfestedPlanksSlabItem(ModBlocks.INFESTED_PLANKS_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<InfestedPlanksStairs.InfestedPlanksStairsItem> INFESTED_PLANKS_STAIRS =
            customBlockItem("infested_planks_stairs", () -> new InfestedPlanksStairs.InfestedPlanksStairsItem(ModBlocks.INFESTED_PLANKS_STAIRS.get(), new Item.Properties()));
    public static final RegistryObject<InfestedPlanksFence.InfestedPlanksItem> INFESTED_PLANKS_FENCE =
            customBlockItem("infested_planks_fence", () -> new InfestedPlanksFence.InfestedPlanksItem(ModBlocks.INFESTED_PLANKS_FENCE.get(), new Item.Properties()));
}