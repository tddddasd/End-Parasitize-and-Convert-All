package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class WeirdMincedFlesh extends Item {

    public WeirdMincedFlesh(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(6)  
                .saturationMod(0.4F)  
                .effect(() -> new MobEffectInstance(
                        ModEffects.COTH.get(),  
                        15 * 20,  
                        0  
                ), 0.3F)  
                .build();
    }
}