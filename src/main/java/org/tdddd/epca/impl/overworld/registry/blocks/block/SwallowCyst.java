package org.tdddd.epca.impl.overworld.registry.blocks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.blocks.ModBlockEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSilverfish;

public class SwallowCyst extends BaseEntityBlock implements InfestedBlockInterface {
    // 26.1.2: BaseEntityBlock 新增抽象方法 codec()，需要 simpleCodec + (Properties) 构造器
    public static final MapCodec<SwallowCyst> CODEC = simpleCodec(SwallowCyst::new);
    public static final BooleanProperty LIVING = BooleanProperty.create("living");
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 8.0D, 14.0D);

    public SwallowCyst(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIVING, true));
    }

    public SwallowCyst() {
        this(Properties.of()
                .noOcclusion()
                .strength(0.9f, 0.9f)
                .sound(SoundType.SLIME_BLOCK)
                .isViewBlocking((state, world, pos) -> false)
                .isSuffocating((state, world, pos) -> false)
                .pushReaction(PushReaction.DESTROY)
                .mapColor(DyeColor.RED)
                .randomTicks()
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIVING);
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
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwallowCystBlockEntity(pos, state);
    }

    
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below());
    }

    /**
     * 26.1.2：{@code neighborChanged} 的第 5 个参数由 {@code BlockPos fromPos} 变为
     * {@code @Nullable Orientation orientation}（红石朝向模型重写）。
     */
    @Override
    protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Block block,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);  
        } else {
            super.neighborChanged(state, level, pos, block, orientation, isMoving);
        }
    }

    /**
     * 26.1.2：{@code onRemove(state, level, pos, newState, isMoving)} 已被
     * {@code affectNeighborsAfterRemoval(state, ServerLevel, pos, isMoving)} 取代
     * （新签名拿不到 newState）。用 {@code level.getBlockState(pos).is(state.getBlock())}
     * 判断“是否真的被替换/移除”，掉落物品栏的行为保留。
     */
    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        if (!level.getBlockState(pos).is(state.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SwallowCystBlockEntity be) {
                ItemStackHandler inventory = be.getInventory();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
        }
    }

    /**
     * 26.1.2：{@code getCloneItemStack(BlockGetter, BlockPos, BlockState)} →
     * {@code getCloneItemStack(LevelReader, BlockPos, BlockState, boolean)}。
     * 1.20.1 用物品 NBT（{@code ItemStack#setTag}）携带 {@code Living}；26.1.2 的
     * {@code ItemStack} 已无直接 NBT 存取，而本方块的状态由 {@code simpleCodec} 承载，
     * 放置时按默认状态（{@code living = true}）恢复，行为与 1.20.1 的“活体囊肿”一致。
     */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this);
    }

    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    /**
     * 26.1.2：{@code Block#use(state, level, pos, player, hand, hit)} 被拆成
     * {@code useItemOn(itemStack, state, level, pos, player, hand, hit)} 与
     * {@code useWithoutItem(state, level, pos, player, hit)}。囊肿的交互与手持物无关，
     * 因此两者都转到同一个实现（空手右键也需要能打开界面）。
     */
    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return openCystMenu(state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openCystMenu(state, level, pos, player);
    }

    private InteractionResult openCystMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SwallowCystBlockEntity cyst && player instanceof ServerPlayer serverPlayer) {
                // 26.1.2: NetworkHooks.openScreen(...) → ServerPlayer#openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)
                serverPlayer.openMenu(cyst, buf -> buf.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SWALLOW_CYST.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
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
}