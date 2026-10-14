package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Ground navigation for parasites.
 *
 * <p>Two changes on top of vanilla {@code GroundPathNavigation}:</p>
 * <ul>
 *   <li>{@link #canCutCorner} is always true, so a parasite aims straight at the node after the
 *       next one and walks the diagonal instead of turning through both straight legs.</li>
 *   <li>{@link #createPathFinder} swaps in {@link ParasiteNodeEvaluator}, which is the vanilla
 *       evaluator again: it adds <em>no</em> neighbours. The gap jump is a move-control event, not
 *       a pathfinder one.</li>
 * </ul>
 *
 * <p>The vanilla node evaluator is still built first and its door / floating / fence switches are
 * copied across, so nothing except neighbour generation changes.</p>
 *
 * <p>26 notes (see the README for the full list):</p>
 * <ul>
 *   <li>{@code BlockPathTypes} is {@code PathType} in 26, so {@link #canCutCorner} takes that type.
 *       This is the one line in the p11 set whose signature must be confirmed against 26.1.2.</li>
 *   <li>{@code NodeEvaluator} in 26 no longer reads a {@code Level} of its own
 *       ({@code PathfindingContext} carries that), but the door / floating / fence accessors used
 *       here are configuration, not world access, so they are unchanged.</li>
 * </ul>
 */
public class ParasiteGroundNavigation extends GroundPathNavigation {

    /**
     * Per-instance override of the group width. Kept separate from {@link #maxGapJumpWidth()} so the
     * mobs that install this navigation in their constructor never have to override the method (the
     * navigation constructor would run before their own subclass fields are initialised).
     */
    private final int maxGapJumpWidth;

    public ParasiteGroundNavigation(Mob mob, Level level) {
        this(mob, level, ParasitePathing.MAX_GAP_JUMP_HUMANOID);
    }

    /**
     * @param maxGapJumpWidth widest pit this instance may clear in blocks; pass
     *                        {@link ParasitePathing#MAX_GAP_JUMP_NONE} for "no gap jump at all"
     *                        (used by the infested skeleton). The diagonal corner cutting below is
     *                        unaffected either way.
     */
    public ParasiteGroundNavigation(Mob mob, Level level, int maxGapJumpWidth) {
        super(mob, level);
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    /** Widest pit this mob group may clear, in blocks. Overridden per group. */
    protected int maxGapJumpWidth() {
        return this.maxGapJumpWidth;
    }

    protected ParasiteNodeEvaluator createParasiteNodeEvaluator() {
        return new ParasiteNodeEvaluator(this.maxGapJumpWidth());
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        // Build the vanilla navigation first: that configures the default node evaluator and tells
        // us which door / floating / fence switches this navigation class wants.
        super.createPathFinder(maxVisitedNodes);
        NodeEvaluator defaults = this.nodeEvaluator;

        ParasiteNodeEvaluator evaluator = this.createParasiteNodeEvaluator();
        evaluator.setCanPassDoors(defaults.canPassDoors());
        evaluator.setCanOpenDoors(defaults.canOpenDoors());
        evaluator.setCanFloat(defaults.canFloat());
        evaluator.setCanWalkOverFences(defaults.canWalkOverFences());

        this.nodeEvaluator = evaluator;
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    public boolean canCutCorner(PathType pathType) {
        // Parasites step diagonally through a corner instead of walking both straight legs.
        return true;
    }
}
