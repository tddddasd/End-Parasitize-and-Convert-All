package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedSweetBerryBushBlockEntity;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import javax.annotation.Nullable;

public class InfestedSweetBerryBush extends SweetBerryBushBlock implements InfestedBlockInterface, EntityBlock {
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public InfestedSweetBerryBush(Properties properties) {
        super(properties);
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
    protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        Block block = state.getBlock();
        return block == ModBlocks.INFESTED_DIRT.get() ||
                super.mayPlaceOn(state, world, pos);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 60;
    }

    // ========== EntityBlock 实现（关联 BlockEntity） ==========
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

    // ========== 原有交互（减速 + 伤害 + 30秒COTH） ==========
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        boolean isParasite = living instanceof IParasite;

        if (!isParasite) {
            // 减速（所有 age 均生效，因为只有 1 和 2）
            living.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));

            // 当 age=2 且移动时造成伤害 + 30秒 COTH
            if (!level.isClientSide && state.getValue(AGE) >= 3) {
                if (living.xOld != living.getX() || living.zOld != living.getZ()) {
                    living.hurt(level.damageSources().sweetBerryBush(), 1.0F);
                    living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 0)); // I级 30秒
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) >= 3) {
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
}