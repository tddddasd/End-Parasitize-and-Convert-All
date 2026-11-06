package org.tdddd.epca.impl.network.packet.s2c;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.network.ModNetwork;

public class SyncSoulProtectionPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSoulProtectionPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_soul_protection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoulProtectionPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncSoulProtectionPacket::encode,
                    SyncSoulProtectionPacket::decode);

    public static final int MAX_ENTRIES = 256;

    public static final int INFINITE_DURATION = -1;

    private static final int[] EMPTY = new int[0];

    private final int[] entityIds;
    private final int[] remainingTicks;

    public SyncSoulProtectionPacket(int[] entityIds, int[] remainingTicks) {
        if (entityIds.length != remainingTicks.length) {
            throw new IllegalArgumentException("entityIds and remainingTicks must have the same length");
        }
        this.entityIds = entityIds;
        this.remainingTicks = remainingTicks;
    }

    public static SyncSoulProtectionPacket empty() {
        return new SyncSoulProtectionPacket(EMPTY, EMPTY);
    }

    public int size() {
        return this.entityIds.length;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entityIds.length);
        for (int index = 0; index < this.entityIds.length; index++) {
            buf.writeVarInt(this.entityIds[index]);
            buf.writeVarInt(this.remainingTicks[index]);
        }
    }

    public static SyncSoulProtectionPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {

            throw new DecoderException("soul protection batch size out of range: " + size);
        }
        int[] entityIds = new int[size];
        int[] remainingTicks = new int[size];
        for (int index = 0; index < size; index++) {
            entityIds[index] = buf.readVarInt();
            remainingTicks[index] = buf.readVarInt();
        }
        return new SyncSoulProtectionPacket(entityIds, remainingTicks);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncSoulProtectionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SoulProtectionClientCache.replace(packet.entityIds, packet.remainingTicks));
    }
}
