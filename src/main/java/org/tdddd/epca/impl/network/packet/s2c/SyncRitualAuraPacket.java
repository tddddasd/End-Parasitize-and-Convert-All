package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.SacrificeRitualClientCache;

import java.util.function.Supplier;

public class SyncRitualAuraPacket {

    public static final int MAX_ENTRIES = 32;

    private final BlockPos[] centers;

    private final int[] remainingTicks;

    public SyncRitualAuraPacket(BlockPos[] centers, int[] remainingTicks) {
        this.centers = centers;
        this.remainingTicks = remainingTicks;
    }

    public void encode(FriendlyByteBuf buf) {
        int size = Math.min(this.centers.length, this.remainingTicks.length);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            buf.writeBlockPos(this.centers[i]);
            buf.writeVarInt(Math.max(0, this.remainingTicks[i]));
        }
    }

    public static SyncRitualAuraPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();

        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException(
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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SacrificeRitualClientCache.applyBatch(this.centers, this.remainingTicks));
        ctx.get().setPacketHandled(true);
    }
}
