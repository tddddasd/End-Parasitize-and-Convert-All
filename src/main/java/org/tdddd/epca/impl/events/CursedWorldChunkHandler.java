package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.cursed.CursedChunkConverter;
import org.tdddd.epca.impl.overworld.cursed.CursedWorlds;

/**
 * Drives the "cursed world" chunk conversion: freshly generated chunks are queued from the chunk-load event
 * and converted on the following server tick.
 *
 * <p>The split exists because {@link ChunkEvent.Load} is fired before the chunk reaches
 * {@code ChunkStatus.FULL} and, on the server, from a worldgen worker thread - touching blocks there would
 * deadlock the game. Only the cursed dimension is affected; every other level and every already-existing
 * chunk is left completely alone.
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedWorldChunkHandler {

    /**
     * Conversions per level and tick. A chunk is up to 98k blocks of work, so a burst of queued chunks is
     * spread over several ticks instead of stalling one tick for a long time.
     */
    private static final int MAX_CHUNKS_PER_TICK = 4;

    private CursedWorldChunkHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // isNewChunk() is false for a chunk read back from disk, so existing chunks are never enqueued.
        if (!event.isNewChunk()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!CursedWorlds.isCursed(level)) return;
        // Only the position is recorded here: the chunk is not complete yet and this may run off-thread.
        CursedChunkConverter.enqueue(level, event.getChunk().getPos().x, event.getChunk().getPos().z);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!CursedWorlds.isCursed(level)) continue;
            CursedChunkConverter.drain(level, MAX_CHUNKS_PER_TICK);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CursedChunkConverter.clear(level.dimension());
        }
    }
}
