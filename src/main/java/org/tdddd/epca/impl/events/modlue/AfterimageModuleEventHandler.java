package org.tdddd.epca.impl.events.modlue;

import org.tdddd.epca.impl.epca;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.registry.items.item.AfterimageModule;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorItem;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public class AfterimageModuleEventHandler {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(epca.MODID, "afterimage_speed");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
            SPEED_MODIFIER_ID,
            0.15, // 15%
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    
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
            if (storedItem.getItem() instanceof AfterimageModule) {
                return true;
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

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

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        boolean hasModifier = speedAttr.getModifier(SPEED_MODIFIER_ID) != null;

        if (!hasModifier) {
            speedAttr.addTransientModifier(SPEED_MODIFIER);
        } else {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    // 间接伤害免疫
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

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

        DamageSource source = event.getSource();
        if (source.getDirectEntity() instanceof Projectile || !source.isDirect()) {
            if (player.getRandom().nextDouble() < 0.3) {
                event.setAmount(0);
            }
        }
    }
}