package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.tdddd.epca.impl.network.packet.s2c.SyncSoulProtectionPacket;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side mirror of "which entities currently carry {@code epca:soul_protection}".
 *
 * <p>Vanilla does not sync a mob's active effects to other clients (see
 * {@link SyncSoulProtectionPacket}), so this cache is filled by that packet and read by
 * {@code SoulProtectionHeartRenderer}: the renderer shows the flame when the entity has the effect
 * client-side (the local player, and any entity whose effect the client was sent) <em>or</em> when
 * this cache knows about it.</p>
 *
 * <h2>Duration handling</h2>
 * <p>Each entry stores the remaining duration the server reported together with the client tick it
 * arrived on, and {@link #remainingTicks(int)} counts that value down locally. Without the countdown
 * a 20 tick sync interval could not drive a 15 tick fade-out: the last value received before the
 * effect ends is about 20 ticks, and the next batch simply omits the entity, so the countdown is what
 * makes the flame leave smoothly. {@link SyncSoulProtectionPacket#INFINITE_DURATION} (-1, i.e. an
 * infinite effect) is reported verbatim and never counts down.</p>
 *
 * <p>The cache is cleared whenever the client level changes, so ids never leak between dimensions or
 * sessions.</p>
 */
public final class SoulProtectionClientCache {

    /** Remaining duration that means "never fades out": an infinite effect or an unknown one. */
    public static final int INFINITE_DURATION = SyncSoulProtectionPacket.INFINITE_DURATION;

    /** One entity: the duration the server last reported and when it arrived. */
    private static final class Entry {
        private final int remainingTicksAtSync;
        private final long syncedAtTick;

        private Entry(int remainingTicksAtSync, long syncedAtTick) {
            this.remainingTicksAtSync = remainingTicksAtSync;
            this.syncedAtTick = syncedAtTick;
        }
    }

    /** entityId -> last reported remaining duration. */
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    /** Identity of the client level the entries belong to; a change clears everything. */
    private static ClientLevel levelIdentity;

    /** Client ticks seen since the last level change; drives the local duration countdown. */
    private static long clientTickCounter;

    private SoulProtectionClientCache() {
    }

    /**
     * Ages the cache; called once per client tick from
     * {@code ClientEvents#onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post)},
     * next to {@code GasCloudManager.clientTick()}. It only advances a counter and drops the cache
     * when the client level changed.
     */
    public static void clientTick() {
        if (refreshLevel()) {
            clientTickCounter++;
        }
    }

    /**
     * Replaces the whole cached set with a batch from the server. Called on the client thread from
     * {@link SyncSoulProtectionPacket#handle}. A batch is always authoritative for the receiving
     * player, so entries the server no longer reports disappear here as well.
     */
    public static void replace(int[] entityIds, int[] remainingTicks) {
        refreshLevel();
        ENTRIES.clear();
        int count = Math.min(entityIds.length, remainingTicks.length);
        for (int index = 0; index < count; index++) {
            ENTRIES.put(entityIds[index], new Entry(remainingTicks[index], clientTickCounter));
        }
    }

    /** True when the server reported this entity as carrying the effect in the latest batch. */
    public static boolean isActive(int entityId) {
        return ENTRIES.containsKey(entityId);
    }

    /**
     * Remaining ticks of the effect on the cached entity, counted down locally since the last sync,
     * or {@link #INFINITE_DURATION} when the entity is unknown or its effect is infinite. The
     * renderer treats a negative value as "no fade-out".
     */
    public static int remainingTicks(int entityId) {
        Entry entry = ENTRIES.get(entityId);
        if (entry == null || entry.remainingTicksAtSync < 0) {
            return INFINITE_DURATION;
        }
        long elapsed = clientTickCounter - entry.syncedAtTick;
        long remaining = entry.remainingTicksAtSync - elapsed;
        if (remaining <= 0L) {
            return 0;
        }
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    /** Number of cached entities; used by diagnostics. */
    public static int trackedCount() {
        return ENTRIES.size();
    }

    /** Drops everything and forgets the level identity. */
    public static void clear() {
        ENTRIES.clear();
        levelIdentity = null;
        clientTickCounter = 0L;
    }

    /** Clears the cache when the client level changed (or is gone); true while a level exists. */
    private static boolean refreshLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            if (levelIdentity != null || !ENTRIES.isEmpty()) {
                clear();
            }
            return false;
        }
        if (levelIdentity != level) {
            ENTRIES.clear();
            clientTickCounter = 0L;
            levelIdentity = level;
        }
        return true;
    }
}
