package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LivingArmorAdaptation {
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
        
        CompoundTag armorTag = armorStack.getOrCreateTag();

        
        if (!armorTag.contains("AdaptationCount")) {
            armorTag.putInt("AdaptationCount", 0);
        }

        int currentCount = armorTag.getInt("AdaptationCount");

        
        if (currentCount < MAX_ADAPTATIONS_PER_PIECE) {
            armorTag.putInt("AdaptationCount", currentCount + 1);
        }
    }

    
    public static int getAdaptationCountForArmor(ItemStack armorStack) {
        if (!armorStack.hasTag()) {
            return 0;
        }

        CompoundTag tag = armorStack.getTag();
        if (tag.contains("AdaptationCount")) {
            return tag.getInt("AdaptationCount");
        }

        return 0;
    }

    
    public static int getTotalAdaptationCount(Player player) {
        int total = 0;
        for (ItemStack armor : player.getArmorSlots()) {
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
        for (ItemStack armor : player.getArmorSlots()) {
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