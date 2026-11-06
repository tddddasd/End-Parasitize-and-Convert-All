package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.level.pathfinder.BlockPathTypes;

public class ParasiteAmphibiousNodeEvaluator extends ParasiteNodeEvaluator {

    public ParasiteAmphibiousNodeEvaluator(int maxGapJumpWidth) {
        super(maxGapJumpWidth);
    }

    @Override
    protected boolean isAmphibious() {
        return true;
    }

    @Override
    protected boolean isLeapLandingType(BlockPathTypes pathType) {
        return super.isLeapLandingType(pathType) || pathType == BlockPathTypes.WATER;
    }
}
