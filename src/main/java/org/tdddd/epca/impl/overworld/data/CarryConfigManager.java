package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CarryConfigManager implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FOLDER = "entity_carry"; 

    private Map<ResourceLocation, CarryConfig> configs = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<ResourceLocation, CarryConfig> newConfigs = new HashMap<>();

        
        resourceManager.listResources(FOLDER, file -> file.getPath().endsWith(".json"))
                .forEach((fileId, resource) -> {
                    
                    
                    String path = fileId.getPath();
                    String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                    ResourceLocation entityId = new ResourceLocation(fileId.getNamespace(), fileName);

                    try (InputStream stream = resource.open()) {
                        JsonElement json = JsonParser.parseReader(new InputStreamReader(stream));
                        CarryConfig config = CarryConfig.CODEC.parse(JsonOps.INSTANCE, json)
                                .getOrThrow(false, LOGGER::error);
                        newConfigs.put(entityId, config);
                        LOGGER.debug("Loaded carry config for entity: {}", entityId);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load carry config {}: {}", fileId, e.getMessage());
                    }
                });

        this.configs = newConfigs;
        LOGGER.info("Loaded {} carry configs", configs.size());
    }

    public boolean isEntityCarryable(EntityType<?> carrierType, EntityType<?> targetType) {
        ResourceLocation carrierId = ForgeRegistries.ENTITY_TYPES.getKey(carrierType);
        if (carrierId == null) return false;

        CarryConfig config = configs.get(carrierId);
        if (config == null) return false;

        return config.getCarryable().contains(ForgeRegistries.ENTITY_TYPES.getKey(targetType));
    }

    
    public static final CarryConfigManager INSTANCE = new CarryConfigManager();
}