package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LivingArmorAdaptation {
    private static final EquipmentSlot[] ARMOR_SLOTS =
            new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int MAX_ADAPTATIONS_PER_PIECE = 9; 
    private static final float ADAPTATION_REDUCTION_PER_STACK = 0.0125f; 
    private static final float MAX_REDUCTION = 0.45f; 

    public static float getLimitedDamage(Player player, DamageSource source, float damage) {
        
        if (isFireDamage(source)) {
            return damage;
        }

        float damageReduction = getDamageReduction(player);
        float minDamage = (100 * (1 - damageReduction)) + 1;
        float reducedDamage = damage * (1 - damageReduction);

        return Math.min(damage, Math.max(reducedDamage, minDamage));
    }

    
    public static void addAdaptation(ItemStack armorStack) {
        // 26.1.2: item NBT is replaced by the minecraft:custom_data component.
        int currentCount = getAdaptationCountForArmor(armorStack);

        if (currentCount < MAX_ADAPTATIONS_PER_PIECE) {
            int newCount = currentCount + 1;
            CustomData.update(DataComponents.CUSTOM_DATA, armorStack, tag -> tag.putInt("AdaptationCount", newCount));
        }
    }

    
    public static int getAdaptationCountForArmor(ItemStack armorStack) {
        CompoundTag tag = armorStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getIntOr("AdaptationCount", 0);
    }

    
    public static int getTotalAdaptationCount(Player player) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof LivingArmorItem) {
                total += getAdaptationCountForArmor(armor);
            }
        }
        return total;
    }

    
    public static float getDamageReduction(Player player) {
        int adaptationCount = getTotalAdaptationCount(player);
        return Math.min(adaptationCount * ADAPTATION_REDUCTION_PER_STACK, MAX_REDUCTION);
    }

    
    public static int getLivingArmorCount(Player player) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof LivingArmorItem) {
                count++;
            }
        }
        return count;
    }

    
    public static boolean isFireDamage(DamageSource source) {
        
        return source.is(DamageTypeTags.IS_FIRE);
    }
}