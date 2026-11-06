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

public abstract class WingChestItem extends Item {
    public final WingChestManager.WingType wingType;

    public WingChestItem(ArmorMaterial material, ArmorType type, Properties properties, WingChestManager.WingType wingType) {
        super(properties.humanoidArmor(material, type));
        this.wingType = wingType;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);

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
