package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.render.ClientRaidBarriers;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: replace the client's whole set of raid barriers.
 *
 * <p>A full state replace (rather than add/remove deltas) keeps the client in sync after a restart, a
 * dimension change or an expiry without needing sequence numbers.</p>
 */
public class RaidBarrierS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RaidBarrierS2CPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("raid_barriers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidBarrierS2CPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RaidBarrierS2CPacket::encode, RaidBarrierS2CPacket::decode);

    private final List<InfestedRaidBarrierData.Barrier> barriers;

    public RaidBarrierS2CPacket(List<InfestedRaidBarrierData.Barrier> barriers) {
        this.barriers = List.copyOf(barriers);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.barriers.size());
        for (InfestedRaidBarrierData.Barrier b : this.barriers) {
            buf.writeUtf(b.dimension().toString());
            buf.writeBlockPos(b.center());
            buf.writeDouble(b.radius());
            buf.writeLong(b.startMillis());
            buf.writeLong(b.endMillis());
        }
    }

    public static RaidBarrierS2CPacket decode(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<InfestedRaidBarrierData.Barrier> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Identifier dim = Identifier.parse(buf.readUtf());
            BlockPos center = buf.readBlockPos();
            double radius = buf.readDouble();
            long start = buf.readLong();
            long end = buf.readLong();
            list.add(new InfestedRaidBarrierData.Barrier(dim, center, radius, start, end));
        }
        return new RaidBarrierS2CPacket(list);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaidBarrierS2CPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientRaidBarriers.replace(packet.barriers));
    }

    /** Pushes the current barrier set of one level to everyone in it. */
    public static void sync(ServerLevel level, InfestedRaidBarrierData data) {
        PacketDistributor.sendToPlayersInDimension(level, new RaidBarrierS2CPacket(data.all()));
    }
}
