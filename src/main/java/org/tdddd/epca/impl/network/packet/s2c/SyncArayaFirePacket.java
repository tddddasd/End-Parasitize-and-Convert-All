package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.function.Supplier;

/**
 * Server -&gt; client batch sync of the render-only fire blocks the aura keeps alive.
 *
 * <p>The server decides where and when, exactly as the request asks ("服务端权威"): it rolls the fire
 * field around each holder, picks the positions, and reports each block's lifetime and height, which the
 * client only draws. The client never invents a position, never extends a lifetime and never spawns a
 * fire of its own, so a blocked or lagging client cannot drift from the server's picture.</p>
 *
 * <h2>Payload</h2>
 * <p>A count, the absolute game tick the whole field was rolled at, and then that many entries of
 * {@code (blockPos, lifetimeTicks, height)}. The roll tick is sent once per batch instead of per entry,
 * because a field is always rolled as a whole; it is what makes every client put the same fire out at
 * the same instant, even one that connects in the middle of a wave. The height travels as a byte in
 * 1/255-block steps, which is finer than a pixel at any sane distance and keeps the packet small.</p>
 */
public class SyncArayaFirePacket {

    /** Hard cap on how many fire blocks one batch may carry, on the sending and receiving side. */
    public static final int MAX_ENTRIES = 256;

    private final long rolledAtTick;
    private final int[] xs;
    private final int[] ys;
    private final int[] zs;
    private final int[] lifetimes;
    private final float[] heights;

    public SyncArayaFirePacket(long rolledAtTick, int[] xs, int[] ys, int[] zs, int[] lifetimes,
                               float[] heights) {
        this.rolledAtTick = rolledAtTick;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.lifetimes = lifetimes;
        this.heights = heights;
    }

    public void encode(FriendlyByteBuf buf) {
        int size = Math.min(Math.min(this.xs.length, this.ys.length), Math.min(this.zs.length,
                this.lifetimes.length));
        size = Math.min(size, this.heights.length);
        size = Math.min(size, MAX_ENTRIES);
        buf.writeVarInt(size);
        buf.writeVarLong(this.rolledAtTick);
        for (int i = 0; i < size; i++) {
            buf.writeBlockPos(new net.minecraft.core.BlockPos(this.xs[i], this.ys[i], this.zs[i]));
            buf.writeVarInt(Math.max(1, this.lifetimes[i]));
            // 1/255-block steps: the client multiplies by the same factor.
            int height = Math.round(this.heights[i] * 255.0F);
            buf.writeByte(Math.max(0, Math.min(255, height)));
        }
    }

    /** Height decode factor of the byte written by {@link #encode}. */
    public static final float HEIGHT_SCALE = 255.0F;

    public static SyncArayaFirePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        // Checked before the arrays are allocated, so a malformed count cannot blow up the client.
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException(
                    "SyncArayaFirePacket: " + size + " entries exceeds " + MAX_ENTRIES);
        }
        long rolledAtTick = buf.readVarLong();
        int[] xs = new int[size];
        int[] ys = new int[size];
        int[] zs = new int[size];
        int[] lifetimes = new int[size];
        float[] heights = new float[size];
        for (int i = 0; i < size; i++) {
            net.minecraft.core.BlockPos pos = buf.readBlockPos();
            xs[i] = pos.getX();
            ys[i] = pos.getY();
            zs[i] = pos.getZ();
            lifetimes[i] = buf.readVarInt();
            heights[i] = (buf.readByte() & 0xFF) / HEIGHT_SCALE;
        }
        return new SyncArayaFirePacket(rolledAtTick, xs, ys, zs, lifetimes, heights);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ArayaClientCache.applyFires(this.xs, this.ys, this.zs,
                this.rolledAtTick, this.lifetimes, this.heights));
        ctx.get().setPacketHandled(true);
    }
}
