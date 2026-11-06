package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;

public class ParasiteAmphibiousNavigation extends AmphibiousPathNavigation {

    public ParasiteAmphibiousNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    protected int maxGapJumpWidth() {
        return ParasitePathing.MAX_GAP_JUMP_HUMANOID;
    }

    protected ParasiteNodeEvaluator createParasiteNodeEvaluator() {
        return new ParasiteAmphibiousNodeEvaluator(this.maxGapJumpWidth());
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {

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
        return true;
    }
}
