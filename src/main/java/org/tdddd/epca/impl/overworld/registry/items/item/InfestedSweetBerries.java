package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedSweetBerries extends Item {
    public InfestedSweetBerries(Properties properties) {
        super(properties.food(createFoodProperties(), createConsumable()));
    }

    private static FoodProperties createFoodProperties() {

        return new FoodProperties.Builder()
                .nutrition(2)
                .saturationModifier(0.4F)
                .build();
    }

    private static Consumable createConsumable() {
        return Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.COTH,
                        10 * 20,
                        0
                ), 1.0F))
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        MobEffects.HUNGER,
                        10 * 20,
                        0
                ), 1.0F))
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        MobEffects.NAUSEA, // 26.1.2 rename of CONFUSION
                        10 * 20,
                        0
                ), 1.0F))
                .build();
    }
}