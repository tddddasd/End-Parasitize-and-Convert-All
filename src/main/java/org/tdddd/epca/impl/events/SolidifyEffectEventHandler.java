package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber
public class SolidifyEffectEventHandler {

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        
        if (entity.hasEffect(ModEffects.SOLIDIFY.get())) {
            if (entity instanceof Player player) {
                
                restrictPlayerMovement(player);
            } else if (entity instanceof Mob mob) {
                
                freezeNonPlayerEntity(mob);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            
            if (attacker.hasEffect(ModEffects.SOLIDIFY.get())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();

        
        if (entity.hasEffect(ModEffects.SOLIDIFY.get())) {
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