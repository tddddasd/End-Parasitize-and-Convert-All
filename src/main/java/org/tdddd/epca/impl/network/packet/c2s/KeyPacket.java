package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.registry.items.item.DiseasedHeart;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedFlesh;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.ParasiteViscera;

import java.util.function.Supplier;

public class KeyPacket {

    public KeyPacket() {
        
    }

    public KeyPacket(FriendlyByteBuf buf) {
        
    }

    public void toBytes(FriendlyByteBuf buf) {
        
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            
            ServerPlayer player = context.getSender();
            if (player != null) {
                
                handleKeyAction(player);
            }
        });
        return true;
    }

    private void handleKeyAction(ServerPlayer player) {
        
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        
        if (!mainHandItem.isEmpty() && (mainHandItem.isEdible() || mainHandItem.getItem() instanceof InfestedFlesh || mainHandItem.getItem() instanceof DiseasedHeart
                || mainHandItem.getItem() instanceof ParasiteViscera) &&
                offHandItem.getItem() instanceof LivingArmorBox) {

            LivingArmorBox box = (LivingArmorBox) offHandItem.getItem();
            box.handleLeftClickInGUI(player, offHandItem, mainHandItem);
        }

        
        else if (!offHandItem.isEmpty() && (offHandItem.isEdible() || offHandItem.getItem() instanceof InfestedFlesh || offHandItem.getItem() instanceof DiseasedHeart
                || offHandItem.getItem() instanceof ParasiteViscera) &&
                mainHandItem.getItem() instanceof LivingArmorBox) {

            LivingArmorBox box = (LivingArmorBox) mainHandItem.getItem();
            box.handleLeftClickInGUI(player, mainHandItem, offHandItem);
        }
    }
}