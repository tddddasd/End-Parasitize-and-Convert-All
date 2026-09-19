package org.tdddd.epca.impl.overworld.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.fluid.ModFluids;
import org.tdddd.epca.impl.overworld.registry.blocks.block.*;
import org.tdddd.epca.impl.overworld.registry.items.item.*;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedCarvedPumpkin;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorMaterial;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * 26.1.2 起注册表项必须带上自己的 id（{@code Item.Properties#setId}），
 * 否则注册表事件会以 {@code NullPointerException: Item id not set} 中止。
 * 因此这里用 {@link DeferredRegister.Items} 的 {@code registerItem}（它会 setId），
 * 而不是继承自 {@code DeferredRegister<Item>} 的 {@code register}。
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(epca.MODID);

    // ==================== 便捷注册静态方法 ====================

    /** 注册最基础的 Item（无特殊属性，堆叠 64） */
    private static DeferredItem<Item> simpleItem(String name) {
        return ITEMS.registerItem(name, Item::new);
    }

    /** 注册自定义 Item 子类（使用默认属性，堆叠 64） */
    private static <T extends Item> DeferredItem<T> customItem(String name, Function<Item.Properties, T> factory) {
        return ITEMS.registerItem(name, factory);
    }

    /** 注册自定义 Item 子类（自定义属性） */
    private static <T extends Item> DeferredItem<T> customItem(String name, Function<Item.Properties, T> factory,
                                                              UnaryOperator<Item.Properties> properties) {
        return ITEMS.registerItem(name, factory, properties);
    }

    /** 注册刷怪蛋（实体类型通过 {@code Item.Properties#spawnEgg} 携带，背景色/斑点色不再有参数） */
    private static DeferredItem<Item> spawnEgg(String name, Supplier<? extends EntityType<? extends Mob>> entitySupplier) {
        return ITEMS.registerItem(name + "_spawn_egg",
                properties -> new SpawnEggItem(properties.spawnEgg(entitySupplier.get())));
    }

    /** 注册普通方块物品（使用默认属性） */
    private static DeferredItem<Item> blockItem(String name, DeferredHolder<Block, ? extends Block> block) {
        return ITEMS.registerItem(name, properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    /** 注册特殊 BlockItem 子类（如 InfestedLogItem） */
    private static <T extends BlockItem> DeferredItem<T> customBlockItem(String name, Function<Item.Properties, T> factory) {
        return ITEMS.registerItem(name, factory, () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    // ==================== 物品注册 ====================

    // 特殊物品（自定义 Item 子类，无特殊属性）
    public static final DeferredItem<Item> CLUSTER = customItem("cluster", Cluster::new);
    public static final DeferredItem<Item> PARASITE_VISCERA = customItem("parasite_viscera", ParasiteViscera::new);
    public static final DeferredItem<Item> FINS_FIN = customItem("fins_fin", FinsFin::new);
    public static final DeferredItem<Item> INFESTED_BONE = customItem("infested_bone", InfestedBone::new);
    public static final DeferredItem<Item> WEIRD_MINCED_FLESH = customItem("weird_minced_flesh", WeirdMincedFlesh::new);
    public static final DeferredItem<Item> INFESTED_FLESH = customItem("infested_flesh", InfestedFlesh::new);
    public static final DeferredItem<Item> RESHAPE_FLESH = customItem("reshape_flesh", ReshapeFlesh::new);
    public static final DeferredItem<Item> RESHAPE_SHELL = customItem("reshape_shell", ReshapeShell::new);
    public static final DeferredItem<Item> TWISTED_BONE = customItem("twisted_bone", TwistedBone::new);
    public static final DeferredItem<Item> TIGHT_TENDONS = customItem("tight_tendons", TightTendons::new);
    public static final DeferredItem<Item> GASBAG_DEBRIS = customItem("gasbag_debris", GasbagDebris::new);
    public static final DeferredItem<Item> BECKON_MEMBRANE = customItem("beckon_membrane", BeckonMembrane::new);
    public static final DeferredItem<Item> DISEASED_HEART = customItem("diseased_heart", DiseasedHeart::new);
    public static final DeferredItem<Item> INFESTED_COAL = customItem("infested_coal", InfestedCoal::new);
    public static final DeferredItem<Item> INFESTED_RAW_COPPER = customItem("infested_raw_copper", InfestedRawCopper::new);
    public static final DeferredItem<Item> INFESTED_RAW_IRON = customItem("infested_raw_iron", InfestedRawIron::new);
    public static final DeferredItem<Item> INFESTED_RAW_GOLD = customItem("infested_raw_gold", InfestedRawGold::new);
    public static final DeferredItem<Item> INFESTED_LAPIS_LAZULI = customItem("infested_lapis_lazuli", InfestedLapisLazuli::new);
    public static final DeferredItem<Item> INFESTED_EMERALD = customItem("infested_emerald", InfestedEmerald::new);
    public static final DeferredItem<Item> INFESTED_REDSTONE = customItem("infested_redstone", InfestedRedstone::new);
    public static final DeferredItem<Item> INFESTED_DIAMOND = customItem("infested_diamond", InfestedDiamond::new);
    public static final DeferredItem<Item> INFESTED_RUBBISH = customItem("infested_rubbish", InfestedRubbish::new);
    public static final DeferredItem<Item> INFESTED_STICK = customItem("infested_stick", InfestedStick::new);
    public static final DeferredItem<Item> INFESTED_SLIME_BALL = customItem("infested_slime_ball", InfestedSlimeBall::new);
    public static final DeferredItem<Item> EPCA_ICON = customItem("epca_icon", EPCAIcon::new);
    public static final DeferredItem<Item> INFESTED_NETHERSEA_BRAND_MOR = customItem("infested_nethersea_brand_mor", InfestedNetherseaBrandMor::new);
    public static final DeferredItem<Item> INFESTED_NETHERSEA_ICECREAM =
            customItem("infested_nethersea_icecream", InfestedNetherseaIcecream::new,
                    properties -> properties.stacksTo(16).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> INFESTED_SWEET_BERRIES = customItem("infested_sweet_berries", InfestedSweetBerries::new);

    // 26.1.2: 铜粒改由原版提供（minecraft:copper_nugget），本模组不再注册自己的 copper_nugget。

    // 特殊物品（有特定堆叠或稀有度）
    public static final DeferredItem<Item> BLOODY_CLOCK = ITEMS.registerItem("erosion_clock",
            properties -> new BloodyClock(properties.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> KILL_STICK = ITEMS.registerItem("endless_wand",
            properties -> new KillStick(properties.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFESTED_ENDER_PEARL = ITEMS.registerItem("infested_ender_pearl",
            properties -> new InfestedEnderPearl(properties.stacksTo(16)));
    public static final DeferredItem<Item> FEEDING_MODULE_I = ITEMS.registerItem("feeding_module_i",
            properties -> new AutomaticFeedingModuleI(properties.stacksTo(1)));
    public static final DeferredItem<Item> AFTERIMAGE_MODULE = ITEMS.registerItem("afterimage_module",
            properties -> new AfterimageModule(properties.stacksTo(1)));
    public static final DeferredItem<Item> FLESH_ARMOR_MODULE_I = ITEMS.registerItem("flesh_armor_module_i",
            properties -> new FleshArmorModuleI(properties.stacksTo(1)));
    public static final DeferredItem<Item> NETHERITE_MODULE_I = ITEMS.registerItem("netherite_module_i",
            properties -> new NetheriteModuleI(properties.stacksTo(1)));
    public static final DeferredItem<Item> FLIGHT_MODULE_I = ITEMS.registerItem("flight_module_i",
            properties -> new FlightModuleI(properties.stacksTo(1)));
    public static final DeferredItem<Item> LIVING_ARMOR_BOX = ITEMS.registerItem("living_armor_box",
            properties -> new LivingArmorBox(properties.stacksTo(1)));
    public static final DeferredItem<Item> EPCA_NOTE = ITEMS.registerItem("epca_note",
            properties -> new EPCANote(properties.stacksTo(1)));
    public static final DeferredItem<Item> BIOMASS_COUNT_ICON = ITEMS.registerItem("biomass_count_icon",
            properties -> new BiomassCountIcon(properties.stacksTo(1)));
    public static final DeferredItem<Item> ENDER_BLADE_SCRAP = ITEMS.registerItem("ender_blade_scrap",
            properties -> new EnderBladeScrap(properties.rarity(Rarity.UNCOMMON)));

    // 工具 / 武器（耐久、堆叠 1）
    public static final DeferredItem<Item> WOODEN_SPEAR = ITEMS.registerItem("wooden_spear",
            properties -> new WoodenSpear(properties.stacksTo(1).durability(64)));
    public static final DeferredItem<Item> STONE_SPEAR = ITEMS.registerItem("stone_spear",
            properties -> new StoneSpear(properties.stacksTo(1).durability(131)));
    public static final DeferredItem<Item> FLINT_SPEAR = ITEMS.registerItem("flint_spear",
            properties -> new FlintSpear(properties.stacksTo(1).durability(145)));
    public static final DeferredItem<Item> COPPER_SPEAR = ITEMS.registerItem("copper_spear",
            properties -> new CopperSpear(properties.stacksTo(1).durability(190)));
    public static final DeferredItem<Item> IRON_SPEAR = ITEMS.registerItem("iron_spear",
            properties -> new IronSpear(properties.stacksTo(1).durability(250)));
    public static final DeferredItem<Item> GOLDEN_SPEAR = ITEMS.registerItem("golden_spear",
            properties -> new GoldenSpear(properties.stacksTo(1).durability(99)));
    public static final DeferredItem<Item> DIAMOND_SPEAR = ITEMS.registerItem("diamond_spear",
            properties -> new DiamondSpear(properties.stacksTo(1).durability(1561)));
    public static final DeferredItem<Item> NETHERITE_SPEAR = ITEMS.registerItem("netherite_spear",
            properties -> new NetheriteSpear(properties.stacksTo(1).durability(2031)));

    // 盔甲（26.1.2: ArmorItem 已删除，盔甲属性由 Item.Properties#humanoidArmor 提供）
    public static final DeferredItem<LivingArmorItem> LIVING_HELMET = ITEMS.registerItem("living_helmet",
            properties -> new LivingArmorItem(LivingArmorMaterial.MATERIAL, ArmorType.HELMET,
                    properties.humanoidArmor(LivingArmorMaterial.MATERIAL, ArmorType.HELMET)));
    public static final DeferredItem<LivingArmorItem> LIVING_CHESTPLATE = ITEMS.registerItem("living_chestplate",
            properties -> new LivingArmorItem(LivingArmorMaterial.MATERIAL, ArmorType.CHESTPLATE,
                    properties.humanoidArmor(LivingArmorMaterial.MATERIAL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<LivingArmorItem> LIVING_LEGGINGS = ITEMS.registerItem("living_leggings",
            properties -> new LivingArmorItem(LivingArmorMaterial.MATERIAL, ArmorType.LEGGINGS,
                    properties.humanoidArmor(LivingArmorMaterial.MATERIAL, ArmorType.LEGGINGS)));
    public static final DeferredItem<LivingArmorItem> LIVING_BOOTS = ITEMS.registerItem("living_boots",
            properties -> new LivingArmorItem(LivingArmorMaterial.MATERIAL, ArmorType.BOOTS,
                    properties.humanoidArmor(LivingArmorMaterial.MATERIAL, ArmorType.BOOTS)));

    // 流体桶
    public static final DeferredItem<Item> ACID_SOLUTION_BUCKET = ITEMS.registerItem("acid_bucket",
            properties -> new BucketItem(ModFluids.ACID_SOLUTION.get(), properties.stacksTo(1)));
    public static final DeferredItem<Item> INFESTED_SPIDER_WEB_PROJECTILE =
            ITEMS.registerItem("infested_spider_web_projectile", Item::new);
    public static final DeferredItem<Item> INFESTED_SPIDER_WEB_BLOOD_PROJECTILE =
            ITEMS.registerItem("infested_spider_web_blood_projectile", Item::new);
    public static final DeferredItem<Item> INFESTED_CAVE_SPIDER_WEB_PROJECTILE =
            ITEMS.registerItem("infested_cave_spider_web_projectile", Item::new);
    // ==================== 刷怪蛋 ====================
    public static final DeferredItem<Item> BUGLIN_SPAWN_EGG = spawnEgg("curbug", ModEntities.CURBUG);
    public static final DeferredItem<Item> YAWNING_NYA_SPAWN_EGG = spawnEgg("yawning_nya", ModEntities.YAWNING_NYA);
    public static final DeferredItem<Item> RUPTER_SPAWN_EGG = spawnEgg("ripper", ModEntities.RIPPER);
    public static final DeferredItem<Item> SMALL_INCOMPLETE_FORM_SPAWN_EGG = spawnEgg("small_incomplete_form", ModEntities.SMALL_INCOMPLETE_FORM);
    public static final DeferredItem<Item> MEDIUM_INCOMPLETE_FORM_SPAWN_EGG = spawnEgg("medium_incomplete_form", ModEntities.MEDIUM_INCOMPLETE_FORM);
    public static final DeferredItem<Item> INFESTED_ZOMBIE_SPAWN_EGG = spawnEgg("infested_zombie", ModEntities.INFESTED_ZOMBIE);
    public static final DeferredItem<Item> WALKING_ZOMBIE_HEAD_SPAWN_EGG = spawnEgg("walking_zombie_head", ModEntities.WALKING_ZOMBIE_HEAD);
    public static final DeferredItem<Item> INFESTED_HUSK_SPAWN_EGG = spawnEgg("infested_husk", ModEntities.INFESTED_HUSK);
    public static final DeferredItem<Item> WALKING_HUSK_HEAD_SPAWN_EGG = spawnEgg("walking_husk_head", ModEntities.WALKING_HUSK_HEAD);
    public static final DeferredItem<Item> INFESTED_DROWNED_SPAWN_EGG = spawnEgg("infested_drowned", ModEntities.INFESTED_DROWNED);
    public static final DeferredItem<Item> WALKING_DROWNED_HEAD_SPAWN_EGG = spawnEgg("walking_drowned_head", ModEntities.WALKING_DROWNED_HEAD);
    public static final DeferredItem<Item> BIOMASS_SMALL_SPAWN_EGG = spawnEgg("biomass_small", ModEntities.BIOMASS_SMALL);
    public static final DeferredItem<Item> INFESTED_PILLAGER_SPAWN_EGG = spawnEgg("infested_pillager", ModEntities.INFESTED_PILLAGER);
    public static final DeferredItem<Item> WALKING_PILLAGER_HEAD_SPAWN_EGG = spawnEgg("walking_pillager_head", ModEntities.WALKING_PILLAGER_HEAD);
    public static final DeferredItem<Item> INFESTED_VINDICATOR_SPAWN_EGG = spawnEgg("infested_vindicator", ModEntities.INFESTED_VINDICATOR);
    public static final DeferredItem<Item> WALKING_VINDICATOR_HEAD_SPAWN_EGG = spawnEgg("walking_vindicator_head", ModEntities.WALKING_VINDICATOR_HEAD);
    public static final DeferredItem<Item> INFESTED_VILLAGER_SPAWN_EGG = spawnEgg("infested_villager", ModEntities.INFESTED_VILLAGER);
    public static final DeferredItem<Item> WALKING_VILLAGER_HEAD_SPAWN_EGG = spawnEgg("walking_villager_head", ModEntities.WALKING_VILLAGER_HEAD);
    public static final DeferredItem<Item> INFESTED_ZOMBIE_VILLAGER_SPAWN_EGG = spawnEgg("infested_zombie_villager", ModEntities.INFESTED_ZOMBIE_VILLAGER);
    public static final DeferredItem<Item> WALKING_ZOMBIE_VILLAGER_HEAD_SPAWN_EGG = spawnEgg("walking_zombie_villager_head", ModEntities.WALKING_ZOMBIE_VILLAGER_HEAD);
    public static final DeferredItem<Item> FINS_SPAWN_EGG = spawnEgg("fins", ModEntities.FINS);
    public static final DeferredItem<Item> INFESTED_PIG_SPAWN_EGG = spawnEgg("infested_pig", ModEntities.INFESTED_PIG);
    public static final DeferredItem<Item> WALKING_PIG_HEAD_SPAWN_EGG = spawnEgg("walking_pig_head", ModEntities.WALKING_PIG_HEAD);
    public static final DeferredItem<Item> INFESTED_SHEEP_SPAWN_EGG = spawnEgg("infested_sheep", ModEntities.INFESTED_SHEEP);
    public static final DeferredItem<Item> WALKING_SHEEP_HEAD_SPAWN_EGG = spawnEgg("walking_sheep_head", ModEntities.WALKING_SHEEP_HEAD);
    public static final DeferredItem<Item> LARGE_INCOMPLETE_FORM_SPAWN_EGG = spawnEgg("large_incomplete_form", ModEntities.LARGE_INCOMPLETE_FORM);
    public static final DeferredItem<Item> INFESTED_COW_SPAWN_EGG = spawnEgg("infested_cow", ModEntities.INFESTED_COW);
    public static final DeferredItem<Item> WALKING_COW_HEAD_SPAWN_EGG = spawnEgg("walking_cow_head", ModEntities.WALKING_COW_HEAD);
    public static final DeferredItem<Item> NULLTHING_SPAWN_EGG = spawnEgg("nullthing", ModEntities.NULLTHING);
    public static final DeferredItem<Item> MOZZIE_SPAWN_EGG = spawnEgg("mozzie", ModEntities.MOZZIE);
    public static final DeferredItem<Item> INFESTED_WOLF_SPAWN_EGG = spawnEgg("infested_wolf", ModEntities.INFESTED_WOLF);
    public static final DeferredItem<Item> WALKING_WOLF_HEAD_SPAWN_EGG = spawnEgg("walking_wolf_head", ModEntities.WALKING_WOLF_HEAD);
    public static final DeferredItem<Item> RESHAPE_LONGARMS_SPAWN_EGG = spawnEgg("reshape_longarms", ModEntities.RESHAPE_LONGARMS);
    public static final DeferredItem<Item> BIOMASS_MEDIUM_SPAWN_EGG = spawnEgg("biomass_medium", ModEntities.BIOMASS_MEDIUM);
    public static final DeferredItem<Item> WALKING_CHICKEN_HEAD_SPAWN_EGG = spawnEgg("walking_chicken_head", ModEntities.WALKING_CHICKEN_HEAD);
    public static final DeferredItem<Item> INFESTED_CHICKEN_SPAWN_EGG = spawnEgg("infested_chicken", ModEntities.INFESTED_CHICKEN);
    public static final DeferredItem<Item> FLYING_CARRIER_SPAWN_EGG = spawnEgg("flying_carrier", ModEntities.FLYING_CARRIER);
    public static final DeferredItem<Item> INFESTED_ENDERMAN_SPAWN_EGG = spawnEgg("infested_enderman", ModEntities.INFESTED_ENDERMAN);
    public static final DeferredItem<Item> WALKING_ENDERMAN_HEAD_SPAWN_EGG = spawnEgg("walking_enderman_head", ModEntities.WALKING_ENDERMAN_HEAD);
    public static final DeferredItem<Item> INFESTED_ENDERMITE_SPAWN_EGG = spawnEgg("infested_endermite", ModEntities.INFESTED_ENDERMITE);
    public static final DeferredItem<Item> INFESTED_SILVERFISH_SPAWN_EGG = spawnEgg("infested_silverfish", ModEntities.INFESTED_SILVERFISH);
    public static final DeferredItem<Item> LIGHT_CARRIER_SPAWN_EGG = spawnEgg("light_carrier", ModEntities.LIGHT_CARRIER);
    public static final DeferredItem<Item> INFESTED_SKELETON_SPAWN_EGG = spawnEgg("infested_skeleton", ModEntities.INFESTED_SKELETON);
    public static final DeferredItem<Item> WALKING_SKELETON_HEAD_SPAWN_EGG = spawnEgg("walking_skeleton_head", ModEntities.WALKING_SKELETON_HEAD);
    public static final DeferredItem<Item> WALKING_FOX_HEAD_SPAWN_EGG = spawnEgg("walking_fox_head", ModEntities.WALKING_FOX_HEAD);
    public static final DeferredItem<Item> RESHAPE_YELLOWEYE_SPAWN_EGG = spawnEgg("reshape_yelloweye", ModEntities.RESHAPE_YELLOWEYE);
    public static final DeferredItem<Item> INFESTED_FOX_SPAWN_EGG = spawnEgg("infested_fox", ModEntities.INFESTED_FOX);
    public static final DeferredItem<Item> INFESTED_SLIME_SPAWN_EGG = ITEMS.registerItem("infested_slime_spawn_egg", InfestedSlimeSpawnEgg::new);
    public static final DeferredItem<Item> STAGE_I_BECKON_SPAWN_EGG = customItem("stage_i_beckon_spawn_egg", StageIBeckonSpawnEgg::new);
    public static final DeferredItem<Item> STAGE_II_BECKON_SPAWN_EGG = customItem("stage_ii_beckon_spawn_egg", StageIIBeckonSpawnEgg::new);
    public static final DeferredItem<Item> LIVING_FLESH_SPAWN_EGG =
            ITEMS.registerItem("living_flesh_spawn_egg",
                    properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.LIVING_FLESH_SIZE0.get())));
    public static final DeferredItem<Item> INFESTED_BAT_SPAWN_EGG = spawnEgg("infested_bat", ModEntities.INFESTED_BAT);

    // ==================== 方块物品 ====================

    public static final DeferredItem<Item> INFESTED_REMAINS_SMALL = blockItem("infested_remains_small", ModBlocks.INFESTED_REMAINS_SMALL);
    public static final DeferredItem<Item> INFESTED_REMAINS_MEDIUM = blockItem("infested_remains_medium", ModBlocks.INFESTED_REMAINS_MEDIUM);
    public static final DeferredItem<Item> INFESTED_REMAINS_LARGE = blockItem("infested_remains_large", ModBlocks.INFESTED_REMAINS_LARGE);
    public static final DeferredItem<Item> INFESTED_DIRT = blockItem("infested_dirt", ModBlocks.INFESTED_DIRT);
    public static final DeferredItem<Item> INFESTED_SAND = blockItem("infested_sand", ModBlocks.INFESTED_SAND);
    public static final DeferredItem<Item> INFESTED_GRASS = blockItem("infested_grass", ModBlocks.INFESTED_GRASS);
    public static final DeferredItem<Item> INFESTED_FERN = blockItem("infested_fern", ModBlocks.INFESTED_FERN);
    public static final DeferredItem<Item> INFESTED_SWEET_BERRY_BUSH = blockItem("infested_sweet_berry_bush", ModBlocks.INFESTED_SWEET_BERRY_BUSH);
    public static final DeferredItem<Item> INFESTED_RESIDUE = blockItem("infested_residue", ModBlocks.INFESTED_RESIDUE);
    public static final DeferredItem<Item> INFESTED_LEAVES = blockItem("infested_leaves", ModBlocks.INFESTED_LEAVES);
    public static final DeferredItem<Item> INFESTED_FLOWERING_LEAVES = blockItem("infested_flowering_leaves", ModBlocks.INFESTED_FLOWERING_LEAVES);
    public static final DeferredItem<Item> INFESTED_VINE = blockItem("infested_vine", ModBlocks.INFESTED_VINE);
    public static final DeferredItem<Item> INFESTED_STONE = blockItem("infested_stone", ModBlocks.INFESTED_STONE);
    public static final DeferredItem<Item> INFESTED_STONE_SLAB = blockItem("infested_stone_slab", ModBlocks.INFESTED_STONE_SLAB);
    public static final DeferredItem<Item> INFESTED_STONE_STAIRS = blockItem("infested_stone_stairs", ModBlocks.INFESTED_STONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_STONE_WALL = blockItem("infested_stone_wall", ModBlocks.INFESTED_STONE_WALL);
    public static final DeferredItem<Item> INFESTED_COBBLESTONE = blockItem("infested_cobblestone", ModBlocks.INFESTED_COBBLESTONE);
    public static final DeferredItem<Item> INFESTED_COBBLESTONE_SLAB = blockItem("infested_cobblestone_slab", ModBlocks.INFESTED_COBBLESTONE_SLAB);
    public static final DeferredItem<Item> INFESTED_COBBLESTONE_STAIRS = blockItem("infested_cobblestone_stairs", ModBlocks.INFESTED_COBBLESTONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_COBBLESTONE_WALL = blockItem("infested_cobblestone_wall", ModBlocks.INFESTED_COBBLESTONE_WALL);
    public static final DeferredItem<Item> INFESTED_STONE_BRICKS = blockItem("infested_stone_bricks", ModBlocks.INFESTED_STONE_BRICKS);
    public static final DeferredItem<Item> INFESTED_STONE_BRICKS_SLAB = blockItem("infested_stone_bricks_slab", ModBlocks.INFESTED_STONE_BRICKS_SLAB);
    public static final DeferredItem<Item> INFESTED_STONE_BRICKS_STAIRS = blockItem("infested_stone_bricks_stairs", ModBlocks.INFESTED_STONE_BRICKS_STAIRS);
    public static final DeferredItem<Item> INFESTED_STONE_BRICKS_WALL = blockItem("infested_stone_bricks_wall", ModBlocks.INFESTED_STONE_BRICKS_WALL);
    public static final DeferredItem<Item> INFESTED_CRACKED_STONE_BRICKS = blockItem("infested_cracked_stone_bricks", ModBlocks.INFESTED_CRACKED_STONE_BRICKS);
    public static final DeferredItem<Item> INFESTED_CHISELED_STONE_BRICKS = blockItem("infested_chiseled_stone_bricks", ModBlocks.INFESTED_CHISELED_STONE_BRICKS);
    public static final DeferredItem<Item> INFESTED_POLISHED_STONE = blockItem("infested_polished_stone", ModBlocks.INFESTED_POLISHED_STONE);
    public static final DeferredItem<Item> INFESTED_POLISHED_STONE_SLAB = blockItem("infested_polished_stone_slab", ModBlocks.INFESTED_POLISHED_STONE_SLAB);
    public static final DeferredItem<Item> INFESTED_POLISHED_STONE_STAIRS = blockItem("infested_polished_stone_stairs", ModBlocks.INFESTED_POLISHED_STONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_SANDSTONE = blockItem("infested_sandstone", ModBlocks.INFESTED_SANDSTONE);
    public static final DeferredItem<Item> INFESTED_SANDSTONE_SLAB = blockItem("infested_sandstone_slab", ModBlocks.INFESTED_SANDSTONE_SLAB);
    public static final DeferredItem<Item> INFESTED_SANDSTONE_STAIRS = blockItem("infested_sandstone_stairs", ModBlocks.INFESTED_SANDSTONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_SANDSTONE_WALL = blockItem("infested_sandstone_wall", ModBlocks.INFESTED_SANDSTONE_WALL);
    public static final DeferredItem<Item> INFESTED_CHISELED_RED_SANDSTONE = blockItem("infested_chiseled_red_sandstone", ModBlocks.INFESTED_CHISELED_RED_SANDSTONE);
    public static final DeferredItem<Item> INFESTED_CHISELED_SANDSTONE = blockItem("infested_chiseled_sandstone", ModBlocks.INFESTED_CHISELED_SANDSTONE);
    public static final DeferredItem<Item> INFESTED_SMOOTH_SANDSTONE = blockItem("infested_smooth_sandstone", ModBlocks.INFESTED_SMOOTH_SANDSTONE);
    public static final DeferredItem<Item> INFESTED_SMOOTH_SANDSTONE_SLAB = blockItem("infested_smooth_sandstone_slab", ModBlocks.INFESTED_SMOOTH_SANDSTONE_SLAB);
    public static final DeferredItem<Item> INFESTED_SMOOTH_SANDSTONE_STAIRS = blockItem("infested_smooth_sandstone_stairs", ModBlocks.INFESTED_SMOOTH_SANDSTONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_CUT_SANDSTONE = blockItem("infested_cut_sandstone", ModBlocks.INFESTED_CUT_SANDSTONE);
    public static final DeferredItem<Item> INFESTED_CUT_SANDSTONE_SLAB = blockItem("infested_cut_sandstone_slab", ModBlocks.INFESTED_CUT_SANDSTONE_SLAB);
    public static final DeferredItem<Item> INFESTED_COAL_ORE = blockItem("infested_coal_ore", ModBlocks.INFESTED_COAL_ORE);
    public static final DeferredItem<Item> INFESTED_COPPER_ORE = blockItem("infested_copper_ore", ModBlocks.INFESTED_COPPER_ORE);
    public static final DeferredItem<Item> INFESTED_IRON_ORE = blockItem("infested_iron_ore", ModBlocks.INFESTED_IRON_ORE);
    public static final DeferredItem<Item> INFESTED_GOLD_ORE = blockItem("infested_gold_ore", ModBlocks.INFESTED_GOLD_ORE);
    public static final DeferredItem<Item> INFESTED_LAPIS_ORE = blockItem("infested_lapis_ore", ModBlocks.INFESTED_LAPIS_ORE);
    public static final DeferredItem<Item> INFESTED_REDSTONE_ORE = blockItem("infested_redstone_ore", ModBlocks.INFESTED_REDSTONE_ORE);
    public static final DeferredItem<Item> INFESTED_EMERALD_ORE = blockItem("infested_emerald_ore", ModBlocks.INFESTED_EMERALD_ORE);
    public static final DeferredItem<Item> INFESTED_DIAMOND_ORE = blockItem("infested_diamond_ore", ModBlocks.INFESTED_DIAMOND_ORE);
    public static final DeferredItem<Item> INFESTED_SNOW = blockItem("infested_snow", ModBlocks.INFESTED_SNOW);
    public static final DeferredItem<Item> INFESTED_SNOW_BLOCK = blockItem("infested_snow_block", ModBlocks.INFESTED_SNOW_BLOCK);
    public static final DeferredItem<Item> INFESTED_INFESTED_COBBLESTONE = blockItem("infested_infested_cobblestone", ModBlocks.INFESTED_INFESTED_COBBLESTONE);
    public static final DeferredItem<Item> INFESTED_INFESTED_STONE = blockItem("infested_infested_stone", ModBlocks.INFESTED_INFESTED_STONE);
    public static final DeferredItem<Item> INFESTED_INFESTED_STONE_BRICKS = blockItem("infested_infested_stone_bricks", ModBlocks.INFESTED_INFESTED_STONE_BRICKS);
    public static final DeferredItem<Item> INFESTED_INFESTED_CRACKED_STONE_BRICKS = blockItem("infested_infested_cracked_stone_bricks", ModBlocks.INFESTED_INFESTED_CRACKED_STONE_BRICKS);
    public static final DeferredItem<Item> INFESTED_INFESTED_CHISELED_STONE_BRICKS = blockItem("infested_infested_chiseled_stone_bricks", ModBlocks.INFESTED_INFESTED_CHISELED_STONE_BRICKS);
    public static final DeferredItem<Item> INFESTED_NETHERSEA_BRAND_GROWN = blockItem("infested_nethersea_brand_grown", ModBlocks.INFESTED_NETHERSEA_BRAND_GROWN);
    public static final DeferredItem<Item> INFESTED_NETHERSEA_BRAND_SOLID = blockItem("infested_nethersea_brand_solid", ModBlocks.INFESTED_NETHERSEA_BRAND_SOLID);
    public static final DeferredItem<Item> SWALLOW_CYST = blockItem("swallow_cyst", ModBlocks.SWALLOW_CYST);
    public static final DeferredItem<Item> INFESTED_POINTED_DRIPSTONE = blockItem("infested_pointed_dripstone", ModBlocks.INFESTED_POINTED_DRIPSTONE);
    public static final DeferredItem<Item> BECKON_CORE = blockItem("beckon_core", ModBlocks.BECKON_CORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_STONE = blockItem("infested_heavy_stone", ModBlocks.INFESTED_HEAVY_STONE);
    public static final DeferredItem<Item> INFESTED_INFESTED_HEAVY_STONE = blockItem("infested_infested_heavy_stone", ModBlocks.INFESTED_INFESTED_HEAVY_STONE);
    public static final DeferredItem<Item> INFESTED_HEAVY_COAL_ORE = blockItem("infested_heavy_coal_ore", ModBlocks.INFESTED_HEAVY_COAL_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_COPPER_ORE = blockItem("infested_heavy_copper_ore", ModBlocks.INFESTED_HEAVY_COPPER_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_IRON_ORE = blockItem("infested_heavy_iron_ore", ModBlocks.INFESTED_HEAVY_IRON_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_GOLD_ORE = blockItem("infested_heavy_gold_ore", ModBlocks.INFESTED_HEAVY_GOLD_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_LAPIS_ORE = blockItem("infested_heavy_lapis_ore", ModBlocks.INFESTED_HEAVY_LAPIS_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_REDSTONE_ORE = blockItem("infested_heavy_redstone_ore", ModBlocks.INFESTED_HEAVY_REDSTONE_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_EMERALD_ORE = blockItem("infested_heavy_emerald_ore", ModBlocks.INFESTED_HEAVY_EMERALD_ORE);
    public static final DeferredItem<Item> INFESTED_HEAVY_DIAMOND_ORE = blockItem("infested_heavy_diamond_ore", ModBlocks.INFESTED_HEAVY_DIAMOND_ORE);
    public static final DeferredItem<Item> INFESTED_DUSTLIKE = blockItem("infested_dustlike", ModBlocks.INFESTED_DUSTLIKE);
    public static final DeferredItem<Item> INFESTED_PLANKSLIKE = blockItem("infested_plankslike", ModBlocks.INFESTED_PLANKSLIKE);
    public static final DeferredItem<Item> INFESTED_ROCKLIKE = blockItem("infested_rocklike", ModBlocks.INFESTED_ROCKLIKE);
    public static final DeferredItem<Item> INFESTED_METALLIKE = blockItem("infested_metallike", ModBlocks.INFESTED_METALLIKE);
    public static final DeferredItem<Item> INFESTED_HARDLIKE = blockItem("infested_hardlike", ModBlocks.INFESTED_HARDLIKE);
    public static final DeferredItem<Item> INFESTED_HEAVY_COBBLESTONE = blockItem("infested_heavy_cobblestone", ModBlocks.INFESTED_HEAVY_COBBLESTONE);
    public static final DeferredItem<Item> INFESTED_HEAVY_COBBLESTONE_STAIRS = blockItem("infested_heavy_cobblestone_stairs", ModBlocks.INFESTED_HEAVY_COBBLESTONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_HEAVY_COBBLESTONE_SLAB = blockItem("infested_heavy_cobblestone_slab", ModBlocks.INFESTED_HEAVY_COBBLESTONE_SLAB);
    public static final DeferredItem<Item> INFESTED_HEAVY_COBBLESTONE_WALL = blockItem("infested_heavy_cobblestone_wall", ModBlocks.INFESTED_HEAVY_COBBLESTONE_WALL);
    public static final DeferredItem<Item> INFESTED_CHISELED_DEEPSLATE = blockItem("infested_chiseled_deepslate", ModBlocks.INFESTED_CHISELED_DEEPSLATE);
    public static final DeferredItem<Item> INFESTED_POLISHED_HEAVY_STONE = blockItem("infested_polished_heavy_stone", ModBlocks.INFESTED_POLISHED_HEAVY_STONE);
    public static final DeferredItem<Item> INFESTED_POLISHED_HEAVY_STONE_STAIRS = blockItem("infested_polished_heavy_stone_stairs", ModBlocks.INFESTED_POLISHED_HEAVY_STONE_STAIRS);
    public static final DeferredItem<Item> INFESTED_POLISHED_HEAVY_STONE_SLAB = blockItem("infested_polished_heavy_stone_slab", ModBlocks.INFESTED_POLISHED_HEAVY_STONE_SLAB);
    public static final DeferredItem<Item> INFESTED_POLISHED_HEAVY_STONE_WALL = blockItem("infested_polished_heavy_stone_wall", ModBlocks.INFESTED_POLISHED_HEAVY_STONE_WALL);
    public static final DeferredItem<Item> INFESTED_LILY_PAD = blockItem("infested_lily_pad", ModBlocks.INFESTED_LILY_PAD);
    public static final DeferredItem<Item> INFESTED_CRACKED_HEAVY_BRICKS = blockItem("infested_cracked_heavy_bricks", ModBlocks.INFESTED_CRACKED_HEAVY_BRICKS);
    public static final DeferredItem<Item> INFESTED_HEAVY_BRICKS = blockItem("infested_heavy_bricks", ModBlocks.INFESTED_HEAVY_BRICKS);
    public static final DeferredItem<Item> INFESTED_HEAVY_BRICKS_STAIRS = blockItem("infested_heavy_bricks_stairs", ModBlocks.INFESTED_HEAVY_BRICKS_STAIRS);
    public static final DeferredItem<Item> INFESTED_HEAVY_BRICKS_SLAB = blockItem("infested_heavy_bricks_slab", ModBlocks.INFESTED_HEAVY_BRICKS_SLAB);
    public static final DeferredItem<Item> INFESTED_HEAVY_BRICKS_WALL = blockItem("infested_heavy_bricks_wall", ModBlocks.INFESTED_HEAVY_BRICKS_WALL);
    public static final DeferredItem<Item> INFESTED_CARVED_PUMPKIN = ITEMS.registerItem("infested_carved_pumpkin",
            properties -> new InfestedCarvedPumpkin(ModBlocks.INFESTED_CARVED_PUMPKIN.get(), properties));
    public static final DeferredItem<Item> INFESTED_PUMPKIN = blockItem("infested_pumpkin", ModBlocks.INFESTED_PUMPKIN);
    public static final DeferredItem<Item> INFESTED_TALL_GRASS = blockItem("infested_tall_grass", ModBlocks.INFESTED_TALL_GRASS);
    public static final DeferredItem<Item> INFESTED_TALL_FERN = blockItem("infested_tall_fern", ModBlocks.INFESTED_TALL_FERN);
    public static final DeferredItem<Item> INFESTED_SHORT_GRASS = blockItem("infested_short_grass", ModBlocks.INFESTED_SHORT_GRASS);
    public static final DeferredItem<Item> INFESTED_CACTUS = blockItem("infested_cactus", ModBlocks.INFESTED_CACTUS);
    public static final DeferredItem<Item> INFESTED_SUGAR_CANE = blockItem("infested_sugar_cane", ModBlocks.INFESTED_SUGAR_CANE);
    public static final DeferredItem<Item> INFESTED_SPIDER_WEB = blockItem("infested_spider_web", ModBlocks.INFESTED_SPIDER_WEB);
    public static final DeferredItem<Item> INFESTED_SPIDER_WEB_BLOOD = blockItem("infested_spider_web_blood", ModBlocks.INFESTED_SPIDER_WEB_BLOOD);
    public static final DeferredItem<Item> INFESTED_CAVE_SPIDER_WEB = blockItem("infested_cave_spider_web", ModBlocks.INFESTED_CAVE_SPIDER_WEB);
    public static final DeferredItem<Item> INFESTED_CRACKED_HEAVY_TILES = blockItem("infested_cracked_heavy_tiles", ModBlocks.INFESTED_CRACKED_HEAVY_TILES);
    public static final DeferredItem<Item> INFESTED_HEAVY_TILES = blockItem("infested_heavy_tiles", ModBlocks.INFESTED_HEAVY_TILES);
    public static final DeferredItem<Item> INFESTED_HEAVY_TILES_STAIRS = blockItem("infested_heavy_tiles_stairs", ModBlocks.INFESTED_HEAVY_TILES_STAIRS);
    public static final DeferredItem<Item> INFESTED_HEAVY_TILES_SLAB = blockItem("infested_heavy_tiles_slab", ModBlocks.INFESTED_HEAVY_TILES_SLAB);
    public static final DeferredItem<Item> INFESTED_HEAVY_TILES_WALL = blockItem("infested_heavy_tiles_wall", ModBlocks.INFESTED_HEAVY_TILES_WALL);
    public static final DeferredItem<Item> INFESTED_MUDDY_MANGROVE_ROOTS = blockItem("infested_muddy_mangrove_roots", ModBlocks.INFESTED_MUDDY_MANGROVE_ROOTS);
    public static final DeferredItem<Item> INFESTED_DEAD_BUSH = blockItem("infested_dead_bush", ModBlocks.INFESTED_DEAD_BUSH);

    // 特殊 BlockItem 子类（使用自定义的 Item 内部类）
    public static final DeferredItem<InfestedLog.InfestedLogItem> INFESTED_LOG =
            customBlockItem("infested_log", properties -> new InfestedLog.InfestedLogItem(ModBlocks.INFESTED_LOG.get(), properties));
    public static final DeferredItem<InfestedWood.InfestedWoodItem> INFESTED_WOOD =
            customBlockItem("infested_wood", properties -> new InfestedWood.InfestedWoodItem(ModBlocks.INFESTED_WOOD.get(), properties));
    public static final DeferredItem<InfestedStrippedLog.InfestedStrippedLogItem> INFESTED_STRIPPED_LOG =
            customBlockItem("infested_stripped_log", properties -> new InfestedStrippedLog.InfestedStrippedLogItem(ModBlocks.INFESTED_STRIPPED_LOG.get(), properties));
    public static final DeferredItem<InfestedStrippedWood.InfestedStrippedWoodItem> INFESTED_STRIPPED_WOOD =
            customBlockItem("infested_stripped_wood", properties -> new InfestedStrippedWood.InfestedStrippedWoodItem(ModBlocks.INFESTED_STRIPPED_WOOD.get(), properties));
    public static final DeferredItem<InfestedPlanks.InfestedPlanksItem> INFESTED_PLANKS =
            customBlockItem("infested_planks", properties -> new InfestedPlanks.InfestedPlanksItem(ModBlocks.INFESTED_PLANKS.get(), properties));
    public static final DeferredItem<InfestedPlanksSlab.InfestedPlanksSlabItem> INFESTED_PLANKS_SLAB =
            customBlockItem("infested_planks_slab", properties -> new InfestedPlanksSlab.InfestedPlanksSlabItem(ModBlocks.INFESTED_PLANKS_SLAB.get(), properties));
    public static final DeferredItem<InfestedPlanksStairs.InfestedPlanksStairsItem> INFESTED_PLANKS_STAIRS =
            customBlockItem("infested_planks_stairs", properties -> new InfestedPlanksStairs.InfestedPlanksStairsItem(ModBlocks.INFESTED_PLANKS_STAIRS.get(), properties));
    public static final DeferredItem<InfestedPlanksFence.InfestedPlanksItem> INFESTED_PLANKS_FENCE =
            customBlockItem("infested_planks_fence", properties -> new InfestedPlanksFence.InfestedPlanksItem(ModBlocks.INFESTED_PLANKS_FENCE.get(), properties));
    public static final DeferredItem<InfestedMangroveRoots.InfestedMangroveRootsItem> INFESTED_MANGROVE_ROOTS =
            customBlockItem("infested_mangrove_roots", properties -> new InfestedMangroveRoots.InfestedMangroveRootsItem(ModBlocks.INFESTED_MANGROVE_ROOTS.get(), properties));
}
