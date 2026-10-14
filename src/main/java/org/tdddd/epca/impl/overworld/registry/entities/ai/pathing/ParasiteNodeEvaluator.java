package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Vanilla walking node evaluator. The parasite gap jump does <b>not</b> add pathfinder neighbours.
 *
 * <p>History: an earlier revision overrode {@code getNeighbors} and appended landing nodes for every
 * gap width up to {@code maxGapJumpWidth} in all eight horizontal directions and three vertical
 * offsets on <em>every</em> node expansion, with two block-state lookups per intermediate column.
 * Each expansion therefore cost up to {@code maxGap * 8 * 3} candidate landings, thousands of block
 * lookups per mob per tick instead of the vanilla ten-odd, and it injected long-range edges into the
 * search graph. The result was a server-thread stall ("world freezes") as soon as a mob began
 * pathfinding. The switch is still present and still off; the leap now lives in the move control
 * instead.</p>
 *
 * <p>Diagonal movement is unaffected by this switch: it comes from
 * {@link ParasiteGroundNavigation#canCutCorner} and
 * {@link ParasiteAmphibiousNavigation#canCutCorner}, which keep vanilla speeds and vanilla
 * neighbour generation.</p>
 *
 * <p>26 note: this file deliberately touches nothing but {@code WalkNodeEvaluator} and {@code Node}.
 * It never reads a {@code Level} and never calls {@code getCachedPathType}, because 26 has no
 * {@code NodeEvaluator.level}; the gap logic needs no evaluator state at all. If
 * {@code WalkNodeEvaluator} no longer exists in 26, the single fix is to extend whatever class
 * {@code GroundPathNavigation.createPathFinder} installs instead - nothing else in the p11 set
 * depends on this file.</p>
 */
public class ParasiteNodeEvaluator extends WalkNodeEvaluator {

    /** Master switch for the abandoned leap-landing neighbours. Keep false - it is the whole point. */
    private static final boolean ENABLE_GAP_LANDINGS = false;

    /** The widest pit this mob group may jump, in blocks. Informational only: never read here. */
    private final int maxGapJumpWidth;

    public ParasiteNodeEvaluator(int maxGapJumpWidth) {
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    protected int getMaxGapJumpWidth() {
        return this.maxGapJumpWidth;
    }

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
        // Vanilla only: no extra world lookups and no long-range edges in the search graph. The gap
        // jump is a move-control event (see ParasiteGapJump) and does no work here at all.
        if (!ENABLE_GAP_LANDINGS) {
            return super.getNeighbors(outputArray, node);
        }

        // Dead by design. If this ever needs implementing again it must not scan the world per
        // expansion: that is exactly what stalled the server thread.
        return super.getNeighbors(outputArray, node);
    }
}
