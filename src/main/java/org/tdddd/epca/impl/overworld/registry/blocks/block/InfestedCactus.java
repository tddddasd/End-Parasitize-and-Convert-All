package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class InfestedCactus extends Block implements InfestedBlockInterface {

    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");

    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
    protected static final VoxelShape COLLISION_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    public InfestedCactus(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL_SPAWN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add( NATURAL_SPAWN);

    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return simpleCodec(InfestedCactus::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        if (context instanceof EntityCollisionContext entityCtx) {
            Entity entity = entityCtx.getEntity();
            if (entity instanceof InfestedSilverfish) {
                return Shapes.empty();
            }
        }

        return COLLISION_SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof ItemEntity item) {
            item.discard();
            return;
        }

        if (entity instanceof LivingEntity living) {
            if (IParasite.isParasiteByTagOrInterface(living)) {
                return;
            }
            living.hurt(entity.damageSources().cactus(), 1.5F);
            living.addEffect(new MobEffectInstance(
                    ModEffects.COTH,
                    600,
                    0,
                    false,
                    true)
            );
        }
    }

    @Override
    // 26.1.2: Block#playerWillDestroy now returns the (possibly transformed) BlockState.
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            if (player.getMainHandItem().isEmpty()) {
                if (level.getRandom().nextFloat() < 0.75F) {
                    player.hurt(player.damageSources().cactus(), 1.5F);
                    player.addEffect(new MobEffectInstance(
                            ModEffects.COTH,
                            600,
                            0,
                            false,
                            true)
                    );
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) return false;
        boolean validBelow = belowState.is(Blocks.SAND) ||
                belowState.is(Blocks.RED_SAND) ||
                belowState.is(ModBlocks.INFESTED_CACTUS.get()) ||
                belowState.is(ModBlocks.INFESTED_SAND.get()) ||
                belowState.is(Blocks.CACTUS);
        if (!validBelow) return false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState sideState = level.getBlockState(pos.relative(dir));
            if (!sideState.isAir() && !sideState.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    // 26.1.2: the neighbour position was replaced by the redstone Orientation.
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) return;
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                int height = 1;
                BlockPos checkPos = pos.below();
                while (level.getBlockState(checkPos).getBlock() instanceof InfestedCactus || level.getBlockState(checkPos).getBlock() instanceof CactusBlock) {
                    height++;
                    checkPos = checkPos.below();
                }
                if (height < 3) {
                    level.setBlock(above, state, 2);
                    level.setBlock(pos, state, 2);
                }
            }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}