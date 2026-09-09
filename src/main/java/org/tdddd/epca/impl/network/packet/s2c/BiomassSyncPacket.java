package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;

import java.util.function.Supplier;

public class BiomassSyncPacket {
    private final boolean isNestLeader;
    private final int points;

    public BiomassSyncPacket(boolean isNestLeader, int points) {
        this.isNestLeader = isNestLeader;
        this.points = points;
    }

    public BiomassSyncPacket(FriendlyByteBuf buf) {
        this.isNestLeader = buf.readBoolean();
        this.points = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isNestLeader);
        buf.writeInt(points);
    }

    public static void handle(BiomassSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> BiomassClientData.update(packet.isNestLeader, packet.points));
        ctx.get().setPacketHandled(true);
    }
}