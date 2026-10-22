package org.tdddd.epca.impl.overworld.data;

import net.minecraft.resources.Identifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class EntityBiomassCountManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    
    private static final Map<String, Integer> MAX_BIOMASS_COUNTS = new HashMap<>();

    
    private static final String BIOMASS_COUNT_KEY = "epca_kill_count";

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        MAX_BIOMASS_COUNTS.clear();

        resourceManager.listResources("kill_entity_counts", file -> file.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open()) {
                        Map<String, Integer> biomassCounts = GSON.fromJson(
                                new InputStreamReader(stream),
                                new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType()
                        );

                        MAX_BIOMASS_COUNTS.putAll(biomassCounts);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    
    public static int getMaxBiomassCount(LivingEntity entity) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return MAX_BIOMASS_COUNTS.getOrDefault(entityId, 0);
    }

    
    public static int getMaxBiomassCount(String entityId) {
        return MAX_BIOMASS_COUNTS.getOrDefault(entityId, 0);
    }

    
    public static int getCurrentBiomassCount(LivingEntity entity) {
        CompoundTag nbt = entity.getPersistentData();
        return nbt.getInt(BIOMASS_COUNT_KEY).orElse(0);
    }

    
    public static void setBiomassCount(LivingEntity entity, int count) {
        CompoundTag nbt = entity.getPersistentData();
        nbt.putInt(BIOMASS_COUNT_KEY, count);
    }

    
    public static void incrementBiomassCount(LivingEntity entity) {
        int current = getCurrentBiomassCount(entity);
        setBiomassCount(entity, current + 1);
    }

    
    public static boolean isBiomassCountMaxed(LivingEntity entity) {
        int max = getMaxBiomassCount(entity);
        if (max == 0) return false; 

        int current = getCurrentBiomassCount(entity);
        return current >= max;
    }

    
    public static boolean hasBiomassCount(LivingEntity entity) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return MAX_BIOMASS_COUNTS.containsKey(entityId);
    }

    
    public static Map<String, Integer> getAllMaxBiomassCounts() {
        return new HashMap<>(MAX_BIOMASS_COUNTS);
    }

    // ---- unified access for every biomass carrier (creatures, blocks) ----

    public static int getCurrentBiomassCount(BiomassStorage storage) {
        return storage.getBiomassCount();
    }

    public static int getMaxBiomassCount(BiomassStorage storage) {
        return storage.getMaxBiomassCount();
    }

    public static void setBiomassCount(BiomassStorage storage, int count) {
        storage.setBiomassCount(count);
    }

    public static void incrementBiomassCount(BiomassStorage storage) {
        addBiomassCount(storage, 1);
    }

    public static void addBiomassCount(BiomassStorage storage, int amount) {
        int next = Math.min(storage.getBiomassCount() + amount, storage.getMaxBiomassCount());
        storage.setBiomassCount(Math.max(next, 0));
    }

    public static void decrementBiomassCount(BiomassStorage storage) {
        addBiomassCount(storage, -1);
    }

    public static boolean isBiomassCountMaxed(BiomassStorage storage) {
        return storage.getBiomassCount() >= storage.getMaxBiomassCount();
    }
}
