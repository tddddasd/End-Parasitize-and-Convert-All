package org.tdddd.epca.impl.client.effect;

import java.util.HashMap;
import java.util.Map;

public final class SoulProtectionClientCache {

    public static final int UNKNOWN_REMAINING_TICKS = -1;

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    private static long clientTickCounter;

    private SoulProtectionClientCache() {
    }

    public static void clientTick() {
        clientTickCounter++;
    }

    public static void applyBatch(int[] entityIds, int[] remainingTicks) {
        ENTRIES.clear();
        int count = Math.min(entityIds.length, remainingTicks.length);
        for (int i = 0; i < count; i++) {
            ENTRIES.put(Integer.valueOf(entityIds[i]),
                    new Entry(remainingTicks[i], clientTickCounter));
        }
    }

    public static void remove(int entityId) {
        ENTRIES.remove(Integer.valueOf(entityId));
    }

    public static boolean hasEffect(int entityId) {
        return ENTRIES.containsKey(Integer.valueOf(entityId));
    }

    public static int getRemainingTicks(int entityId) {
        Entry entry = ENTRIES.get(Integer.valueOf(entityId));
        if (entry == null || entry.remainingTicks < 0) {
            return UNKNOWN_REMAINING_TICKS;
        }
        long elapsed = clientTickCounter - entry.updatedAtTick;
        long remaining = entry.remainingTicks - elapsed;
        return remaining <= 0L ? 0 : (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void clear() {
        ENTRIES.clear();
    }

    private static final class Entry {
        private final int remainingTicks;
        private final long updatedAtTick;

        private Entry(int remainingTicks, long updatedAtTick) {
            this.remainingTicks = remainingTicks;
            this.updatedAtTick = updatedAtTick;
        }
    }
}
