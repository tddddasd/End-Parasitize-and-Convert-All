package org.tdddd.epca.impl.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEventHandler {
    private static final String HAS_RECEIVED_BLOOD_PAPER_KEY = "epca_has_received_blood_paper";

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        player.getServer().execute(() -> {
            checkAndGrantEvolutionProgress(player);
        });
    }

    
    private static void checkAndGrantEvolutionProgress(ServerPlayer player) {
        try {
            
            ServerLevel overworld = player.getServer().overworld();
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
            
            Advancement advancement = player.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("epca", "sense_of_crisis"));

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
            for (ServerLevel level : player.server.getAllLevels()) {
                EvolutionManager em = EvolutionManager.forDimension(level);
                em.syncToPlayer(player);
            }
        }
    }
}