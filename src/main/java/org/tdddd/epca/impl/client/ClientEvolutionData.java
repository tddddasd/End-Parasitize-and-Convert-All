package org.tdddd.epca.impl.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class ClientEvolutionData {
    private static final Map<ResourceLocation, Integer> DIMENSION_STAGES = new HashMap<>();

    public static void updateStage(ResourceLocation dim, int stage) {
        DIMENSION_STAGES.put(dim, stage);
    }

    public static int getStageForDimension(Level level) {
        if (level == null) return 0;
        return DIMENSION_STAGES.getOrDefault(level.dimension().location(), 0);
    }

    
    public static void clear() {
        DIMENSION_STAGES.clear();
    }
}