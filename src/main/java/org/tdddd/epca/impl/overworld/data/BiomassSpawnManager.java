package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class BiomassSpawnManager implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, BiomassSpawnConfig> CONFIGS = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        CONFIGS.clear();

        resourceManager.listResources("biomass_spawns",
                        file -> file.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (var reader = new InputStreamReader(resource.open())) {
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        BiomassSpawnConfig config = GSON.fromJson(json, BiomassSpawnConfig.class);

                        String fileName = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
                        String entityId = fileName.substring(0, fileName.length() - 5);
                        String fullEntityId = new ResourceLocation("epca", entityId).toString();
                        CONFIGS.put(fullEntityId, config);
                    } catch (Exception e) {
                    }
                });
    }

    public static BiomassSpawnConfig getConfig(EntityType<?> entityType) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        return key != null ? CONFIGS.get(key.toString()) : null;
    }
}