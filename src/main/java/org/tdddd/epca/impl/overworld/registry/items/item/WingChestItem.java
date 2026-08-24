package org.tdddd.epca.impl.overworld.registry.items.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public abstract class WingChestItem extends ArmorItem {
    public final WingChestManager.WingType wingType;

    public WingChestItem(ArmorMaterial material, Type type, Properties properties, WingChestManager.WingType wingType) {
        super(material, type, properties);
        this.wingType = wingType;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("AdaptationCount")) {
            tag.putInt("AdaptationCount", 0);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Player player = event.player;
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);

        
        if (chestItem.getItem() == this &&
                !WingChestManager.activeWingTypes.containsKey(player.getUUID())) {
            WingChestManager.registerWingPlayer(player, wingType);
        }
    }
}