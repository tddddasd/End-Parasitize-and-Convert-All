package org.tdddd.epca.impl.mixin.common;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedPointedDripstone;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    private static boolean isPointedDripstoneWithDirection(BlockState state, Direction direction) {
        Block block = state.getBlock();
        if (block instanceof PointedDripstoneBlock && state.getValue(PointedDripstoneBlock.TIP_DIRECTION) == direction) {
            return true;
        }
        if (block instanceof InfestedPointedDripstone && state.getValue(PointedDripstoneBlock.TIP_DIRECTION) == direction) {
            return true;
        }
        return false;
    }
}