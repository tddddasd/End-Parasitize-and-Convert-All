package org.tdddd.epca.impl.overworld.registry.items.item;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Wing chestplate base.
 *
 * <h2>26.1.2: {@code ArmorItem} no longer exists</h2>
 * <p>Armour became data-driven: {@code net.minecraft.world.item.ArmorItem} and its nested
 * {@code ArmorItem.Type} are deleted. The same treatment as {@code LivingArmorItem} is applied -
 * extend a plain {@link Item} and let the constructor install
 * {@code Item.Properties#humanoidArmor(ArmorMaterial, ArmorType)}, where {@link ArmorType} is the
 * replacement enum for the old {@code ArmorItem.Type}.</p>
 *
 * <h2>26.1.2: item NBT is gone</h2>
 * <p>{@code ItemStack#getOrCreateTag()} is deleted; per-stack custom data lives in the
 * {@code minecraft:custom_data} component and is written through {@link CustomData#update}.</p>
 */
public abstract class WingChestItem extends Item {
    public final WingChestManager.WingType wingType;

    public WingChestItem(ArmorMaterial material, ArmorType type, Properties properties, WingChestManager.WingType wingType) {
        super(properties.humanoidArmor(material, type));
        this.wingType = wingType;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        
        // 1.20.1: CompoundTag tag = stack.getOrCreateTag();
        //         if (!tag.contains("AdaptationCount")) tag.putInt("AdaptationCount", 0);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (!tag.contains("AdaptationCount")) {
                tag.putInt("AdaptationCount", 0);
            }
        });
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {

        Player player = event.getEntity();
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);

        
        if (chestItem.getItem() == this &&
                !WingChestManager.activeWingTypes.containsKey(player.getUUID())) {
            WingChestManager.registerWingPlayer(player, wingType);
        }
    }
}