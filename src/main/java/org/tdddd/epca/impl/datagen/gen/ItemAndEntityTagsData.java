package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModTags;

import java.util.concurrent.CompletableFuture;

public class ItemAndEntityTagsData {
    public static class ItemTagsGen extends ItemTagsProvider {
        public ItemTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lp,
                           BlockTagsProvider blockTags, @javax.annotation.Nullable ExistingFileHelper efh) {
            super(output, lp, blockTags.contentsGetter(), epca.MODID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {

            tag(ItemTags.PIGLIN_LOVED).add(modItem("golden_spear"), modItem("infested_gold_ore")
            , modItem("infested_heavy_gold_ore"), modItem("infested_raw_gold"));

            tag(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
                    new ResourceLocation("forge", "infested_flesh")))
                    .add(modItem("infested_flesh"), modItem("weird_minced_flesh"), modItem("diseased_heart"));

            tag(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
                    new ResourceLocation("forge", "enchanting_fuels")))
                    .add(modItem("infested_lapis_lazuli"));

            tag(ModTags.ORGAN_PART).add(

                    modItem("infested_flesh"),

                    modItem("diseased_heart"),

                    modItem("infested_bone"),

                    modItem("parasite_viscera"),

                    modItem("weird_minced_flesh"),

                    modItem("infested_slime_ball"),

                    modItem("reshape_flesh"),
                    modItem("twisted_bone"),
                    modItem("tight_tendons"),
                    modItem("gasbag_debris"),
                    modItem("reshape_shell"),

                    modItem("infested_ender_pearl"),
                    modItem("ender_blade_scrap"),

                    modItem("fins_fin"),

                    modItem("beckon_membrane"));
        }

        private Item modItem(String name) {
            return ForgeRegistries.ITEMS.getValue(new ResourceLocation(epca.MODID, name));
        }
    }

    public static class EntityTagsGen extends EntityTypeTagsProvider {
        public EntityTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lp,
                              @javax.annotation.Nullable ExistingFileHelper efh) {
            super(output, lp, epca.MODID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {

            tag(ModTags.INFESTED)
                    .add(modEntity("infested_bat"), modEntity("infested_chicken"), modEntity("infested_cow"),
                         modEntity("infested_drowned"), modEntity("infested_enderman"),
                         modEntity("infested_endermite"), modEntity("infested_fox"), modEntity("infested_husk"),
                         modEntity("infested_pig"), modEntity("infested_pillager"), modEntity("infested_sheep"),
                         modEntity("infested_silverfish"), modEntity("infested_skeleton"),
                         modEntity("infested_slime_size0"), modEntity("infested_slime_size1"),
                         modEntity("infested_slime_size3"), modEntity("infested_spider"),
                         modEntity("infested_villager"), modEntity("infested_vindicator"), modEntity("infested_wolf"),
                         modEntity("infested_zombie"), modEntity("infested_zombie_villager"),
                         modEntity("walking_chicken_head"), modEntity("walking_cow_head"),
                         modEntity("walking_drowned_head"), modEntity("walking_enderman_head"),
                         modEntity("walking_fox_head"), modEntity("walking_husk_head"), modEntity("walking_pig_head"),
                         modEntity("walking_pillager_head"), modEntity("walking_sheep_head"),
                         modEntity("walking_skeleton_head"), modEntity("walking_spider_head"),
                         modEntity("walking_villager_head"), modEntity("walking_vindicator_head"),
                         modEntity("walking_wolf_head"), modEntity("walking_zombie_head"),
                         modEntity("walking_zombie_villager_head"));

            tag(ModTags.ONESENT)
                    .add(modEntity("curbug"), modEntity("fins"), modEntity("flying_carrier"),
                         modEntity("light_carrier"), modEntity("mozzie"), modEntity("ripper"));

            tag(ModTags.POVERTY)
                    .add(modEntity("large_incomplete_form"), modEntity("living_flesh_size0"),
                         modEntity("living_flesh_size1"), modEntity("living_flesh_size2"),
                         modEntity("living_flesh_size3"), modEntity("living_flesh_size4"),
                         modEntity("medium_incomplete_form"), modEntity("small_incomplete_form"));

            tag(ModTags.RESHAPE)
                    .add(modEntity("reshape_longarms"), modEntity("reshape_yelloweye"));

            tag(ModTags.LINK)
                    .add(modEntity("stage_i_beckon"), modEntity("stage_ii_beckon"));

            tag(ModTags.PARASITE)
                    .add(modEntity("bio_tort_incarnation"), modEntity("biomass_medium"), modEntity("biomass_small"),
                         modEntity("curbug"), modEntity("fins"), modEntity("flying_carrier"),
                         modEntity("infested_bat"), modEntity("infested_chicken"), modEntity("infested_cow"),
                         modEntity("infested_drowned"), modEntity("infested_enderman"),
                         modEntity("infested_endermite"), modEntity("infested_fox"), modEntity("infested_husk"),
                         modEntity("infested_pig"), modEntity("infested_pillager"), modEntity("infested_sheep"),
                         modEntity("infested_silverfish"), modEntity("infested_skeleton"),
                         modEntity("infested_slime_size0"), modEntity("infested_slime_size1"),
                         modEntity("infested_slime_size3"), modEntity("infested_spider"),
                         modEntity("infested_villager"), modEntity("infested_vindicator"), modEntity("infested_wolf"),
                         modEntity("infested_zombie"), modEntity("infested_zombie_villager"),
                         modEntity("large_incomplete_form"), modEntity("light_carrier"),
                         modEntity("living_flesh_size0"), modEntity("living_flesh_size1"),
                         modEntity("living_flesh_size2"), modEntity("living_flesh_size3"),
                         modEntity("living_flesh_size4"), modEntity("medium_incomplete_form"), modEntity("mozzie"),
                         modEntity("reshape_longarms"), modEntity("reshape_yelloweye"), modEntity("ripper"),
                         modEntity("small_incomplete_form"), modEntity("stage_i_beckon"), modEntity("stage_ii_beckon"),
                         modEntity("walking_chicken_head"), modEntity("walking_cow_head"),
                         modEntity("walking_drowned_head"), modEntity("walking_enderman_head"),
                         modEntity("walking_fox_head"), modEntity("walking_husk_head"), modEntity("walking_pig_head"),
                         modEntity("walking_pillager_head"), modEntity("walking_sheep_head"),
                         modEntity("walking_skeleton_head"), modEntity("walking_spider_head"),
                         modEntity("walking_villager_head"), modEntity("walking_vindicator_head"),
                         modEntity("walking_wolf_head"), modEntity("walking_zombie_head"),
                         modEntity("walking_zombie_villager_head"), modEntity("yawning_nya"));
        }

        @SuppressWarnings("unchecked")
        private EntityType<?> modEntity(String name) {
            return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(epca.MODID, name));
        }
    }
}
