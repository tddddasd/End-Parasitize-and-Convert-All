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
}