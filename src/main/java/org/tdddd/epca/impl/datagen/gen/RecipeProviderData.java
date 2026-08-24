package org.tdddd.epca.impl.datagen.gen;

import com.google.gson.JsonObject;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.epca;

import java.util.function.Consumer;

/**
 * 数据生成器：自动生成模组配方。
 * - 矛的合成（材料+木棍）
 * - 矛/受染原矿的回收烧炼
 * - 狱髓矛锻造升级
 */
public class RecipeProviderData extends RecipeProvider {
    public RecipeProviderData(PackOutput output) { super(output); }

    private static ResourceLocation modId(String path) {
        return new ResourceLocation(epca.MODID, path);
    }
    private static Item modItem(String name) {
        return ForgeRegistries.ITEMS.getValue(modId(name));
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> c) {
        // ── 矛合成 ──
        spearCrafting(c, Items.FLINT,          modItem("flint_spear"));
        spearCrafting(c, Items.COPPER_INGOT,   modItem("copper_spear"));
        spearCrafting(c, Items.IRON_INGOT,     modItem("iron_spear"));
        spearCrafting(c, Items.GOLD_INGOT,     modItem("golden_spear"));
        spearCrafting(c, Items.DIAMOND,        modItem("diamond_spear"));
        // wooden/stone use tags, not single items
        spearCraftingTag(c, ItemTags.PLANKS,            modItem("wooden_spear"));
        spearCraftingTag(c, ItemTags.STONE_CRAFTING_MATERIALS, modItem("stone_spear"));

        // ── 矛回收烧炼 ──
        spearRecycling(c, modItem("copper_spear"),  Items.COPPER_INGOT, 0.7F);
        spearRecycling(c, modItem("iron_spear"),    Items.IRON_NUGGET,  0.1F);
        spearRecycling(c, modItem("golden_spear"),  Items.GOLD_NUGGET,  0.1F);

        // ── 受染原矿回收烧炼 ──
        rawRecycling(c, modItem("infested_raw_copper"), Items.COPPER_INGOT, 0.7F);
        rawRecycling(c, modItem("infested_raw_iron"),   Items.IRON_NUGGET,  0.1F);
        rawRecycling(c, modItem("infested_raw_gold"),   Items.GOLD_NUGGET,  0.1F);

        // ── 狱髓矛锻造升级 ──
        netheriteSmithing(c, modItem("diamond_spear"), modItem("netherite_spear"));
    }

    // ═══════════════════════ 合成 ═══════════════════════

    /** 单物品材料合成矛 pattern: [W  ] [ S ] [  S] */
    private void spearCrafting(Consumer<FinishedRecipe> c, Item material, Item result) {
        String matName = ForgeRegistries.ITEMS.getKey(material).getPath();
        String resultName = ForgeRegistries.ITEMS.getKey(result).getPath();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("W  ").pattern(" S ").pattern("  S")
                .define('W', material).define('S', Items.STICK)
                .unlockedBy("has_" + matName, has(material))
                .save(c, modId(resultName + "_crafting_shaped"));
    }

    /** 标签材料合成矛 */
    private void spearCraftingTag(Consumer<FinishedRecipe> c, TagKey<Item> materialTag, Item result) {
        String tagPath = materialTag.location().getPath();
        String resultName = ForgeRegistries.ITEMS.getKey(result).getPath();
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("W  ").pattern(" S ").pattern("  S")
                .define('W', materialTag).define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(c, modId(resultName + "_crafting_shaped"));
    }

    // ═══════════════════════ 烧炼回收 ═══════════════════════

    /** 矛熔炼回收 (smelting 200t + blasting 100t) */
    private void spearRecycling(Consumer<FinishedRecipe> c, Item spear, Item result, float xp) {
        String spearName = ForgeRegistries.ITEMS.getKey(spear).getPath();
        String resultName = ForgeRegistries.ITEMS.getKey(result).getPath();
        String group = "smelting_" + spearName;

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(spear), RecipeCategory.MISC, result, xp, 200)
                .group(group)
                .unlockedBy("has_" + spearName, has(spear))
                .save(c, modId(spearName + "_smelting"));

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(spear), RecipeCategory.MISC, result, xp, 100)
                .group(group)
                .unlockedBy("has_" + spearName, has(spear))
                .save(c, modId(spearName + "_blasting"));
    }

    /** 受染原矿回收 */
    private void rawRecycling(Consumer<FinishedRecipe> c, Item raw, Item result, float xp) {
        String rawName = ForgeRegistries.ITEMS.getKey(raw).getPath();
        String resultName = ForgeRegistries.ITEMS.getKey(result).getPath();
        String group = "smelting_" + rawName;

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(raw), RecipeCategory.MISC, result, xp, 200)
                .group(group)
                .unlockedBy("has_" + rawName, has(raw))
                .save(c, modId(rawName + "_smelting"));

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(raw), RecipeCategory.MISC, result, xp, 100)
                .group(group)
                .unlockedBy("has_" + rawName, has(raw))
                .save(c, modId(rawName + "_blasting"));
    }

    // ═══════════════════════ 锻造 ═══════════════════════

    /** 狱髓锻造台升级 */
    private void netheriteSmithing(Consumer<FinishedRecipe> c, Item base, Item result) {
        String resultName = ForgeRegistries.ITEMS.getKey(result).getPath();
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(base),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.COMBAT, result)
                .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                .save(c, modId(resultName + "_smithing"));
    }
}
