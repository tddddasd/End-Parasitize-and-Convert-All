package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HiveLeaderOrganTeleportPacket {

    public HiveLeaderOrganTeleportPacket() {
    }

    public HiveLeaderOrganTeleportPacket(FriendlyByteBuf buf) {
    }

    public static void encode(HiveLeaderOrganTeleportPacket msg, FriendlyByteBuf buf) {
    }

    public static HiveLeaderOrganTeleportPacket decode(FriendlyByteBuf buf) {
        return new HiveLeaderOrganTeleportPacket();
    }

    public static void handle(HiveLeaderOrganTeleportPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            HiveLeaderOrganTeleportHandler.handleRequest(sender);
        });
        context.setPacketHandled(true);
    }
}
