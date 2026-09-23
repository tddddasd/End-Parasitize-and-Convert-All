package org.tdddd.epca.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureEffect;

import java.util.HashMap;
import java.util.Map;

public class ClientEvolutionData {
    private static final Map<Identifier, Integer> DIMENSION_STAGES = new HashMap<>();

    /**
     * The last client level instance seen. Changing world or dimension replaces the object, which is
     * how "the stage has to be re-learned" is detected.
     *
     * <p>Ported from the 1.20.1 twin; this field and the trigger below were the two pieces of the
     * world-barrier feature that lived in this class.</p>
     */
    private static Level lastSeenLevel;

    public static void updateStage(Identifier dim, int stage) {
        Minecraft mc = Minecraft.getInstance();

        // Entering a new world / switching dimension: forget what was known. The next sync is then
        // treated as a first sync (previous == null) and does not trigger the rupture, so leaving a
        // stage-5 world and joining a stage-10 one does not blow up the sky on login.
        if (mc.level != null && mc.level != lastSeenLevel) {
            lastSeenLevel = mc.level;
            DIMENSION_STAGES.clear();
            SkyRuptureEffect.stop();
        }

        Integer previous = DIMENSION_STAGES.put(dim, stage);

        // -- world barrier rupture: only on a stage INCREASE, and only in the dimension the player is
        //    currently in --
        // previous == null: first sync (joining a world / changing dimension), record only, no trigger.
        // A stage decrease (an admin rolling back) does not trigger either.
        if (previous != null && stage > previous) {
            if (mc.level != null && mc.level.dimension().identifier().equals(dim)) {
                SkyRuptureEffect.trigger(stage);
            }
        }
    }

    public static int getStageForDimension(Level level) {
        if (level == null) return 0;
        // 26.1.2: ResourceKey#location() was renamed to #identifier().
        return DIMENSION_STAGES.getOrDefault(level.dimension().identifier(), 0);
    }

    
    public static void clear() {
        DIMENSION_STAGES.clear();
        lastSeenLevel = null;
        SkyRuptureEffect.stop();
    }
}
