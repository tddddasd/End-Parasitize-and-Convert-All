package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class EntityKillCountManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    
    private static final Map<String, Integer> MAX_KILL_COUNTS = new HashMap<>();

    
    private static final String KILL_COUNT_KEY = "epca_kill_count";

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        MAX_KILL_COUNTS.clear();

        resourceManager.listResources("kill_entity_counts", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        Map<String, Integer> killCounts = GSON.fromJson(
                                new InputStreamReader(stream),
                                new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType()
                        );

                        MAX_KILL_COUNTS.putAll(killCounts);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    
    public static int getMaxKillCount(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return MAX_KILL_COUNTS.getOrDefault(entityId, 0);
    }

    
    public static int getMaxKillCount(String entityId) {
        return MAX_KILL_COUNTS.getOrDefault(entityId, 0);
    }

    
    public static int getCurrentKillCount(LivingEntity entity) {
        CompoundTag nbt = entity.getPersistentData();
        return nbt.getInt(KILL_COUNT_KEY);
    }

    
    public static void setKillCount(LivingEntity entity, int count) {
        CompoundTag nbt = entity.getPersistentData();
        nbt.putInt(KILL_COUNT_KEY, count);
    }

    
    public static void incrementKillCount(LivingEntity entity) {
        int current = getCurrentKillCount(entity);
        setKillCount(entity, current + 1);
    }

    
    public static boolean isKillCountMaxed(LivingEntity entity) {
        int max = getMaxKillCount(entity);
        if (max == 0) return false; 

        int current = getCurrentKillCount(entity);
        return current >= max;
    }

    
    public static boolean hasKillCount(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return MAX_KILL_COUNTS.containsKey(entityId);
    }

    
    public static Map<String, Integer> getAllMaxKillCounts() {
        return new HashMap<>(MAX_KILL_COUNTS);
    }
}