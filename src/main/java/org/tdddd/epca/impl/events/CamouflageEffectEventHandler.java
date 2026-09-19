package org.tdddd.epca.impl.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.registry.effects.buff.CamouflageEffect;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.epca;

@EventBusSubscriber(modid = epca.MODID)
public class CamouflageEffectEventHandler {

    
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) return;

        if (!CamouflageEffect.hasCamouflageEffect(target)) return;

        LivingEntity attacker = event.getEntity();
        boolean isTargetParasite = isParasite(target);
        boolean isAttackerParasite = isParasite(attacker);

        boolean shouldCancel = false;
        if (isTargetParasite) {
            
            if (!(attacker instanceof Player)) {
                shouldCancel = true;
            }
        } else {
            
            if (isAttackerParasite) {
                shouldCancel = true;
            }
        }

        if (shouldCancel) {
            event.setCanceled(true);
        }
    }

    
    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        
        LivingEntity victim = event.getEntity();
        
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity ? (LivingEntity) source.getEntity() : null;

        if (attacker == null || victim == null) return;

        
        CamouflageEffect.tryRemoveOnAttack(attacker, victim);
    }

    
    private static boolean isParasite(LivingEntity entity) {
        return IParasite.isParasiteByTagOrInterface(entity);
    }
}