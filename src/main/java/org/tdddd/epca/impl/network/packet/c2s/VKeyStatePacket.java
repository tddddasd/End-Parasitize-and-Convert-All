package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VKeyStatePacket {
    private final boolean pressed;

    public VKeyStatePacket(boolean pressed) {
        this.pressed = pressed;
    }

    public VKeyStatePacket(FriendlyByteBuf buf) {
        this.pressed = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(pressed);
    }

    public static void handle(VKeyStatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getPersistentData().putBoolean("VKeyPressed", packet.pressed);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}