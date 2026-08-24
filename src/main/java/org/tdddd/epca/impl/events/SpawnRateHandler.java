package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class SpawnRateHandler {
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof IParasite) {
            Level level = entity.level();
            float rate = DifficultyEffects.getSpawnRateMultiplier(level);
            if (rate <= 0) {
                
                entity.discard();
            } else if (rate < 1.0f && level.random.nextFloat() > rate) {
                
                entity.discard();
            }
        }
    }
}