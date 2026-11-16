package org.tdddd.epca.impl.overworld.registry.blocks.block;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

import java.util.WeakHashMap;

public class InfestedSugarCane extends BushBlock implements InfestedBlockInterface {

    public static final BooleanProperty NATURAL_SPAWN = BooleanProperty.create("natural_spawn");
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    private static final WeakHashMap<Entity, Long> LAST_DAMAGE_TIME = new WeakHashMap<>();
    private static final WeakHashMap<Entity, Double> LAST_Y_POS = new WeakHashMap<>();

    public InfestedSugarCane(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NATURAL_SPAWN, true)
                .setValue(AGE, 0)
                .setValue(TOP, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL_SPAWN, AGE, TOP);
    }

    /**
     * 玩家放置的虫染甘蔗（{@code natural_spawn=false}）不需要水，只检查下方是否是可支撑方块；
     * 自然生成 / 自然生长的甘蔗（{@code natural_spawn=true}）仍沿用原版甘蔗的水判定。
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        Block belowBlock = belowState.getBlock();

        if (belowBlock == this || belowBlock == Blocks.SUGAR_CANE) {
            return true;
        }

        if (!isValidSoil(belowState)) {
            return false;
        }

        // 玩家放置：不需要水
        if (state.hasProperty(NATURAL_SPAWN) && !state.getValue(NATURAL_SPAWN)) {
            return true;
        }

        return hasWaterAround(level, below);
    }

    private static boolean isValidSoil(BlockState belowState) {
        return belowState.is(Blocks.SAND) ||
                belowState.is(Blocks.RED_SAND) ||
                belowState.is(Blocks.DIRT) ||
                belowState.is(Blocks.GRASS_BLOCK) ||
                belowState.is(Blocks.PODZOL) ||
                belowState.is(Blocks.COARSE_DIRT) ||
                belowState.is(ModBlocks.INFESTED_DIRT.get()) ||
                belowState.is(ModBlocks.INFESTED_SAND.get());
    }

    private static boolean hasWaterAround(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 自然生长（随机刻向上长高）仍然需要水：检查整柱最底部甘蔗下面那格土壤周围是否有水。
     */
    private boolean hasWaterAtColumnBase(LevelReader level, BlockPos pos) {
        BlockPos bottom = pos;
        for (int guard = 0; guard < 32; guard++) {
            Block belowBlock = level.getBlockState(bottom.below()).getBlock();
            if (belowBlock != this && belowBlock != Blocks.SUGAR_CANE) {
                break;
            }
            bottom = bottom.below();
        }
        return hasWaterAround(level, bottom.below());
    }

    /**
     * 玩家放置时直接写死 {@code natural_spawn=false}，这样放置前的 {@code canSurvive} 就不会再要求水。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        if (context.getPlayer() != null) {
            state = state.setValue(NATURAL_SPAWN, false);
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player) {
            level.setBlock(pos, state.setValue(NATURAL_SPAWN, false), 3);
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return Blocks.SUGAR_CANE.getSoundType(Blocks.SUGAR_CANE.defaultBlockState(), level, pos, entity);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                               net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        if (!level.isClientSide()) {
            boolean isTop = true;
            BlockState aboveState = level.getBlockState(pos.above());
            if (aboveState.getBlock() == this || aboveState.getBlock() == Blocks.SUGAR_CANE) {
                isTop = false;
            }
            level.setBlock(pos, state.setValue(TOP, isTop), 2);
        }
        super.neighborChanged(state, level, pos, neighborBlock, orientation, isMoving);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 15) {
            if (random.nextInt(3) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            }
            return;
        }

        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) {
            return;
        }

        // 自然生长依旧需要水（与原版甘蔗一致）
        if (!hasWaterAtColumnBase(level, pos)) {
            return;
        }

        int height = 1;
        BlockPos checkPos = pos.below();
        while (level.getBlockState(checkPos).getBlock() == this || level.getBlockState(checkPos).getBlock() == Blocks.SUGAR_CANE) {
            height++;
            checkPos = checkPos.below();
        }

        if (height >= 3) {
            return;
        }

        level.setBlock(above, this.defaultBlockState()
                .setValue(AGE, 0)
                .setValue(TOP, true), 2);
        level.setBlock(pos, state.setValue(AGE, 0).setValue(TOP, false), 2);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) return;

        if (!(entity instanceof LivingEntity living) || ParasiteEvents.isParasiteEntity(living) && living instanceof Player) {
            return;
        }

        if (entity instanceof Player player && HiveLeaderManager.isHiveLeader(player.getUUID())) {
            return;
        }

        long currentTick = level.getGameTime();
        double currentY = entity.getY();

        Long lastTime = LAST_DAMAGE_TIME.get(entity);
        Double lastY = LAST_Y_POS.get(entity);

        if (lastTime == null) {
            LAST_DAMAGE_TIME.put(entity, currentTick);
            LAST_Y_POS.put(entity, currentY);
            return;
        }

        if (currentTick - lastTime >= 10) {
            if (Math.abs(currentY - lastY) > 0.001) {
                living.hurt(living.damageSources().cactus(), 1.0F);
                if (level.getRandom().nextFloat() < 0.7F) {
                    living.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, 900, 0));
                }
            }
            LAST_DAMAGE_TIME.put(entity, currentTick);
            LAST_Y_POS.put(entity, currentY);
        }
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
