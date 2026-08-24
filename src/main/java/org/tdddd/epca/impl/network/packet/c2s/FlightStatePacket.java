package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.registry.items.item.WingChestManager;

import java.util.UUID;
import java.util.function.Supplier;

public class FlightStatePacket {
    private final boolean flying;
    private final UUID playerId;

    public FlightStatePacket(UUID playerId, boolean flying) {
        this.playerId = playerId;
        this.flying = flying;
    }

    public FlightStatePacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.flying = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(flying);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                ServerLevel level = ctx.getSender().serverLevel();
                ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(playerId);

                if (player != null) {
                    WingChestManager.syncFlightState(player, flying);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}