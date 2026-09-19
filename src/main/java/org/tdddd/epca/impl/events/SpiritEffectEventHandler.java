package org.tdddd.epca.impl.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.registry.effects.buff.SpiritEffect;

@EventBusSubscriber(modid = "epca")
public class SpiritEffectEventHandler {
    
    @SubscribeEvent
    public static void onLivingChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && SpiritEffect.hasSpiritEffect(event.getNewAboutToBeSetTarget())) {
            
            event.setCanceled(true);
        }
    }
}