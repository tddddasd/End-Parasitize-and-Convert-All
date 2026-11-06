package org.tdddd.epca.impl.overworld.registry.blocks.block;

import org.tdddd.epca.impl.events.ParasiteEvents;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InfestedSpiderWebBlood extends WebBlock implements InfestedBlockInterface {
    public static final BooleanProperty SPIDER = BooleanProperty.create("spider");

    public static final int PLACED_CYCLE = 2048;
    public static final IntegerProperty PLACED_AT = IntegerProperty.create("placed_at", 0, PLACED_CYCLE - 1);

    public static final int PLACED_NEVER = 0;

    public static final int DECAY_TICKS = 20 * 60;

    public InfestedSpiderWebBlood(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(SPIDER, false)
                .setValue(PLACED_AT, PLACED_NEVER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPIDER, PLACED_AT);
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

    public void entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        entity.makeStuckInBlock(blockState, new Vec3(0.5, 0.05000000074505806 * 2, 0.5));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            if (isSpiderWeb(state)) {

                if (state.hasProperty(PLACED_AT) && state.getValue(PLACED_AT) != PLACED_NEVER) {
                    return;
                }
                BlockState stamped = state.setValue(PLACED_AT, expiryStamp(level.getGameTime()));
                if (stamped != state) {
                    level.setBlock(pos, stamped, 3);
                }
                level.scheduleTick(pos, this, DECAY_TICKS);
            } else {
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    public static int expiryStamp(long gameTime) {
        int stamp = (int) ((gameTime + DECAY_TICKS) % PLACED_CYCLE);
        return stamp == PLACED_NEVER ? 1 : stamp;
    }

    public static int nowStamp(long gameTime) {
        int stamp = (int) (gameTime % PLACED_CYCLE);
        return stamp == PLACED_NEVER ? 1 : stamp;
    }

    public static boolean stampDue(long gameTime, int stamp) {
        int now = nowStamp(gameTime);
        long elapsed = (now - (stamp - DECAY_TICKS)) % PLACED_CYCLE;
        if (elapsed < 0) {
            elapsed += PLACED_CYCLE;
        }
        return elapsed >= DECAY_TICKS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        processEntitiesInBlock(state, level, pos);

        if (isSpiderWeb(state) && state.hasProperty(PLACED_AT)) {
            int stamp = state.getValue(PLACED_AT);
            if (stamp != PLACED_NEVER && stampDue(level.getGameTime(), stamp)) {
                level.destroyBlock(pos, false);
                return;
            }
        }

        if (level.getBlockState(pos).getBlock() == this) {
            level.scheduleTick(pos, this, 1);
        }
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

        long lastEffect = data.getLong("InfestedWebLastEffect").orElse(0L);
        if (now - lastEffect >= 20) {
            living.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, 20 * 20, 0));
            living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 15 * 20, 0));
            data.putLong("InfestedWebLastEffect", now);
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state) {
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
