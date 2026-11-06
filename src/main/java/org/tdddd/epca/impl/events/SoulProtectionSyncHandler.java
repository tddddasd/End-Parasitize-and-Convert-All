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

@Mod.EventBusSubscriber
public final class SoulProtectionSyncHandler {

    public static final int SYNC_INTERVAL_TICKS = 20;

    public static final double BLOCKS_PER_TRACKING_UNIT = 16.0D;

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

            int offset = Math.floorMod(level.dimension().location().hashCode(), SYNC_INTERVAL_TICKS);
            if (Math.floorMod(tickCounter, SYNC_INTERVAL_TICKS) == offset) {
                syncLevel(level);
            }
        }
    }

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

            ModNetwork.sendToPlayer(player, new SyncSoulProtectionPacket(entityIds, remainingTicks));
        }
    }

    private static double trackingRange(ServerLevel level, LivingEntity entity) {
        int units = entity.getType().clientTrackingRange();
        if (units <= 0) {
            return 0.0D;
        }
        return level.getServer().getScaledTrackingDistance((int) (units * BLOCKS_PER_TRACKING_UNIT));
    }

    private static final class Batch {
        private final List<Integer> entityIds = new ArrayList<>();
        private final List<Integer> remainingTicks = new ArrayList<>();
    }
}
