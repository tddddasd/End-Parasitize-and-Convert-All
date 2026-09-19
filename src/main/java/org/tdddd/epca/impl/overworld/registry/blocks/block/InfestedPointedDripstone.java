package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

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

    /**
     * 26.1.2: the neighbour update became
     * {@code updateShape(state, LevelReader, ScheduledTickAccess, pos, directionToNeighbour,
     * neighbourPos, neighbourState, random)} — the level is a reader and ticking goes through
     * {@code ScheduledTickAccess}, so {@code BlockTicks} is no longer reachable from here.
     */
    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                  BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                  RandomSource random) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        }
        Direction tipDir = state.getValue(TIP_DIRECTION);
        if (tipDir == Direction.DOWN && ticks.getBlockTicks().hasScheduledTick(pos, this)) {
            return state;
        }
        if (direction == tipDir.getOpposite() && !this.canSurvive(state, level, pos)) {
            if (tipDir == Direction.DOWN) {
                ticks.scheduleTick(pos, this, 2);
            } else {
                ticks.scheduleTick(pos, this, 1);
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
        LevelReader level = context.getLevel();
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
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
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
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        
    }

    // ---------------------------------------------------------------------
    // 26.1.2: PointedDripstoneBlock made every one of its helpers private
    // (calculateTipDirection, calculateDripstoneThickness,
    // isValidPointedDripstonePlacement, isStalactite, isTip,
    // spawnFallingStalactite, isCompatible*), so the infested variant keeps its
    // own copies with the identical logic, rewritten against the public surface.
    // ---------------------------------------------------------------------

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

    /** 26.1.2: {@code fallOn} takes a {@code double} fall distance. */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        boolean damaged = false;
        
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            damaged = entity.causeFallDamage(fallDistance + 2.0F, 2.0F, level.damageSources().stalagmite());
        } else {
            
            damaged = entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
        }

        
        if (damaged && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.COTH, 30 * 20, 0));
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
        return isCompatible(state) && state.getValue(TIP_DIRECTION) == Direction.DOWN;
    }

    private static boolean isTip(BlockState state, boolean includeMerge) {
        if (!isCompatible(state)) {
            return false;
        }
        DripstoneThickness thickness = state.getValue(THICKNESS);
        return thickness == DripstoneThickness.TIP || (includeMerge && thickness == DripstoneThickness.TIP_MERGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
