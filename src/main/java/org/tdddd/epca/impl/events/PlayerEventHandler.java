package org.tdddd.epca.impl.events;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;

@EventBusSubscriber(modid = epca.MODID)
public class PlayerEventHandler {
    private static final String HAS_RECEIVED_BLOOD_PAPER_KEY = "epca_has_received_blood_paper";

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        ServerLevel serverLevel = player.level();
        serverLevel.getServer().execute(() -> {
            checkAndGrantEvolutionProgress(player);
        });
    }

    
    private static void checkAndGrantEvolutionProgress(ServerPlayer player) {
        try {
            
            ServerLevel overworld = player.level().getServer().overworld();
            EvolutionManager evolutionManager = EvolutionManager.forOverworld(overworld);

            int currentStage = evolutionManager.getStage();

            
            if (currentStage >= 5 && currentStage <= 10) {
                grantSenseOfCrisisAdvancement(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private static void grantSenseOfCrisisAdvancement(ServerPlayer player) {
        try {
            
            AdvancementHolder advancement = player.level().getServer().getAdvancements()
                    .get(Identifier.fromNamespaceAndPath("epca", "sense_of_crisis"));

            if (advancement != null) {
                
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!progress.isDone()) {
                    
                    player.getAdvancements().award(advancement, "unlock");
                }
            }
        } catch (Exception e) {
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (ServerLevel level : player.level().getServer().getAllLevels()) {
                EvolutionManager em = EvolutionManager.forDimension(level);
                em.syncToPlayer(player);
            }
        }
    }
}