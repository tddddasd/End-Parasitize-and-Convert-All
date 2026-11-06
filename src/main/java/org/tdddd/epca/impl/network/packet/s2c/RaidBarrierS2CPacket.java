package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.client.render.ClientRaidBarriers;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RaidBarrierS2CPacket {
    private final List<InfestedRaidBarrierData.Barrier> barriers;

    public RaidBarrierS2CPacket(List<InfestedRaidBarrierData.Barrier> barriers) {
        this.barriers = List.copyOf(barriers);
    }

    public static void sendAll(ServerLevel level, InfestedRaidBarrierData data) {
        ModNetwork.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension),
                new RaidBarrierS2CPacket(data.all()));
    }

    public static void sendTo(ServerPlayer player, InfestedRaidBarrierData data) {
        ModNetwork.sendToPlayer(player, new RaidBarrierS2CPacket(data.all()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.barriers.size());
        for (InfestedRaidBarrierData.Barrier b : this.barriers) {
            buf.writeResourceLocation(b.dimension());
            buf.writeBlockPos(b.center());
            buf.writeDouble(b.radius());
            buf.writeLong(b.startMillis());
            buf.writeLong(b.endMillis());
        }
    }

    public static RaidBarrierS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<InfestedRaidBarrierData.Barrier> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation dim = buf.readResourceLocation();
            BlockPos center = buf.readBlockPos();
            double radius = buf.readDouble();
            long start = buf.readLong();
            long end = buf.readLong();
            list.add(new InfestedRaidBarrierData.Barrier(dim, center, radius, start, end));
        }
        return new RaidBarrierS2CPacket(list);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientRaidBarriers.replace(this.barriers));
        ctx.get().setPacketHandled(true);
    }
}
