package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ContaminatedWater;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class ContaminatedWaterLimitHandler {
    private static final int MAX_CONTAMINATED_WATER = 24;
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof ContaminatedWater)) return;

        ServerLevel level = (ServerLevel) event.getLevel();

        int existingCount = 0;
        for (Entity e : level.getEntities().getAll()) {
            if (e instanceof ContaminatedWater && e != entity) {
                existingCount++;
            }
        }

        if (existingCount >= MAX_CONTAMINATED_WATER) {
            entity.discard();
        }
    }
}