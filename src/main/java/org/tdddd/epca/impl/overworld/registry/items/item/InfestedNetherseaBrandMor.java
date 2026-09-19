package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedNetherseaBrandMor extends Item {
    public InfestedNetherseaBrandMor(Properties properties) {
        super(properties.food(createFoodProperties(), createConsumable()));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    private static FoodProperties createFoodProperties() {
        
        return new FoodProperties.Builder()
                .nutrition(1)  
                .build();
    }

    private static Consumable createConsumable() {
        return Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.COTH,  
                        30 * 20,  
                        0  
                ), 0.7F))  
                .build();
    }
}