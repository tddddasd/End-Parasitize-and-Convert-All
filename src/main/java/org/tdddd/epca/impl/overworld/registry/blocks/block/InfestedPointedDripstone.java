package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

public class InfestedPointedDripstone extends PointedDripstoneBlock implements InfestedBlockInterface {

    public InfestedPointedDripstone(Properties properties) {
        super(properties);
    }

    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityCtx) {
            Entity entity = entityCtx.getEntity();
            if (entity instanceof InfestedSilverfish) {
                return Shapes.empty();
            }
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction tipDir = state.getValue(TIP_DIRECTION);
        return isValidPointedDripstonePlacement(level, pos, tipDir);
    }

    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        }
        Direction tipDir = state.getValue(TIP_DIRECTION);
        if (tipDir == Direction.DOWN && level.getBlockTicks().hasScheduledTick(pos, this)) {
            return state;
        }
        if (direction == tipDir.getOpposite() && !this.canSurvive(state, level, pos)) {
            if (tipDir == Direction.DOWN) {
                level.scheduleTick(pos, this, 2);
            } else {
                level.scheduleTick(pos, this, 1);
            }
            return state;
        }
        boolean isTipMerge = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
        DripstoneThickness newThickness = calculateDripstoneThickness(level, pos, tipDir, isTipMerge);
        return state.setValue(THICKNESS, newThickness);
    }

    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction lookDir = context.getNearestLookingVerticalDirection().getOpposite();
        Direction tipDir = calculateTipDirection(level, pos, lookDir);
        if (tipDir == null) {
            return null;
        }
        boolean merge = !context.isSecondaryUseActive();
        DripstoneThickness thickness = calculateDripstoneThickness(level, pos, tipDir, merge);
        if (thickness == null) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(TIP_DIRECTION, tipDir)
                .setValue(THICKNESS, thickness)
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == net.minecraft.world.level.material.Fluids.WATER);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            Direction dir = state.getValue(TIP_DIRECTION);
            if (dir == Direction.DOWN) {
                spawnFallingStalactite(level, pos, state);
            } else {
                level.destroyBlock(pos, true);
            }
        }
    }

    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        
    }

    
    private static boolean isCompatible(BlockState state) {
        return state.getBlock() instanceof PointedDripstoneBlock;
    }

    private static boolean isCompatibleWithDirection(BlockState state, Direction direction) {
        return isCompatible(state) && state.getValue(TIP_DIRECTION) == direction;
    }

    
    private static boolean isValidPointedDripstonePlacement(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos oppositePos = pos.relative(direction.getOpposite());
        BlockState oppositeState = level.getBlockState(oppositePos);
        return oppositeState.isFaceSturdy(level, oppositePos, direction)
                || isCompatibleWithDirection(oppositeState, direction);
    }

    
    @Nullable
    private static Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction preferredDir) {
        if (isValidPointedDripstonePlacement(level, pos, preferredDir)) {
            return preferredDir;
        } else if (isValidPointedDripstonePlacement(level, pos, preferredDir.getOpposite())) {
            return preferredDir.getOpposite();
        } else {
            return null;
        }
    }

    
    @Nullable
    private static DripstoneThickness calculateDripstoneThickness(LevelReader level, BlockPos pos,
                                                                  Direction tipDir, boolean mergeIfTip) {
        Direction opposite = tipDir.getOpposite();
        BlockState aboveState = level.getBlockState(pos.relative(tipDir));
        if (isCompatibleWithDirection(aboveState, opposite)) {
            if (!mergeIfTip && aboveState.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE) {
                return DripstoneThickness.TIP;
            } else {
                return DripstoneThickness.TIP_MERGE;
            }
        } else if (!isCompatibleWithDirection(aboveState, tipDir)) {
            return DripstoneThickness.TIP;
        } else {
            DripstoneThickness aboveThickness = aboveState.getValue(THICKNESS);
            if (aboveThickness != DripstoneThickness.TIP && aboveThickness != DripstoneThickness.TIP_MERGE) {
                BlockState belowState = level.getBlockState(pos.relative(opposite));
                if (!isCompatibleWithDirection(belowState, tipDir)) {
                    return DripstoneThickness.BASE;
                } else {
                    return DripstoneThickness.MIDDLE;
                }
            } else {
                return DripstoneThickness.FRUSTUM;
            }
        }
    }

    
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        boolean damaged = false;
        
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            damaged = entity.causeFallDamage(fallDistance + 2.0F, 2.0F, level.damageSources().stalagmite());
        } else {
            
            damaged = entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
        }

        
        if (damaged && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 30 * 20, 0));
        }
    }

    private static void spawnFallingStalactite(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockState currentState = state;
        while (isStalactite(currentState)) {
            
            FallingBlockEntity entity = FallingBlockEntity.fall(level, mutable, currentState);
            if (isTip(currentState, true)) {
                int height = Math.max(1 + pos.getY() - mutable.getY(), 6);
                float damage = 1.0F * height;
                entity.setHurtsEntities(damage, 40);
                break; 
            }
            mutable.move(Direction.DOWN);
            currentState = level.getBlockState(mutable);
        }
    }

    private static boolean isStalactite(BlockState state) {
        return (state.getBlock() instanceof PointedDripstoneBlock) && state.getValue(TIP_DIRECTION) == Direction.DOWN;
    }

    private static boolean isTip(BlockState state, boolean includeMerge) {
        if (!(state.getBlock() instanceof PointedDripstoneBlock)) return false;
        DripstoneThickness thickness = state.getValue(THICKNESS);
        return thickness == DripstoneThickness.TIP || (includeMerge && thickness == DripstoneThickness.TIP_MERGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}