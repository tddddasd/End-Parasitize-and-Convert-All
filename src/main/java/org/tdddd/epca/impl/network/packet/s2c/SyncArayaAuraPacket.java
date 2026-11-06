package org.tdddd.epca.impl.network.packet.s2c;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;
import org.tdddd.epca.impl.network.ModNetwork;

public class SyncArayaAuraPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncArayaAuraPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_araya_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncArayaAuraPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncArayaAuraPacket::encode, SyncArayaAuraPacket::decode);

    public static final int MAX_ENTRIES = ArayaConstants.AURA_MAX_ENTRIES;

    private final int[] entityIds;

    private final double[] positions;

    public SyncArayaAuraPacket(int[] entityIds, double[] positions) {
        this.entityIds = entityIds;
        this.positions = positions;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        int size = Math.min(this.entityIds.length, this.positions.length / 3);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            buf.writeVarInt(this.entityIds[i]);
            buf.writeDouble(this.positions[i * 3]);
            buf.writeDouble(this.positions[i * 3 + 1]);
            buf.writeDouble(this.positions[i * 3 + 2]);
        }
    }

    public static SyncArayaAuraPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();

        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException(
                    "SyncArayaAuraPacket: " + size + " entries exceeds " + MAX_ENTRIES);
        }
        int[] entityIds = new int[size];
        double[] positions = new double[size * 3];
        for (int i = 0; i < size; i++) {
            entityIds[i] = buf.readVarInt();
            positions[i * 3] = buf.readDouble();
            positions[i * 3 + 1] = buf.readDouble();
            positions[i * 3 + 2] = buf.readDouble();
        }
        return new SyncArayaAuraPacket(entityIds, positions);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncArayaAuraPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ArayaClientCache.applyHolders(packet.entityIds, packet.positions));
    }
}
