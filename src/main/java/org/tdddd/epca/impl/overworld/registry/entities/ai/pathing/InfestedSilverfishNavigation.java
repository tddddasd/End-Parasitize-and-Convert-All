package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Ground navigation for the infested silverfish: ordinary ground walking, but with infested blocks
 * reported as {@link BlockPathTypes#OPEN} so the mob can path into and inside them.
 */
public class InfestedSilverfishNavigation extends GroundPathNavigation {

    public InfestedSilverfishNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new InfestedSilverfishNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean hasValidPathType(BlockPathTypes pathType) {
        return pathType == BlockPathTypes.OPEN || super.hasValidPathType(pathType);
    }
}