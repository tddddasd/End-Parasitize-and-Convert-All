package org.tdddd.epca.impl.utils.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility methods for entity interactions with blocks - light source breaking, ground detection, etc.
 */
public final class EntityBreakUtils {

    private EntityBreakUtils() {}

    private static final int LIGHT_BREAK_RANGE = 8;
    private static final int LIGHT_BREAK_COOLDOWN = 60;

    /**
     * Try to break nearby light sources (torches, lanterns, etc.) if brightness is too high.
     * Returns the remaining cooldown.
     */
    public static int tryBreakLightSources(LivingEntity entity, int currentCooldown) {
        if (currentCooldown > 0) return currentCooldown - 1;

        BlockPos entityPos = entity.blockPosition();
        Level level = entity.level();

        for (BlockPos pos : BlockPos.betweenClosed(
                entityPos.offset(-LIGHT_BREAK_RANGE, -LIGHT_BREAK_RANGE, -LIGHT_BREAK_RANGE),
                entityPos.offset(LIGHT_BREAK_RANGE, LIGHT_BREAK_RANGE, LIGHT_BREAK_RANGE))) {

            if (level.getBrightness(LightLayer.BLOCK, pos) > 7) {
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();
                if (block instanceof TorchBlock || block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN
                        || block == Blocks.SEA_LANTERN || block == Blocks.GLOWSTONE
                        || block == Blocks.SHROOMLIGHT || block == Blocks.JACK_O_LANTERN) {
                    level.destroyBlock(pos, true, entity);
                    return LIGHT_BREAK_COOLDOWN;
                }
            }
        }
        return 0;
    }

    /**
     * Get the ground height (first solid block top) at a position.
     */
    public static double getGroundHeightAt(Level level, BlockPos pos, int maxSearchUp) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int startY = Math.min(pos.getY() + maxSearchUp, level.getMaxBuildHeight());
        mutable.setY(startY);
        while (mutable.getY() > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(mutable);
            if (state.isSolid()) {
                return mutable.getY() + 1;
            }
            mutable.setY(mutable.getY() - 1);
        }
        return level.getMinBuildHeight();
    }

    /**
     * Check if there's a solid block at or below the given position, within maxDown range.
     */
    public static boolean hasSolidGround(Level level, BlockPos pos, int maxDown) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i <= maxDown; i++) {
            mutable.setY(pos.getY() - i);
            if (level.getBlockState(mutable).isFaceSturdy(level, mutable, Direction.UP)) {
                return true;
            }
        }
        return false;
    }
}
