package org.tdddd.epca.impl.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;

public class AltarCraftingCategory implements IRecipeCategory<AltarCraftingRecipe> {
    public static final RecipeType<AltarCraftingRecipe> TYPE =
            RecipeType.create(epca.MODID, "altar_crafting", AltarCraftingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public AltarCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 60);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.PACKED_MUD_PEDESTAL.get()));
        ResourceLocation arrowTex = new ResourceLocation("jei", "textures/jei/atlas/gui/recipe_arrow.png");
        this.arrow = guiHelper.createDrawable(arrowTex, 0, 0, 24, 17);
    }

    @Override
    public RecipeType<AltarCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.epca.altar_crafting");
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AltarCraftingRecipe recipe, IFocusGroup focuses) {
        int x = 10, y = 20;
        for (AltarCraftingRecipe.IngredientEntry entry : recipe.getInputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStacks(entry.getDisplayStacks());
            x += 18;
            if (x > 120) { x = 10; y += 18; }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 140, 20)
                .addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(AltarCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 118, 22);
    }
}