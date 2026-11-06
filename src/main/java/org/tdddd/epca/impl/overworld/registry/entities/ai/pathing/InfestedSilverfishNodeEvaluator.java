package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

public class InfestedSilverfishNodeEvaluator extends WalkNodeEvaluator {

    private final BlockPos.MutableBlockPos epcaScratch = new BlockPos.MutableBlockPos();

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
        if (mob != null && mob.level().getBlockState(this.epcaScratch.set(x, y, z)).getBlock() instanceof InfestedBlockInterface) {
            return PathType.OPEN;
        }
        return super.getPathTypeOfMob(context, x, y, z, mob);
    }
}
