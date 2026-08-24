package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.tdddd.epca.impl.overworld.registry.ModItems;

public class LivingArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = new int[]{858, 1056, 990, 726}; 
    private static final int[] PROTECTION_VALUES = new int[]{4, 8, 10, 4}; 

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getSlot().getIndex()];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return PROTECTION_VALUES[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.RESHAPE_SHELL.get());
    }

    @Override
    public String getName() {
        return "epca:living_armor";
    }

    @Override
    public float getToughness() {
        return 3.0F; 
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F; 
    }
}