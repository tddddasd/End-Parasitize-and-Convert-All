package org.tdddd.epca.impl.client;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class ClientEvolutionData {
    private static final Map<Identifier, Integer> DIMENSION_STAGES = new HashMap<>();

    public static void updateStage(Identifier dim, int stage) {
        DIMENSION_STAGES.put(dim, stage);
    }

    public static int getStageForDimension(Level level) {
        if (level == null) return 0;
        // 26.1.2: ResourceKey#location() was renamed to #identifier().
        return DIMENSION_STAGES.getOrDefault(level.dimension().identifier(), 0);
    }

    
    public static void clear() {
        DIMENSION_STAGES.clear();
    }
}