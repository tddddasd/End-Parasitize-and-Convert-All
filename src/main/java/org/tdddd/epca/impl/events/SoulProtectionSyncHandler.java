package org.tdddd.epca.impl.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncSoulProtectionPacket;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = epca.MODID)
public final class SoulProtectionSyncHandler {

    public static final int SYNC_INTERVAL_TICKS = 20;

    private static final int BOOKKEEPING_INTERVAL_TICKS = SYNC_INTERVAL_TICKS * 5;

    private static final Map<UUID, Boolean> LAST_BATCH_NON_EMPTY = new HashMap<>();

    private static long serverTickCounter;

    private SoulProtectionSyncHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        serverTickCounter++;

        int levelIndex = 0;
        for (ServerLevel level : server.getAllLevels()) {

            if ((serverTickCounter + levelIndex) % SYNC_INTERVAL_TICKS == 0L && !level.players().isEmpty()) {
                syncLevel(level);
            }
            levelIndex++;
        }

        if (serverTickCounter % BOOKKEEPING_INTERVAL_TICKS == 0L) {
            pruneBookkeeping(server);
        }
    }

    private static void syncLevel(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        Map<ServerPlayer, List<LivingEntity>> batches = new HashMap<>();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living)
                    || !living.hasEffect(ModEffects.SOUL_PROTECTION)) {
                continue;
            }
            for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayersWatching(living)) {
                List<LivingEntity> batch = batches.computeIfAbsent(watcher, key -> new ArrayList<>());
                if (batch.size() < SyncSoulProtectionPacket.MAX_ENTRIES) {
                    batch.add(living);
                }
            }
        }

        for (ServerPlayer player : players) {
            List<LivingEntity> batch = batches.get(player);
            if (batch == null || batch.isEmpty()) {

                if (Boolean.TRUE.equals(LAST_BATCH_NON_EMPTY.remove(player.getUUID()))) {
                    ModNetwork.sendToPlayer(player, SyncSoulProtectionPacket.empty());
                }
                continue;
            }
            ModNetwork.sendToPlayer(player, buildPacket(batch));
            LAST_BATCH_NON_EMPTY.put(player.getUUID(), Boolean.TRUE);
        }
    }

    private static SyncSoulProtectionPacket buildPacket(List<LivingEntity> batch) {
        int[] entityIds = new int[batch.size()];
        int[] remainingTicks = new int[batch.size()];
        for (int index = 0; index < batch.size(); index++) {
            LivingEntity living = batch.get(index);
            entityIds[index] = living.getId();
            MobEffectInstance effect = living.getEffect(ModEffects.SOUL_PROTECTION);

            remainingTicks[index] = effect == null
                    ? SyncSoulProtectionPacket.INFINITE_DURATION
                    : effect.getDuration();
        }
        return new SyncSoulProtectionPacket(entityIds, remainingTicks);
    }

    private static void pruneBookkeeping(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
        }
        LAST_BATCH_NON_EMPTY.keySet().retainAll(online);
    }
}
