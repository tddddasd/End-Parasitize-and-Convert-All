package org.tdddd.epca.impl.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.data.WorldDifficultyData;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class MasterDifficultyAdvancementHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var server = player.getServer();
        if (server == null) return;

        
        boolean allowCommands = server.getWorldData().getAllowCommands();
        
        boolean isHardDifficulty = server.getLevel(player.level().dimension()).getDifficulty() == Difficulty.HARD;

        
        ServerLevel level = player.serverLevel();
        WorldDifficultyData difficultyData = WorldDifficultyData.get(level);
        boolean isMasterDifficulty = difficultyData.getDifficulty() == DifficultyLevel.MASTER;

        
        if (!allowCommands && isHardDifficulty && isMasterDifficulty) {
            
            Advancement advancement = server.getAdvancements().getAdvancement(
                    new ResourceLocation(epca.MODID, "master_difficulty")
            );
            if (advancement != null) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!progress.isDone()) {
                    for (String criterion : progress.getRemainingCriteria()) {
                        player.getAdvancements().award(advancement, criterion);
                    }
                }
            }
        }
    }
}