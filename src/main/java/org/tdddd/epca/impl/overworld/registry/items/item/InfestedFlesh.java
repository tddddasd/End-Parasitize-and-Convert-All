package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedFlesh extends Item {
    public InfestedFlesh(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(5)  
                .saturationMod(0.1F)  
                .effect(() -> new MobEffectInstance(
                        ModEffects.COTH.get(),  
                        15 * 20,  
                        0  
                ), 0.7F)  
                .effect(() -> new MobEffectInstance(
                        MobEffects.POISON,  
                        5 * 20,  
                        0  
                ), 0.9F)  
                .build();
    }
}