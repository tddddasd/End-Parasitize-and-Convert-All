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

@EventBusSubscriber(modid = epca.MODID)
public final class CursedWorldChunkHandler {

    private static final int MAX_CHUNKS_PER_TICK = 4;

    private CursedWorldChunkHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!CursedWorlds.isCursed(level)) return;

        CursedChunkConverter.enqueue(level, event.getChunk().getPos().x(), event.getChunk().getPos().z());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
            org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeSpawner.onServerTick(level);
        }
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
