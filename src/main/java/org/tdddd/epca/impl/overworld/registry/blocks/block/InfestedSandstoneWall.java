package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class InfestedSandstoneWall extends WallBlock implements InfestedBlockInterface {
    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    public InfestedSandstoneWall(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(NATURAL_SPAWN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NATURAL_SPAWN);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.SANDSTONE_WALL.getSoundType(Blocks.SANDSTONE_WALL.defaultBlockState(), level, pos, entity);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        
        BlockState baseState = super.defaultBlockState()
                .setValue(UP, state.getValue(UP))
                .setValue(NORTH_WALL, state.getValue(NORTH_WALL))
                .setValue(SOUTH_WALL, state.getValue(SOUTH_WALL))
                .setValue(EAST_WALL, state.getValue(EAST_WALL))
                .setValue(WEST_WALL, state.getValue(WEST_WALL))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        VoxelShape shape = super.getCollisionShape(baseState, level, pos, context);
        if (shape == null) shape = Shapes.block();

        if (context instanceof EntityCollisionContext entityCtx && entityCtx.getEntity() instanceof InfestedSilverfish) {
            return Shapes.empty();
        }
        return shape;
    }

    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState baseState = super.defaultBlockState()
                .setValue(UP, state.getValue(UP))
                .setValue(NORTH_WALL, state.getValue(NORTH_WALL))
                .setValue(SOUTH_WALL, state.getValue(SOUTH_WALL))
                .setValue(EAST_WALL, state.getValue(EAST_WALL))
                .setValue(WEST_WALL, state.getValue(WEST_WALL))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        VoxelShape shape = super.getShape(baseState, level, pos, context);
        return shape != null ? shape : Shapes.block();
    }
}