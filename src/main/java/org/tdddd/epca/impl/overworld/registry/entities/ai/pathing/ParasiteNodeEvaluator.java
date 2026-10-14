package org.tdddd.epca.impl.overworld.registry.entities.ai.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Vanilla walking node evaluator, currently DISABLED for extra gap-jump neighbours.
 *
 * <p>History: an earlier revision appended landing nodes for every gap width up to
 * {@code maxGapJumpWidth} in all eight horizontal directions on <em>every</em> expansion. That made
 * each expanded node scan up to {@code maxGap * 8 * 3} candidate landings with two block-state
 * lookups per intermediate column, and it injected long-range edges into the search graph. The
 * result was a server-thread stall ("world freezes") as soon as a mob started pathfinding, so the
 * feature is off until it is redesigned to be cheap (throttled, cached, and limited to a single
 * forward direction) instead of running in the hot path of the pathfinder.</p>
 *
 * <p>Diagonal movement is unaffected by this switch: it comes from
 * {@link ParasiteGroundNavigation#canCutCorner} and
 * {@link ParasiteAmphibiousNavigation#canCutCorner}, which keep vanilla speeds and vanilla
 * neighbour generation.</p>
 *
 * <p><b>The dead code below is the stall.</b> It is kept only so the cause is not lost, and it is
 * never reached. Do not re-enable it, and do not move any of that scanning into the live path. The
 * parasite gap jump now lives in {@code ParasiteGapJump} (move-control layer, throttled) instead.</p>
 */
public class ParasiteNodeEvaluator extends WalkNodeEvaluator {

    /** Master switch for the experimental leap-landing neighbours. Keep false. */
    private static final boolean ENABLE_GAP_LANDINGS = false;

    /** Vertical offsets tried for a landing, in order: level, one down, one up. */
    private static final int[] LANDING_OFFSETS = {0, -1, 1};

    private final int maxGapJumpWidth;

    public ParasiteNodeEvaluator(int maxGapJumpWidth) {
        this.maxGapJumpWidth = maxGapJumpWidth;
    }

    protected int getMaxGapJumpWidth() {
        return this.maxGapJumpWidth;
    }

    /** Path types a parasite is willing to land on after a leap. */
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
        // Vanilla only: no extra world lookups and no long-range edges in the search graph.
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
                    // Never write past the engine's own neighbor array.
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

    /** The node across the gap, or null when this direction and reach is not a legal leap. */
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

        // Reuse the cached node the engine already knows about, exactly like vanilla does, so the
        // pathfinder's open/closed bookkeeping stays consistent.
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
        // The whole gap must be open at body and head height, otherwise this is not a hole.
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

        // The landing column needs a floor to stand on.
        return !this.level.getBlockState(new BlockPos(x, landingY - 1, z)).isAir();
    }

    private boolean isEmptyColumn(int x, int y, int z) {
        return this.level.getBlockState(new BlockPos(x, y, z)).isAir()
                && this.level.getBlockState(new BlockPos(x, y + 1, z)).isAir();
    }
}
