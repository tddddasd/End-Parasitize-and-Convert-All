package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.registry.items.item.SmallItemFrame;

import java.util.function.Supplier;

public class SmallItemFrameDataPacket {
    private final InteractionHand hand;
    private final String data;

    public SmallItemFrameDataPacket(InteractionHand hand, String data) {
        this.hand = hand;
        this.data = data;
    }

    public static void encode(SmallItemFrameDataPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeUtf(msg.data);
    }

    public static SmallItemFrameDataPacket decode(FriendlyByteBuf buf) {
        return new SmallItemFrameDataPacket(
                buf.readEnum(InteractionHand.class),
                buf.readUtf()
        );
    }

    public static void handle(SmallItemFrameDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(msg.hand);
                if (stack.getItem() instanceof SmallItemFrame) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putString("item_data", msg.data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}