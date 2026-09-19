package org.tdddd.epca.impl.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorAdaptation;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

@EventBusSubscriber(modid = epca.MODID)
public class LivingArmorEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        
        if (event.getEntity() instanceof Player player) {
            
            int livingArmorCount = LivingArmorAdaptation.getLivingArmorCount(player);
            if (livingArmorCount > 0) {
                DamageSource source = event.getSource();
                float originalDamage = event.getAmount();

                
                if (LivingArmorAdaptation.isFireDamage(source)) {
                    
                    float amplifiedDamage = originalDamage * 4.0f;
                    event.setAmount(amplifiedDamage);

                    
                    return;
                }

                
                
                float limitedDamage = getLimitedDamageWithMaxReduction(player, originalDamage);

                
                if (limitedDamage != originalDamage) {
                    event.setAmount(limitedDamage);
                }

                
                addAdaptationToAllLivingArmor(player);
            }
        }
    }

    
    private static float getLimitedDamageWithMaxReduction(Player player, float originalDamage) {
        
        float maxReduction = LivingArmorBox.MAX_ADAPTATIONS_PER_PIECE * LivingArmorBox.ADAPTATION_REDUCTION_PER_STACK;

        
        float reducedDamage = originalDamage * (1 - maxReduction);

        
        return Math.max(reducedDamage, 0);
    }

    
    private static void addAdaptationToAllLivingArmor(Player player) {
        
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armorStack = player.getItemBySlot(slot);
            
            if (armorStack.getItem() instanceof LivingArmorItem) {
                
                LivingArmorAdaptation.addAdaptation(armorStack);
            }
        }
    }
}