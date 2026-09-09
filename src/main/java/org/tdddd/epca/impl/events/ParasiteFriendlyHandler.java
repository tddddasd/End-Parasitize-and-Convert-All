package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.utils.ParasiteHelper;

@Mod.EventBusSubscriber
public class ParasiteFriendlyHandler {

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity target = event.getNewTarget();

        
        if (!ParasiteHelper.isParasite(entity)) {
            return;
        }

        
        if (target != null && ModConfig.isInImmunityWhitelist(target)) {
            
            if (entity instanceof Mob) {
                ((Mob) entity).setTarget(null);
            }
            event.setNewTarget(null);
            return;
        }

        
        if (ModConfig.isParasitePeaceful()) {
            
            if (target != null && !ModConfig.isInTargetWhitelist(target)) {
                if (entity instanceof Mob) {
                    ((Mob) entity).setTarget(null);
                }
                event.setNewTarget(null);
            }
        }
    }
}