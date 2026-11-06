package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

public class InfestedSilverfishNodeEvaluator extends WalkNodeEvaluator {

    private final BlockPos.MutableBlockPos epcaScratch = new BlockPos.MutableBlockPos();

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
        if (level.getBlockState(this.epcaScratch.set(x, y, z)).getBlock() instanceof InfestedBlockInterface) {
            return BlockPathTypes.OPEN;
        }
        return super.getBlockPathType(level, x, y, z, mob);
    }
}
