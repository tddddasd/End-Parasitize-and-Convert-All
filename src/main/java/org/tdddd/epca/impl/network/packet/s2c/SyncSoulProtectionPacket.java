package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;

import java.util.function.Supplier;

/**
 * Server -&gt; client batch sync of the entities that currently carry {@code epca:soul_protection}.
 *
 * <p>Vanilla only sends {@code ClientboundUpdateMobEffectPacket} for an effect to the affected player
 * and to that player's passengers, so without this packet a client would only ever see the effect on
 * the local player (third person) and on ridden entities - not on the creatures the effect was
 * actually granted to. The server half of the feature therefore sends one of these batches per
 * player every {@code SoulProtectionSyncHandler.SYNC_INTERVAL_TICKS} ticks, and the client half
 * mirrors the batch into {@link SoulProtectionClientCache}, which the flame renderer reads.</p>
 *
 * <h2>Payload</h2>
 * <p>The batch is the <em>complete</em> set of soul-protected entities the receiving player may know
 * about, so the client replaces its whole cache with it: an empty batch therefore propagates every
 * removal, and the periodic refresh keeps the locally counted-down durations honest when a beacon (or
 * any other reapplier) keeps extending the effect. It is a count followed by that many
 * {@code (entityId, remainingDurationTicks)} varint pairs, i.e. two to four bytes per entity plus the
 * count.</p>
 */
public class SyncSoulProtectionPacket {

    /** Hard cap on how many entries one batch may carry, on both the sending and the receiving side. */
    public static final int MAX_ENTRIES = 256;

    /**
     * Remaining-duration value of an effect that never expires ({@code MobEffectInstance} uses
     * {@code -1}). The client treats any negative value as "do not fade out".
     */
    public static final int INFINITE_DURATION = -1;

    /** Entity ids of the batch; {@code entityIds.length == remainingTicks.length}. */
    private final int[] entityIds;

    /** Remaining duration in ticks per entity, or {@link #INFINITE_DURATION}. */
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
        // Checked before the arrays are allocated, so a malformed count cannot blow up the client.
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
