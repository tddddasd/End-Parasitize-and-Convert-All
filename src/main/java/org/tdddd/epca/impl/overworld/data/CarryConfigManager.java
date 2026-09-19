package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CarryConfigManager implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FOLDER = "entity_carry"; 

    private Map<Identifier, CarryConfig> configs = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Identifier, CarryConfig> newConfigs = new HashMap<>();

        
        resourceManager.listResources(FOLDER, file -> file.getPath().endsWith(".json"))
                .forEach((fileId, resource) -> {
                    
                    
                    String path = fileId.getPath();
                    String fileName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                    Identifier entityId = Identifier.fromNamespaceAndPath(fileId.getNamespace(), fileName);

                    try (InputStream stream = resource.open()) {
                        JsonElement json = JsonParser.parseReader(new InputStreamReader(stream));
                        // 26.1.2 ships DataFixerUpper 9, which removed the two-argument
                        // getOrThrow(boolean, Consumer). The no-argument form throws instead, and the
                        // catch below logs the offending file and skips it rather than storing null.
                        CarryConfig config = CarryConfig.CODEC.parse(JsonOps.INSTANCE, json)
                                .getOrThrow();
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
        Identifier carrierId = BuiltInRegistries.ENTITY_TYPE.getKey(carrierType);
        if (carrierId == null) return false;

        CarryConfig config = configs.get(carrierId);
        if (config == null) return false;

        return config.getCarryable().contains(BuiltInRegistries.ENTITY_TYPE.getKey(targetType));
    }

    
    public static final CarryConfigManager INSTANCE = new CarryConfigManager();
}