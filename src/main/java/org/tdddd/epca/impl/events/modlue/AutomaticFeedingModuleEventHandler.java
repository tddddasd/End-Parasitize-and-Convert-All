package org.tdddd.epca.impl.events.modlue;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.items.item.AutomaticFeedingModuleI;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

import java.util.List;

@Mod.EventBusSubscriber
public class AutomaticFeedingModuleEventHandler {

    private static final int FEEDING_INTERVAL = 5; 

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        
        if (player.level().isClientSide()) {
            return;
        }

        
        if (player.tickCount % FEEDING_INTERVAL != 0) {
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

        
        if (!hasAutomaticFeedingModuleI(boxStack)) {
            return;
        }

        
        FoodData foodData = player.getFoodData();
        int foodLevel = foodData.getFoodLevel();
        float saturationLevel = foodData.getSaturationLevel();

        
        if (foodLevel < 20 || saturationLevel < 20.0f) {
            
            int consumed = boxItem.consumeBiomass(boxStack, 1);

            if (consumed > 0) {
                if (foodLevel < 20) {
                    
                    foodData.eat(1, 0.0f);
                } else if (saturationLevel < 20.0f) {
                    
                    foodData.setSaturation(foodData.getSaturationLevel() + 1.0f);
                }
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

    
    private static boolean hasAutomaticFeedingModuleI(ItemStack boxStack) {
        if (!(boxStack.getItem() instanceof LivingArmorBox)) {
            return false;
        }

        LivingArmorBox box = (LivingArmorBox) boxStack.getItem();
        List<ItemStack> storedItems = box.getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof AutomaticFeedingModuleI) {
                return true;
            }
        }

        return false;
    }
}