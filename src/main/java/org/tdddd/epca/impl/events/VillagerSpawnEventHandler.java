package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class VillagerSpawnEventHandler {

    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Villager villager) {

            villager.goalSelector.addGoal(1, new AvoidEntityGoal<>(
                    villager,
                    LivingEntity.class,
                    entity -> ParasiteEvents.isParasiteByTagOrInterface(entity),
                    8.0F,
                    0.9F,
                    1.0F,
                    entity -> {
                        if (!ParasiteEvents.isParasiteByTagOrInterface(entity) || !entity.isAlive()) {
                            return false;
                        }

                        double distanceSqr = villager.distanceToSqr(entity);
                        return distanceSqr < 64.0D && villager.hasLineOfSight(entity);
                    }
            ));
        }
    }
}
