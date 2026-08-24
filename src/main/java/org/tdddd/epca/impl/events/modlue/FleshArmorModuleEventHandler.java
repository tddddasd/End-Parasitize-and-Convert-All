package org.tdddd.epca.impl.events.modlue;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.items.item.FleshArmorModuleI;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class FleshArmorModuleEventHandler {

    private static final UUID FLESH_ARMOR_HEALTH_MODIFIER_ID = UUID.fromString("a1b2c3dd-e5f6-7890-abcd-ef1234567890");
    private static final String MODIFIER_NAME = "FleshArmorHealthBonus";

    
    private static final int CHECK_INTERVAL = 10;

    
    private static final Map<UUID, Double> playerArmorStatsCache = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        
        if (player.level().isClientSide()) {
            return;
        }

        
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        
        if (!isWearingFullLivingArmor(player)) {
            removeHealthModifier(player);
            playerArmorStatsCache.remove(player.getUUID());
            return;
        }

        
        ItemStack boxStack = findLivingArmorBox(player);
        if (boxStack.isEmpty()) {
            removeHealthModifier(player);
            playerArmorStatsCache.remove(player.getUUID());
            return;
        }

        LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();

        
        if (!boxItem.getState(boxStack)) {
            removeHealthModifier(player);
            playerArmorStatsCache.remove(player.getUUID());
            return;
        }

        
        if (!hasFleshArmorModuleI(boxStack)) {
            removeHealthModifier(player);
            playerArmorStatsCache.remove(player.getUUID());
            return;
        }

        
        double currentTotalStats = calculateTotalArmorStats(player);

        
        UUID playerUUID = player.getUUID();
        Double previousTotalStats = playerArmorStatsCache.get(playerUUID);

        
        if (previousTotalStats == null || Math.abs(currentTotalStats - previousTotalStats) > 0.01) {
            
            playerArmorStatsCache.put(playerUUID, currentTotalStats);

            
            applyHealthModifier(player, currentTotalStats);
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

    
    private static boolean hasFleshArmorModuleI(ItemStack boxStack) {
        if (!(boxStack.getItem() instanceof LivingArmorBox)) {
            return false;
        }

        LivingArmorBox box = (LivingArmorBox) boxStack.getItem();
        List<ItemStack> storedItems = box.getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof FleshArmorModuleI) {
                return true;
            }
        }

        return false;
    }

    
    private static double calculateTotalArmorStats(Player player) {
        
        double totalArmor = player.getAttributeValue(Attributes.ARMOR);

        
        double totalToughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        
        return totalArmor + totalToughness;
    }

    
    private static void applyHealthModifier(Player player, double totalStats) {
        
        float currentHealth = player.getHealth();
        float maxHealthBefore = player.getMaxHealth();
        float healthRatio = currentHealth / maxHealthBefore;

        
        double healthBonus = totalStats / 2.0;

        
        removeHealthModifier(player);

        
        if (healthBonus > 0) {
            AttributeModifier healthModifier = new AttributeModifier(
                    FLESH_ARMOR_HEALTH_MODIFIER_ID,
                    MODIFIER_NAME,
                    healthBonus,
                    AttributeModifier.Operation.ADDITION
            );

            if (!player.getAttribute(Attributes.MAX_HEALTH).hasModifier(healthModifier)) {
                player.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(healthModifier);

                
                float maxHealthAfter = player.getMaxHealth();

                
                float newHealth = Math.min(healthRatio * maxHealthAfter, maxHealthAfter);

                
                player.setHealth(newHealth);
            }
        }
    }

    
    private static void removeHealthModifier(Player player) {
        if (player.getAttribute(Attributes.MAX_HEALTH).getModifier(FLESH_ARMOR_HEALTH_MODIFIER_ID) != null) {
            
            float currentHealth = player.getHealth();
            float maxHealthBefore = player.getMaxHealth();
            float healthRatio = currentHealth / maxHealthBefore;

            
            player.getAttribute(Attributes.MAX_HEALTH).removeModifier(FLESH_ARMOR_HEALTH_MODIFIER_ID);

            
            float maxHealthAfter = player.getMaxHealth();

            
            float newHealth = Math.min(healthRatio * maxHealthAfter, maxHealthAfter);

            
            player.setHealth(newHealth);
        }
    }
}