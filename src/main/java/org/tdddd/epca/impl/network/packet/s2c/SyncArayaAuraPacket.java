package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.function.Supplier;

public class SyncArayaAuraPacket {

    public static final int MAX_ENTRIES = ArayaConstants.AURA_MAX_ENTRIES;

    private final int[] entityIds;

    private final double[] positions;

    public SyncArayaAuraPacket(int[] entityIds, double[] positions) {
        this.entityIds = entityIds;
        this.positions = positions;
    }

    public void encode(FriendlyByteBuf buf) {
        int size = Math.min(this.entityIds.length, this.positions.length / 3);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            buf.writeVarInt(this.entityIds[i]);
            buf.writeDouble(this.positions[i * 3]);
            buf.writeDouble(this.positions[i * 3 + 1]);
            buf.writeDouble(this.positions[i * 3 + 2]);
        }
    }

    public static SyncArayaAuraPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();

        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException(
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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ArayaClientCache.applyHolders(this.entityIds, this.positions));
        ctx.get().setPacketHandled(true);
    }
}
