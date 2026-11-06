package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HiveLeaderDecomposeParasitePacket {

    public HiveLeaderDecomposeParasitePacket() {
    }

    public HiveLeaderDecomposeParasitePacket(FriendlyByteBuf buf) {
    }

    public static void encode(HiveLeaderDecomposeParasitePacket msg, FriendlyByteBuf buf) {
    }

    public static HiveLeaderDecomposeParasitePacket decode(FriendlyByteBuf buf) {
        return new HiveLeaderDecomposeParasitePacket();
    }

    public static void handle(HiveLeaderDecomposeParasitePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            HiveLeaderDecomposeParasiteHandler.handleRequest(sender);
        });
        context.setPacketHandled(true);
    }
}
