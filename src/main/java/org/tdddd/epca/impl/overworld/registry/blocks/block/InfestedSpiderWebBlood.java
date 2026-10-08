package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InfestedSpiderWebBlood extends WebBlock implements InfestedBlockInterface {
    public static final BooleanProperty SPIDER = BooleanProperty.create("spider");
    /**
     * Absolute game tick at which a mob-placed web expires, or {@link #PLACED_NEVER} when it was not
     * mob-placed. Kept as BLOCKSTATE data because block state is serialised with the chunk, so the
     * countdown survives save/load and chunk unload/reload. The old static
     * {@code HashMap<BlockPos, Long>} did not, which is why the timer restarted after a reload.
     */
    public static final IntegerProperty PLACED_AT = IntegerProperty.create("placed_at", 0, 32767);
    /** Sentinel for {@link #PLACED_AT}: not a mob-placed web. */
    public static final int PLACED_NEVER = 0;
    /** The decay duration. Unchanged: 60 seconds. */
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

    /**
     * Reads the {@code spider} flag defensively. See {@code InfestedSpiderWeb#isSpiderWeb} for why
     * {@code getValue} must not be called directly on this property.
     */
    public static boolean isSpiderWeb(BlockState state) {
        if (state == null || !state.hasProperty(SPIDER)) {
            return false;
        }
        Boolean spider = state.getValue(SPIDER);
        return spider != null && spider;
    }

    /**
     * Returns {@code state} with the {@code spider} flag set, or {@code state} unchanged when the
     * property is absent. Never call {@code setValue} on this property directly.
     */
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
                // Guard against recursion: setBlock below re-enters onPlace, and by then the stamp is
                // already written, so the second pass falls straight through.
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

    /** The absolute expiry tick truncated into {@link #PLACED_AT}'s range. See the base web class. */
    public static int expiryStamp(long gameTime) {
        int stamp = (int) ((gameTime + DECAY_TICKS) % 32767L);
        return stamp == PLACED_NEVER ? 1 : stamp;
    }

    /** The current tick in the same truncated space as {@link #expiryStamp}. */
    public static int nowStamp(long gameTime) {
        int stamp = (int) (gameTime % 32767L);
        return stamp == PLACED_NEVER ? 1 : stamp;
    }

    /**
     * True when a web stamped {@code stamp} has reached or passed its expiry. Uses modular elapsed
     * time so it still fires when the expiry went by while the chunk was unloaded.
     */
    public static boolean stampDue(long gameTime, int stamp) {
        int now = nowStamp(gameTime);
        long elapsed = (now - (stamp - DECAY_TICKS)) % 32767L;
        if (elapsed < 0) {
            elapsed += 32767L;
        }
        return elapsed >= DECAY_TICKS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        processEntitiesInBlock(state, level, pos);

        // Decay, driven by the PERSISTED blockstate stamp. Runs on the 1-tick re-schedule below, so a
        // web whose expiry passed while unloaded vanishes on the first tick after it loads.
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

    /**
     *  COTH +
     *
     * <p><b></b>
     *  {@link IParasite#isParasiteByTagOrInterface}
     * {@code instanceof IParasite} /  {@code "Parasite"}
     *  {@code NestLeaderManager.isNestLeader} {@code IParasite.java:77-91}
     * {@code InfestedBlockHandler.java:23}{@code InfestedVine.java:65-68}
     * {@code InfestedCactus.java:78}{@code InfestedSnow.java:90}{@code InfestedPumpkinBehaviour.java:472}
     *  {@code living instanceof IParasite && living instanceof Player}
     * </p>
     *
     * <p><b></b>{@code BlockBehaviour#onPlace}
     * {@code _tmp_vanilla_src/.../BlockBehaviour.java:158} BlockEntity /
     *  {@code PLACE_TIME}
     * {@code minecraft:cobweb  epca:infested_spider_web} {@code block_conversions/*.json:47}
     *
     *
     * {@code if (living instanceof IParasite p && p.getFollowTarget() == null) continue;}
     * {@code FollowTarget}  {@code IParasite.java:28,47-75}</p>
     */
    private void processEntitiesInBlock(BlockState state, ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.1);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                Objects::nonNull);

        for (LivingEntity living : entities) {
            if (IParasite.isParasiteByTagOrInterface(living)) {
                continue;
            }
            applyCothAndDamage(living, level);
        }
    }

    private void applyCothAndDamage(LivingEntity living, ServerLevel level) {
        var data = living.getPersistentData();
        long now = level.getGameTime();

        long lastEffect = data.getLong("InfestedWebLastEffect");
        if (now - lastEffect >= 20) {
            living.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 20 * 20, 0));
            living.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 15 * 20, 0));
            data.putLong("InfestedWebLastEffect", now);
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