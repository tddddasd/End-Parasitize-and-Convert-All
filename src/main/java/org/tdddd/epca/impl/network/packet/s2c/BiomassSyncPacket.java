package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.BiomassClientData;


public class BiomassSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BiomassSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("biomass_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BiomassSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(BiomassSyncPacket::encode, BiomassSyncPacket::new);

    private final boolean isNestLeader;
    private final int points;

    public BiomassSyncPacket(boolean isNestLeader, int points) {
        this.isNestLeader = isNestLeader;
        this.points = points;
    }

    public BiomassSyncPacket(RegistryFriendlyByteBuf buf) {
        this.isNestLeader = buf.readBoolean();
        this.points = buf.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(isNestLeader);
        buf.writeInt(points);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BiomassSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BiomassClientData.update(packet.isNestLeader, packet.points));
    }
}
