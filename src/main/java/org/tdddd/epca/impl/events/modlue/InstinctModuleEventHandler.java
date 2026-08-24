package org.tdddd.epca.impl.events.modlue;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.items.item.InstinctModuleI;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class InstinctModuleEventHandler {

    private static final Random RANDOM = new Random();
    private static final double TRIGGER_CHANCE = 0.25; 
    private static final int EFFECT_DURATION = 300; 
    private static final int MAX_AMPLIFIER = 4; 

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        
        if (event.getSource().getEntity() instanceof Player player) {
            
            if (player.level().isClientSide()) {
                return;
            }

            
            if (!isWearingFullLivingArmor(player)) {
                return;
            }

            
            ItemStack boxStack = findLivingArmorBox(player);
            if (boxStack.isEmpty()) {
                return;
            }

            LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();

            
            if (!boxItem.getState(boxStack)) {
                return;
            }

            
            if (!hasInstinctModuleI(boxStack)) {
                return;
            }

            
            if (RANDOM.nextDouble() < TRIGGER_CHANCE) {
                applyRageEffect(player);
            }
        }
    }

    
    private static boolean isWearingFullLivingArmor(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        return isLivingArmor(helmet) &&
                isLivingArmor(chestplate) &&
                isLivingArmor(leggings) &&
                isLivingArmor(boots);
    }

    
    private static boolean isLivingArmor(ItemStack stack) {
        return stack.getItem() instanceof LivingArmorItem;
    }

    
    private static ItemStack findLivingArmorBox(Player player) {
        
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.getItem() instanceof LivingArmorBox) {
            return mainHand;
        }
        if (offHand.getItem() instanceof LivingArmorBox) {
            return offHand;
        }

        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof LivingArmorBox) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    
    private static boolean hasInstinctModuleI(ItemStack boxStack) {
        if (!(boxStack.getItem() instanceof LivingArmorBox)) {
            return false;
        }

        LivingArmorBox box = (LivingArmorBox) boxStack.getItem();
        List<ItemStack> storedItems = box.getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof InstinctModuleI) {
                return true;
            }
        }

        return false;
    }

    
    private static void applyRageEffect(Player player) {
        MobEffectInstance currentRage = player.getEffect(ModEffects.RAGE.get());

        if (currentRage == null) {
            
            player.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), EFFECT_DURATION, 0, false, true, true));
        } else {
            
            if (RANDOM.nextDouble() < TRIGGER_CHANCE) {
                int currentLevel = currentRage.getAmplifier();
                if (currentLevel < MAX_AMPLIFIER) {
                    
                    player.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), EFFECT_DURATION, currentLevel + 1, false, true, true));
                } else {
                    
                    player.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), EFFECT_DURATION, MAX_AMPLIFIER, false, true, true));
                }
            } else {
                
                player.addEffect(new MobEffectInstance(ModEffects.RAGE.get(), EFFECT_DURATION, currentRage.getAmplifier(), false, true, true));
            }
        }
    }
}