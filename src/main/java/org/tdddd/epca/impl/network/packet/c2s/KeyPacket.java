package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.registry.items.item.DiseasedHeart;
import org.tdddd.epca.impl.overworld.registry.items.item.InfestedFlesh;
import org.tdddd.epca.impl.overworld.registry.items.item.LivingArmorBox;
import org.tdddd.epca.impl.overworld.registry.items.item.ParasiteViscera;


public class KeyPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeyPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("key_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyPacket> STREAM_CODEC =
            CustomPacketPayload.codec(KeyPacket::toBytes, KeyPacket::new);

    public KeyPacket() {

    }

    public KeyPacket(RegistryFriendlyByteBuf buf) {

    }

    public void toBytes(RegistryFriendlyByteBuf buf) {

    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KeyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                packet.handleKeyAction(player);
            }
        });
    }

    
    private static boolean isEdible(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null;
    }

    private void handleKeyAction(ServerPlayer player) {

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        if (!mainHandItem.isEmpty() && (isEdible(mainHandItem) || mainHandItem.getItem() instanceof InfestedFlesh || mainHandItem.getItem() instanceof DiseasedHeart
                || mainHandItem.getItem() instanceof ParasiteViscera) &&
                offHandItem.getItem() instanceof LivingArmorBox) {

            LivingArmorBox box = (LivingArmorBox) offHandItem.getItem();
            box.handleLeftClickInGUI(player, offHandItem, mainHandItem);
        }

        else if (!offHandItem.isEmpty() && (isEdible(offHandItem) || offHandItem.getItem() instanceof InfestedFlesh || offHandItem.getItem() instanceof DiseasedHeart
                || offHandItem.getItem() instanceof ParasiteViscera) &&
                mainHandItem.getItem() instanceof LivingArmorBox) {

            LivingArmorBox box = (LivingArmorBox) mainHandItem.getItem();
            box.handleLeftClickInGUI(player, mainHandItem, offHandItem);
        }
    }
}
