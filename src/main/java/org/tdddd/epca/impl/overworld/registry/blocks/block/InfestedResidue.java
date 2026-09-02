package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, layers * 2.0D, 16.0D);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int layers = state.getValue(LAYERS);
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, layers * 2.0D - 1.9D, 16.0D);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == this.asItem()) {
            if (state.getValue(LAYERS) == 8) {
                BlockPos abovePos = pos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                if (aboveState.isAir() || aboveState.canBeReplaced()) {
                    level.setBlock(abovePos, this.defaultBlockState().setValue(LAYERS, 1), Block.UPDATE_ALL);
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
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
        if (level instanceof Level world && !world.isClientSide) {
            BlockPos belowPos = pos.below();
            BlockState belowState = world.getBlockState(belowPos);
            if (belowState.getBlock() instanceof InfestedResidue) {
                int belowLayers = belowState.getValue(LAYERS);
                if (belowLayers < 8) {
                    int current = state.getValue(LAYERS);
                    int transfer = Math.min(8 - belowLayers, current);
                    if (transfer > 0) {
                        world.setBlock(belowPos, belowState.setValue(LAYERS, belowLayers + transfer), 3);
                        int newLayers = current - transfer;
                        if (newLayers == 0) {
                            return false;
                        } else {
                            world.setBlock(pos, state.setValue(LAYERS, newLayers), 3);
                        }
                    }
                }
            }
        }

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.getBlock() instanceof InfestedResidue && state.getValue(LAYERS) == 8) {
            return true;
        }
        return Block.canSupportCenter(level, belowPos, Direction.UP);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState landedState, FallingBlockEntity fallingBlockEntity) {
        super.onLand(level, pos, state, landedState, fallingBlockEntity);
        if (level.isClientSide) return;

        if (landedState.getBlock() instanceof InfestedResidue) {
            int fallingLayers = state.getValue(LAYERS);
            mergeLayers(level, pos, landedState, fallingLayers);
        }
    }

    public static void mergeLayers(Level level, BlockPos pos, BlockState existingState, int layersToAdd) {
        if (!(existingState.getBlock() instanceof InfestedResidue)) return;

        int existing = existingState.getValue(LAYERS);
        int total = existing + layersToAdd;

        if (total <= 8) {
            BlockState newState = existingState.setValue(LAYERS, total);
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, newState.getBlock());
        } else {
            BlockState fullState = existingState.setValue(LAYERS, 8);
            level.setBlock(pos, fullState, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, fullState.getBlock());

            int remaining = total - 8;
            BlockPos targetPos = pos.above();

            while (true) {
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.isAir() || targetState.canBeReplaced()) {
                    BlockState newState = existingState.getBlock().defaultBlockState().setValue(LAYERS, remaining);
                    level.setBlock(targetPos, newState, Block.UPDATE_ALL);
                    level.updateNeighborsAt(targetPos, newState.getBlock());
                    break;
                } else if (targetState.getBlock() instanceof InfestedResidue) {
                    int targetLayers = targetState.getValue(LAYERS);
                    int totalTarget = targetLayers + remaining;
                    if (totalTarget <= 8) {
                        BlockState newState = targetState.setValue(LAYERS, totalTarget);
                        level.setBlock(targetPos, newState, Block.UPDATE_ALL);
                        level.updateNeighborsAt(targetPos, newState.getBlock());
                        break;
                    } else {
                        BlockState newFull = targetState.setValue(LAYERS, 8);
                        level.setBlock(targetPos, newFull, Block.UPDATE_ALL);
                        level.updateNeighborsAt(targetPos, newFull.getBlock());
                        remaining = totalTarget - 8;
                        targetPos = targetPos.above();
                    }
                } else {
                    targetPos = targetPos.above();
                    if (targetPos.getY() > level.getMaxBuildHeight() || targetPos.getY() < level.getMinBuildHeight()) {
                        BlockState newState = existingState.getBlock().defaultBlockState().setValue(LAYERS, remaining);
                        level.setBlock(targetPos, newState, Block.UPDATE_ALL);
                        level.updateNeighborsAt(targetPos, newState.getBlock());
                        break;
                    }
                }
            }
        }
    }
}