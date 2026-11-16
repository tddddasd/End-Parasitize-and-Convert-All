package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedSweetBerryBushBlockEntity;

/**
 * 虫染甜浆果丛：独立的方块实现（不再继承原版 {@code SweetBerryBushBlock}）。
 * <p>
 * 只有两个生长阶段：
 * <ul>
 *     <li>{@code age=0}：未结果（贴图 {@code infested_sweet_berry_bush}）</li>
 *     <li>{@code age=1}：结果（贴图 {@code infested_sweet_berry_bush_growed}）</li>
 * </ul>
 * 随机刻让 0 → 1 生长；右键结果状态可采摘 {@code epca:infested_sweet_berries} 并回到 0。
 */
public class InfestedSweetBerryBush extends BushBlock implements InfestedBlockInterface, EntityBlock {

    /** 0 = 未结果，1 = 结果。 */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 1);

    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    /** 随机刻生长概率：与原版甜浆果一致（1/5）。 */
    private static final int GROW_CHANCE = 5;

    /** 生长所需最低亮度，与原版一致。 */
    private static final int MIN_LIGHT_LEVEL = 9;

    public InfestedSweetBerryBush(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
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
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 1;
    }

    /**
     * 未结果 → 结果的随机刻生长。只有两个阶段，所以一次随机刻成功即结果。
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 1) {
            return;
        }
        if (level.getRawBrightness(pos.above(), 0) < MIN_LIGHT_LEVEL) {
            return;
        }
        if (random.nextInt(GROW_CHANCE) != 0) {
            return;
        }
        level.setBlock(pos, state.setValue(AGE, 1), 2);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        return state.is(ModBlocks.INFESTED_DIRT.get())
                || state.is(BlockTags.DIRT)
                || state.is(Blocks.FARMLAND);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 60;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfestedSweetBerryBushBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof InfestedSweetBerryBushBlockEntity bushEntity) {
                    InfestedSweetBerryBushBlockEntity.tick(lvl, pos, st, bushEntity);
                }
            };
        }
        return null;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (!ParasiteEvents.isParasiteByTagOrInterface(living)) {

            living.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));

            // 只有结果阶段才会划伤并施加虫咒缚身
            if (!level.isClientSide && state.getValue(AGE) >= 1) {
                if (living.xOld != living.getX() || living.zOld != living.getZ()) {
                    living.hurt(level.damageSources().sweetBerryBush(), 1.0F);
                    living.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 600, 0));
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) >= 1) {
            if (!level.isClientSide) {
                int count = 1 + level.random.nextInt(3);
                popResource(level, pos, new ItemStack(ModItems.INFESTED_SWEET_BERRIES.get(), count));
                level.setBlock(pos, state.setValue(AGE, 0), 3);
                level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public Item asItem() {
        return ModItems.INFESTED_SWEET_BERRIES.get();
    }

    /**
     * 鼠标中键（pick block）：返回 {@code epca:infested_sweet_berries} 物品。
     */
    @Override
    @SuppressWarnings("deprecation")
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.INFESTED_SWEET_BERRIES.get());
    }

    /**
     * Forge 更精确的 pick block 入口（{@code BlockState#getCloneItemStack}）。
     */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ModItems.INFESTED_SWEET_BERRIES.get());
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}
