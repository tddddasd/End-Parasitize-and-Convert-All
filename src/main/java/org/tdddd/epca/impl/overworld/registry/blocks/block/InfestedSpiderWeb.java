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

import java.util.*;

public class InfestedSpiderWeb extends WebBlock implements InfestedBlockInterface {
    public static final BooleanProperty SPIDER = BooleanProperty.create("spider");
    /**
     * Cycle length for {@link #PLACED_AT}, in ticks. MUST stay small: a block's state count is the
     * product of all its properties' ranges, and vanilla materialises EVERY combination at
     * registration (it builds the state definition and the per-state neighbour tables up front). With
     * a range of 32768 this one property made each web block 65536 states - 196608 across the three -
     * which hangs the game during block registration, before any world loads. 1024 still comfortably
     * exceeds {@link #DECAY_TICKS} (1200 is close, so 1024 would be too tight - see the note below).
     */
    public static final int PLACED_CYCLE = 2048;
    /**
     * Absolute game tick at which a mob-placed web expires, or {@link #PLACED_NEVER} when the web was
     * not placed by a mob. Stored as BLOCKSTATE data on purpose: block state is serialised with the
     * chunk, so it survives a save/load, a chunk unload/reload and a world close mid-countdown.
     *
     * <p>Range kept to [0, {@link #PLACED_CYCLE}) so the block has only 2 * 2048 = 4096 states.</p>
     */
    public static final IntegerProperty PLACED_AT =
            IntegerProperty.create("placed_at", 0, PLACED_CYCLE - 1);
    /** Sentinel for {@link #PLACED_AT}: not a mob-placed web, so no decay is scheduled. */
    public static final int PLACED_NEVER = 0;
    /** The decay duration. Unchanged: 60 seconds. */
    public static final int DECAY_TICKS = 20 * 60;

    public InfestedSpiderWeb(Properties properties) {
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
     * Reads the {@code spider} flag defensively.
     *
     * <p>{@code BlockState#getValue} throws {@code IllegalArgumentException} when the property is not
     * part of the state, so any state that somehow lacks it (a different block, a state built from a
     * legacy definition, a future edit that drops the property) must degrade to {@code false}-like
     * behaviour instead of taking the server down. Callers use this instead of {@code getValue} on
     * this property.</p>
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
     * property is absent. This is the only sanctioned way to set the flag: the projectile that spits
     * the web, the head's self-destruct and anything else must go through it, because
     * {@code BlockState#setValue} throws when the property is missing and that exception is a hard
     * server crash inside entity ticking.
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
            // Record the absolute expiry ON THE BLOCKSTATE, so it is saved with the chunk. The value
            // is truncated to the property's 15-bit range (see expiryStamp) - a shorter cycle is
            // harmless because the web is destroyed the first time the stamp passes; it can only
            // ever expire EARLIER than 60 s, never later.
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

    /**
     * The absolute expiry tick, truncated into the {@link #PLACED_CYCLE}-tick sealed range
     * (1..{@code PLACED_CYCLE - 1}). Recorded once, at placement. Because the cycle (2048) exceeds a
     * web's whole life ({@link #DECAY_TICKS} = 1200), the stamp identifies the expiry unambiguously
     * within that lifetime, and the value can only make a web expire EARLIER than 60 s, never later.
     */
    public static int expiryStamp(long gameTime) {
        int stamp = (int) ((gameTime + DECAY_TICKS) % (long) PLACED_CYCLE);
        return stamp == PLACED_NEVER ? 1 : stamp;
    }

    /** The current tick in the same truncated space as {@link #expiryStamp}, used by the tick check. */
    public static int nowStamp(long gameTime) {
        int stamp = (int) (gameTime % (long) PLACED_CYCLE);
        return stamp == PLACED_NEVER ? 1 : stamp;
    }

    /**
     * True when a web stamped {@code stamp} has reached or passed its expiry.
     *
     * <p>Uses modular elapsed time, so it still fires when the expiry went by while the chunk was
     * unloaded - the web then vanishes on the FIRST tick after it loads, which is the required
     * behaviour. The modulo is safe because a web only ever lives {@link #DECAY_TICKS} ticks.</p>
     */
    public static boolean stampDue(long gameTime, int stamp) {
        int now = nowStamp(gameTime);
        long elapsed = (now - (stamp - DECAY_TICKS)) % (long) PLACED_CYCLE;
        if (elapsed < 0) {
            elapsed += PLACED_CYCLE;
        }
        return elapsed >= DECAY_TICKS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        processEntitiesInBlock(state, level, pos);

        // Decay, driven by the PERSISTED blockstate stamp instead of the old in-memory map. This runs
        // on the normal 1-tick re-schedule below, so a web whose expiry passed while the chunk was
        // unloaded vanishes on the first tick after it loads again.
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
     *  COTH+
     *
     * <p><b></b>
     *  {@link IParasite#isParasiteByTagOrInterface}
     * {@code entity instanceof IParasite} /
     * {@code "Parasite"} {@code NestLeaderManager.isNestLeader}
     *  {@code IParasite.java:77-91}
     * {@code InfestedBlockHandler.java:23} COTH
     * {@code InfestedVine.java:65-68} entityInside  COTH+
     * {@code InfestedCactus.java:78}{@code InfestedSnow.java:90}
     * {@code InfestedPumpkinBehaviour.java:472}
     *
     *  {@code living instanceof IParasite && living instanceof Player}
     *
     * <p><b></b>
     * {@code BlockBehaviour#onPlace} {@code _tmp_vanilla_src/.../BlockBehaviour.java:158}
     *  BlockEntity /  {@code PLACE_TIME}
     *  {@code minecraft:cobweb  epca:infested_spider_web}
     * {@code data/epca/block_conversions/*.json:47}
     *
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