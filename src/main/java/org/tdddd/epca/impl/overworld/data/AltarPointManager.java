package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class AltarPointManager implements ResourceManagerReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final int DEFAULT_POINTS = 0;          
    private static final Map<ResourceLocation, Integer> POINTS_MAP = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        POINTS_MAP.clear();

        
        resourceManager.listResources("altar_points", path -> path.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonObject root = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
                        parseConfig(root);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void parseConfig(JsonObject root) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String blockId = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                if (obj.has("points")) {
                    int points = obj.get("points").getAsInt();
                    if (points < 0) {
                        points = 0;
                    }
                    ResourceLocation key = ResourceLocation.tryParse(blockId);
                    if (key != null) {
                        POINTS_MAP.put(key, points);
                    }
                }
            }
        }
    }

    
    public static int getPoints(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null) return DEFAULT_POINTS;
        return POINTS_MAP.getOrDefault(key, DEFAULT_POINTS);
    }

    
    public static int getPoints(String blockId) {
        ResourceLocation key = ResourceLocation.tryParse(blockId);
        if (key == null) return DEFAULT_POINTS;
        return POINTS_MAP.getOrDefault(key, DEFAULT_POINTS);
    }

    
    public static Map<ResourceLocation, Integer> getAllPoints() {
        return new HashMap<>(POINTS_MAP);
    }
}