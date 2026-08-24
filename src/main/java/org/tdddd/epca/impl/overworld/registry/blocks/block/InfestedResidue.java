package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import net.minecraft.world.entity.item.FallingBlockEntity;

public class InfestedResidue extends FallingBlock implements InfestedBlockInterface {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);

    public InfestedResidue() {
        super(Properties.of()
                .noOcclusion()
                .strength(0.0f)
                .sound(SoundType.NETHER_WART)
                .isViewBlocking((state, world, pos) -> false)
                .isSuffocating((state, world, pos) -> false)
                .instabreak()
                .pushReaction(PushReaction.DESTROY)
                .requiresCorrectToolForDrops()
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int layers = state.getValue(LAYERS);
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, layers * 2.0D - 1.9D, 16.0D);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.getItemInHand(hand).getItem() == this.asItem()) {
            int current = state.getValue(LAYERS);
            if (current < 8) {
                level.setBlock(pos, state.setValue(LAYERS, current + 1), 3);
                if (!player.isCreative()) player.getItemInHand(hand).shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (!living.hasEffect(ModEffects.COTH.get())) {
                living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 0, false, true, true));
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (isInfestedDirt(belowState) && state.getValue(LAYERS) == 1) {
            return false;
        }
        
        if (belowState.getBlock() instanceof InfestedResidue && belowState.getValue(LAYERS) == 8) {
            return true;
        }
        
        return Block.canSupportCenter(level, belowPos, Direction.UP);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState landedState, FallingBlockEntity fallingBlockEntity) {
        super.onLand(level, pos, state, landedState, fallingBlockEntity);
        if (level.isClientSide) return;

        
        if (landedState.getBlock() instanceof InfestedResidue) {
            int existing = landedState.getValue(LAYERS);
            int falling = state.getValue(LAYERS);
            int total = existing + falling;

            if (total <= 8) {
                level.setBlock(pos, landedState.setValue(LAYERS, total), 3);
            } else {
                level.setBlock(pos, landedState.setValue(LAYERS, 8), 3);
                int remaining = total - 8;
                BlockPos above = pos.above();
                BlockState aboveState = state.setValue(LAYERS, remaining);
                FallingBlockEntity fallingEntity = FallingBlockEntity.fall(level, above, aboveState);
                level.addFreshEntity(fallingEntity);
            }
            
            BlockState newState = level.getBlockState(pos);
            checkAndRemoveIfSingleOnDirt(level, pos, newState);
            return;
        }

        
        checkAndRemoveIfSingleOnDirt(level, pos, state);
    }

    
    private void checkAndRemoveIfSingleOnDirt(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(LAYERS) == 1) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (isInfestedDirt(belowState)) {
                level.destroyBlock(pos, false); 
            }
        }
    }

    
    private boolean isInfestedDirt(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.INFESTED_DIRT.get();
    }
}