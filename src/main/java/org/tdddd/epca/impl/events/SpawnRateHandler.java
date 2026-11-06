package org.tdddd.epca.impl.events;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID)
public class SpawnRateHandler {
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (ParasiteEvents.isParasiteEntity(entity)) {
            Level level = entity.level();
            float rate = DifficultyEffects.getSpawnRateMultiplier(level);
            if (rate <= 0) {

                entity.discard();
            } else if (rate < 1.0f && level.getRandom().nextFloat() > rate) {

                entity.discard();
            }
        }
    }
}
