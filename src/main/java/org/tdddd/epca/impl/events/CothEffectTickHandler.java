package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class CothEffectTickHandler {
    private static final List<EffectRemovalRequest> removalRequests = new ArrayList<>();

    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        
        if (!removalRequests.isEmpty()) {
            List<EffectRemovalRequest> toRemove = new ArrayList<>(removalRequests);
            removalRequests.clear();

            for (EffectRemovalRequest request : toRemove) {
                request.process();
            }
        }
    }

    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LivingEntity player = event.player;

        
        if (player.level().isClientSide) return;

        
        if (player.getPersistentData().getBoolean("ShouldRemoveCothEffect")) {
            player.getPersistentData().remove("ShouldRemoveCothEffect");
            int amplifierToRemove = player.getPersistentData().getInt("CothEffectToRemoveAmplifier");
            player.getPersistentData().remove("CothEffectToRemoveAmplifier");

            MobEffectInstance cothEffect = player.getEffect(ModEffects.COTH.get());

            
            if (cothEffect != null && cothEffect.getAmplifier() == amplifierToRemove) {
                player.removeEffect(ModEffects.COTH.get());

                
                
            }
        }

        
        checkAndFixEffectLevels(player);
    }

    
    private static void checkAndFixEffectLevels(LivingEntity entity) {
        MobEffectInstance currentCoth = entity.getEffect(ModEffects.COTH.get());
        if (currentCoth == null) return;

        
        
    }

    
    public static void scheduleEffectRemoval(LivingEntity entity, int amplifier) {
        removalRequests.add(new EffectRemovalRequest(entity, amplifier));
    }

    
    private static class EffectRemovalRequest {
        private final LivingEntity entity;
        private final int amplifier;

        public EffectRemovalRequest(LivingEntity entity, int amplifier) {
            this.entity = entity;
            this.amplifier = amplifier;
        }

        public void process() {
            if (!entity.isAlive()) return;

            MobEffectInstance cothEffect = entity.getEffect(ModEffects.COTH.get());
            
            if (cothEffect != null && cothEffect.getAmplifier() == amplifier) {
                entity.removeEffect(ModEffects.COTH.get());
            }
        }
    }
}