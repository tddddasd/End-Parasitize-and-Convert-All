package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class ReshapeFlesh extends Item {
    public ReshapeFlesh(Properties properties) {
        super(properties.food(createFoodProperties(), createConsumable()));
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(7)  
                .saturationModifier(0.2F)  
                .build();
    }

    private static Consumable createConsumable() {
        return Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.COTH,  
                        15 * 20,  
                        0  
                ), 0.6F))  
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        MobEffects.POISON,  
                        5 * 20,  
                        0  
                ), 0.9F))  
                .build();
    }
}