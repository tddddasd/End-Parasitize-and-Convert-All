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

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedWorldChunkHandler {

    private static final int MAX_CHUNKS_PER_TICK = 4;

    private CursedWorldChunkHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {

        if (!event.isNewChunk()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!CursedWorlds.isCursed(level)) return;

        CursedChunkConverter.enqueue(level, event.getChunk().getPos().x, event.getChunk().getPos().z);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
                org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeSpawner.onServerTick(level);
            org.tdddd.epca.impl.compat.vs.InfestedPumpkinShip.tick(level);
            }
        }
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
