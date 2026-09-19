package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.ModConfig;

@EventBusSubscriber
public class DisabledEntityHandler {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return; 
        }

        Entity entity = event.getEntity();

        
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            
            if (ModConfig.isInDisabledEntitiesWhitelist(livingEntity)) {
                
                entity.discard();
                event.setCanceled(true); 
            }
        }
    }
}
