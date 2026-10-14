package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

/**
 * Infested tall dry grass (虫染高枯草丛).
 *
 * <p>Despite the name it is a <b>one block high</b> plant, exactly like
 * {@link InfestedShortDryGrass} and exactly like the vanilla {@code minecraft:tall_dry_grass},
 * whose own blockstate is a plain single {@code minecraft:block/cross} model. It is <b>not</b> a
 * {@link InfestedDoublePlantBlock} and has no {@code half} property. Only the registry id, the
 * texture and the model differ from the short variant.
 */
public class InfestedTallDryGrass extends BushBlock implements InfestedBlockInterface {
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public InfestedTallDryGrass(Properties properties) {
        super(properties);
    }

    @Override
    public com.mojang.serialization.MapCodec<BushBlock> codec() {
        return simpleCodec(InfestedTallDryGrass::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        Block block = state.getBlock();
        return block == ModBlocks.INFESTED_SAND.get()
                || state.is(BlockTags.SUPPORTS_VEGETATION);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 60;
    }
}
