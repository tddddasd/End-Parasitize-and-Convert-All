package org.tdddd.epca.impl.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.tdddd.epca.impl.epca;

import java.util.*;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(epca.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new AltarCraftingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
        List<CraftingRecipe> recipes = manager.getAllRecipesFor(RecipeType.CRAFTING);
        List<AltarCraftingRecipe> wrappers = new ArrayList<>();

        for (CraftingRecipe recipe : recipes) {
            if (recipe.isSpecial()) continue;
            Map<Ingredient, Integer> counts = new LinkedHashMap<>();
            for (Ingredient ing : recipe.getIngredients()) {
                if (!ing.isEmpty())
                    counts.put(ing, counts.getOrDefault(ing, 0) + 1);
            }
            if (counts.isEmpty()) continue;

            List<AltarCraftingRecipe.IngredientEntry> entries = new ArrayList<>();
            for (Map.Entry<Ingredient, Integer> e : counts.entrySet())
                entries.add(new AltarCraftingRecipe.IngredientEntry(e.getKey(), e.getValue()));

            ItemStack output = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
            if (!output.isEmpty())
                wrappers.add(new AltarCraftingRecipe(entries, output));
        }

        registration.addRecipes(AltarCraftingCategory.TYPE, wrappers);
    }
}