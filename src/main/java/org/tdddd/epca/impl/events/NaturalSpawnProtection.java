package org.tdddd.epca.impl.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mod.EventBusSubscriber
public class NaturalSpawnProtection {
    
    private static final Map<Integer, Map<Class<?>, Integer>> STAGE_MINIMUM_SPAWN_CONFIG = new HashMap<>();

    
    static {
        
        Map<Class<?>, Integer> stage0 = new HashMap<>();
        stage0.put(IOnesent.class, 10);
        stage0.put(IInfested.class, 5);
        stage0.put(IPoverty.class, 3);
        STAGE_MINIMUM_SPAWN_CONFIG.put(0, stage0);

        
        Map<Class<?>, Integer> stage1 = new HashMap<>();
        stage1.put(IOnesent.class, 10);
        stage1.put(IInfested.class, 5);
        stage1.put(IPoverty.class, 3);
        stage1.put(IReshape.class, 1);
        STAGE_MINIMUM_SPAWN_CONFIG.put(1, stage1);

        
        Map<Class<?>, Integer> stage2 = new HashMap<>();
        stage2.put(IOnesent.class, 5);
        stage2.put(IInfested.class, 15);
        stage2.put(IPoverty.class, 5);
        stage2.put(IReshape.class, 1);
        STAGE_MINIMUM_SPAWN_CONFIG.put(2, stage2);

        
        Map<Class<?>, Integer> stage3 = new HashMap<>();
        stage3.put(IOnesent.class, 5);
        stage3.put(IInfested.class, 15);
        stage3.put(IPoverty.class, 5);
        stage3.put(IReshape.class, 2);
        STAGE_MINIMUM_SPAWN_CONFIG.put(3, stage3);

        
        Map<Class<?>, Integer> stage4 = new HashMap<>();
        stage4.put(IOnesent.class, 3);
        stage4.put(IInfested.class, 15);
        stage4.put(IPoverty.class, 5);
        stage4.put(IReshape.class, 3);
        STAGE_MINIMUM_SPAWN_CONFIG.put(4, stage4);

        
        Map<Class<?>, Integer> stage5 = new HashMap<>();
        stage5.put(IOnesent.class, 2);
        stage5.put(IInfested.class, 10);
        stage5.put(IPoverty.class, 5);
        stage5.put(IReshape.class, 4);
        STAGE_MINIMUM_SPAWN_CONFIG.put(5, stage5);

        
        Map<Class<?>, Integer> stage6 = new HashMap<>();
        stage6.put(IInfested.class, 10);
        stage6.put(IPoverty.class, 5);
        stage6.put(IReshape.class, 5);
        STAGE_MINIMUM_SPAWN_CONFIG.put(6, stage6);

        
        Map<Class<?>, Integer> stage7 = new HashMap<>();
        stage7.put(IInfested.class, 5);
        stage7.put(IPoverty.class, 5);
        stage7.put(IReshape.class, 5);
        STAGE_MINIMUM_SPAWN_CONFIG.put(7, stage7);

        
        Map<Class<?>, Integer> stage8 = new HashMap<>();
        stage8.put(IPoverty.class, 5);
        stage8.put(IReshape.class, 5);
        STAGE_MINIMUM_SPAWN_CONFIG.put(8, stage8);

        
        Map<Class<?>, Integer> stage9 = new HashMap<>();
        stage9.put(IPoverty.class, 5);
        stage9.put(IReshape.class, 6);
        STAGE_MINIMUM_SPAWN_CONFIG.put(9, stage9);

        
        Map<Class<?>, Integer> stage10 = new HashMap<>();
        stage10.put(IPoverty.class, 5);
        stage10.put(IReshape.class, 7);
        STAGE_MINIMUM_SPAWN_CONFIG.put(10, stage10);
    }

    
    public static final int CHECK_RADIUS = 256;

    
    private static final double PLAYER_COUNT_BONUS = 0.10;

    
    private static final int MAX_THRESHOLD = 30;

    
    public static Class<?> getMobCategory(Mob mob) {
        if (mob instanceof IOnesent) {
            return IOnesent.class;
        } else if (mob instanceof IInfested) {
            return IInfested.class;
        } else if (mob instanceof IPoverty) {
            return IPoverty.class;
        } else if (mob instanceof IReshape) {
            return IReshape.class;
        }
        return null;
    }

    
    public static int calculateAdjustedThreshold(ServerLevel level, Class<?> category) {
        if (level == null) {
            return 0;
        }

        int stage = EvolutionManager.getStageForDimension(level);
        Map<Class<?>, Integer> config = STAGE_MINIMUM_SPAWN_CONFIG.get(stage);

        if (config == null || !config.containsKey(category)) {
            return 0;
        }

        int baseThreshold = config.get(category);

        
        int playerCount = 0;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(level.dimension())) {
                playerCount++;
            }
        }

        
        double multiplier = 1.0 + ((playerCount - 1) * PLAYER_COUNT_BONUS);
        int adjustedThreshold = (int) Math.ceil(baseThreshold * multiplier);

        
        return Math.min(adjustedThreshold, MAX_THRESHOLD);
    }

    
    public static boolean canBeNaturallyDespawned(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        Class<?> category = getMobCategory(mob);
        if (category == null) {
            return true;
        }

        
        List<ServerPlayer> players = serverLevel.getServer().getPlayerList().getPlayers();

        
        ServerPlayer nearestPlayer = findNearestPlayerInDimension(mob, players, serverLevel);
        if (nearestPlayer == null) {
            return true;
        }

        
        double distanceSq = mob.distanceToSqr(nearestPlayer);
        if (distanceSq > CHECK_RADIUS * CHECK_RADIUS) {
            return true;
        }

        
        int threshold = calculateAdjustedThreshold(serverLevel, category);
        if (threshold <= 0) {
            return true;
        }

        
        int countInRadius = countMobsInRadius(serverLevel, nearestPlayer, category, CHECK_RADIUS);

        
        return countInRadius > threshold;
    }

    
    private static ServerPlayer findNearestPlayerInDimension(Mob mob, List<ServerPlayer> players, ServerLevel level) {
        ServerPlayer nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            if (!player.level().dimension().equals(level.dimension())) {
                continue;
            }

            double distance = mob.distanceToSqr(player);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    
    private static int countMobsInRadius(ServerLevel level, ServerPlayer player, Class<?> category, int radius) {
        int count = 0;

        for (Mob mob : level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(radius))) {
            if (category.isInstance(mob)) {
                count++;
            }
        }

        return count;
    }

    
    public static final String NATURAL_SPAWN_TAG = "epca_natural_spawn";

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        
        if (!(mob instanceof IParasite)) return;

        MobSpawnType spawnType = event.getSpawnType();
        
        boolean isNatural = spawnType == MobSpawnType.NATURAL ||
                spawnType == MobSpawnType.CHUNK_GENERATION;
        
        
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            CompoundTag persistentData = mob.getPersistentData();
            persistentData.putBoolean(NATURAL_SPAWN_TAG, true);
        }
    }

    
    public static boolean isNaturallySpawned(Mob mob) {
        return mob.getPersistentData().getBoolean(NATURAL_SPAWN_TAG);
    }
}