package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedNetherseaBrandMor extends Item {
    public InfestedNetherseaBrandMor(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    public int getUseDuration(ItemStack itemstack) {
        return 24;
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(1)  
                .effect(() -> new MobEffectInstance(
                        ModEffects.COTH.get(),  
                        30 * 20,  
                        0  
                ), 0.7F)  
                .build();
    }
}