package org.tdddd.epca.impl.network.packet.s2c;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * Server to client batch sync of the render-only fire blocks the aura keeps alive.
 *
 * <p>The server decides where and when: it rolls the fire field around each holder, picks the positions,
 * and reports each block's lifetime and height, which the client only draws. The client never invents a
 * position, never extends a lifetime and never spawns a fire of its own, so a lagging client cannot drift
 * from the server's picture.</p>
 *
 * <h2>Payload</h2>
 * <p>A count, the absolute game tick the whole field was rolled at, and then that many entries of
 * {@code (blockPos, lifetimeTicks, height)}. The roll tick is sent once per batch instead of per entry,
 * because a field is always rolled as a whole; it is what makes every client put the same fire out at the
 * same instant, even one that connects in the middle of a wave. The height travels as a byte in
 * 1/255-block steps, which is finer than a pixel at any sane distance and keeps the packet small.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <p>A {@link CustomPacketPayload} with a {@link StreamCodec} instead of a {@code SimpleChannel} message;
 * the wire format is byte-for-byte the same.</p>
 */
public class SyncArayaFirePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncArayaFirePacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_araya_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncArayaFirePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncArayaFirePacket::encode, SyncArayaFirePacket::decode);

    /** Hard cap on how many fire blocks one batch may carry, on the sending and receiving side. */
    public static final int MAX_ENTRIES = 256;

    /** Height decode factor of the byte written by {@link #encode}. */
    public static final float HEIGHT_SCALE = 255.0F;

    private final long rolledAtTick;
    private final long[] bornAgoTicks;
    private final int[] xs;
    private final int[] ys;
    private final int[] zs;
    private final int[] lifetimes;
    private final float[] heights;

    public SyncArayaFirePacket(long rolledAtTick, long[] bornAgoTicks, int[] xs, int[] ys, int[] zs,
                               int[] lifetimes, float[] heights) {
        this.rolledAtTick = rolledAtTick;
        this.bornAgoTicks = bornAgoTicks;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.lifetimes = lifetimes;
        this.heights = heights;
    }

    /** Ticks this entry was born before the batch's {@link #rolledAtTick}; zero for a fresh roll. */
    public long[] bornAgoTicks() {
        return this.bornAgoTicks;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        int size = Math.min(Math.min(this.xs.length, this.ys.length),
                Math.min(this.zs.length, this.lifetimes.length));
        size = Math.min(size, this.heights.length);
        size = Math.min(size, this.bornAgoTicks.length);
        size = Math.min(size, MAX_ENTRIES);
        buf.writeVarInt(size);
        buf.writeVarLong(this.rolledAtTick);
        for (int i = 0; i < size; i++) {
            buf.writeBlockPos(new BlockPos(this.xs[i], this.ys[i], this.zs[i]));
            buf.writeVarInt(Math.max(1, this.lifetimes[i]));
            int height = Math.round(this.heights[i] * HEIGHT_SCALE);
            buf.writeByte(Math.max(0, Math.min(255, height)));
            buf.writeVarInt((int) Math.min(Integer.MAX_VALUE, Math.max(0L, this.bornAgoTicks[i])));
        }
    }

    public static SyncArayaFirePacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        // Checked before the arrays are allocated, so a malformed count cannot blow up the client.
        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException(
                    "SyncArayaFirePacket: " + size + " entries exceeds " + MAX_ENTRIES);
        }
        long rolledAtTick = buf.readVarLong();
        int[] xs = new int[size];
        int[] ys = new int[size];
        int[] zs = new int[size];
        int[] lifetimes = new int[size];
        float[] heights = new float[size];
        long[] bornAgoTicks = new long[size];
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            xs[i] = pos.getX();
            ys[i] = pos.getY();
            zs[i] = pos.getZ();
            lifetimes[i] = buf.readVarInt();
            heights[i] = (buf.readByte() & 0xFF) / HEIGHT_SCALE;
            bornAgoTicks[i] = buf.readVarInt();
        }
        return new SyncArayaFirePacket(rolledAtTick, bornAgoTicks, xs, ys, zs, lifetimes, heights);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncArayaFirePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ArayaClientCache.applyFires(packet.xs, packet.ys, packet.zs,
                packet.rolledAtTick, packet.bornAgoTicks, packet.lifetimes, packet.heights));
    }
}
