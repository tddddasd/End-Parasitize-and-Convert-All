package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.HashSet;
import java.util.Set;

public class InfestedVine extends MultifaceBlock implements SimpleWaterloggedBlock, InfestedBlockInterface {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    
    private static final VoxelShape UP_SHAPE = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape DOWN_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final VoxelShape EAST_SHAPE = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);

    public InfestedVine() {
        
        super(Properties.of()
                .noCollission()
                .randomTicks()
                .strength(0.2F)
                .sound(SoundType.VINE)
                .noOcclusion()
                .ignitedByLava());

        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            applyCothEffects(livingEntity, true);
        }
    }

    private void applyCothEffects(LivingEntity entity, boolean apply) {
        
        if (IParasite.isParasiteByTagOrInterface(entity)) {
            
            return;
        }

        if (apply) {
            
            
            MobEffectInstance cothEffect = new MobEffectInstance(
                    ModEffects.COTH.get(), 
                    600, 
                    0, 
                    false, 
                    true, 
                    true 
            );

            MobEffectInstance slownessEffect = new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 
                    4, 
                    1, 
                    false, 
                    false, 
                    false 
            );

            
            if (!entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ||
                    entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 0) {
                entity.addEffect(cothEffect);
                entity.addEffect(slownessEffect);
            }
        }
        
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    public boolean canAttachTo(BlockGetter level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos attachedPos = pos.relative(direction);
        BlockState attachedState = level.getBlockState(attachedPos);

        
        return attachedState.isFaceSturdy(level, attachedPos, direction.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        
        BlockPos abovePos = blockpos.above();
        BlockState aboveState = level.getBlockState(abovePos);

        
        if (aboveState.getBlock() instanceof InfestedVine) {
            
            BlockState placementState = this.defaultBlockState();
            for (Direction dir : Direction.values()) {
                if (hasFace(aboveState, dir)) {
                    placementState = placementState.setValue(getFaceProperty(dir), true);
                }
            }

            
            if (!hasAnyFace(placementState)) {
                placementState = placementState.setValue(getFaceProperty(clickedFace), true);
            }

            
            FluidState fluidstate = level.getFluidState(blockpos);
            placementState = placementState.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);

            
            if (!level.isClientSide) {
                level.scheduleTick(blockpos, this, 1);
            }

            return placementState;
        }

        
        BlockState blockstate = super.getStateForPlacement(context);
        if (blockstate != null) {
            FluidState fluidstate = level.getFluidState(blockpos);
            return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
        } else {
            return null;
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        
        if (!level.isClientSide) {
            checkAndConvertVinesBelow(level, pos, state);
        }
    }

    public void tick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        
        if (!level.isClientSide) {
            checkAndConvertVinesBelow(level, pos, state);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        
        if (!level.isClientSide && fromPos.equals(pos.above())) {
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);

            if (aboveState.getBlock() instanceof InfestedVine) {
                checkAndConvertVinesBelow(level, abovePos, aboveState);
            }
        }
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.getItemInHand().is(this.asItem()) || super.canBeReplaced(state, context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        
        for (Direction dir : Direction.values()) {
            if (hasFace(state, dir)) {
                
                BlockPos abovePos = pos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                if (aboveState.getBlock() instanceof InfestedVine) {
                    
                    continue;
                }

                
                if (!canAttachTo(level, state, pos, dir)) {
                    state = state.setValue(getFaceProperty(dir), false);
                }
            }
        }

        
        if (!hasAnyFace(state)) {
            return level.getBlockState(pos).getFluidState().createLegacyBlock();
        }

        return state;
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return this.spreader;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        
        for (Direction direction : Direction.values()) {
            if (hasFace(state, direction)) {
                
                BlockPos abovePos = pos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                if (aboveState.getBlock() instanceof InfestedVine) {
                    return true;
                }

                
                if (canAttachTo(level, state, pos, direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();

        if (hasFace(state, Direction.UP)) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, UP_SHAPE);
        if (hasFace(state, Direction.DOWN)) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, DOWN_SHAPE);
        if (hasFace(state, Direction.NORTH)) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, NORTH_SHAPE);
        if (hasFace(state, Direction.SOUTH)) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, SOUTH_SHAPE);
        if (hasFace(state, Direction.EAST)) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, EAST_SHAPE);
        if (hasFace(state, Direction.WEST)) shape = net.minecraft.world.phys.shapes.Shapes.or(shape, WEST_SHAPE);

        return shape;
    }

    
    private void checkAndConvertVinesBelow(Level level, BlockPos pos, BlockState state) {
        Set<BlockPos> visited = new HashSet<>();
        checkAndConvertVinesBelowRecursive(level, pos.below(), state, visited, 0);
    }

    private void checkAndConvertVinesBelowRecursive(Level level, BlockPos pos, BlockState targetState, Set<BlockPos> visited, int depth) {
        if (visited.contains(pos) || depth > 128) { 
            return;
        }

        visited.add(pos);

        BlockState currentState = level.getBlockState(pos);

        
        if (currentState.is(Blocks.VINE)) {
            
            BlockState newState;
            if (currentState.is(Blocks.VINE)) {
                newState = this.defaultBlockState();
            } else {
                newState = currentState;
            }

            
            boolean hasAnyMatchingFace = false;
            for (Direction dir : Direction.values()) {
                boolean shouldHaveFace = hasFace(targetState, dir);
                boolean currentlyHasFace = hasFace(currentState, dir);

                
                if (shouldHaveFace) {
                    
                    newState = newState.setValue(getFaceProperty(dir), currentlyHasFace);
                    if (currentlyHasFace) {
                        hasAnyMatchingFace = true;
                    }
                } else {
                    
                    newState = newState.setValue(getFaceProperty(dir), false);
                }
            }

            
            if (hasAnyMatchingFace) {
                
                FluidState fluidState = currentState.getFluidState();
                newState = newState.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);

                
                if (!newState.equals(currentState)) {
                    level.setBlock(pos, newState, 3);
                }

                
                checkAndConvertVinesBelowRecursive(level, pos.below(), targetState, visited, depth + 1);
            }
            
        }
        
    }

    
    public static boolean hasFace(BlockState state, Direction direction) {
        BooleanProperty property = getFaceProperty(direction);
        return state.hasProperty(property) && state.getValue(property);
    }

    
    public static boolean hasAnyFace(BlockState state) {
        for (Direction direction : Direction.values()) {
            if (hasFace(state, direction)) {
                return true;
            }
        }
        return false;
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