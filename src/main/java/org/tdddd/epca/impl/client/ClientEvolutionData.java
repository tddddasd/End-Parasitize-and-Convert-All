package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureEffect;

import java.util.HashMap;
import java.util.Map;

public class ClientEvolutionData {
    private static final Map<ResourceLocation, Integer> DIMENSION_STAGES = new HashMap<>();

    /**
     *  /
     * ""
     */
    private static Level lastSeenLevel;

    public static void updateStage(ResourceLocation dim, int stage) {
        Minecraft mc = Minecraft.getInstance();

        //  /
        // ""previous == null
        // " 5  10 "
        if (mc.level != null && mc.level != lastSeenLevel) {
            lastSeenLevel = mc.level;
            DIMENSION_STAGES.clear();
            SkyRuptureEffect.stop();
        }

        Integer previous = DIMENSION_STAGES.put(dim, stage);

        //  ""
        // previous == null /
        if (previous != null && stage > previous) {
            if (mc.level != null && mc.level.dimension().location().equals(dim)) {
                SkyRuptureEffect.trigger(stage);
            }
        }
    }

    public static int getStageForDimension(Level level) {
        if (level == null) return 0;
        return DIMENSION_STAGES.getOrDefault(level.dimension().location(), 0);
    }

    
    public static void clear() {
        DIMENSION_STAGES.clear();
        lastSeenLevel = null;
        SkyRuptureEffect.stop();
    }
}

