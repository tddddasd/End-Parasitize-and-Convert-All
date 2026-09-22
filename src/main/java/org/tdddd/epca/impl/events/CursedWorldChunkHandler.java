package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.cursed.CursedChunkConverter;
import org.tdddd.epca.impl.overworld.cursed.CursedWorlds;

/**
 * Drives the "cursed world" chunk conversion: freshly generated chunks are queued from the chunk-load event
 * and converted on the following server tick.
 *
 * <p>The split exists because {@link ChunkEvent.Load} is fired before the chunk reaches
 * {@code ChunkStatus.FULL} and, on the server, from a worldgen worker thread - touching blocks there would
 * deadlock the game (see the NeoForge javadoc on that event). Only the cursed dimension is affected; every
 * other level and every already-existing chunk is left completely alone.
 */
@EventBusSubscriber(modid = epca.MODID)
public final class CursedWorldChunkHandler {

    /**
     * Conversions per level and tick. A chunk is up to 98k blocks of work, so a burst of queued chunks is
     * spread over several ticks instead of stalling one tick for a long time.
     */
    private static final int MAX_CHUNKS_PER_TICK = 4;

    private CursedWorldChunkHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!CursedWorlds.isCursed(level)) return;
        // ChunkPos is a record in 26.1.2, so the coordinates are accessor methods.
        CursedChunkConverter.enqueue(level, event.getChunk().getPos().x(), event.getChunk().getPos().z());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
