package org.tdddd.epca.impl.events.modlue;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;
import org.tdddd.epca.impl.overworld.registry.items.item.NetheriteModuleI;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class NetheriteModuleEventHandler {

    private static final UUID MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("a1b2c3da-e5f6-7890-abcd-ef1234567893");
    private static final UUID ARMOR_MODIFIER_ID = UUID.fromString("a1b2c3da-e5f6-7890-abcd-ef1234567894");

    private static final String MOVEMENT_SPEED_MODIFIER_NAME = "NetheriteMovementSpeedReduction";
    private static final String ARMOR_MODIFIER_NAME = "NetheriteArmorBonus";

    
    private static final int CHECK_INTERVAL = 10;

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
            removeAttributeModifiers(player);
            return;
        }

        
        ItemStack boxStack = findLivingArmorBox(player);
        if (boxStack.isEmpty()) {
            removeAttributeModifiers(player);
            return;
        }

        LivingArmorBox boxItem = (LivingArmorBox) boxStack.getItem();

        
        if (!boxItem.getState(boxStack)) {
            removeAttributeModifiers(player);
            return;
        }

        
        if (!hasNetheriteModuleI(boxStack)) {
            removeAttributeModifiers(player);
            return;
        }

        
        applyAttributeModifiers(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        
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

        
        if (!hasNetheriteModuleI(boxStack)) {
            return;
        }

        
        DamageSource source = event.getSource();
        if (isFireDamage(source)) {
            
            float reducedDamage = event.getAmount() / 4.0f;
            event.setAmount(reducedDamage);
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

    
    private static boolean hasNetheriteModuleI(ItemStack boxStack) {
        if (!(boxStack.getItem() instanceof LivingArmorBox)) {
            return false;
        }

        LivingArmorBox box = (LivingArmorBox) boxStack.getItem();
        List<ItemStack> storedItems = box.getStoredItems(boxStack);

        for (ItemStack storedItem : storedItems) {
            if (storedItem.getItem() instanceof NetheriteModuleI) {
                return true;
            }
        }

        return false;
    }

    
    private static boolean isFireDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    
    private static void applyAttributeModifiers(Player player) {
        
        removeAttributeModifiers(player);

        
        AttributeModifier speedModifier = new AttributeModifier(
                MOVEMENT_SPEED_MODIFIER_ID,
                MOVEMENT_SPEED_MODIFIER_NAME,
                -0.01,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        
        AttributeModifier armorModifier = new AttributeModifier(
                ARMOR_MODIFIER_ID,
                ARMOR_MODIFIER_NAME,
                2.0,
                AttributeModifier.Operation.ADDITION
        );

        
        if (!player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(speedModifier)) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(speedModifier);
        }

        if (!player.getAttribute(Attributes.ARMOR).hasModifier(armorModifier)) {
            player.getAttribute(Attributes.ARMOR).addTransientModifier(armorModifier);
        }
    }

    
    private static void removeAttributeModifiers(Player player) {
        if (player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(MOVEMENT_SPEED_MODIFIER_ID) != null) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MOVEMENT_SPEED_MODIFIER_ID);
        }

        if (player.getAttribute(Attributes.ARMOR).getModifier(ARMOR_MODIFIER_ID) != null) {
            player.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_ID);
        }
    }
}