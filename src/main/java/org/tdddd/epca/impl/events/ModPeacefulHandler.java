package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.ModConfig;

@Mod.EventBusSubscriber
public class ModPeacefulHandler {

    
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        
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
        
        LivingEntity newTarget = event.getNewTarget();
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