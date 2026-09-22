package org.tdddd.epca.impl.network.packet.s2c;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.epca.impl.client.effect.SoulProtectionClientCache;
import org.tdddd.epca.impl.network.ModNetwork;

/**
 * Server to client batch sync of the entities that currently carry {@code epca:soul_protection}.
 *
 * <h2>Why it exists</h2>
 * <p>Vanilla only sends {@code ClientboundUpdateMobEffectPacket} to the affected player themselves
 * and to player passengers, so a mob's active effects are invisible to every other client. The
 * visible soul protection aura is client-side, so without this packet the flame could never be drawn
 * on a creature - only on the local player. The sender
 * ({@code impl/events/SoulProtectionSyncHandler}) pushes the authoritative set once per
 * {@code SoulProtectionSyncHandler.SYNC_INTERVAL_TICKS} ticks to every player that is tracking each
 * affected entity, and an empty batch when a player's set becomes empty, so removals propagate.</p>
 *
 * <h2>Payload</h2>
 * <p>A varint count followed by {@code count} pairs of varints: the entity id and the effect's
 * remaining duration in ticks. {@code -1} means infinite (vanilla's
 * {@code MobEffectInstance#getDuration()} value for an infinite instance), which the client must
 * never fade out early. A full 256 entry batch is therefore at most a few hundred bytes.</p>
 */
public class SyncSoulProtectionPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSoulProtectionPacket> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_soul_protection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoulProtectionPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncSoulProtectionPacket::encode,
                    SyncSoulProtectionPacket::decode);

    /** Hard cap on the entries of one batch, mirrored by the sender. */
    public static final int MAX_ENTRIES = 256;

    /** Remaining duration that means "never fades out": an infinite effect or an unknown one. */
    public static final int INFINITE_DURATION = -1;

    /** Shared empty batch, so an "everything is gone" sync allocates nothing. */
    private static final int[] EMPTY = new int[0];

    private final int[] entityIds;
    private final int[] remainingTicks;

    public SyncSoulProtectionPacket(int[] entityIds, int[] remainingTicks) {
        if (entityIds.length != remainingTicks.length) {
            throw new IllegalArgumentException("entityIds and remainingTicks must have the same length");
        }
        this.entityIds = entityIds;
        this.remainingTicks = remainingTicks;
    }

    /** The batch that tells a client its cached set is now empty. */
    public static SyncSoulProtectionPacket empty() {
        return new SyncSoulProtectionPacket(EMPTY, EMPTY);
    }

    /** Number of entries in this batch. */
    public int size() {
        return this.entityIds.length;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entityIds.length);
        for (int index = 0; index < this.entityIds.length; index++) {
            buf.writeVarInt(this.entityIds[index]);
            buf.writeVarInt(this.remainingTicks[index]);
        }
    }

    public static SyncSoulProtectionPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            // Never allocate an attacker-sized array; a real batch is capped at MAX_ENTRIES.
            throw new DecoderException("soul protection batch size out of range: " + size);
        }
        int[] entityIds = new int[size];
        int[] remainingTicks = new int[size];
        for (int index = 0; index < size; index++) {
            entityIds[index] = buf.readVarInt();
            remainingTicks[index] = buf.readVarInt();
        }
        return new SyncSoulProtectionPacket(entityIds, remainingTicks);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncSoulProtectionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SoulProtectionClientCache.replace(packet.entityIds, packet.remainingTicks));
    }
}
