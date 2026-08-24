package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.ModConfig;

@Mod.EventBusSubscriber
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
