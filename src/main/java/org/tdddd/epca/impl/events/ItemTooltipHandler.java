package org.tdddd.epca.impl.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedLog;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public class ItemTooltipHandler {
    private static final Map<RegistryObject<? extends Item>, String> TOOLTIP_MAP = new HashMap<>();

    static {
        
        registerTooltip(ModItems.BUGLIN_SPAWN_EGG , "item.epca.category_onesent");
        registerTooltip(ModItems.RUPTER_SPAWN_EGG , "item.epca.category_onesent");
        registerTooltip(ModItems.FINS_SPAWN_EGG , "item.epca.category_onesent");
        registerTooltip(ModItems.MOZZIE_SPAWN_EGG , "item.epca.category_onesent");
        registerTooltip(ModItems.FLYING_CARRIER_SPAWN_EGG , "item.epca.category_onesent");
        registerTooltip(ModItems.LIGHT_CARRIER_SPAWN_EGG , "item.epca.category_onesent");

        
        registerTooltip(ModItems.SMALL_INCOMPLETE_FORM_SPAWN_EGG , "item.epca.category_poverty");
        registerTooltip(ModItems.MEDIUM_INCOMPLETE_FORM_SPAWN_EGG , "item.epca.category_poverty");
        registerTooltip(ModItems.LARGE_INCOMPLETE_FORM_SPAWN_EGG , "item.epca.category_poverty");
        registerTooltip(ModItems.LIVING_FLESH_SPAWN_EGG , "item.epca.category_poverty");

        
        registerTooltip(ModItems.INFESTED_ZOMBIE_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_ZOMBIE_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HUSK_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_HUSK_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_DROWNED_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_DROWNED_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PILLAGER_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_PILLAGER_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_VINDICATOR_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_VINDICATOR_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_VILLAGER_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_VILLAGER_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_ZOMBIE_VILLAGER_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_ZOMBIE_VILLAGER_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PIG_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_PIG_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SHEEP_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_SHEEP_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COW_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_COW_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_WOLF_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_WOLF_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_CHICKEN_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CHICKEN_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_ENDERMAN_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_ENDERMAN_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_ENDERMITE_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SILVERFISH_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SKELETON_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_SKELETON_HEAD_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SLIME_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_FOX_SPAWN_EGG , "item.epca.category_infested");
        registerTooltip(ModItems.WALKING_FOX_HEAD_SPAWN_EGG , "item.epca.category_infested");

        
        registerTooltip(ModItems.RESHAPE_LONGARMS_SPAWN_EGG, "item.epca.category_reshape");
        registerTooltip(ModItems.RESHAPE_YELLOWEYE_SPAWN_EGG, "item.epca.category_reshape");

        
        registerTooltip(ModItems.STAGE_I_BECKON_SPAWN_EGG, "item.epca.category_link");
        registerTooltip(ModItems.STAGE_II_BECKON_SPAWN_EGG, "item.epca.category_link");

        //blocks

        //poverty
        registerTooltip(ModItems.SWALLOW_CYST, "item.epca.category_poverty");

        //Infested
        registerTooltip(ModItems.INFESTED_REMAINS_SMALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_REMAINS_MEDIUM, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_REMAINS_LARGE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_DIRT, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SAND, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_GRASS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_FERN, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SWEET_BERRY_BUSH, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_RESIDUE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_LEAVES, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_FLOWERING_LEAVES, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_VINE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COBBLESTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COBBLESTONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COBBLESTONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COBBLESTONE_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_BRICKS_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_BRICKS_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STONE_BRICKS_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CRACKED_STONE_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CHISELED_STONE_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_STONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_STONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_STONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SANDSTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SANDSTONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SANDSTONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SANDSTONE_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CHISELED_RED_SANDSTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CHISELED_SANDSTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SMOOTH_SANDSTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SMOOTH_SANDSTONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SMOOTH_SANDSTONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CUT_SANDSTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CUT_SANDSTONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COAL_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_COPPER_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_IRON_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_GOLD_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_LAPIS_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_REDSTONE_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_EMERALD_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_DIAMOND_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SNOW, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SNOW_BLOCK, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_INFESTED_COBBLESTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_INFESTED_STONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_INFESTED_STONE_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_INFESTED_CRACKED_STONE_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_INFESTED_CHISELED_STONE_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_NETHERSEA_BRAND_GROWN, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_NETHERSEA_BRAND_SOLID, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POINTED_DRIPSTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_STONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_INFESTED_HEAVY_STONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_COAL_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_COPPER_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_IRON_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_GOLD_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_LAPIS_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_REDSTONE_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_EMERALD_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_DIAMOND_ORE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_DUSTLIKE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PLANKSLIKE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_ROCKLIKE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_METALLIKE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HARDLIKE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_COBBLESTONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_COBBLESTONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_COBBLESTONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_COBBLESTONE_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CHISELED_DEEPSLATE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_HEAVY_STONE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_HEAVY_STONE_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_HEAVY_STONE_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_POLISHED_HEAVY_STONE_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_LOG, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_WOOD, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STRIPPED_LOG, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_STRIPPED_WOOD, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PLANKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PLANKS_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PLANKS_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PLANKS_FENCE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_LILY_PAD, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CRACKED_HEAVY_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_BRICKS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_BRICKS_SLAB, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_BRICKS_STAIRS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_HEAVY_BRICKS_WALL, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CARVED_PUMPKIN, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_PUMPKIN, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_TALL_GRASS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_TALL_FERN, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SHORT_GRASS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_CACTUS, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SUGAR_CANE, "item.epca.category_infested");
        registerTooltip(ModItems.INFESTED_SPIDER_WEB, "item.epca.category_infested");

        //link
        registerTooltip(ModItems.BECKON_CORE, "item.epca.category_link");
    }

    private static void registerTooltip(RegistryObject<? extends Item> ro, String translationKey) {
        if (ro != null) {
            TOOLTIP_MAP.put(ro, translationKey);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        for (Map.Entry<RegistryObject<? extends Item>, String> entry : TOOLTIP_MAP.entrySet()) {
            if (entry.getKey().isPresent() && entry.getKey().get() == item) {
                event.getToolTip().add(Component.translatable(entry.getValue()).withStyle(ChatFormatting.GRAY));
                break;
            }
        }
    }
}
