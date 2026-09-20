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


public class RecipeProviderData extends RecipeProvider {

    public RecipeProviderData(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    
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
        
        spearCrafting(Items.FLINT,          modItem("flint_spear"));
        spearCrafting(Items.COPPER_INGOT,   modItem("copper_spear"));
        spearCrafting(Items.IRON_INGOT,     modItem("iron_spear"));
        spearCrafting(Items.GOLD_INGOT,     modItem("golden_spear"));
        spearCrafting(Items.DIAMOND,        modItem("diamond_spear"));
        // wooden/stone use tags, not single items
        spearCraftingTag(ItemTags.PLANKS,            modItem("wooden_spear"));
        spearCraftingTag(ItemTags.STONE_CRAFTING_MATERIALS, modItem("stone_spear"));

        
        
        spearRecycling(modItem("copper_spear"),  Items.COPPER_NUGGET, 0.1F);
        spearRecycling(modItem("iron_spear"),    Items.IRON_NUGGET,  0.1F);
        spearRecycling(modItem("golden_spear"),  Items.GOLD_NUGGET,  0.1F);

        
        rawRecycling(modItem("infested_raw_copper"), Items.COPPER_NUGGET, 0.1F);
        rawRecycling(modItem("infested_raw_iron"),   Items.IRON_NUGGET,  0.1F);
        rawRecycling(modItem("infested_raw_gold"),   Items.GOLD_NUGGET,  0.1F);

        
        netheriteSmithing(modItem("diamond_spear"), modItem("netherite_spear"));
    }

    

    
    private void spearCrafting(Item material, Item result) {
        String matName = BuiltInRegistries.ITEM.getKey(material).getPath();
        String resultName = BuiltInRegistries.ITEM.getKey(result).getPath();
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.COMBAT, result)
                .pattern("W  ").pattern(" S ").pattern("  S")
                .define('W', material).define('S', Items.STICK)
                .unlockedBy("has_" + matName, has(material))
                .save(this.output, recipeId(resultName + "_crafting_shaped"));
    }

    
    private void spearCraftingTag(TagKey<Item> materialTag, Item result) {
        String resultName = BuiltInRegistries.ITEM.getKey(result).getPath();
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.COMBAT, result)
                .pattern("W  ").pattern(" S ").pattern("  S")
                .define('W', materialTag).define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(this.output, recipeId(resultName + "_crafting_shaped"));
    }

    

    
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
