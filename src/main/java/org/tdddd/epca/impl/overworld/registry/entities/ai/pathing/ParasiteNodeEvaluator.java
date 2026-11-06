package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class ParasiteNodeEvaluator extends WalkNodeEvaluator {

    private static final boolean ENABLE_GAP_LANDINGS = false;

    private static final int[] LANDING_OFFSETS = {0, -1, 1};

    private final int maxGapJumpWidth;

    public ParasiteNodeEvaluator(int maxGapJumpWidth) {
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    protected int getMaxGapJumpWidth() {
        return this.maxGapJumpWidth;
    }

    protected boolean isLeapLandingType(BlockPathTypes pathType) {
        return pathType == BlockPathTypes.OPEN
                || pathType == BlockPathTypes.WALKABLE
                || pathType == BlockPathTypes.WALKABLE_DOOR
                || pathType == BlockPathTypes.DOOR_OPEN
                || pathType == BlockPathTypes.TRAPDOOR
                || pathType == BlockPathTypes.LEAVES;
    }

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {

        if (!ENABLE_GAP_LANDINGS) {
            return super.getNeighbors(outputArray, node);
        }

        int count = super.getNeighbors(outputArray, node);

        int maxGap = this.getMaxGapJumpWidth();
        if (maxGap <= 0 || this.level == null || this.mob == null) {
            return count;
        }

        for (int gap = 1; gap <= maxGap; gap++) {
            int reach = gap + 1;
            for (int stepX = -1; stepX <= 1; stepX++) {
                for (int stepZ = -1; stepZ <= 1; stepZ++) {
                    if (stepX == 0 && stepZ == 0) {
                        continue;
                    }

                    if (count >= outputArray.length) {
                        return count;
                    }
                    Node landing = this.findLeapLanding(node, stepX, stepZ, reach);
                    if (landing != null) {
                        outputArray[count++] = landing;
                    }
                }
            }
        }
        return count;
    }

    private Node findLeapLanding(Node from, int stepX, int stepZ, int reach) {
        int landingY = this.firstClearLandingY(from, stepX, stepZ, reach);
        if (landingY == Integer.MIN_VALUE) {
            return null;
        }

        int x = from.x + stepX * reach;
        int z = from.z + stepZ * reach;
        BlockPathTypes pathType = this.getBlockPathType(this.level, x, landingY, z, this.mob);
        if (!this.isLeapLandingType(pathType)) {
            return null;
        }

        Node landing = this.getNode(x, landingY, z);
        landing.type = pathType;
        return landing;
    }

    private int firstClearLandingY(Node from, int stepX, int stepZ, int reach) {
        for (int i = 0; i < LANDING_OFFSETS.length; i++) {
            int y = from.y + LANDING_OFFSETS[i];
            if (this.isLeapPathClear(from, stepX, stepZ, reach, y)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private boolean isLeapPathClear(Node from, int stepX, int stepZ, int reach, int landingY) {

        for (int i = 1; i < reach; i++) {
            if (!this.isEmptyColumn(from.x + stepX * i, from.y, from.z + stepZ * i)) {
                return false;
            }
        }

        int x = from.x + stepX * reach;
        int z = from.z + stepZ * reach;
        if (!this.isEmptyColumn(x, landingY, z)) {
            return false;
        }

        return !this.level.getBlockState(new BlockPos(x, landingY - 1, z)).isAir();
    }

    private boolean isEmptyColumn(int x, int y, int z) {
        return this.level.getBlockState(new BlockPos(x, y, z)).isAir()
                && this.level.getBlockState(new BlockPos(x, y + 1, z)).isAir();
    }
}
