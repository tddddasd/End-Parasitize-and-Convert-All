package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@EventBusSubscriber
public class SolidifyEffectEventHandler {

    @SubscribeEvent
    public static void onLivingUpdate(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        
        if (entity.hasEffect(ModEffects.SOLIDIFY)) {
            if (entity instanceof Player player) {
                
                restrictPlayerMovement(player);
            } else if (entity instanceof Mob mob) {
                
                freezeNonPlayerEntity(mob);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            
            if (attacker.hasEffect(ModEffects.SOLIDIFY)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        
        if (entity.hasEffect(ModEffects.SOLIDIFY)) {
            event.setCanceled(true);
        }
    }

    
    private static void restrictPlayerMovement(Player player) {
        
        player.setJumping(false);
        player.setDeltaMovement(Vec3.ZERO);

        
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }
    }

    
    private static void freezeNonPlayerEntity(Mob mob) {
        
        mob.setDeltaMovement(Vec3.ZERO);

        
        mob.setYRot(mob.yRotO);
        mob.setXRot(mob.xRotO);

        
        mob.getNavigation().stop();

        
        mob.setJumping(false);
    }
}