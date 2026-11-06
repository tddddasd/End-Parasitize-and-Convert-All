package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.function.Supplier;

public class SyncArayaFirePacket {

    public static final int MAX_ENTRIES = 256;

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

    public void encode(FriendlyByteBuf buf) {
        int size = Math.min(Math.min(this.xs.length, this.ys.length), Math.min(this.zs.length,
                this.lifetimes.length));
        size = Math.min(size, this.heights.length);
        size = Math.min(size, this.bornAgoTicks.length);
        size = Math.min(size, MAX_ENTRIES);
        buf.writeVarInt(size);
        buf.writeVarLong(this.rolledAtTick);
        for (int i = 0; i < size; i++) {
            buf.writeBlockPos(new net.minecraft.core.BlockPos(this.xs[i], this.ys[i], this.zs[i]));
            buf.writeVarInt(Math.max(1, this.lifetimes[i]));

            int height = Math.round(this.heights[i] * 255.0F);
            buf.writeByte(Math.max(0, Math.min(255, height)));

            buf.writeVarInt((int) Math.min(Integer.MAX_VALUE, Math.max(0L, this.bornAgoTicks[i])));
        }
    }

    public static final float HEIGHT_SCALE = 255.0F;

    public static SyncArayaFirePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();

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
        long[] bornAgoTicks = new long[size];
        for (int i = 0; i < size; i++) {
            net.minecraft.core.BlockPos pos = buf.readBlockPos();
            xs[i] = pos.getX();
            ys[i] = pos.getY();
            zs[i] = pos.getZ();
            lifetimes[i] = buf.readVarInt();
            heights[i] = (buf.readByte() & 0xFF) / HEIGHT_SCALE;
            bornAgoTicks[i] = buf.readVarInt();
        }
        return new SyncArayaFirePacket(rolledAtTick, bornAgoTicks, xs, ys, zs, lifetimes, heights);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ArayaClientCache.applyFires(this.xs, this.ys, this.zs,
                this.rolledAtTick, this.bornAgoTicks, this.lifetimes, this.heights));
        ctx.get().setPacketHandled(true);
    }
}
