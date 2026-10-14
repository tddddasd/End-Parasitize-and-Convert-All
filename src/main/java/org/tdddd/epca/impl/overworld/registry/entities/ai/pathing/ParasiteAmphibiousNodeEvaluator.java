package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

/**
 * Amphibious flavour of {@link ParasiteNodeEvaluator}, for the infested drowned.
 *
 * <p>In the 1.20.1 tree this class also overrode {@code isAmphibious()} to return true, because the
 * abandoned gap-landing neighbour code used it to decide whether water counted as a legal landing.
 * That code is gone and {@code getNeighbors} is vanilla again, so the override did nothing except
 * depend on a protected hook whose name and existence could not be confirmed in 26.1.2. It has been
 * dropped on purpose.</p>
 *
 * <p>Water behaviour is unaffected: the infested drowned takes
 * {@link ParasiteAmphibiousNavigation}, which builds an {@code AmphibiousPathNavigation} evaluator
 * first and copies its {@code canFloat} (and door / fence) switches onto this evaluator, and the
 * drowned keeps its own {@code InfestedMoveControl} for swimming.</p>
 */
public class ParasiteAmphibiousNodeEvaluator extends ParasiteNodeEvaluator {

    public ParasiteAmphibiousNodeEvaluator(int maxGapJumpWidth) {
        super(maxGapJumpWidth);
    }
}
