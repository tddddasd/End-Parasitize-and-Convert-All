package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class ParasiteNodeEvaluator extends WalkNodeEvaluator {

    private static final boolean ENABLE_GAP_LANDINGS = false;

    private final int maxGapJumpWidth;

    public ParasiteNodeEvaluator(int maxGapJumpWidth) {
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    protected int getMaxGapJumpWidth() {
        return this.maxGapJumpWidth;
    }

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {

        if (!ENABLE_GAP_LANDINGS) {
            return super.getNeighbors(outputArray, node);
        }

        return super.getNeighbors(outputArray, node);
    }
}
