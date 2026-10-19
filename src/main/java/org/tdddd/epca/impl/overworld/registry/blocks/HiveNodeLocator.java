package org.tdddd.epca.impl.overworld.registry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;

/** Finds the nearest hive node block, used by the Link effect and the hive structure test. */
public final class HiveNodeLocator {

    private HiveNodeLocator() {
    }

    /** @return the closest hive node within {@code radius} blocks, or {@code null} */
    public static BlockPos findNearest(ServerLevel level, BlockPos origin, int radius) {
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).is(ModBlocks.HIVE_NODE.get())) {
                continue;
            }
            double distSq = pos.distSqr(origin);
            if (distSq < bestSq) {
                bestSq = distSq;
                best = pos.immutable();
            }
        }
        return best;
    }
}
