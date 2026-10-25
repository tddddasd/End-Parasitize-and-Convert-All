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
public class ParaCurseEffectTickHandler {
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

        
        if (player.getPersistentData().getBoolean("ShouldRemoveParaCurseEffect")) {
            player.getPersistentData().remove("ShouldRemoveParaCurseEffect");
            int amplifierToRemove = player.getPersistentData().getInt("ParaCurseEffectToRemoveAmplifier");
            player.getPersistentData().remove("ParaCurseEffectToRemoveAmplifier");

            MobEffectInstance para_curseEffect = player.getEffect(ModEffects.PARA_CURSE.get());

            
            if (para_curseEffect != null && para_curseEffect.getAmplifier() == amplifierToRemove) {
                player.removeEffect(ModEffects.PARA_CURSE.get());

                
                
            }
        }

        
        checkAndFixEffectLevels(player);
    }

    
    private static void checkAndFixEffectLevels(LivingEntity entity) {
        MobEffectInstance currentParaCurse = entity.getEffect(ModEffects.PARA_CURSE.get());
        if (currentParaCurse == null) return;

        
        
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

            MobEffectInstance para_curseEffect = entity.getEffect(ModEffects.PARA_CURSE.get());
            
            if (para_curseEffect != null && para_curseEffect.getAmplifier() == amplifier) {
                entity.removeEffect(ModEffects.PARA_CURSE.get());
            }
        }
    }
}