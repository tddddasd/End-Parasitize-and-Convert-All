package org.tdddd.epca.impl.overworld.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.InfestedSourcePacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncAllInfestedSourcesPacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which blocks are infested and tells clients about them.
 *
 * <p>Block changes are <b>batched per server tick</b>. Mass infestation (a beckon converting an area, a
 * hive node spreading) used to send one payload per block to every player, which is what made large
 * infested regions laggy; now at most one batched payload goes out per level per tick.</p>
 */
@EventBusSubscriber(modid = epca.MODID)
public class InfestedBlockManager {

    /** Blocks infested since the last flush, per level. */
    private static final List<Pending> PENDING = new ArrayList<>();

    private static final class Pending {
        final ServerLevel level;
        final Set<BlockPos> added = new HashSet<>();
        final Set<BlockPos> removed = new HashSet<>();

        Pending(ServerLevel level) {
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
    }

    public static void removeInfestedBlock(ServerLevel level, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        InfestedBlockSavedData.get(level).remove(immutable);
        Pending pending = pendingFor(level);
        pending.added.remove(immutable);
        pending.removed.add(immutable);
    }

    /** Sends one batched payload per level that saw changes this tick. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Pending pending : PENDING) {
            if (pending.added.isEmpty() && pending.removed.isEmpty()) {
                continue;
            }
            ModNetwork.sendToDimension(pending.level,
                    new InfestedSourcePacket.BatchInfestedSourcePacket(
                            new ArrayList<>(pending.added), new ArrayList<>(pending.removed)));
            pending.added.clear();
            pending.removed.clear();
        }
        PENDING.removeIf(pending -> pending.added.isEmpty() && pending.removed.isEmpty());
    }

    public static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.level();
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        SyncAllInfestedSourcesPacket packet = new SyncAllInfestedSourcesPacket(data.getAll());
        ModNetwork.sendToPlayer(player, packet);
    }
}
