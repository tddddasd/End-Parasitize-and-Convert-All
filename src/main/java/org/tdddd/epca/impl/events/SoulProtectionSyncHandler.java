package org.tdddd.epca.impl.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncSoulProtectionPacket;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server half of the {@code epca:soul_protection} flame sync.
 *
 * <p>Vanilla only sends {@code ClientboundUpdateMobEffectPacket} for an effect to the affected player
 * and to that player's passengers, so a client would otherwise never learn that a creature carries
 * the effect. This handler therefore pushes the authoritative set of soul-protected entities to every
 * player as a {@link SyncSoulProtectionPacket} batch.</p>
 *
 * <h2>Cadence and cost</h2>
 * <ul>
 *   <li>Every level is synced once per {@link #SYNC_INTERVAL_TICKS} ticks, and the levels are
 *       staggered across that window by the hash of their dimension id, so no single tick sends every
 *       level's batch.</li>
 *   <li>One pass over the level's entity list per sync; only entities that really carry the effect
 *       then cost a pass over the level's players, so the common case (nobody has the effect) is one
 *       entity iteration per level per second.</li>
 *   <li>Every player always receives a batch, empty ones included, because the batch is the complete
 *       set: that is what propagates removals and what refreshes the durations the client counts down
 *       locally.</li>
 *   <li>A batch carries at most {@link SyncSoulProtectionPacket#MAX_ENTRIES} entries.</li>
 *   <li>Each player only receives entities within that entity's vanilla client tracking range
 *       ({@code EntityType#clientTrackingRange()} chunks, scaled by
 *       {@code MinecraftServer#getScaledTrackingDistance} exactly like {@code ChunkMap} does), so the
 *       client is only told about entities it can be shown.</li>
 * </ul>
 *
 * <p>It is a plain server tick subscriber, so it runs unchanged on an integrated server and on a
 * dedicated server; it never touches a client class.</p>
 */
@Mod.EventBusSubscriber
public final class SoulProtectionSyncHandler {

    /** Ticks between two batches for the same level. */
    public static final int SYNC_INTERVAL_TICKS = 20;

    /** Blocks per unit of {@code EntityType#clientTrackingRange()} (vanilla: chunks to blocks). */
    public static final double BLOCKS_PER_TRACKING_UNIT = 16.0D;

    /** Server tick counter, used to stagger the levels and to time the interval. */
    private static int tickCounter;

    private SoulProtectionSyncHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            // Stagger the levels over the interval by their dimension id, so the batches of a server
            // with several levels never all go out on the same tick. The hash is order-independent,
            // so a changing level iteration order cannot skip or double-sync a level.
            int offset = Math.floorMod(level.dimension().location().hashCode(), SYNC_INTERVAL_TICKS);
            if (Math.floorMod(tickCounter, SYNC_INTERVAL_TICKS) == offset) {
                syncLevel(level);
            }
        }
    }

    /** Collects the soul-protected entities of one level per player and sends one batch each. */
    private static void syncLevel(ServerLevel level) {
        MobEffect effectType = ModEffects.SOUL_PROTECTION.get();
        List<ServerPlayer> players = level.players();
        if (effectType == null || players.isEmpty()) {
            return;
        }

        Map<ServerPlayer, Batch> batches = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.hasEffect(effectType)) {
                continue;
            }
            MobEffectInstance instance = living.getEffect(effectType);
            if (instance == null) {
                continue;
            }
            int remainingTicks = instance.isInfiniteDuration()
                    ? SyncSoulProtectionPacket.INFINITE_DURATION
                    : instance.getDuration();
            double range = trackingRange(level, living);
            if (range <= 0.0D) {
                // Vanilla never tracks this type for a client, so there is nothing to show it.
                continue;
            }
            double rangeSqr = range * range;
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(living) > rangeSqr) {
                    continue;
                }
                Batch batch = batches.computeIfAbsent(player, key -> new Batch());
                if (batch.entityIds.size() >= SyncSoulProtectionPacket.MAX_ENTRIES) {
                    continue;
                }
                batch.entityIds.add(living.getId());
                batch.remainingTicks.add(remainingTicks);
            }
        }

        for (ServerPlayer player : players) {
            Batch batch = batches.get(player);
            int size = batch == null ? 0 : batch.entityIds.size();
            int[] entityIds = new int[size];
            int[] remainingTicks = new int[size];
            for (int i = 0; i < size; i++) {
                entityIds[i] = batch.entityIds.get(i).intValue();
                remainingTicks[i] = batch.remainingTicks.get(i).intValue();
            }
            // Always sent, even when empty: the batch is the full set, so an empty one clears the
            // client cache and thereby propagates a removal.
            ModNetwork.sendToPlayer(player, new SyncSoulProtectionPacket(entityIds, remainingTicks));
        }
    }

    /**
     * Client tracking range of one entity in blocks, computed exactly like
     * {@code ChunkMap.TrackedEntity}: the type's chunk range converted to blocks and then scaled by
     * the server's tracking distance.
     *
     * @return the range in blocks, or 0 when the type is never tracked
     */
    private static double trackingRange(ServerLevel level, LivingEntity entity) {
        int units = entity.getType().clientTrackingRange();
        if (units <= 0) {
            return 0.0D;
        }
        return level.getServer().getScaledTrackingDistance((int) (units * BLOCKS_PER_TRACKING_UNIT));
    }

    /** Mutable per-player batch under construction. */
    private static final class Batch {
        private final List<Integer> entityIds = new ArrayList<>();
        private final List<Integer> remainingTicks = new ArrayList<>();
    }
}
