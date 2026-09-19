package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.overworld.data.NestLeaderClientCache;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端 → 客户端：巢穴领袖 UUID 集合。
 *
 * <p><b>26.1.2 改动</b>：{@code SimpleChannel} → {@link CustomPacketPayload}。
 * <b>线上字段与顺序不变</b>：{@code int size} + 依次 {@code uuid}。
 */
public class SyncNestLeadersPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNestLeadersPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_nest_leaders"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNestLeadersPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncNestLeadersPacket::encode, SyncNestLeadersPacket::new);

    private final Set<UUID> leaders;

    public SyncNestLeadersPacket(Set<UUID> leaders) {
        this.leaders = new HashSet<>(leaders);
    }

    public SyncNestLeadersPacket(RegistryFriendlyByteBuf buf) {
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

    public static void handle(SyncNestLeadersPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> NestLeaderClientCache.updateLeaders(packet.leaders));
    }
}
