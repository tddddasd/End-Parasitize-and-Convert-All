package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

/**
 * Node evaluator for the infested silverfish.
 *
 * <p>Infested blocks already exempt this mob from collision (their {@code getCollisionShape} returns
 * {@link net.minecraft.world.phys.shapes.Shapes#empty()} for an infested silverfish), but the
 * pathfinder evaluates blocks with an empty collision context and therefore still saw full solid
 * cubes. Reporting {@link BlockPathTypes#OPEN} here gives the pathfinder the same view the collision
 * system already has, for this mob only.</p>
 */
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