package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.CompletableFuture;


public class ItemAndEntityTagsData {

    
    public static class ItemTagsGen extends ItemTagsProvider {
        public ItemTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lp) {
            super(output, lp, epca.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            // piglin_loved
            tag(net.minecraft.tags.ItemTags.PIGLIN_LOVED)
                    .add(modItem("golden_spear"), modItem("infested_gold_ore"),
                            modItem("infested_heavy_gold_ore"), modItem("infested_raw_gold"));

            
            tag(TagKey.create(BuiltInRegistries.ITEM.key(),
                    Identifier.fromNamespaceAndPath(epca.MODID, "infested_flesh")))
                    .add(modItem("infested_flesh"), modItem("weird_minced_flesh"), modItem("diseased_heart"));

            
            tag(TagKey.create(BuiltInRegistries.ITEM.key(),
                    Identifier.fromNamespaceAndPath("neoforge", "enchanting_fuels")))
                    .add(modItem("infested_lapis_lazuli"));

            // epca:organ_part GUI
            //  entity  data/epca/loot_table/entities/*.json
            tag(org.tdddd.epca.impl.overworld.registry.ModTags.ORGAN_PART).add(
                    //  infested_* / incomplete_form
                    modItem("infested_flesh"),
                    // infested_cow/drowned/husk/pig/fox/pillager/sheep/villager/vindicator/wolf/zombie/zombie_villager
                    modItem("diseased_heart"),
                    // infested_skeleton / medium_incomplete_form / large_incomplete_form
                    modItem("infested_bone"),
                    // fins / mozzie / ripper
                    modItem("parasite_viscera"),
                    // small / medium / large incomplete_form
                    modItem("weird_minced_flesh"),
                    // infested_slime_size0
                    modItem("infested_slime_ball"),
                    // reshape_longarms / reshape_yelloweye
                    modItem("reshape_flesh"),
                    modItem("twisted_bone"),
                    modItem("tight_tendons"),
                    modItem("gasbag_debris"),
                    modItem("reshape_shell"),
                    //  / infested_enderman
                    modItem("infested_ender_pearl"),
                    modItem("ender_blade_scrap"),
                    // fins
                    modItem("fins_fin"),
                    // stage_i_beckon / stage_ii_beckon
                    modItem("beckon_membrane"));
        }

        private Item modItem(String name) {
            return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(epca.MODID, name));
        }
    }

    
    public static class EntityTagsGen extends EntityTypeTagsProvider {
        public EntityTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lp) {
            super(output, lp, epca.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(),
                    Identifier.fromNamespaceAndPath(epca.MODID, "infested_undead")))
                    .add(modEntity("infested_zombie"), modEntity("infested_husk"),
                         modEntity("infested_drowned"), modEntity("infested_zombie_villager"),
                         modEntity("infested_skeleton"),
                         modEntity("walking_zombie_head"), modEntity("walking_husk_head"),
                         modEntity("walking_drowned_head"), modEntity("walking_zombie_villager_head"),
                         modEntity("walking_skeleton_head"));
        }

        @SuppressWarnings("unchecked")
        private EntityType<?> modEntity(String name) {
            return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath(epca.MODID, name));
        }
    }
}

