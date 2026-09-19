package org.tdddd.epca.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.tdddd.epca.impl.epca;

import java.util.concurrent.CompletableFuture;

/**
 * 数据生成器：自动生成模组配方。
 * - 矛的合成（材料+木棍）
 * - 矛/受染原矿的回收烧炼
 * - 狱髓矛锻造升级
 *
 * <h2>26.1.2 重写</h2>
 * 1.20.1 的 {@code RecipeProvider(PackOutput)} + {@code buildRecipes(Consumer<FinishedRecipe>)}
 * 已被删除。26.1.2 的 {@link RecipeProvider} 是「配方内容」基类
 * （构造器 {@code (HolderLookup.Provider, RecipeOutput)}，抽象方法 {@code buildRecipes()}），
 * 真正的 {@code DataProvider} 是 {@link RecipeProvider.Runner} 子类，
 * 因此本类拆成「内容 + {@link Runner}」。
 * <ul>
 *   <li>{@code ShapedRecipeBuilder.shaped(...)} 新增 {@code HolderGetter<Item>}（{@code this.items}）；</li>
 *   <li>{@code save(consumer, Identifier)} → {@code save(RecipeOutput, ResourceKey<Recipe<?>>)}；</li>
 *   <li>{@code FinishedRecipe} 已删除；{@code has(...)} 仍是 {@link RecipeProvider} 的 protected 方法；</li>
 *   <li>{@code TagKey#location()} 在 26.1.2 是 record 组件（{@code location()}），用法不变。</li>
 * </ul>
 * 配方内容（图案、材料、数量、解锁条件、id）与 1.20.1 完全一致。
 */
public class RecipeProviderData extends RecipeProvider {

    public RecipeProviderData(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** 26.1.2 的 DataProvider 形态。 */
    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new RecipeProviderData(registries, output);
        }

        @Override
        public String getName() {
            return "EPCA Recipes";
        }
    }

    private static ResourceKey<Recipe<?>> recipeId(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(epca.MODID, path));
    }

    private static Item modItem(String name) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(epca.MODID, name));
    }

    @Override
    protected void buildRecipes() {
        // ── 矛合成 ──
        spearCrafting(Items.FLINT,          modItem("flint_spear"));
        spearCrafting(Items.COPPER_INGOT,   modItem("copper_spear"));
        spearCrafting(Items.IRON_INGOT,     modItem("iron_spear"));
        spearCrafting(Items.GOLD_INGOT,     modItem("golden_spear"));
        spearCrafting(Items.DIAMOND,        modItem("diamond_spear"));
        // wooden/stone use tags, not single items
        spearCraftingTag(ItemTags.PLANKS,            modItem("wooden_spear"));
        spearCraftingTag(ItemTags.STONE_CRAFTING_MATERIALS, modItem("stone_spear"));

        // ── 矛回收烧炼 ──
        // 26.1.2: 铜粒用原版 minecraft:copper_nugget（模组自己的 copper_nugget 已删除）。
        spearRecycling(modItem("copper_spear"),  Items.COPPER_NUGGET, 0.1F);
        spearRecycling(modItem("iron_spear"),    Items.IRON_NUGGET,  0.1F);
        spearRecycling(modItem("golden_spear"),  Items.GOLD_NUGGET,  0.1F);

        // ── 受染原矿回收烧炼 ──
        rawRecycling(modItem("infested_raw_copper"), Items.COPPER_NUGGET, 0.1F);
        rawRecycling(modItem("infested_raw_iron"),   Items.IRON_NUGGET,  0.1F);
        rawRecycling(modItem("infested_raw_gold"),   Items.GOLD_NUGGET,  0.1F);

        // ── 狱髓矛锻造升级 ──
        netheriteSmithing(modItem("diamond_spear"), modItem("netherite_spear"));
    }

    // ═══════════════════════ 合成 ═══════════════════════

    /** 单物品材料合成矛 pattern: [W  ] [ S ] [  S] */
    private void spearCrafting(Item material, Item result) {
        String matName = BuiltInRegistries.ITEM.getKey(material).getPath();
        String resultName = BuiltInRegistries.ITEM.getKey(result).getPath();
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.COMBAT, result)
                .pattern("W  ").pattern(" S ").pattern("  S")
                .define('W', material).define('S', Items.STICK)
                .unlockedBy("has_" + matName, has(material))
                .save(this.output, recipeId(resultName + "_crafting_shaped"));
    }

    /** 标签材料合成矛 */
    private void spearCraftingTag(TagKey<Item> materialTag, Item result) {
        String resultName = BuiltInRegistries.ITEM.getKey(result).getPath();
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.COMBAT, result)
                .pattern("W  ").pattern(" S ").pattern("  S")
                .define('W', materialTag).define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(this.output, recipeId(resultName + "_crafting_shaped"));
    }

    // ═══════════════════════ 烧炼回收 ═══════════════════════

    /** 矛熔炼回收 (smelting 200t + blasting 100t) */
    private void spearRecycling(Item spear, Item result, float xp) {
        String spearName = BuiltInRegistries.ITEM.getKey(spear).getPath();
        String group = "smelting_" + spearName;

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(spear), RecipeCategory.MISC,
                        net.minecraft.world.item.crafting.CookingBookCategory.MISC, result, xp, 200)
                .group(group)
                .unlockedBy("has_" + spearName, has(spear))
                .save(this.output, recipeId(spearName + "_smelting"));

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(spear), RecipeCategory.MISC,
                        net.minecraft.world.item.crafting.CookingBookCategory.MISC, result, xp, 100)
                .group(group)
                .unlockedBy("has_" + spearName, has(spear))
                .save(this.output, recipeId(spearName + "_blasting"));
    }

    /** 受染原矿回收 */
    private void rawRecycling(Item raw, Item result, float xp) {
        String rawName = BuiltInRegistries.ITEM.getKey(raw).getPath();
        String group = "smelting_" + rawName;

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(raw), RecipeCategory.MISC,
                        net.minecraft.world.item.crafting.CookingBookCategory.MISC, result, xp, 200)
                .group(group)
                .unlockedBy("has_" + rawName, has(raw))
                .save(this.output, recipeId(rawName + "_smelting"));

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(raw), RecipeCategory.MISC,
                        net.minecraft.world.item.crafting.CookingBookCategory.MISC, result, xp, 100)
                .group(group)
                .unlockedBy("has_" + rawName, has(raw))
                .save(this.output, recipeId(rawName + "_blasting"));
    }

    // ═══════════════════════ 锻造 ═══════════════════════

    /** 狱髓锻造台升级 */
    private void netheriteSmithing(Item base, Item result) {
        String resultName = BuiltInRegistries.ITEM.getKey(result).getPath();
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(base),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.COMBAT, result)
                .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(this.output, recipeId(resultName + "_smithing"));
    }
}
