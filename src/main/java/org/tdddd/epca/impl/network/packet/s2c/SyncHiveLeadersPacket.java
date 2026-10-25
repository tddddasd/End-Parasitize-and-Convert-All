package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.HiveLeaderClientCache;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class SyncHiveLeadersPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncHiveLeadersPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_hive_leaders"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHiveLeadersPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncHiveLeadersPacket::encode, SyncHiveLeadersPacket::new);

    private final Set<UUID> leaders;

    public SyncHiveLeadersPacket(Set<UUID> leaders) {
        this.leaders = new HashSet<>(leaders);
    }

    public SyncHiveLeadersPacket(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        leaders = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            leaders.add(buf.readUUID());
        }
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(leaders.size());
        for (UUID uuid : leaders) {
            buf.writeUUID(uuid);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncHiveLeadersPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HiveLeaderClientCache.updateLeaders(packet.leaders));
    }
}
