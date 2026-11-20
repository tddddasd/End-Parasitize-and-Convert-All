package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.InfestedWebBlockEntity;

import java.util.*;

public class InfestedSpiderWeb extends WebBlock implements EntityBlock, InfestedBlockInterface {
    public static final BooleanProperty SPIDER = BooleanProperty.create("spider");

    public static final int DECAY_TICKS = 20 * 60;

    public InfestedSpiderWeb(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(SPIDER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPIDER);
    }

    public static boolean isSpiderWeb(BlockState state) {
        if (state == null || !state.hasProperty(SPIDER)) {
            return false;
        }
        Boolean spider = state.getValue(SPIDER);
        return spider != null && spider;
    }

    public static BlockState markSpiderWeb(BlockState state, boolean spider) {
        if (state == null || !state.hasProperty(SPIDER)) {
            return state;
        }
        return state.setValue(SPIDER, spider);
    }

    public void entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(blockState, new Vec3(0.5, 0.05000000074505806 * 2, 0.5));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            if (isSpiderWeb(state)) {
                // the decay deadline lives on the block entity now (a 2048-value placed_at property on
                // the block state used to blow up WorldEdit's state map at server start)
                InfestedWebBlockEntity.stampExpiry(level, pos, DECAY_TICKS, () -> newBlockEntity(pos, state));
                level.scheduleTick(pos, this, DECAY_TICKS);
            } else {
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        processEntitiesInBlock(state, level, pos);

        if (isSpiderWeb(state) && InfestedWebBlockEntity.isExpired(level, pos)) {
            level.destroyBlock(pos, false);
            return;
        }

        if (level.getBlockState(pos).getBlock() == this) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return InfestedWebBlockEntity.spider(pos, state);
    }

    private void processEntitiesInBlock(BlockState state, ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.1);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                Objects::nonNull);

        for (LivingEntity living : entities) {
            if (ParasiteEvents.isParasiteByTagOrInterface(living)) {
                continue;
            }
            applyParaCurseAndDamage(living, level);
        }
    }

    private void applyParaCurseAndDamage(LivingEntity living, ServerLevel level) {
        var data = living.getPersistentData();
        long now = level.getGameTime();

        long lastEffect = data.getLong("InfestedWebLastEffect");
        if (now - lastEffect >= 20) {
            living.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 20 * 20, 0));
            data.putLong("InfestedWebLastEffect", now);
        }

        double dx = living.getX() - living.xOld;
        double dy = living.getY() - living.yOld;
        double dz = living.getZ() - living.zOld;
        if (dx * dx + dy * dy + dz * dz > 0.0) {
            long lastDamage = data.getLong("InfestedWebLastDamage");
            if (now - lastDamage >= 20) {
                living.hurt(living.damageSources().generic(), 1.0F);
                data.putLong("InfestedWebLastDamage", now);
            }
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
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
