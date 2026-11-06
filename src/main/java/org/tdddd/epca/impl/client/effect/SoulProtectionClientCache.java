package org.tdddd.epca.impl.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.tdddd.epca.impl.network.packet.s2c.SyncSoulProtectionPacket;

import java.util.HashMap;
import java.util.Map;

public final class SoulProtectionClientCache {

    public static final int INFINITE_DURATION = SyncSoulProtectionPacket.INFINITE_DURATION;

    private static final class Entry {
        private final int remainingTicksAtSync;
        private final long syncedAtTick;

        private Entry(int remainingTicksAtSync, long syncedAtTick) {
            this.remainingTicksAtSync = remainingTicksAtSync;
            this.syncedAtTick = syncedAtTick;
        }
    }

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    private static ClientLevel levelIdentity;

    private static long clientTickCounter;

    private SoulProtectionClientCache() {
    }

    public static void clientTick() {
        if (refreshLevel()) {
            clientTickCounter++;
        }
    }

    public static void replace(int[] entityIds, int[] remainingTicks) {
        refreshLevel();
        ENTRIES.clear();
        int count = Math.min(entityIds.length, remainingTicks.length);
        for (int index = 0; index < count; index++) {
            ENTRIES.put(entityIds[index], new Entry(remainingTicks[index], clientTickCounter));
        }
    }

    public static boolean isActive(int entityId) {
        return ENTRIES.containsKey(entityId);
    }

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

    public static int trackedCount() {
        return ENTRIES.size();
    }

    public static void clear() {
        ENTRIES.clear();
        levelIdentity = null;
        clientTickCounter = 0L;
    }

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
