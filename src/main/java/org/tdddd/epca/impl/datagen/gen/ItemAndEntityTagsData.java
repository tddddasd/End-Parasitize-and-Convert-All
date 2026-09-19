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

/**
 * 物品标签和实体类型标签数据生成器。
 *
 * <p><b>26.1.2 改动</b>
 * <ul>
 *   <li>三个 Provider 的构造器都去掉了 {@code ExistingFileHelper}（已删除）；
 *       {@code ItemTagsProvider} 也不再需要 blockTags 的 contentsGetter。</li>
 *   <li>注册表字段改为单数：{@code BuiltInRegistries.ITEMS} → {@code ITEM}、
 *       {@code ENTITY_TYPES} → {@code ENTITY_TYPE}（26.1.2 统一改成单数注册表名）。</li>
 *   <li>{@code ForgeRegistries} → {@code BuiltInRegistries}；
 *       {@code tag(...).add(...)} 用法不变。</li>
 *   <li>原 {@code data/forge/tags/**} 的命名空间已不存在。两个自定义标签分别改名：
 *       {@code forge:infested_flesh} → {@code epca:infested_flesh}（列出的全是本模组物品，
 *       是模组私有标签；资源侧的 {@code src/main/resources/data/epca/tags/item/infested_flesh.json}
 *       与 {@code data/epca/recipe/shaped/erosion_clock_crafting_shaped.json} 已经用这个名字），
 *       {@code forge:enchanting_fuels} → {@code neoforge:enchanting_fuels}
 *       （NeoForge 26.1.2 自带 {@code data/neoforge/tags/item/enchanting_fuels.json}，
 *       内容为 {@code #c:gems/lapis}；不带 {@code "replace": true} 写入同路径即为「扩展该标签」，
 *       这是 NeoForge 期望的用法）。</li>
 *   <li>输出目录是<b>单数</b>注册表名（{@code data/<ns>/tags/item/}、
 *       {@code tags/block/}、{@code tags/entity_type/}），由
 *       {@code PackOutput.createRegistryTagsPathProvider} 决定，Provider 侧无需改动。</li>
 * </ul>
 */
public class ItemAndEntityTagsData {

    /** 物品标签 */
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

            // epca:infested_flesh —— 模组私有标签（原 forge:infested_flesh）
            tag(TagKey.create(BuiltInRegistries.ITEM.key(),
                    Identifier.fromNamespaceAndPath(epca.MODID, "infested_flesh")))
                    .add(modItem("infested_flesh"), modItem("weird_minced_flesh"), modItem("diseased_heart"));

            // neoforge:enchanting_fuels —— 扩展 NeoForge 自带标签（原 forge:enchanting_fuels）
            tag(TagKey.create(BuiltInRegistries.ITEM.key(),
                    Identifier.fromNamespaceAndPath("neoforge", "enchanting_fuels")))
                    .add(modItem("infested_lapis_lazuli"));
        }

        private Item modItem(String name) {
            return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(epca.MODID, name));
        }
    }

    /** 实体类型标签 */
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
