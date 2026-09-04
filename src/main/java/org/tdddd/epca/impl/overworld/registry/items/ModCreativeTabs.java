package org.tdddd.epca.impl.overworld.registry.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModItems;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, epca.MODID);

    public static final RegistryObject<CreativeModeTab> EPCA_MAIN_TAB = CREATIVE_TABS.register("main_tab",
            () -> {
                CustomTab.Builder builder = (CustomTab.Builder) new CustomTab.Builder(CreativeModeTab.Row.TOP, 0)
                        .icon(() -> new ItemStack(ModItems.PARASITE_VISCERA.get()))
                        .title(Component.translatable("itemGroup.epca.main_tab"));
                
                builder.add(i(ModItems.PARASITE_VISCERA));
                builder.add(i(ModItems.FINS_FIN));
                builder.add(i(ModItems.DISEASED_HEART));
                builder.add(i(ModItems.INFESTED_SLIME_BALL));
                builder.add(i(ModItems.INFESTED_BONE));
                builder.add(i(ModItems.WEIRD_MINCED_FLESH));
                builder.add(i(ModItems.INFESTED_FLESH));
                builder.add(i(ModItems.INFESTED_ENDER_PEARL));
                builder.add(i(ModItems.ENDER_BLADE_SCRAP));
                builder.add(i(ModItems.RESHAPE_FLESH));
                builder.add(i(ModItems.RESHAPE_SHELL));
                builder.add(i(ModItems.TWISTED_BONE));
                builder.add(i(ModItems.TIGHT_TENDONS));
                builder.add(i(ModItems.GASBAG_DEBRIS));
                builder.add(i(ModItems.BECKON_MEMBRANE));
                builder.add(i(ModItems.BLOODY_CLOCK));
                builder.add(i(ModItems.LIVING_ARMOR_BOX));
                builder.add(i(ModItems.FEEDING_MODULE_I));
                builder.add(i(ModItems.INSTINCT_MODULE_I));
                builder.add(i(ModItems.FLESH_ARMOR_MODULE_I));
                builder.add(i(ModItems.NETHERITE_MODULE_I));
                builder.add(i(ModItems.FLIGHT_MODULE_I));
                builder.add(i(ModItems.LIVING_HELMET));
                builder.add(i(ModItems.LIVING_CHESTPLATE));
                builder.add(i(ModItems.LIVING_LEGGINGS));
                builder.add(i(ModItems.LIVING_BOOTS));
                builder.add(i(ModItems.WOODEN_SPEAR));
                builder.add(i(ModItems.STONE_SPEAR));
                builder.add(i(ModItems.FLINT_SPEAR));
                builder.add(i(ModItems.COPPER_SPEAR));
                builder.add(i(ModItems.IRON_SPEAR));
                builder.add(i(ModItems.GOLDEN_SPEAR));
                builder.add(i(ModItems.DIAMOND_SPEAR));
                builder.add(i(ModItems.NETHERITE_SPEAR));
                builder.add(i(ModItems.INFESTED_RUBBISH));
                builder.add(i(ModItems.INFESTED_STICK));
                builder.add(i(ModItems.INFESTED_COAL));
                builder.add(i(ModItems.INFESTED_RAW_COPPER));
                builder.add(i(ModItems.INFESTED_RAW_IRON));
                builder.add(i(ModItems.INFESTED_RAW_GOLD));
                builder.add(i(ModItems.INFESTED_LAPIS_LAZULI));
                builder.add(i(ModItems.INFESTED_EMERALD));
                builder.add(i(ModItems.INFESTED_REDSTONE));
                builder.add(i(ModItems.INFESTED_DIAMOND));
                builder.add(i(ModItems.INFESTED_SWEET_BERRIES));
                builder.add(i(ModItems.INFESTED_NETHERSEA_BRAND_MOR));
                builder.add(i(ModItems.INFESTED_NETHERSEA_ICECREAM));
                builder.add(i(ModItems.KILL_STICK));
                builder.add(i(ModItems.EPCA_NOTE));
                builder.add(i(ModItems.SMALL_ITEM_FRAME));

                
                builder.add(new CustomTab.ITabEntry.Subheading1(
                        Component.translatable(" ")));
                builder.add(i(ModItems.BUGLIN_SPAWN_EGG));
                builder.add(i(ModItems.RUPTER_SPAWN_EGG));
                builder.add(i(ModItems.MOZZIE_SPAWN_EGG));
                builder.add(i(ModItems.FINS_SPAWN_EGG));
                builder.add(i(ModItems.LIGHT_CARRIER_SPAWN_EGG));
                builder.add(i(ModItems.FLYING_CARRIER_SPAWN_EGG));
                builder.add(i(ModItems.SMALL_INCOMPLETE_FORM_SPAWN_EGG));
                builder.add(i(ModItems.MEDIUM_INCOMPLETE_FORM_SPAWN_EGG));
                builder.add(i(ModItems.LARGE_INCOMPLETE_FORM_SPAWN_EGG));
                builder.add(i(ModItems.LIVING_FLESH_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_ZOMBIE_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_ZOMBIE_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_HUSK_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_HUSK_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_DROWNED_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_DROWNED_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_ZOMBIE_VILLAGER_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_ZOMBIE_VILLAGER_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_VILLAGER_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_VILLAGER_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_PILLAGER_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_PILLAGER_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_VINDICATOR_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_VINDICATOR_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_PIG_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_PIG_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_SHEEP_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_SHEEP_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_COW_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_COW_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_WOLF_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_WOLF_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_CHICKEN_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_CHICKEN_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_ENDERMAN_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_ENDERMAN_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_ENDERMITE_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_SILVERFISH_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_SLIME_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_SKELETON_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_SKELETON_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_FOX_SPAWN_EGG));
                builder.add(i(ModItems.WALKING_FOX_HEAD_SPAWN_EGG));
                builder.add(i(ModItems.INFESTED_BAT_SPAWN_EGG));
                builder.add(i(ModItems.RESHAPE_LONGARMS_SPAWN_EGG));
                builder.add(i(ModItems.RESHAPE_YELLOWEYE_SPAWN_EGG));
                builder.add(i(ModItems.STAGE_I_BECKON_SPAWN_EGG));
                builder.add(i(ModItems.STAGE_II_BECKON_SPAWN_EGG));

                
                builder.add(new CustomTab.ITabEntry.Subheading2(
                        Component.translatable(" ")));
                builder.add(i(ModItems.INFESTED_DIRT));
                builder.add(i(ModItems.INFESTED_SAND));
                builder.add(i(ModItems.INFESTED_STONE));
                builder.add(i(ModItems.INFESTED_STONE_STAIRS));
                builder.add(i(ModItems.INFESTED_STONE_SLAB));
                builder.add(i(ModItems.INFESTED_STONE_WALL));
                builder.add(i(ModItems.INFESTED_COBBLESTONE));
                builder.add(i(ModItems.INFESTED_COBBLESTONE_STAIRS));
                builder.add(i(ModItems.INFESTED_COBBLESTONE_SLAB));
                builder.add(i(ModItems.INFESTED_COBBLESTONE_WALL));
                builder.add(i(ModItems.INFESTED_STONE_BRICKS));
                builder.add(i(ModItems.INFESTED_STONE_BRICKS_STAIRS));
                builder.add(i(ModItems.INFESTED_STONE_BRICKS_SLAB));
                builder.add(i(ModItems.INFESTED_STONE_BRICKS_WALL));
                builder.add(i(ModItems.INFESTED_CRACKED_STONE_BRICKS));
                builder.add(i(ModItems.INFESTED_CHISELED_STONE_BRICKS));
                builder.add(i(ModItems.INFESTED_POLISHED_STONE));
                builder.add(i(ModItems.INFESTED_POLISHED_STONE_STAIRS));
                builder.add(i(ModItems.INFESTED_POLISHED_STONE_SLAB));
                builder.add(i(ModItems.INFESTED_SANDSTONE));
                builder.add(i(ModItems.INFESTED_SANDSTONE_STAIRS));
                builder.add(i(ModItems.INFESTED_SANDSTONE_SLAB));
                builder.add(i(ModItems.INFESTED_SANDSTONE_WALL));
                builder.add(i(ModItems.INFESTED_CHISELED_SANDSTONE));
                builder.add(i(ModItems.INFESTED_CHISELED_RED_SANDSTONE));
                builder.add(i(ModItems.INFESTED_SMOOTH_SANDSTONE));
                builder.add(i(ModItems.INFESTED_SMOOTH_SANDSTONE_STAIRS));
                builder.add(i(ModItems.INFESTED_SMOOTH_SANDSTONE_SLAB));
                builder.add(i(ModItems.INFESTED_CUT_SANDSTONE));
                builder.add(i(ModItems.INFESTED_CUT_SANDSTONE_SLAB));
                builder.add(i(ModItems.INFESTED_POINTED_DRIPSTONE));
                builder.add(i(ModItems.INFESTED_HEAVY_STONE));
                builder.add(i(ModItems.INFESTED_HEAVY_COBBLESTONE));
                builder.add(i(ModItems.INFESTED_HEAVY_COBBLESTONE_STAIRS));
                builder.add(i(ModItems.INFESTED_HEAVY_COBBLESTONE_SLAB));
                builder.add(i(ModItems.INFESTED_HEAVY_COBBLESTONE_WALL));
                builder.add(i(ModItems.INFESTED_CHISELED_DEEPSLATE));
                builder.add(i(ModItems.INFESTED_POLISHED_HEAVY_STONE));
                builder.add(i(ModItems.INFESTED_POLISHED_HEAVY_STONE_STAIRS));
                builder.add(i(ModItems.INFESTED_POLISHED_HEAVY_STONE_SLAB));
                builder.add(i(ModItems.INFESTED_POLISHED_HEAVY_STONE_WALL));
                builder.add(i(ModItems.INFESTED_HEAVY_BRICKS));
                builder.add(i(ModItems.INFESTED_CRACKED_HEAVY_BRICKS));
                builder.add(i(ModItems.INFESTED_HEAVY_BRICKS_SLAB));
                builder.add(i(ModItems.INFESTED_HEAVY_BRICKS_STAIRS));
                builder.add(i(ModItems.INFESTED_HEAVY_BRICKS_WALL));
                builder.add(i(ModItems.INFESTED_HEAVY_TILES));
                builder.add(i(ModItems.INFESTED_CRACKED_HEAVY_TILES));
                builder.add(i(ModItems.INFESTED_HEAVY_TILES_SLAB));
                builder.add(i(ModItems.INFESTED_HEAVY_TILES_STAIRS));
                builder.add(i(ModItems.INFESTED_HEAVY_TILES_WALL));
                builder.add(i(ModItems.INFESTED_LOG));
                builder.add(i(ModItems.INFESTED_WOOD));
                builder.add(i(ModItems.INFESTED_STRIPPED_LOG));
                builder.add(i(ModItems.INFESTED_STRIPPED_WOOD));
                builder.add(i(ModItems.INFESTED_PLANKS));
                builder.add(i(ModItems.INFESTED_PLANKS_STAIRS));
                builder.add(i(ModItems.INFESTED_PLANKS_SLAB));
                builder.add(i(ModItems.INFESTED_PLANKS_FENCE));
                builder.add(i(ModItems.INFESTED_LEAVES));
                builder.add(i(ModItems.INFESTED_FLOWERING_LEAVES));
                builder.add(i(ModItems.INFESTED_SNOW_BLOCK));
                builder.add(i(ModItems.INFESTED_SNOW));
                builder.add(i(ModItems.INFESTED_INFESTED_STONE));
                builder.add(i(ModItems.INFESTED_INFESTED_COBBLESTONE));
                builder.add(i(ModItems.INFESTED_INFESTED_STONE_BRICKS));
                builder.add(i(ModItems.INFESTED_INFESTED_CRACKED_STONE_BRICKS));
                builder.add(i(ModItems.INFESTED_INFESTED_CHISELED_STONE_BRICKS));
                builder.add(i(ModItems.INFESTED_INFESTED_HEAVY_STONE));
                builder.add(i(ModItems.INFESTED_VINE));
                builder.add(i(ModItems.INFESTED_REMAINS_SMALL));
                builder.add(i(ModItems.INFESTED_REMAINS_MEDIUM));
                builder.add(i(ModItems.INFESTED_REMAINS_LARGE));
                builder.add(i(ModItems.INFESTED_RESIDUE));
                builder.add(i(ModItems.INFESTED_GRASS));
                builder.add(i(ModItems.INFESTED_FERN));
                builder.add(i(ModItems.INFESTED_SHORT_GRASS));
                builder.add(i(ModItems.INFESTED_TALL_GRASS));
                builder.add(i(ModItems.INFESTED_TALL_FERN));
                builder.add(i(ModItems.INFESTED_SWEET_BERRY_BUSH));
                builder.add(i(ModItems.INFESTED_PUMPKIN));
                builder.add(i(ModItems.INFESTED_CARVED_PUMPKIN));
                builder.add(i(ModItems.INFESTED_SUGAR_CANE));
                builder.add(i(ModItems.INFESTED_CACTUS));
                builder.add(i(ModItems.INFESTED_LILY_PAD));
                builder.add(i(ModItems.INFESTED_COAL_ORE));
                builder.add(i(ModItems.INFESTED_COPPER_ORE));
                builder.add(i(ModItems.INFESTED_IRON_ORE));
                builder.add(i(ModItems.INFESTED_GOLD_ORE));
                builder.add(i(ModItems.INFESTED_LAPIS_ORE));
                builder.add(i(ModItems.INFESTED_REDSTONE_ORE));
                builder.add(i(ModItems.INFESTED_EMERALD_ORE));
                builder.add(i(ModItems.INFESTED_DIAMOND_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_COAL_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_COPPER_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_IRON_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_GOLD_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_LAPIS_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_REDSTONE_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_EMERALD_ORE));
                builder.add(i(ModItems.INFESTED_HEAVY_DIAMOND_ORE));
                builder.add(i(ModItems.SWALLOW_CYST));
                builder.add(i(ModItems.INFESTED_SPIDER_WEB));
                builder.add(i(ModItems.INFESTED_SPIDER_WEB_BLOOD));
                builder.add(i(ModItems.INFESTED_CAVE_SPIDER_WEB));
                builder.add(i(ModItems.INFESTED_NETHERSEA_BRAND_GROWN));
                builder.add(i(ModItems.INFESTED_NETHERSEA_BRAND_SOLID));
                builder.add(i(ModItems.INFESTED_DUSTLIKE));
                builder.add(i(ModItems.INFESTED_PLANKSLIKE));
                builder.add(i(ModItems.INFESTED_ROCKLIKE));
                builder.add(i(ModItems.INFESTED_METALLIKE));
                builder.add(i(ModItems.INFESTED_HARDLIKE));
                builder.add(i(ModItems.BECKON_CORE));
                builder.add(i(ModItems.ACID_SOLUTION_BUCKET));
                builder.add(i(ModItems.PACKED_MUD_PEDESTAL));
                builder.add(i(ModItems.PACKED_MUD_ALTAR_STONE));

                return builder.build();
            }
    );

    
    public static CustomTab.ITabEntry e() {
        return CustomTab.ITabEntry.EMPTY;
    }

    public static CustomTab.ITabEntry n() {
        return CustomTab.ITabEntry.LINE_BREAK;
    }

    
    private static CustomTab.ITabEntry.Item i(Supplier<? extends Item> itemSupplier) {
        return new CustomTab.ITabEntry.Item(() -> new ItemStack(itemSupplier.get()));
    }

    
    public static CustomTab.ITabEntry.Item iStack(Supplier<ItemStack> itemStackSupplier) {
        return new CustomTab.ITabEntry.Item(itemStackSupplier);
    }

    
    public static CustomTab.ITabEntry.ConditionalItem c(Supplier<? extends Item> itemSupplier, Supplier<Boolean> condition) {
        return new CustomTab.ITabEntry.ConditionalItem(() -> new ItemStack(itemSupplier.get()), condition);
    }

    
    private static CustomTab.ITabEntry.DuplicateItem r(Supplier<? extends Item> itemSupplier) {
        return new CustomTab.ITabEntry.DuplicateItem(() -> new ItemStack(itemSupplier.get()));
    }

    private static CustomTab.ITabEntry.DuplicateItem d(Supplier<? extends Item> itemSupplier) {
        return new CustomTab.ITabEntry.DuplicateItem(() -> new ItemStack(itemSupplier.get()));
    }

    private static CustomTab.ITabEntry.DuplicateItem d(ItemLike item) {
        return new CustomTab.ITabEntry.DuplicateItem(() -> new ItemStack(item));
    }

    private static CustomTab.ITabEntry.Subheading s(String translationKey) {
        return new CustomTab.ITabEntry.Subheading(Component.translatable("itemGroup.epca.main_tab." + translationKey));
    }

    private static CustomTab.ITabEntry.Subheading0 s0(String translationKey) {
        return new CustomTab.ITabEntry.Subheading0(Component.translatable(" "));
    }

    private static CustomTab.ITabEntry.Subheading1 s1(String translationKey) {
        return new CustomTab.ITabEntry.Subheading1(Component.translatable(" "));
    }

    private static CustomTab.ITabEntry.Subheading2 s2(String translationKey) {
        return new CustomTab.ITabEntry.Subheading2(Component.translatable(" "));
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}