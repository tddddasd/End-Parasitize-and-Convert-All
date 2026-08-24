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

import java.util.concurrent.CompletableFuture;

/**
 * 物品标签和实体类型标签数据生成器。
 */
public class ItemAndEntityTagsData {

    /** 物品标签 */
    public static class ItemTagsGen extends ItemTagsProvider {
        public ItemTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lp,
                           BlockTagsProvider blockTags, @javax.annotation.Nullable ExistingFileHelper efh) {
            super(output, lp, blockTags.contentsGetter(), epca.MODID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            // piglin_loved
            tag(ItemTags.PIGLIN_LOVED).add(modItem("golden_spear"), modItem("infested_gold_ore")
            , modItem("infested_heavy_gold_ore"), modItem("infested_raw_gold"));

            // forge:infested_flesh
            tag(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
                    new ResourceLocation("forge", "infested_flesh")))
                    .add(modItem("infested_flesh"), modItem("weird_minced_flesh"), modItem("diseased_heart"));

            // forge:enchanting_fuels
            tag(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),
                    new ResourceLocation("forge", "enchanting_fuels")))
                    .add(modItem("infested_lapis_lazuli"));
        }

        private Item modItem(String name) {
            return ForgeRegistries.ITEMS.getValue(new ResourceLocation(epca.MODID, name));
        }
    }

    /** 实体类型标签 */
    public static class EntityTagsGen extends EntityTypeTagsProvider {
        public EntityTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lp,
                              @javax.annotation.Nullable ExistingFileHelper efh) {
            super(output, lp, epca.MODID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(),
                    new ResourceLocation(epca.MODID, "infested_undead")))
                    .add(modEntity("infested_zombie"), modEntity("infested_husk"),
                         modEntity("infested_drowned"), modEntity("infested_zombie_villager"),
                         modEntity("infested_skeleton"),
                         modEntity("walking_zombie_head"), modEntity("walking_husk_head"),
                         modEntity("walking_drowned_head"), modEntity("walking_zombie_villager_head"),
                         modEntity("walking_skeleton_head"));
        }

        @SuppressWarnings("unchecked")
        private EntityType<?> modEntity(String name) {
            return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(epca.MODID, name));
        }
    }
}
