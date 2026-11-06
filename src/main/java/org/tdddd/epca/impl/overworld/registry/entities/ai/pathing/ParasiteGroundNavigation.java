package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;

public class ParasiteGroundNavigation extends GroundPathNavigation {

    private final int maxGapJumpWidth;

    public ParasiteGroundNavigation(Mob mob, Level level) {
        this(mob, level, ParasitePathing.MAX_GAP_JUMP_HUMANOID);
    }

    public ParasiteGroundNavigation(Mob mob, Level level, int maxGapJumpWidth) {
        super(mob, level);
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    protected int maxGapJumpWidth() {
        return this.maxGapJumpWidth;
    }

    protected ParasiteNodeEvaluator createParasiteNodeEvaluator() {
        return new ParasiteNodeEvaluator(this.maxGapJumpWidth());
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
