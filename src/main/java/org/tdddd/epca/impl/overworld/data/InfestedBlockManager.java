package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncAllInfestedSourcesPacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfestedBlockManager {

    /**
     * Positions carried by one batch packet. A position costs 8 bytes on the wire, so 4096 entries
     * stay around 32 KB - far below the protocol packet limit even when a bulk operation (world
     * edit, sacrifice ritual) touches a whole region inside a single tick.
     */
    private static final int MAX_POSITIONS_PER_PACKET = 4096;

    /** Upper bound of batch packets per level and tick; the remainder rolls into the next tick. */
    private static final int MAX_PACKETS_PER_TICK = 4;

    /** Positions carried by one page of the full snapshot. */
    private static final int SNAPSHOT_PAGE_POSITIONS = 8192;

    /** Full snapshot pages sent per player and tick. */
    private static final int SNAPSHOT_PAGES_PER_TICK = 2;

    /**
     * Absolute iteration cap of the per tick flush and snapshot loops. Both loops are already bounded
     * by a packet/page budget, but the login audit explicitly wants a hard cap plus a "did the cursor
     * move" check, so a future edit cannot turn this into a frozen server thread.
     */
    private static final int MAX_LOOP_ITERATIONS = 64;

    private static final List<Pending> PENDING = new ArrayList<>();

    private static final Map<ServerPlayer, SnapshotCursor> SNAPSHOTS = new IdentityHashMap<>();

    private static final class Pending {
        final ServerLevel level;
        final Set<BlockPos> added = new HashSet<>();
        final Set<BlockPos> removed = new HashSet<>();

        Pending(ServerLevel level) {
            this.level = level;
        }

        boolean isEmpty() {
            return this.added.isEmpty() && this.removed.isEmpty();
        }
    }

    private static final class SnapshotCursor {
        final ServerLevel level;
        /** {@code null} until the first pump tick materialises it. */
        BlockPos[] positions;
        int index;
        int pagesSent;
        boolean firstPage = true;

        SnapshotCursor(ServerLevel level) {
            this.level = level;
        }
    }

    private static Pending pendingFor(ServerLevel level) {
        for (Pending pending : PENDING) {
            if (pending.level == level) {
                return pending;
            }
        }
        Pending created = new Pending(level);
        PENDING.add(created);
        return created;
    }

    public static void addInfestedBlock(ServerLevel level, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        InfestedBlockSavedData.get(level).add(immutable);
        Pending pending = pendingFor(level);
        pending.removed.remove(immutable);
        pending.added.add(immutable);
        // a unit holding an infested block always carries the EPCA parasite like biome
        LikeBiomeManager.markUnitInfested(level, immutable);
    }

    public static void removeInfestedBlock(ServerLevel level, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        InfestedBlockSavedData.get(level).remove(immutable);
        Pending pending = pendingFor(level);
        pending.added.remove(immutable);
        pending.removed.add(immutable);
        // re-check the unit at the end of the tick: the break event fires before the block is gone
        LikeBiomeManager.scheduleRefresh(level, immutable);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        flush();
        pumpSnapshots();
    }

    private static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Pending pending : PENDING) {
            if (pending.isEmpty()) {
                continue;
            }
            int packets = 0;
            int iterations = 0;
            while (packets < MAX_PACKETS_PER_TICK && !pending.isEmpty()) {
                if (++iterations > MAX_LOOP_ITERATIONS) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] InfestedBlockManager.flush hit the iteration cap ({}), "
                                    + "added={} removed={} left over - releasing the tick",
                            MAX_LOOP_ITERATIONS, pending.added.size(), pending.removed.size());
                    break;
                }
                List<BlockPos> added = drain(pending.added, MAX_POSITIONS_PER_PACKET);
                List<BlockPos> removed = drain(pending.removed, MAX_POSITIONS_PER_PACKET);
                if (added.isEmpty() && removed.isEmpty()) {
                    // nothing was drained although the queue reports work: stop instead of spinning
                    break;
                }
                ModNetwork.INSTANCE.send(PacketDistributor.DIMENSION.with(pending.level::dimension),
                        new InfestedSourcePacket.BatchInfestedSourcePacket(added, removed));
                packets++;
            }
        }
        PENDING.removeIf(Pending::isEmpty);
    }

    private static List<BlockPos> drain(Set<BlockPos> source, int limit) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<BlockPos> out = new ArrayList<>(Math.min(limit, source.size()));
        Iterator<BlockPos> iterator = source.iterator();
        while (iterator.hasNext() && out.size() < limit) {
            out.add(iterator.next());
            iterator.remove();
        }
        return out;
    }

    /**
     * Queues a paged full snapshot for one player. A heavily infested world holds hundreds of
     * thousands of positions (observed: 420k, i.e. a 3.4 MB packet), so sending them in one go can
     * exceed the protocol maximum packet size and stall the join. Pages are spread over ticks.
     *
     * <p>The position array itself is <b>not</b> built here: this is called from a packet handler
     * that may run on the login path, and copying a 420k entry set twice (HashSet copy plus array)
     * there is exactly the kind of login stall this class exists to avoid. The copy happens on the
     * first pump tick instead.</p>
     */
    public static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SNAPSHOTS.put(player, new SnapshotCursor(level));
    }

    private static void pumpSnapshots() {
        if (SNAPSHOTS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<ServerPlayer, SnapshotCursor>> iterator = SNAPSHOTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayer, SnapshotCursor> entry = iterator.next();
            ServerPlayer player = entry.getKey();
            SnapshotCursor cursor = entry.getValue();
            if (player.hasDisconnected()) {
                iterator.remove();
                continue;
            }
            if (cursor.positions == null) {
                Set<BlockPos> all = InfestedBlockSavedData.get(cursor.level).getAll();
                cursor.positions = all.toArray(new BlockPos[0]);
            }
            if (cursor.positions.length == 0) {
                // an empty world must still send the reset page, otherwise a re-login keeps the
                // previous session's infested positions highlighted on the client
                ModNetwork.sendToPlayer(player, new SyncAllInfestedSourcesPacket(List.of(), true));
                cursor.pagesSent = 1;
                iterator.remove();
                continue;
            }
            int pages = 0;
            int iterations = 0;
            while (cursor.index < cursor.positions.length && pages < SNAPSHOT_PAGES_PER_TICK) {
                if (++iterations > MAX_LOOP_ITERATIONS) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] InfestedBlockManager.pumpSnapshots hit the iteration cap "
                                    + "({}) at {}/{} - dropping the rest of the snapshot",
                            MAX_LOOP_ITERATIONS, cursor.index, cursor.positions.length);
                    cursor.index = cursor.positions.length;
                    break;
                }
                int step = Math.max(1, SNAPSHOT_PAGE_POSITIONS);
                int end = Math.min(cursor.index + step, cursor.positions.length);
                if (end <= cursor.index) {
                    org.tdddd.epca.impl.epca.LOGGER.error(
                            "[epca] InfestedBlockManager.pumpSnapshots cursor did not advance "
                                    + "({} -> {}) - dropping the rest of the snapshot",
                            cursor.index, end);
                    cursor.index = cursor.positions.length;
                    break;
                }
                List<BlockPos> page = new ArrayList<>(end - cursor.index);
                for (int i = cursor.index; i < end; i++) {
                    page.add(cursor.positions[i]);
                }
                cursor.index = end;
                ModNetwork.sendToPlayer(player, new SyncAllInfestedSourcesPacket(page, cursor.firstPage));
                cursor.firstPage = false;
                pages++;
                cursor.pagesSent++;
            }
            if (cursor.index >= cursor.positions.length) {
                iterator.remove();
            }
        }
    }

    /** Drops per player bookkeeping (used when the world is unloaded). */
    public static void clearRuntimeState() {
        SNAPSHOTS.clear();
        PENDING.clear();
    }
}
