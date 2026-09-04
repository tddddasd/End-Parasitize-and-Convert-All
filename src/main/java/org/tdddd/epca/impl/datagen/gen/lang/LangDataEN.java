package org.tdddd.epca.impl.datagen.gen.lang;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.tdddd.epca.impl.epca;

public class LangDataEN extends LanguageProvider {
    public LangDataEN(PackOutput output, String locale) {
        super(output, epca.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // Item groups / basic UI
        add("itemGroup." + epca.MODID + ".main_tab", "E-PCA");

        add("category.epca.altar_crafting", "Altar Crafting");

        // Gamerules
        add("gamerule.epca_hardnessConversionBlock", "Convert blocks based on hardness");
        add("gamerule.epca_hardnessConversionBlock.description", "When a block doesn't have a transformation configuration mapping, is it allowed to convert it into Infested Residue, Rocklike, or Plankslike blocks based on its hardness");

        // Tooltips
        add("tooltip.epca.max_damage_type", "Max Damage Type: %s (\u00d7%s)");
        add("tooltip.epca.min_kill_count", "Min Kill Count: %d");
        add("tooltip.epca.broken_adaptation_time", "Broken Adaptation: %.1fs");
        add("tooltip.epca.adaptation_level", "Adaptation Level: %d/%d");
        add("tooltip.epca.attack_count", "Hurt count: %d/%d");
        add("tooltip.epca.block_count", "Block count: %d/%d");
        add("tooltip.epca.player_attack_count", "Attack count: %d/%d");
        add("tooltip.epca.module_description", "Module Information:");
        add("tooltip.epca.defense", "Defense: %s");
        add("tooltip.epca.attack", "Attack: %s");
        add("tooltip.epca.biomass", "Consume: %s");
        add("tooltip.epca.negative", "Negative: %s");
        add("tooltip.epca.item_proficiency", "Proficiency: %d/%d [Damage Bonus +%s%%]");
        add("item.small_item_frame.added", "Recorded: %s");
        add("item.small_item_frame.max_reached", "Cannot add (limit reached or duplicate)");
        add("item.small_item_frame.no_offhand", "No item in offhand");
        add("item.small_item_frame.count", "%d/%d stored");

        // Jade plugins
        add("config.jade.plugin_epca.damage_adaptation_info", "Damage Adaptation Info");
        add("config.jade.plugin_epca.kill_count_info", "Kill Count Info");

        // Advancements
        add("advancements.epca.root.description", "The Beginning of the Apocalypse");
        add("advancements.epca.root.title", "Usual?");
        add("advancements.epca.sense_of_crisis.title", "Sense of crisis");
        add("advancements.epca.sense_of_crisis.description", "Attacking parasites will attract more parasites");
        add("advancements.epca.master_difficulty.title", "Only dedicated to those who offer...");
        add("advancements.epca.master_difficulty.description", "Enter a world on hard mode - master difficulty without enabling cheats");

        // Biomes
        add("biome.epca.parasite_biome", "Parasite Biome");

        // Creative tabs / categories
        add("item.epca.category.spawn_eggs", "\u00a7lSpawn Eggs");
        add("item.epca.category.materials_gear", "\u00a7lMaterials & Gear");
        add("item.epca.category.blocks", "\u00a7lBlocks");
        add("item.epca.category_onesent", "Category: Onesent");
        add("item.epca.category_poverty", "Category: Poverty");
        add("item.epca.category_infested", "Category: Infested");
        add("item.epca.category_reshape", "Category: Reshape");
        add("item.epca.category_link", "Category: Link");

        // Bestiary
        add("epca.tab.parent.main_tab", "Parasite Bestiary");
        add("epca.tab.child.onesent", "Onesent");

        // Entities
        add("entity.epca.yawning_nya.join", "\u00a7eYawning_Nya joined the world");
        add("entity.epca.yawning_nya", "Yawning_Nya");
        add("entity.epca.curbug", "Curbug");
        add("entity.epca.ripper", "Ripper");
        add("entity.epca.small_incomplete_form", "Small Incomplete Form");
        add("entity.epca.medium_incomplete_form", "Medium Incomplete Form");
        add("entity.epca.large_incomplete_form", "Large Incomplete Form");
        add("entity.epca.infested_zombie", "Infested Zombie");
        add("entity.epca.walking_zombie_head", "Walking Zombie Head");
        add("entity.epca.infested_husk", "Infested Husk");
        add("entity.epca.walking_husk_head", "Walking Husk Head");
        add("entity.epca.infested_drowned", "Infested Drowned");
        add("entity.epca.walking_drowned_head", "Walking Drowned Head");
        add("entity.epca.biomass_small", "Biomass");
        add("entity.epca.biomass_medium", "Biomass");
        add("entity.epca.stage_i_beckon", "Beckon Stage I");
        add("entity.epca.stage_ii_beckon", "Beckon Stage II");
        add("entity.epca.viral_bomb", "Viral Bomb");
        add("entity.epca.viral_bomb_ii", "Viral Bomb");
        add("entity.epca.infested_pillager", "Infested Pillager");
        add("entity.epca.walking_pillager_head", "Walking Pillager Head");
        add("entity.epca.infested_vindicator", "Infested Vindicator");
        add("entity.epca.walking_vindicator_head", "Walking Vindicator Head");
        add("entity.epca.infested_villager", "Infested Villager");
        add("entity.epca.walking_villager_head", "Walking Villager Head");
        add("entity.epca.infested_zombie_villager", "Infested Zombie Villager");
        add("entity.epca.walking_zombie_villager_head", "Walking Zombie Villager Head");
        add("entity.epca.fins", "Fins");
        add("entity.epca.infested_pig", "Infested Pig");
        add("entity.epca.walking_pig_head", "Walking Pig Head");
        add("entity.epca.infested_sheep", "Infested Sheep");
        add("entity.epca.walking_sheep_head", "Walking Sheep Head");
        add("entity.epca.infested_cow", "Infested Cow");
        add("entity.epca.walking_cow_head", "Walking Cow Head");
        add("entity.epca.infested_wolf", "Infested Wolf");
        add("entity.epca.walking_wolf_head", "Walking Wolf Head");
        add("entity.epca.infested_chicken", "Infested Chicken");
        add("entity.epca.walking_chicken_head", "Walking Chicken Head");
        add("entity.epca.mozzie", "Mozzie");
        add("entity.epca.infested_slime_size0", "Infested Slime");
        add("entity.epca.infested_slime_size1", "Infested Slime");
        add("entity.epca.infested_slime_size3", "Infested Slime");
        add("entity.epca.living_flesh_size0", "Living Flesh");
        add("entity.epca.living_flesh_size1", "Living Flesh");
        add("entity.epca.living_flesh_size2", "Living Flesh");
        add("entity.epca.living_flesh_size3", "Living Flesh");
        add("entity.epca.living_flesh_size4", "Living Flesh");
        add("entity.epca.reshape_longarms", "Reshape-Longarms");
        add("entity.epca.flying_carrier", "Flying Carrier");
        add("entity.epca.infested_enderman", "Infested Enderman");
        add("entity.epca.walking_enderman_head", "Walking Enderman Head");
        add("entity.epca.infested_endermite", "Infested Endermite");
        add("entity.epca.infested_silverfish", "Infested Silverfish");
        add("entity.epca.light_carrier", "Light Carrier");
        add("entity.epca.infested_skeleton", "Infested Skeleton");
        add("entity.epca.walking_skeleton_head", "Walking Skeleton Head");
        add("entity.epca.bone_fragment", "Infested Bone Fragment");
        add("entity.epca.bone_arrow", "Infested Bone Arrow");
        add("entity.epca.infested_ender_pearl", "Infested Ender Pearl");
        add("entity.epca.reshape_yelloweye", "Reshape-Yelloweye");
        add("entity.epca.infested_fox", "Infested Fox");
        add("entity.epca.walking_fox_head", "Walking Fox Head");
        add("entity.epca.reshape_part", "Reshape-Longarms");
        add("entity.epca.infested_pumpkin_head", "Infested Pumpkin Head");
        add("entity.epca.infested_spider_web_projectile", "Infested Spider Web Projectile");
        add("entity.epca.infested_spider_web_blood_projectile", "Infested Blood Spider Web Projectile");
        add("entity.epca.infested_cave_spider_web_projectile", "Infested Cave Spider Web Projectile");
        add("entity.epca.infested_bat", "Infested Bat");

        // Thrown spears
        add("entity.epca.thrown_wooden_spear", "Wooden Spear");
        add("entity.epca.thrown_stone_spear", "Stone Spear");
        add("entity.epca.thrown_flint_spear", "Flint Spear");
        add("entity.epca.thrown_copper_spear", "Copper Spear");
        add("entity.epca.thrown_iron_spear", "Iron Spear");
        add("entity.epca.thrown_golden_spear", "Golden Spear");
        add("entity.epca.thrown_diamond_spear", "Diamond Spear");
        add("entity.epca.thrown_netherite_spear", "Netherite Spear");

        // Effects
        add("effect.epca.bleeding", "Bleeding");
        add("effect.epca.viral", "Viral");
        add("effect.epca.fear", "Fear");
        add("effect.epca.coth", "\u00a7cCOTH");
        add("effect.epca.corrosive", "Corrosive");
        add("effect.epca.rage", "Rage");
        add("effect.epca.needler", "Needler");
        add("effect.epca.deep_sneak", "Deep Sneak");
        add("effect.epca.solidify", "Solidify");
        add("effect.epca.ender_erosion", "Ender Erosion");
        add("effect.epca.spirit", "Spirit");
        add("effect.epca.camouflage", "Camouflage");
        add("effect.epca.contempt_inorganic", "\u00a74Contempt Inorganic");
        add("effect.epca.soul_protection", "Soul Protection");
        add("effect.epca.fear.message", "You're in a state of panic!");

        // Items
        add("item.epca.wooden_spear", "Wooden Spear");
        add("item.epca.stone_spear", "Stone Spear");
        add("item.epca.flint_spear", "Flint Spear");
        add("item.epca.copper_spear", "Copper Spear");
        add("item.epca.iron_spear", "Iron Spear");
        add("item.epca.golden_spear", "Golden Spear");
        add("item.epca.diamond_spear", "Diamond Spear");
        add("item.epca.netherite_spear", "Netherite Spear");

        // Spawn eggs
        add("item.epca.ripper_spawn_egg", "Ripper Spawn Egg");
        add("item.epca.curbug_spawn_egg", "Curbug Spawn Egg");
        add("item.epca.small_incomplete_form_spawn_egg", "Small Incomplete Form Spawn Egg");
        add("item.epca.medium_incomplete_form_spawn_egg", "Medium Incomplete Form Spawn Egg");
        add("item.epca.large_incomplete_form_spawn_egg", "Large Incomplete Form Spawn Egg");
        add("item.epca.infested_zombie_spawn_egg", "Infested Zombie Spawn Egg");
        add("item.epca.walking_zombie_head_spawn_egg", "Walking Zombie Head Spawn Egg");
        add("item.epca.infested_husk_spawn_egg", "Infested Husk Spawn Egg");
        add("item.epca.walking_husk_head_spawn_egg", "Walking Husk Head Spawn Egg");
        add("item.epca.infested_drowned_spawn_egg", "Infested Drowned Spawn Egg");
        add("item.epca.walking_drowned_head_spawn_egg", "Walking Drowned Head Spawn Egg");
        add("item.epca.stage_i_beckon_spawn_egg", "Beckon Stage I Spawn Egg");
        add("item.epca.stage_ii_beckon_spawn_egg", "Beckon Stage II Spawn Egg");
        add("item.epca.infested_pillager_spawn_egg", "Infested Pillager Spawn Egg");
        add("item.epca.walking_pillager_head_spawn_egg", "Walking Pillager Head Spawn Egg");
        add("item.epca.infested_vindicator_spawn_egg", "Infested Vindicator Spawn Egg");
        add("item.epca.walking_vindicator_head_spawn_egg", "Walking Vindicator Head Spawn Egg");
        add("item.epca.infested_villager_spawn_egg", "Infested Villager Spawn Egg");
        add("item.epca.walking_villager_head_spawn_egg", "Walking Villager Head Spawn Egg");
        add("item.epca.infested_zombie_villager_spawn_egg", "Infested Zombie Villager Spawn Egg");
        add("item.epca.walking_zombie_villager_head_spawn_egg", "Walking Zombie Villager Head Spawn Egg");
        add("item.epca.fins_spawn_egg", "Fins Spawn Egg");
        add("item.epca.infested_pig_spawn_egg", "Infested Pig Spawn Egg");
        add("item.epca.walking_pig_head_spawn_egg", "Walking Pig Head Spawn Egg");
        add("item.epca.infested_sheep_spawn_egg", "Infested Sheep Spawn Egg");
        add("item.epca.walking_sheep_head_spawn_egg", "Walking Sheep Head Spawn Egg");
        add("item.epca.infested_cow_spawn_egg", "Infested Cow Spawn Egg");
        add("item.epca.walking_cow_head_spawn_egg", "Walking Cow Head Spawn Egg");
        add("item.epca.infested_wolf_spawn_egg", "Infested Wolf Spawn Egg");
        add("item.epca.walking_wolf_head_spawn_egg", "Walking Wolf Head Spawn Egg");
        add("item.epca.infested_chicken_spawn_egg", "Infested Chicken Spawn Egg");
        add("item.epca.walking_chicken_head_spawn_egg", "Walking Chicken Head Spawn Egg");
        add("item.epca.mozzie_spawn_egg", "Mozzie Spawn Egg");
        add("item.epca.infested_slime_spawn_egg", "Infested Slime Spawn Egg");
        add("item.epca.living_flesh_spawn_egg", "Living Flesh Spawn Egg");
        add("item.epca.reshape_longarms_spawn_egg", "Reshape-Longarms Spawn Egg");
        add("item.epca.flying_carrier_spawn_egg", "Flying Carrier Spawn Egg");
        add("item.epca.infested_enderman_spawn_egg", "Infested Enderman Spawn Egg");
        add("item.epca.walking_enderman_head_spawn_egg", "Walking Enderman Head Spawn Egg");
        add("item.epca.infested_endermite_spawn_egg", "Infested Endermite Spawn Egg");
        add("item.epca.infested_silverfish_spawn_egg", "Infested Silverfish Spawn Egg");
        add("item.epca.light_carrier_spawn_egg", "Light Carrier Spawn Egg");
        add("item.epca.infested_skeleton_spawn_egg", "Infested Skeleton Spawn Egg");
        add("item.epca.walking_skeleton_head_spawn_egg", "Walking Skeleton Head Spawn Egg");
        add("item.epca.reshape_yelloweye_spawn_egg", "Reshape-Yelloweye Spawn Egg");
        add("item.epca.infested_fox_spawn_egg", "Infested Fox Spawn Egg");
        add("item.epca.walking_fox_head_spawn_egg", "Walking Fox Head Spawn Egg");
        add("item.epca.infested_bat_spawn_egg", "Infested Bat Spawn Egg");

        // Materials & special items
        add("item.epca.parasite_viscera", "Parasite Viscera");
        add("item.epca.infested_bone", "Infested Bone");
        add("item.epca.weird_minced_flesh", "Weird Minced Flesh");
        add("item.epca.diseased_heart", "Diseased Heart");
        add("item.epca.infested_flesh", "Infested Flesh");
        add("item.epca.fins_fin", "Fins Fin");
        add("item.epca.reshape_flesh", "Reshape Flesh");
        add("item.epca.reshape_shell", "Reshape Shell");
        add("item.epca.twisted_bone", "Twisted Bone");
        add("item.epca.tight_tendons", "Tight Tendons");
        add("item.epca.gasbag_debris", "Gasbag Debris");
        add("item.epca.beckon_membrane", "Beckon Membrane");
        add("item.epca.erosion_clock", "Crosion Clock");
        add("item.epca.living_armor_box", "Living Armor Box");
        add("item.epca.feeding_module_i", "[Level 1 Automatic feeding Module]");
        add("item.epca.instinct_module_i", "[Level 1 Instinct Module]");
        add("item.epca.flesh_armor_module_i", "[Level 1 Flesh armor Module]");
        add("item.epca.netherite_module_i", "[Level 1 Netherite Module]");
        add("item.epca.flight_module_i", "[Level 1 Flight Module]");
        add("item.epca.living_armor_box_module", "Armor Box Module");
        add("item.epca.infested_sweet_berries", "Infested Sweet Berries");

        // Living armor box tooltips
        add("item.epca.living_armor_box.tooltip.storage_info", "Storage Information:");
        add("item.epca.living_armor_box.tooltip.storage_space", "  %s/%s Items");
        add("item.epca.living_armor_box.tooltip.stored_items", "Stored Items:");
        add("item.epca.living_armor_box.tooltip.empty", "  Empty");
        add("item.epca.living_armor_box.tooltip.more_items", "  ... and %s more items");
        add("item.epca.living_armor_box.tooltip.usage", "Usage:");
        add("item.epca.living_armor_box.tooltip.use_right_click", "  Right Click: Equip/Unequip Living Armor");
        add("item.epca.living_armor_box.tooltip.use_sneak_right_click_with_item", "  Sneak + Right Click (with item): Store Mode");
        add("item.epca.living_armor_box.tooltip.use_sneak_right_click_empty", "  Sneak + Right Click (empty hand): Retrieve Mode");
        add("item.epca.living_armor_box.tooltip.biomass", "Biomass: %s / %s");
        add("item.epca.living_armor_box.tooltip.biomass_usage", "  Hold food and boxes, press V to increase biomass");
        add("item.epca.living_armor_box.tooltip.adaptation_level", "Adaptation Counts: %s");
        add("item.epca.living_armor_box.tooltip.damage_reduction", "Damage Reduction: %s%%");
        add("item.epca.living_armor_box.tooltip.current_state", "Current State: %s");
        add("item.epca.living_armor_box.tooltip.state_equipped", "Equipped");
        add("item.epca.living_armor_box.tooltip.state_unequipped", "Unequipped");

        // Living armor
        add("item.epca.living_helmet", "Living Helmet");
        add("item.epca.living_chestplate", "Living Chestplate");
        add("item.epca.living_leggings", "Living Leggings");
        add("item.epca.living_boots", "Living Boots");

        // Other items
        add("item.epca.acid_bucket", "Acid Bucket");
        add("item.epca.infested_rubbish", "Infested Rubbish");
        add("item.epca.infested_stick", "Infested Stick");
        add("item.epca.infested_slime_ball", "Infested Slime Ball");
        add("item.epca.endless_wand", "Endless Wand");
        add("item.epca.infested_coal", "Infested Coal");
        add("item.epca.infested_raw_copper", "Infested Raw Copper");
        add("item.epca.infested_raw_iron", "Infested Raw Iron");
        add("item.epca.infested_raw_gold", "Infested Raw Gold");
        add("item.epca.infested_lapis_lazuli", "Infested Lapis Lazuli");
        add("item.epca.infested_emerald", "Infested Emerald");
        add("item.epca.infested_redstone", "Infested Redstone");
        add("item.epca.infested_diamond", "Infested Diamond");
        add("item.epca.infested_nethersea_brand_mor", "Infested Nethersea Brand Mor");
        add("item.epca.infested_nethersea_icecream", "Infested Nethersea Icecream");
        add("item.epca.epca_note", "E-PCA Note");
        add("item.epca.infested_ender_pearl", "Infested Ender Pearl");
        add("item.epca.ender_blade_scrap", "Ender Blade Scrap");
        add("item.epca.small_item_frame", "Small Item Filter Frame");

        // Blocks
        add("block.epca.infested_dirt", "Infested Dirt");
        add("block.epca.infested_sand", "Infested Sand");
        add("block.epca.infested_leaves", "Infested Leaves");
        add("block.epca.infested_flowering_leaves", "Infested Flowering Leaves");
        add("block.epca.infested_vine", "Infested Vine");
        add("block.epca.infested_log", "Infested Log");
        add("block.epca.infested_wood", "Infested Wood");
        add("block.epca.infested_stripped_log", "Infested Stripped Log");
        add("block.epca.infested_stripped_wood", "Infested Stripped Wood");
        add("block.epca.infested_planks", "Infested Planks");
        add("block.epca.infested_planks_stairs", "Infested Planks Stairs");
        add("block.epca.infested_planks_slab", "Infested Planks Slab");
        add("block.epca.infested_residue", "Infested Residue");
        add("block.epca.infested_grass", "Infested Grass");
        add("block.epca.infested_fern", "Infested Fern");
        add("block.epca.infested_sweet_berry_bush", "Infested Sweet Berry Bush");
        add("block.epca.infested_remains_small", "Small Infested Remains");
        add("block.epca.infested_remains_medium", "Medium Infested Remains");
        add("block.epca.infested_remains_large", "Large Infested Remains");
        add("block.epca.infested_stone", "Infested Stone");
        add("block.epca.infested_stone_slab", "Infested Stone Slab");
        add("block.epca.infested_stone_stairs", "Infested Stone Stairs");
        add("block.epca.infested_stone_wall", "Infested Stone Wall");
        add("block.epca.infested_cobblestone", "Infested Cobblestone");
        add("block.epca.infested_cobblestone_slab", "Infested Cobblestone Slab");
        add("block.epca.infested_cobblestone_stairs", "Infested Cobblestone Stairs");
        add("block.epca.infested_cobblestone_wall", "Infested Cobblestone Wall");
        add("block.epca.infested_stone_bricks", "Infested Stone Bricks");
        add("block.epca.infested_stone_bricks_slab", "Infested Stone Bricks Slab");
        add("block.epca.infested_stone_bricks_stairs", "Infested Stone Bricks Stairs");
        add("block.epca.infested_stone_bricks_wall", "Infested Stone Bricks Wall");
        add("block.epca.infested_cracked_stone_bricks", "Infested Cracked Stone Bricks");
        add("block.epca.infested_chiseled_stone_bricks", "Infested Chiseled Stone Bricks");
        add("block.epca.infested_polished_stone", "Infested Polished Stone");
        add("block.epca.infested_polished_stone_slab", "Infested Polished Stone Slab");
        add("block.epca.infested_polished_stone_stairs", "Infested Polished Stone Stairs");
        add("block.epca.infested_sandstone", "Infested Sandstone");
        add("block.epca.infested_sandstone_slab", "Infested Sandstone Slab");
        add("block.epca.infested_sandstone_stairs", "Infested Sandstone Stairs");
        add("block.epca.infested_sandstone_wall", "Infested Sandstone Wall");
        add("block.epca.infested_chiseled_sandstone", "Infested Chiseled Sandstone");
        add("block.epca.infested_chiseled_red_sandstone", "Infested Chiseled Red Sandstone");
        add("block.epca.infested_smooth_sandstone", "Infested Smooth Sandstone");
        add("block.epca.infested_smooth_sandstone_slab", "Infested Smooth Sandstone Slab");
        add("block.epca.infested_smooth_sandstone_stairs", "Infested Smooth Sandstone Stairs");
        add("block.epca.infested_cut_sandstone", "Infested Cut Sandstone");
        add("block.epca.infested_cut_sandstone_slab", "Infested Cut Sandstone Slab");
        add("block.epca.infested_coal_ore", "Infested Coal Ore");
        add("block.epca.infested_copper_ore", "Infested Copper Ore");
        add("block.epca.infested_iron_ore", "Infested Iron Ore");
        add("block.epca.infested_gold_ore", "Infested Gold Ore");
        add("block.epca.infested_lapis_ore", "Infested Lapis Lazuli Ore");
        add("block.epca.infested_redstone_ore", "Infested Redstone Ore");
        add("block.epca.infested_emerald_ore", "Infested Emerald Ore");
        add("block.epca.infested_diamond_ore", "Infested Diamond Ore");
        add("block.epca.infested_snow_block", "Infested Snow Block");
        add("block.epca.infested_snow", "Infested Snow");
        add("block.epca.packed_mud_pedestal", "Packed Mud Pedestal");
        add("block.epca.packed_mud_altar_stone", "Packed Mud Altar Stone");
        add("block.epca.swallow_cyst", "Swallow Cyst");
        add("block.epca.infested_infested_cobblestone", "Infested Infested Cobblestone");
        add("block.epca.infested_infested_stone", "Infested Infested Stone");
        add("block.epca.infested_infested_stone_bricks", "Infested Infested Stone Bricks");
        add("block.epca.infested_infested_cracked_stone_bricks", "Infested Infested Cracked Stone Bricks");
        add("block.epca.infested_infested_chiseled_stone_bricks", "Infested Infested Chiseled Stone Bricks");
        add("block.epca.infested_nethersea_brand_grown", "Infested Nethersea Brand Grown");
        add("block.epca.infested_nethersea_brand_solid", "Infested Nethersea Brand Solid");
        add("block.epca.infested_planks_fence", "Infested Planks Fence");
        add("block.epca.infested_pointed_dripstone", "Infested Pointed Dripstone");
        add("block.epca.beckon_core", "Beckon Core");
        add("block.epca.infested_heavy_stone", "Infested Heavy Stone");
        add("block.epca.infested_infested_heavy_stone", "Infested Infested Heavy Stone");
        add("block.epca.infested_heavy_coal_ore", "Infested Heavy Coal Ore");
        add("block.epca.infested_heavy_copper_ore", "Infested Heavy Copper Ore");
        add("block.epca.infested_heavy_iron_ore", "Infested Heavy Iron Ore");
        add("block.epca.infested_heavy_gold_ore", "Infested Heavy Gold Ore");
        add("block.epca.infested_heavy_lapis_ore", "Infested Heavy Lapis Lazuli Ore");
        add("block.epca.infested_heavy_redstone_ore", "Infested Heavy Redstone Ore");
        add("block.epca.infested_heavy_emerald_ore", "Infested Heavy Emerald Ore");
        add("block.epca.infested_heavy_diamond_ore", "Infested Heavy Diamond Ore");
        add("block.epca.infested_dustlike", "Infested Dustlike");
        add("block.epca.infested_plankslike", "Infested Plankslike");
        add("block.epca.infested_rocklike", "Infested Rocklike");
        add("block.epca.infested_metallike", "Infested Metallike");
        add("block.epca.infested_hardlike", "Infested Hardlike");
        add("block.epca.infested_heavy_cobblestone", "Infested Heavy Cobblestone");
        add("block.epca.infested_heavy_cobblestone_stairs", "Infested Heavy Cobblestone Stairs");
        add("block.epca.infested_heavy_cobblestone_slab", "Infested Heavy Cobblestone Slab");
        add("block.epca.infested_heavy_cobblestone_wall", "Infested Heavy Cobblestone Wall");
        add("block.epca.infested_chiseled_deepslate", "Infested Chiseled Deepslate");
        add("block.epca.infested_polished_heavy_stone", "Infested Polished Heavy Stone");
        add("block.epca.infested_polished_heavy_stone_stairs", "Infested Polished Heavy Stone Stairs");
        add("block.epca.infested_polished_heavy_stone_slab", "Infested Polished Heavy Stone Slab");
        add("block.epca.infested_polished_heavy_stone_wall", "Infested Polished Heavy Stone Wall");
        add("block.epca.infested_heavy_bricks", "Infested Heavy Bricks");
        add("block.epca.infested_cracked_heavy_bricks", "Infested Cracked Heavy Bricks");
        add("block.epca.infested_heavy_bricks_stairs", "Infested Heavy Bricks Stairs");
        add("block.epca.infested_heavy_bricks_slab", "Infested Heavy Bricks Slab");
        add("block.epca.infested_heavy_bricks_wall", "Infested Heavy Bricks Wall");
        add("block.epca.infested_lily_pad", "Infested Lily Pad");
        add("block.epca.infested_carved_pumpkin", "Infested Carved Pumpkin");
        add("block.epca.infested_pumpkin", "Infested Pumpkin");
        add("block.epca.infested_short_grass", "Infested Short Grass");
        add("block.epca.infested_tall_grass", "Infested Tall Grass");
        add("block.epca.infested_tall_fern", "Infested Tall Fern");
        add("block.epca.infested_cactus", "Infested Cactus");
        add("block.epca.infested_sugar_cane", "Infested Sugar Cane");
        add("block.epca.infested_spider_web", "Infested Spider Web");
        add("block.epca.infested_spider_web_blood", "Infested Blood Spider Web");
        add("block.epca.infested_cave_spider_web", "Infested Cave Spider Web");
        add("block.epca.infested_heavy_tiles", "Infested Heavy Tile");
        add("block.epca.infested_cracked_heavy_tiles", "Infested Cracked Heavy Tile");
        add("block.epca.infested_heavy_tiles_stairs", "Infested Heavy Tile Stairs");
        add("block.epca.infested_heavy_tiles_slab", "Infested Heavy Tile Slab");
        add("block.epca.infested_heavy_tiles_wall", "Infested Heavy Tile Wall");
        add("block.epca.acid_solution", "Acid");
        add("fluid_type.epca.acid_solution", "Acid");

        // Commands
        add("commands.negativedamage.damage.success", "Successfully dealt %s damage to %s using %s");
        add("commands.negativedamage.damage.success.unknown", "Successfully dealt %s damage to %s");
        add("commands.negativedamage.damage.success.simple", "Dealt %s damage to %s");
        add("commands.negativedamage.damage.failed", "Failed to damage %s using %s (entity may be immune)");
        add("commands.negativedamage.damage.failed.unknown", "Failed to damage %s (entity may be immune)");
        add("commands.negativedamage.damage.failed.simple", "Failed to damage %s (entity may be immune)");
        add("commands.negativedamage.heal.success", "Successfully dealt %s damage to %s using %s");
        add("commands.negativedamage.heal.success.unknown", "Successfully dealt %s damage to %s");
        add("commands.negativedamage.heal.success.simple", "Dealt %s damage to %s");
        add("commands.negativedamage.no_entities", "No valid living entities found");

        // World stages
        add("epca.stage.-2", "The world's barrier is intact and undamaged");
        add("epca.stage.-1", "The world's barrier is intact and undamaged");
        add("epca.stage.0", "The world's barrier has slightly loosened...");
        add("epca.stage.1", "World Barrier Integrity 90%");
        add("epca.stage.2", "World Barrier Integrity 80%");
        add("epca.stage.3", "World Barrier Integrity 70%");
        add("epca.stage.4", "World Barrier Integrity 60%");
        add("epca.stage.5", "World Barrier Integrity 50%");
        add("epca.stage.6", "World Barrier Integrity 40%");
        add("epca.stage.7", "World Barrier Integrity 30%");
        add("epca.stage.8", "World Barrier Integrity 20%");
        add("epca.stage.9", "World Barrier Integrity 10%");
        add("epca.stage.10", "World Barrier Integrity 1%");
        add("epca.stage.11", "World Barrier Integrity <1%");
        add("epca.stage.12", "World Barrier Integrity <1%");
        add("epca.stage.13", "World Barrier Completely ruined...");

        // Difficulty
        add("epca.difficulty.button", "Extra Difficulty - %s");
        add("epca.difficulty.easy", "Easy");
        add("epca.difficulty.normal", "Normal");
        add("epca.difficulty.expert", "Expert");
        add("epca.difficulty.master", "Master");
        add("epca.difficulty.custom", "Custom");
        add("epca.difficulty.legendary", "§5Legendary");

        // Notes & messages
        add("epca.note.title", "E-PCA Note");
        add("epca.message.stage_too_low", "The erosion stage here is below level 3...");

        // Bestiary contents (with images and formatting)
        add("epca.content.onesent", "§l§0Onesent§r\n$[page]$\n§0Curbug\n${img:epca:textures/gui/note_pic/curbug0.png,64}$\n§rThe image above shows a Curbug.");
        add("epca.content.test3", "\n${img:epca:textures/gui/note_pic/yawning_neko.png,512}$\nAuthor\n${img:epca:textures/gui/note_pic/xiao_ku_kmc.png,512}$\nArtist\n${img:epca:textures/gui/note_pic/thomas_lovlin.png,512}$\nSound Designer");
    }
}