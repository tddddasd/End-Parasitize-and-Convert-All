package org.tdddd.epca.impl.network.packet.s2c;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * Server to client batch sync of the sacrifice rituals near the receiving player.
 *
 * <p>The ritual itself lives server side ({@code BlockConversionManager.SacrificeTask}); the client
 * only needs to know <em>where</em> one is running and <em>how much longer</em> it will run, because
 * the aura is drawn until the lightning falls ({@code completeSacrifice}) and then fades out. Vanilla
 * sends no such thing, so the server pushes this batch every
 * {@code BlockConversionManager.RITUAL_SYNC_INTERVAL_TICKS} ticks to every player within
 * {@code BlockConversionManager.RITUAL_SYNC_RADIUS} blocks of a ritual.</p>
 *
 * <h2>Payload</h2>
 * <p>A count followed by that many {@code (BlockPos, remainingDurationTicks)} pairs. The batch is the
 * complete set the receiving player may know about, so the client replaces its whole cache with it: a
 * missing entry therefore means the ritual finished (or was cancelled) and starts the fade-out, and a
 * vanished entry needs no separate packet.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>1.20.1 was a {@code SimpleChannel} message with {@code encode}/{@code decode}/{@code handle}
 * taking a {@code FriendlyByteBuf} and a {@code Supplier<NetworkEvent.Context>}. 26.1.2 registers
 * payloads instead, so this is a {@link CustomPacketPayload} with a {@link StreamCodec} and a static
 * {@code handle(packet, IPayloadContext)}; the payload id is the twin of the 1.20.1 channel name, and
 * the wire format (varint count + packed {@code BlockPos} + varint remaining ticks) is identical.</p>
 */
public class SyncRitualAuraPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRitualAuraPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_ritual_aura"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRitualAuraPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncRitualAuraPacket::encode, SyncRitualAuraPacket::decode);

    /** Hard cap on how many entries one batch may carry, on both the sending and the receiving side. */
    public static final int MAX_ENTRIES = 32;

    /** Ritual centres of the batch. */
    private final BlockPos[] centers;

    /** Remaining duration in ticks per entry, or 0 when it is already finishing. */
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
        // Checked before the arrays are allocated, so a malformed count cannot blow up the client.
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
