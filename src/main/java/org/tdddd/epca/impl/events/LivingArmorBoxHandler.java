package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.items.ILivingArmorBoxStorable;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LivingArmorBoxHandler {
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack heldStack = event.getItemStack();

        
        if (heldStack.getItem() instanceof LivingArmorBox && player.isShiftKeyDown()) {
            
            event.setCanceled(true);

            LivingArmorBox box = (LivingArmorBox) heldStack.getItem();

            
            ItemStack offhandStack = player.getOffhandItem();
            if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof ILivingArmorBoxStorable) {
                
                if (box.storeItem(heldStack, offhandStack)) {
                    offhandStack.shrink(1);
                }
            } else if (offhandStack.isEmpty()) {
                
                int storedCount = box.getStoredItemCount(heldStack);
                if (storedCount > 0) {
                    
                    ItemStack retrieved = box.retrieveItem(heldStack, 0);
                    if (!retrieved.isEmpty()) {
                        player.addItem(retrieved);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        
        if (!player.level().isClientSide()) {
            for (ItemEntity itemEntity : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0D))) {
                ItemStack itemStack = itemEntity.getItem();

                
                if (itemStack.getItem() instanceof LivingArmorBox) {
                    setBoxStateClosed(itemStack);
                }
            }
        }
    }

    
    private static void setBoxStateClosed(ItemStack boxStack) {
        if (boxStack.getItem() instanceof LivingArmorBox livingArmorBox) {
            livingArmorBox.setState(boxStack, false);
        }
    }
}