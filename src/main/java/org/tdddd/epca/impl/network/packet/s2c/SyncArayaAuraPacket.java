package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.ArayaClientCache;
import org.tdddd.epca.impl.events.ArayaConstants;

import java.util.function.Supplier;

/**
 * Server -&gt; client batch sync of the players whose Alayavijnana staff reached its 天杀 threshold.
 *
 * <p>The server owns the counter (it is what the kills are counted on) and therefore also owns who has
 * an active aura; the client only needs to know <em>who</em> and <em>where</em>, because the aura decides
 * two purely local things: whether the BGM loops, and which fire field to show around whom. Vanilla has
 * no packet for "this player's held item has a big number in it", so the aura sweep in
 * {@code ArayaSyncHandler} pushes one of these batches to every player in the level every
 * {@link ArayaConstants#AURA_SYNC_INTERVAL_TICKS} ticks.</p>
 *
 * <h2>Payload</h2>
 * <p>The batch is the complete set for the receiving player, so the client replaces its cache with it: a
 * holder that disappears (dropped the staff, counter reset, logged out or moved out of range) is simply
 * absent from the next batch and the BGM stops without a separate packet. It is a count followed by that
 * many {@code (entityId, x, y, z)} quadruples, with the position as three varint-quantised doubles.</p>
 */
public class SyncArayaAuraPacket {

    /** Hard cap on how many entries one batch may carry, on the sending and the receiving side. */
    public static final int MAX_ENTRIES = ArayaConstants.AURA_MAX_ENTRIES;

    /** Entity ids of the active holders. */
    private final int[] entityIds;

    /** Positions of those holders, three doubles per entry. */
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
        // Checked before the arrays are allocated, so a malformed count cannot blow up the client.
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
