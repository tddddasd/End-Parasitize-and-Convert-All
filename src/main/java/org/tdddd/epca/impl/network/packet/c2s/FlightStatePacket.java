package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.registry.items.item.WingChestManager;

import java.util.UUID;


public class FlightStatePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FlightStatePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("flight_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlightStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec(FlightStatePacket::toBytes, FlightStatePacket::new);

    private final boolean flying;
    private final UUID playerId;

    public FlightStatePacket(UUID playerId, boolean flying) {
        this.playerId = playerId;
        this.flying = flying;
    }

    public FlightStatePacket(RegistryFriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.flying = buf.readBoolean();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(flying);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlightStatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sender) {
                
                ServerLevel level = sender.level();
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(packet.playerId);

                if (player != null) {
                    WingChestManager.syncFlightState(player, packet.flying);
                }
            }
        });
    }
}
