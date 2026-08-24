package org.tdddd.epca.impl.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.List;

public class AltarCraftingRecipe {
    private final List<IngredientEntry> inputs;
    private final ItemStack output;

    public AltarCraftingRecipe(List<IngredientEntry> inputs, ItemStack output) {
        this.inputs = inputs;
        this.output = output;
    }

    public List<IngredientEntry> getInputs() { return inputs; }
    public ItemStack getOutput() { return output; }

    public static class IngredientEntry {
        private final List<ItemStack> displayStacks;
        private final int count;

        public IngredientEntry(Ingredient ingredient, int count) {
            this.count = count;
            this.displayStacks = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems()) {
                ItemStack copy = stack.copy();
                copy.setCount(count);
                displayStacks.add(copy);
            }
        }

        public List<ItemStack> getDisplayStacks() {
            return displayStacks;
        }

        public int getCount() {
            return count;
        }
    }
}