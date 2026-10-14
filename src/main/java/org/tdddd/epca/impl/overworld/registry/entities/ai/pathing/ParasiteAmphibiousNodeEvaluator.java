package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * Amphibious flavour of {@link ParasiteNodeEvaluator}, for the infested drowned.
 *
 * <p>It used to override {@code isAmphibious()} for the abandoned gap-landing neighbour code, which
 * is gone; the override is kept because it is harmless in 1.20.1 and the amphibious evaluator is
 * what {@link ParasiteAmphibiousNavigation} builds. Water behaviour itself comes from the
 * {@code AmphibiousPathNavigation} switches that the navigation copies onto this evaluator.</p>
 */
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
