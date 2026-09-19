package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedFlesh extends Item {
    public InfestedFlesh(Properties properties) {
        super(properties.food(createFoodProperties(), createConsumable()));
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(5)  
                .saturationModifier(0.1F)  
                .build();
    }

    private static Consumable createConsumable() {
        // 26.1.2: food effects moved from FoodProperties.Builder#effect(...) (deleted)
        // to the CONSUMABLE data component, applied on consume with the same probability.
        return Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.COTH,  
                        15 * 20,  
                        0  
                ), 0.7F))  
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        MobEffects.POISON,  
                        5 * 20,  
                        0  
                ), 0.9F))  
                .build();
    }
}