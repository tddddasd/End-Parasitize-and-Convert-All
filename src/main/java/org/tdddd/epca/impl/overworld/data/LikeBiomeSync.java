package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
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

    /**
     * Upper bound of packets per level and tick. A bulk world edit can touch tens of thousands of
     * units in a single tick; sending all of them at once is a bandwidth spike for the client (and
     * each unit also dirties up to 9 chunk sections there), so the remainder is carried over to the
     * next tick instead. With ~7 bytes per unit this caps a tick at roughly 30 KB of traffic.
     */
    private static final int MAX_PACKETS_PER_TICK = 4;

    /**
     * Hard iteration cap of the two drain loops below. Both already stop at a packet budget and at
     * the end of an iterator, but the login audit asks for an explicit cap plus a "the cursor really
     * moved" check so that a future edit (re-adding leftovers inside the loop, a zero page size)
     * fails with one log line instead of a frozen server thread.
     */
    private static final int MAX_DRAIN_ITERATIONS = 65_536;

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
        int packets = 0;

        if (!pending.upserts.isEmpty()) {
            Map<Long, LikeBiome> page = new LinkedHashMap<>();
            Iterator<Long> iterator = pending.upserts.iterator();
            int iterations = 0;
            while (iterator.hasNext()) {
                if (++iterations > MAX_DRAIN_ITERATIONS) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] LikeBiomeSync.flush hit the iteration cap ({}) while "
                                    + "draining upserts, {} key(s) left - releasing the tick",
                            MAX_DRAIN_ITERATIONS, pending.upserts.size());
                    break;
                }
                long key = iterator.next();
                iterator.remove();
                LikeBiome value = data.atKey(key);
                if (value != null) {
                    page.put(key, value);
                }
                if (page.size() >= MAX_UPSERTS_PER_PACKET) {
                    send(level, page, List.of());
                    page = new LinkedHashMap<>();
                    if (++packets >= MAX_PACKETS_PER_TICK) {
                        break;
                    }
                }
            }
            // whatever is still buffered goes out on the next tick, together with the rest
            if (!page.isEmpty() && packets < MAX_PACKETS_PER_TICK) {
                send(level, page, List.of());
                packets++;
            } else if (!page.isEmpty()) {
                for (Map.Entry<Long, LikeBiome> entry : page.entrySet()) {
                    pending.upserts.add(entry.getKey());
                }
            }
        }

        if (packets < MAX_PACKETS_PER_TICK && !pending.removals.isEmpty()) {
            List<Long> page = new ArrayList<>(Math.min(MAX_REMOVALS_PER_PACKET, pending.removals.size()));
            Iterator<Long> iterator = pending.removals.iterator();
            int iterations = 0;
            while (iterator.hasNext()) {
                if (++iterations > MAX_DRAIN_ITERATIONS) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] LikeBiomeSync.flush hit the iteration cap ({}) while "
                                    + "draining removals, {} key(s) left - releasing the tick",
                            MAX_DRAIN_ITERATIONS, pending.removals.size());
                    break;
                }
                long key = iterator.next();
                iterator.remove();
                // only tell the client about units it could actually know about
                if (!data.contains(key)) {
                    page.add(key);
                }
                if (page.size() >= MAX_REMOVALS_PER_PACKET) {
                    send(level, Map.of(), page);
                    page = new ArrayList<>();
                    if (++packets >= MAX_PACKETS_PER_TICK) {
                        break;
                    }
                }
            }
            if (!page.isEmpty() && packets < MAX_PACKETS_PER_TICK) {
                send(level, Map.of(), page);
            } else if (!page.isEmpty()) {
                pending.removals.addAll(page);
            }
        }
    }

    private static void send(ServerLevel level, Map<Long, LikeBiome> upserts, List<Long> removals) {
        ModNetwork.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension),
                new SyncLikeBiomeBatchPacket(level.dimension().location(), upserts, removals));
    }
}
