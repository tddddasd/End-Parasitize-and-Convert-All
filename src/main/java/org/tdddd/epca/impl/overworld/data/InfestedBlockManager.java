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
import java.util.List;
import java.util.Set;

/**
 * Tracks which blocks are infested and tells clients about them.
 *
 * <p>Block changes are <b>batched per server tick</b>. Mass infestation (a beckon converting an area, a
 * hive node spreading) used to send one packet per block to every player, which is what made large
 * infested regions laggy; now at most one batched packet goes out per level per tick.</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

    /** Sends one batched packet per level that saw changes this tick. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        for (Pending pending : PENDING) {
            if (pending.added.isEmpty() && pending.removed.isEmpty()) {
                continue;
            }
            InfestedSourcePacket.BatchInfestedSourcePacket packet =
                    new InfestedSourcePacket.BatchInfestedSourcePacket(
                            new ArrayList<>(pending.added), new ArrayList<>(pending.removed));
            ModNetwork.INSTANCE.send(PacketDistributor.DIMENSION.with(pending.level::dimension), packet);
            pending.added.clear();
            pending.removed.clear();
        }
        PENDING.removeIf(pending -> pending.added.isEmpty() && pending.removed.isEmpty());
    }

    public static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        InfestedBlockSavedData data = InfestedBlockSavedData.get(level);
        SyncAllInfestedSourcesPacket packet = new SyncAllInfestedSourcesPacket(data.getAll());
        ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
