package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.tdddd.epca.impl.overworld.registry.items.item.CopperSpear;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedRawCopper;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Redirect(
            method = "burn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/Recipe;assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack redirectAssemble(Recipe<Container> recipe, Container container, RegistryAccess registryAccess) {
        
        ItemStack original = recipe.assemble(container, registryAccess);

        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        Level level = furnace.getLevel();

        if (level != null && !level.isClientSide) {
            ItemStack input = furnace.getItem(0);

            
            if (input.getItem() instanceof InfestedRawCopper || input.getItem() instanceof CopperSpear) {
                
                TagKey<Item> copperNuggetTag = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "copper_nugget"));

                
                var itemLookup = registryAccess.lookup(Registries.ITEM);
                if (itemLookup.isPresent()) {
                    var tag = itemLookup.get().get(copperNuggetTag);
                    if (tag.isPresent()) {
                        
                        var firstItem = tag.get().stream().findFirst();
                        if (firstItem.isPresent()) {
                            Item copperNugget = firstItem.get().value();
                            
                            ItemStack result = new ItemStack(copperNugget, original.getCount());
                            return result;
                        }
                    }
                }

                
                if (level.random.nextFloat() < 0.9f) {
                    ItemStack modified = original.copy();
                    modified.shrink(1);
                    return modified;
                }
                
                return original;
            }
        }

        
        return original;
    }
}