package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Amphibious twin of {@link ParasiteGroundNavigation}, for the humanoid infested mobs that swim
 * (infested drowned). It keeps {@code AmphibiousPathNavigation}'s water behaviour and only swaps
 * the node evaluator for {@link ParasiteAmphibiousNodeEvaluator}, plus the corner cutting override.
 */
public class ParasiteAmphibiousNavigation extends AmphibiousPathNavigation {

    public ParasiteAmphibiousNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    /** Widest pit this mob group may clear, in blocks. Overridden per group. */
    protected int maxGapJumpWidth() {
        return ParasitePathing.MAX_GAP_JUMP_HUMANOID;
    }

    protected ParasiteNodeEvaluator createParasiteNodeEvaluator() {
        return new ParasiteAmphibiousNodeEvaluator(this.maxGapJumpWidth());
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        // Let the amphibious navigation configure its own evaluator first (water walkability,
        // floating), then copy those switches onto the parasite evaluator.
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
    public boolean canCutCorner(BlockPathTypes pathType) {
        return true;
    }
}
