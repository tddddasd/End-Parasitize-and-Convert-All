package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.utils.ParasiteHelper;

@EventBusSubscriber
public class ParasiteFriendlyHandler {

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();

        
        if (!ParasiteHelper.isParasite(entity)) {
            return;
        }

        
        if (target != null && ModConfig.isInImmunityWhitelist(target)) {
            
            if (entity instanceof Mob) {
                ((Mob) entity).setTarget(null);
            }
            event.setNewAboutToBeSetTarget(null);
            return;
        }

        
        if (ModConfig.isParasitePeaceful()) {
            
            if (target != null && !ModConfig.isInTargetWhitelist(target)) {
                if (entity instanceof Mob) {
                    ((Mob) entity).setTarget(null);
                }
                event.setNewAboutToBeSetTarget(null);
            }
        }
    }
}