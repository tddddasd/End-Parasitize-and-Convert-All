package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class ParasiteViscera extends Item {
    public ParasiteViscera(Properties properties) {
        super(properties.food(createFoodProperties(), createConsumable()));
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(1)  
                .saturationModifier(0.0F)  
                .build();
    }

    private static Consumable createConsumable() {
        return Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.COTH,  
                        30 * 20,  
                        0  
                ), 0.95F))  
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.VIRAL,  
                        5 * 20,  
                        0  
                ), 0.2F))  
                .build();
    }
}