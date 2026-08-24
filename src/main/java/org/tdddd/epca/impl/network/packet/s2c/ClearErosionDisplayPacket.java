package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.ClientErosionData;

import java.util.UUID;
import java.util.function.Supplier;


public class ClearErosionDisplayPacket {
    private final UUID playerUUID;

    public ClearErosionDisplayPacket(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public static void encode(ClearErosionDisplayPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerUUID);
    }

    public static ClearErosionDisplayPacket decode(FriendlyByteBuf buffer) {
        UUID playerUUID = buffer.readUUID();
        return new ClearErosionDisplayPacket(playerUUID);
    }

    public static void handle(ClearErosionDisplayPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            
            if (context.getDirection().getReceptionSide().isClient()) {
                ClientErosionData.clearErosionData(packet.playerUUID);
            }
        });
        context.setPacketHandled(true);
    }

    public UUID getPlayerUUID() { return playerUUID; }
}