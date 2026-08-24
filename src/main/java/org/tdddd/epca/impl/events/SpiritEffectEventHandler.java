package org.tdddd.epca.impl.events;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.effects.buff.SpiritEffect;

@Mod.EventBusSubscriber(modid = "epca")
public class SpiritEffectEventHandler {
    
    @SubscribeEvent
    public static void onLivingChangeTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && SpiritEffect.hasSpiritEffect(event.getNewTarget())) {
            
            event.setCanceled(true);
        }
    }
}