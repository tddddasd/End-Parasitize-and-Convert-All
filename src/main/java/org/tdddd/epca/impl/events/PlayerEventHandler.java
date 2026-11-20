package org.tdddd.epca.impl.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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

    /**
     * EPCA's login initialisation: per dimension evolution stat sync and the like biome snapshot.
     * This runs on the server thread inside {@code PlayerList#placeNewPlayer}.
     * 领巢者名单的登录补发见 {@code HiveLeaderHandler.onPlayerLogin}。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // per dimension erosion stage; reading it may touch the EvolutionDataStorage saved data
        for (ServerLevel level : player.server.getAllLevels()) {
            try {
                EvolutionManager em = EvolutionManager.forDimension(level);
                em.syncToPlayer(player);
            } catch (RuntimeException exception) {
                epca.LOGGER.error("evolution.syncToPlayer {} failed",
                        level.dimension().location(), exception);
            }
        }

        // full like biome snapshot: the client cache starts empty on every login. Queueing it is
        // (and must stay) O(1); the copy of the key list happens on the next tick.
        org.tdddd.epca.impl.overworld.data.LikeBiomeManager.syncAllToPlayer(player);

        // 领巢者名单的登录补发已统一到 HiveLeaderHandler.onPlayerLogin -> HiveLeaderManager.syncTo，
        // 这里不再重复发（同一个事件里发两份内容相同的包没有意义）。
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // like biome units are per dimension, the client cache has to be replaced
            org.tdddd.epca.impl.overworld.data.LikeBiomeManager.syncAllToPlayer(player);
        }
    }
}
