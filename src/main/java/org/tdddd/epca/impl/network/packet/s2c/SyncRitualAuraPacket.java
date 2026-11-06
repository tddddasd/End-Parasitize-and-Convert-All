package org.tdddd.epca.impl.network.packet.s2c;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;
import org.tdddd.epca.impl.network.ModNetwork;

public class SyncRitualAuraPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRitualAuraPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_ritual_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRitualAuraPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncRitualAuraPacket::encode, SyncRitualAuraPacket::decode);

    public static final int MAX_ENTRIES = 32;

    private final BlockPos[] centers;

    private final int[] remainingTicks;

    public SyncRitualAuraPacket(BlockPos[] centers, int[] remainingTicks) {
        this.centers = centers;
        this.remainingTicks = remainingTicks;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        int size = Math.min(this.centers.length, this.remainingTicks.length);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            buf.writeBlockPos(this.centers[i]);
            buf.writeVarInt(Math.max(0, this.remainingTicks[i]));
        }
    }

    public static SyncRitualAuraPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();

        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException(
                    "SyncRitualAuraPacket: " + size + " entries exceeds " + MAX_ENTRIES);
        }
        BlockPos[] centers = new BlockPos[size];
        int[] remainingTicks = new int[size];
        for (int i = 0; i < size; i++) {
            centers[i] = buf.readBlockPos();
            remainingTicks[i] = buf.readVarInt();
        }
        return new SyncRitualAuraPacket(centers, remainingTicks);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncRitualAuraPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SacrificeRitualClientCache.applyBatch(packet.centers, packet.remainingTicks));
    }
}
