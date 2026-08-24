package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class ParasiteViscera extends Item {
    public ParasiteViscera(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(1)  
                .saturationMod(0.0F)  
                .effect(() -> new MobEffectInstance(
                        ModEffects.COTH.get(),  
                        30 * 20,  
                        0  
                ), 0.95F)  
                .effect(() -> new MobEffectInstance(
                        ModEffects.VIRAL.get(),  
                        5 * 20,  
                        0  
                ), 0.2F)  
                .build();
    }
}