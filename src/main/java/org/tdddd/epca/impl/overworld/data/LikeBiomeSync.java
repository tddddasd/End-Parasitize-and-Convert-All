package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.level.ServerLevel;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncLikeBiomeBatchPacket;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coalesces every like biome change that happens inside one server tick and flushes it as a small
 * number of {@link SyncLikeBiomeBatchPacket}s at the end of that tick.
 *
 * <p>This keeps the update latency at one tick while never sending one packet per unit: a whole
 * chunk generation pass, a sacrifice ritual or a world edit that touches thousands of units still
 * results in only a handful of packets per level and tick.</p>
 */
public final class LikeBiomeSync {

    /** Upper bound of unit entries carried by a single packet, keeps packets well below the limit. */
    private static final int MAX_UPSERTS_PER_PACKET = 1024;

    private static final int MAX_REMOVALS_PER_PACKET = 2048;

    private static final Map<ServerLevel, Pending> PENDING = new IdentityHashMap<>();

    private LikeBiomeSync() {
    }

    private static final class Pending {
        final ServerLevel level;
        final Set<Long> upserts = new LinkedHashSet<>();
        final Set<Long> removals = new LinkedHashSet<>();

        Pending(ServerLevel level) {
            this.level = level;
        }

        boolean isEmpty() {
            return this.upserts.isEmpty() && this.removals.isEmpty();
        }
    }

    private static Pending pending(ServerLevel level) {
        return PENDING.computeIfAbsent(level, Pending::new);
    }

    /** Marks a unit as added/changed; the value is read from the saved data at flush time. */
    public static void markUpsert(ServerLevel level, long unitKey) {
        Pending pending = pending(level);
        pending.removals.remove(unitKey);
        pending.upserts.add(unitKey);
    }

    public static void markRemoval(ServerLevel level, long unitKey) {
        Pending pending = pending(level);
        pending.upserts.remove(unitKey);
        pending.removals.add(unitKey);
    }

    public static void markUpserts(ServerLevel level, Iterable<Long> unitKeys) {
        for (Long key : unitKeys) {
            markUpsert(level, key);
        }
    }

    public static void clear(ServerLevel level) {
        PENDING.remove(level);
    }

    public static void clearAll() {
        PENDING.clear();
    }

    public static boolean hasPending(ServerLevel level) {
        Pending pending = PENDING.get(level);
        return pending != null && !pending.isEmpty();
    }

    /** Sends everything that was collected since the previous tick. Runs on the server thread. */
    public static void flushAll() {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<ServerLevel, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next().getValue();
            if (pending.isEmpty()) {
                iterator.remove();
                continue;
            }
            flush(pending);
            if (pending.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static void flush(Pending pending) {
        ServerLevel level = pending.level;
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);

        List<Long> upsertKeys = new ArrayList<>(pending.upserts.size());
        for (Long key : pending.upserts) {
            if (data.atKey(key) != null) {
                upsertKeys.add(key);
            }
        }
        List<Long> removalKeys = new ArrayList<>(pending.removals.size());
        for (Long key : pending.removals) {
            // only tell the client about units it could actually know about
            if (!data.contains(key)) {
                removalKeys.add(key);
            }
        }
        pending.upserts.clear();
        pending.removals.clear();

        int index = 0;
        while (index < upsertKeys.size()) {
            int end = Math.min(index + MAX_UPSERTS_PER_PACKET, upsertKeys.size());
            Map<Long, LikeBiome> upserts = new LinkedHashMap<>();
            for (int i = index; i < end; i++) {
                long key = upsertKeys.get(i);
                LikeBiome value = data.atKey(key);
                if (value != null) {
                    upserts.put(key, value);
                }
            }
            if (!upserts.isEmpty()) {
                send(level, upserts, List.of());
            }
            index = end;
        }

        index = 0;
        while (index < removalKeys.size()) {
            int end = Math.min(index + MAX_REMOVALS_PER_PACKET, removalKeys.size());
            send(level, Map.of(), new ArrayList<>(removalKeys.subList(index, end)));
            index = end;
        }
    }

    private static void send(ServerLevel level, Map<Long, LikeBiome> upserts, List<Long> removals) {
        ModNetwork.sendToDimension(level,
                new SyncLikeBiomeBatchPacket(level.dimension().identifier(), upserts, removals));
    }
}
