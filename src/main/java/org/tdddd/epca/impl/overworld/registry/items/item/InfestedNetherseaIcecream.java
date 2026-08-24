package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class InfestedNetherseaIcecream extends Item {
    public InfestedNetherseaIcecream(Properties properties) {
        super(properties.food(createFoodProperties()));
    }

    private static FoodProperties createFoodProperties() {

        return new FoodProperties.Builder()
                .nutrition(5)
                .saturationMod(0.8F)
                .effect(() -> new MobEffectInstance(
                        ModEffects.COTH.get(),
                        15 * 20,
                        0
                ), 1.0F)
                .effect(() -> new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        10 * 20,
                        1
                ), 1.0F)
                .alwaysEat()
                .build();
    }
}