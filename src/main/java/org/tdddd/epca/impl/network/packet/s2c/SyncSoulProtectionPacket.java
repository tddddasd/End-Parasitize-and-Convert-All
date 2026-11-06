package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;

import java.util.function.Supplier;

public class SyncSoulProtectionPacket {

    public static final int MAX_ENTRIES = 256;

    public static final int INFINITE_DURATION = -1;

    private final int[] entityIds;

    private final int[] remainingTicks;

    public SyncSoulProtectionPacket(int[] entityIds, int[] remainingTicks) {
        this.entityIds = entityIds;
        this.remainingTicks = remainingTicks;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityIds.length);
        for (int i = 0; i < this.entityIds.length; i++) {
            buf.writeVarInt(this.entityIds[i]);
            buf.writeVarInt(this.remainingTicks[i]);
        }
    }

    public static SyncSoulProtectionPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();

        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException(
                    "SyncSoulProtectionPacket: " + size + " entries exceeds " + MAX_ENTRIES);
        }
        int[] entityIds = new int[size];
        int[] remainingTicks = new int[size];
        for (int i = 0; i < size; i++) {
            entityIds[i] = buf.readVarInt();
            remainingTicks[i] = buf.readVarInt();
        }
        return new SyncSoulProtectionPacket(entityIds, remainingTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                SoulProtectionClientCache.applyBatch(this.entityIds, this.remainingTicks));
        ctx.get().setPacketHandled(true);
    }
}
