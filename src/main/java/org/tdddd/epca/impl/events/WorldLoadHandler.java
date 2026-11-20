package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.tdddd.epca.impl.overworld.data.WorldDifficultyData;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;
import org.tdddd.epca.impl.epca;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class WorldLoadHandler {

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WorldDifficultyData data = WorldDifficultyData.get(serverLevel);
            DifficultyLevel pending = null;

            if (FMLEnvironment.dist == Dist.CLIENT) {
                try {

                    Class<?> helperClass = Class.forName("org.tdddd.epca.impl.utils.ClientOnlyHelper");
                    Method getMethod = helperClass.getMethod("getPendingDifficulty");
                    pending = (DifficultyLevel) getMethod.invoke(null);
                } catch (Exception e) {

                }
            }

            if (pending != null) {
                data.setDifficulty(pending);
                if (pending == DifficultyLevel.CUSTOM) {

                    if (data.getCustomSpawnRate() == 1.0f && data.isCustomRewardEnabled()) {

                    }
                }
            }
        }
    }

    /**
     * Drops every per level runtime cache when a dimension unloads.
     *
     * <p>Several subsystems keep {@code IdentityHashMap<ServerLevel, ...>} bookkeeping (sweep cursors,
     * pending like biome refreshes, biome id resolve caches, snapshot queues). Those maps hold the
     * {@link ServerLevel} strongly, so without this hook quitting to the title screen keeps the whole
     * previous world - including its multi hundred thousand entry saved data - reachable, which
     * leaves the integrated server with a permanently smaller heap and makes the next world entry
     * stutter through GC instead of loading.</p>
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // a login snapshot that survives its dimension being unloaded would point at a dead level
        // every one of these maps is rebuildable lazily, so dropping them all is always safe
        org.tdddd.epca.impl.overworld.data.LikeBiomeManager.clearRuntimeState();
        org.tdddd.epca.impl.overworld.data.InfestedBlockManager.clearRuntimeState();
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        org.tdddd.epca.impl.overworld.data.LikeBiomeManager.clearRuntimeState();
        org.tdddd.epca.impl.overworld.data.InfestedBlockManager.clearRuntimeState();
    }
}
