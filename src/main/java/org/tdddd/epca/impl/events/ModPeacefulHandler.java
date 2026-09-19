package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.ModConfig;

@EventBusSubscriber
public class ModPeacefulHandler {

    
    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        
        if (!(event.getSource().getEntity() instanceof Mob attacker)) {
            return;
        }

        
        if (!(event.getEntity() instanceof Mob victim)) {
            return;
        }

        
        if (ModConfig.areModsPeaceful(attacker, victim)) {
            
            event.setCanceled(true);
        }
    }

    
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) {
            return;
        }

        
        if (!(event.getEntity() instanceof Mob attacker)) {
            return;
        }

        
        if (ModConfig.areModsPeaceful(attacker, newTarget)) {
            
            event.setCanceled(true);
        }
    }
}