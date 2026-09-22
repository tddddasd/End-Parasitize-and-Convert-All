package org.tdddd.epca.impl.client.effect;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only cache of the {@code epca:soul_protection} effects the server reported through
 * {@code SyncSoulProtectionPacket}.
 *
 * <p>{@code LivingEntity#getEffect} only knows about an effect on the local player (and its
 * passengers), because vanilla sends the mob-effect packets only to the affected player. For every
 * other creature this cache is the renderer's only source of truth, so
 * {@code SoulProtectionHeartRenderer} draws the flame when the entity either really has the effect on
 * the client or is listed here.</p>
 *
 * <h2>Full-replace batches</h2>
 * <p>{@link #applyBatch} replaces the whole cache: the server sends the complete set of
 * soul-protected entities the player may know about, so an empty batch clears everything and a
 * removal needs no separate packet.</p>
 *
 * <h2>Local countdown</h2>
 * <p>Batches arrive only every {@code SoulProtectionSyncHandler.SYNC_INTERVAL_TICKS} ticks, so every
 * entry remembers the client tick it arrived on and {@link #getRemainingTicks} subtracts the elapsed
 * client ticks (counted by {@link #clientTick()}, which the mod's client tick hook calls). The flame
 * therefore fades out smoothly and on time between two batches instead of jumping every second. When
 * the countdown reaches zero the renderer sees a remaining duration of 0 and fades out, even though
 * the entry itself stays until the next batch confirms the removal.</p>
 *
 * <h2>Level changes</h2>
 * <p>{@link #clear()} is called whenever the client level changes (from the flame renderer's own
 * level-change reset), so no id from an old level can survive into a new one.</p>
 */
public final class SoulProtectionClientCache {

    /**
     * Value {@link #getRemainingTicks(int)} returns when there is no countdown: the entity has the
     * effect with an infinite duration, or the cache has no duration for it. It never triggers a
     * fade-out.
     */
    public static final int UNKNOWN_REMAINING_TICKS = -1;

    /** entityId -> the batch entry for it. */
    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    /** Client tick counter; the reference for the local countdown of every entry. */
    private static long clientTickCounter;

    private SoulProtectionClientCache() {
    }

    /** Advances the local countdown clock; called once per client tick. */
    public static void clientTick() {
        clientTickCounter++;
    }

    /**
     * Replaces the whole cache with one batch.
     *
     * @param entityIds      entity ids of the batch
     * @param remainingTicks remaining duration in ticks per entry, negative for infinite
     */
    public static void applyBatch(int[] entityIds, int[] remainingTicks) {
        ENTRIES.clear();
        int count = Math.min(entityIds.length, remainingTicks.length);
        for (int i = 0; i < count; i++) {
            ENTRIES.put(Integer.valueOf(entityIds[i]),
                    new Entry(remainingTicks[i], clientTickCounter));
        }
    }

    /** Forgets one entity; used when the renderer stops needing an id. */
    public static void remove(int entityId) {
        ENTRIES.remove(Integer.valueOf(entityId));
    }

    /** True when the server said this entity carries the effect. */
    public static boolean hasEffect(int entityId) {
        return ENTRIES.containsKey(Integer.valueOf(entityId));
    }

    /**
     * Remaining duration of a cached effect, counted down locally.
     *
     * @return the remaining ticks, 0 when it has just run out, or
     *         {@link #UNKNOWN_REMAINING_TICKS} for an infinite/unknown duration
     */
    public static int getRemainingTicks(int entityId) {
        Entry entry = ENTRIES.get(Integer.valueOf(entityId));
        if (entry == null || entry.remainingTicks < 0) {
            return UNKNOWN_REMAINING_TICKS;
        }
        long elapsed = clientTickCounter - entry.updatedAtTick;
        long remaining = entry.remainingTicks - elapsed;
        return remaining <= 0L ? 0 : (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    /** Number of cached entities; diagnostics only. */
    public static int size() {
        return ENTRIES.size();
    }

    /** Drops every entry; called when the client level changes. */
    public static void clear() {
        ENTRIES.clear();
    }

    /** One batch entry: the duration the server reported plus the tick it arrived on. */
    private static final class Entry {
        private final int remainingTicks;
        private final long updatedAtTick;

        private Entry(int remainingTicks, long updatedAtTick) {
            this.remainingTicks = remainingTicks;
            this.updatedAtTick = updatedAtTick;
        }
    }
}
