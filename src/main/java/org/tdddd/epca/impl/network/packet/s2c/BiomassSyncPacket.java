package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;

import java.util.function.Supplier;

public class BiomassSyncPacket {
    private final boolean isHiveLeader;
    private final int points;

    public BiomassSyncPacket(boolean isHiveLeader, int points) {
        this.isHiveLeader = isHiveLeader;
        this.points = points;
    }

    public BiomassSyncPacket(FriendlyByteBuf buf) {
        this.isHiveLeader = buf.readBoolean();
        this.points = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isHiveLeader);
        buf.writeInt(points);
    }

    public static void handle(BiomassSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> BiomassClientData.update(packet.isHiveLeader, packet.points));
        ctx.get().setPacketHandled(true);
    }
}