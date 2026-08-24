package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

public class InfestedDoublePlantBlock extends DoublePlantBlock implements InfestedBlockInterface {

    public InfestedDoublePlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        Block block = state.getBlock();
        return block == ModBlocks.INFESTED_DIRT.get() ||
                super.mayPlaceOn(state, world, pos);
    }
}