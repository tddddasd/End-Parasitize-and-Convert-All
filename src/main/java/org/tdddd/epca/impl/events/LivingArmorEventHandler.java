package org.tdddd.epca.impl.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorAdaptation;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LivingArmorEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        
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
        
        for (ItemStack armorStack : player.getArmorSlots()) {
            
            if (armorStack.getItem() instanceof LivingArmorItem) {
                
                LivingArmorAdaptation.addAdaptation(armorStack);
            }
        }
    }
}