package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedSweetBerries extends Item {
    public InfestedSweetBerries(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    private static FoodProperties createFoodProperties() {

        return new FoodProperties.Builder()
                .nutrition(2)
                .saturationMod(0.4F)
                .effect(() -> new MobEffectInstance(
                        ModEffects.COTH.get(),
                        10 * 20,
                        0
                ), 1.0F)
                .effect(() -> new MobEffectInstance(
                        MobEffects.HUNGER,
                        10 * 20,
                        0
                ), 1.0F)
                .effect(() -> new MobEffectInstance(
                        MobEffects.CONFUSION,
                        10 * 20,
                        0
                ), 1.0F)
                .build();
    }
}