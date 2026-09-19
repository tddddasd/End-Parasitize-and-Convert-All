package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedNetherseaIcecream extends Item {
    public InfestedNetherseaIcecream(Properties properties) {
        super(properties.food(createFoodProperties(), createConsumable()));
    }

    private static FoodProperties createFoodProperties() {

        return new FoodProperties.Builder()
                .nutrition(5)
                .saturationModifier(0.8F)
                .alwaysEdible() // 26.1.2 rename of alwaysEat()
                .build();
    }

    private static Consumable createConsumable() {
        return Consumable.builder()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        ModEffects.COTH,
                        15 * 20,
                        0
                ), 1.0F))
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
                        MobEffects.RESISTANCE, // 26.1.2 rename of DAMAGE_RESISTANCE
                        10 * 20,
                        1
                ), 1.0F))
                .build();
    }
}