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

/**
 * Server half of the soul protection sync: periodically tells every player which of the entities it
 * is tracking currently carry {@code epca:soul_protection}, so the client can draw the aura on
 * creatures (vanilla never syncs a mob's active effects to other clients).
 *
 * <p>It is the batch counterpart of {@code InfestedBlockManager#syncAllToPlayer} and
 * {@code SyncAllInfestedSourcesPacket}: one packet per player per interval, carrying the whole set.
 * It runs on {@link ServerTickEvent.Post}, which fires on the logical server in both a singleplayer /
 * LAN world (integrated server) and a dedicated server, and it only reads state the server already
 * has - no client input, no saved data, no chunk loading.</p>
 *
 * <h2>Cost</h2>
 * <ul>
 *   <li>one pass over a level's entities every {@link #SYNC_INTERVAL_TICKS} ticks, and the levels are
 *       staggered so only one of them is scanned per tick;</li>
 *   <li>per affected entity, {@code ChunkMap#getPlayersWatching} returns exactly the players that
 *       already track it (a NeoForge getter for the tracker's {@code seenBy} set), so the packet goes
 *       to precisely the clients that can see the entity and nothing is sent to anybody else;</li>
 *   <li>a player with nothing to report is skipped entirely unless its previous batch was non-empty,
 *       in which case one empty batch is sent so the removal propagates;</li>
 *   <li>each batch is capped at {@link SyncSoulProtectionPacket#MAX_ENTRIES} entries and encoded as
 *       varints.</li>
 * </ul>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class SoulProtectionSyncHandler {

    /** Ticks between two syncs of the same level. */
    public static final int SYNC_INTERVAL_TICKS = 20;

    /** Ticks between two sweeps of the per-player bookkeeping for players that left. */
    private static final int BOOKKEEPING_INTERVAL_TICKS = SYNC_INTERVAL_TICKS * 5;

    /** Players whose last batch was non-empty, so the sender knows it must push an empty one. */
    private static final Map<UUID, Boolean> LAST_BATCH_NON_EMPTY = new HashMap<>();

    /** Server ticks seen by this handler; staggers the levels and paces the bookkeeping. */
    private static long serverTickCounter;

    private SoulProtectionSyncHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        serverTickCounter++;

        int levelIndex = 0;
        for (ServerLevel level : server.getAllLevels()) {
            // Staggered per level: level i is synced on the ticks where (counter + i) is a multiple of
            // the interval, so a multi-dimension server spreads the work over the tick.
            if ((serverTickCounter + levelIndex) % SYNC_INTERVAL_TICKS == 0L && !level.players().isEmpty()) {
                syncLevel(level);
            }
            levelIndex++;
        }

        if (serverTickCounter % BOOKKEEPING_INTERVAL_TICKS == 0L) {
            pruneBookkeeping(server);
        }
    }

    /**
     * Sends one batch per tracking player of this level. Entities with the effect are collected once
     * for the whole level and then attributed to the players that track them.
     */
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
                // Nothing to report. Stay silent unless the previous batch was non-empty, in which
                // case one empty batch is what tells the client that every cached entity is gone.
                if (Boolean.TRUE.equals(LAST_BATCH_NON_EMPTY.remove(player.getUUID()))) {
                    ModNetwork.sendToPlayer(player, SyncSoulProtectionPacket.empty());
                }
                continue;
            }
            ModNetwork.sendToPlayer(player, buildPacket(batch));
            LAST_BATCH_NON_EMPTY.put(player.getUUID(), Boolean.TRUE);
        }
    }

    /** Turns one player's entities into the wire batch: entity id plus remaining duration ticks. */
    private static SyncSoulProtectionPacket buildPacket(List<LivingEntity> batch) {
        int[] entityIds = new int[batch.size()];
        int[] remainingTicks = new int[batch.size()];
        for (int index = 0; index < batch.size(); index++) {
            LivingEntity living = batch.get(index);
            entityIds[index] = living.getId();
            MobEffectInstance effect = living.getEffect(ModEffects.SOUL_PROTECTION);
            // getDuration() is -1 for an infinite effect, which is exactly what the client needs in
            // order not to fade out; a null instance can only mean the effect just expired.
            remainingTicks[index] = effect == null
                    ? SyncSoulProtectionPacket.INFINITE_DURATION
                    : effect.getDuration();
        }
        return new SyncSoulProtectionPacket(entityIds, remainingTicks);
    }

    /** Forgets the bookkeeping of players that are no longer online. */
    private static void pruneBookkeeping(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
        }
        LAST_BATCH_NON_EMPTY.keySet().retainAll(online);
    }
}
