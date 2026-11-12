package org.tdddd.epca.impl.overworld.registry.entities.entity.special.illusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.HiveLeaderManager;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BlackHoleEntity;
import org.tdddd.yawning_neko_api.damages.MinimumDamage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Illusory Butterfly / 虚妄蝶.
 *
 * <p>A 355 second (7100 tick) "time" boss that cannot be buffed and cannot be killed. Its whole fight is
 * a timeline driven by {@link #tickCount}:</p>
 *
 * <pre>
 *   [0s   .. 115s)  PRESENT  - 50% movement slow on every entity in a 64 block sphere, cast once at spawn
 *   [115s .. 160s)  PAST     - immobile, samples every living entity per tick and keeps an event driven
 *                               change log of the blocks / entities that changed inside the 64 block sphere
 *   [160s .. 205s)  PAST     - replays those 45 seconds of recorded changes in reverse, one tick per tick
 *   [205s .. 315s)  FUTURE   - x3 movement / attack speed, random 3s slow every 10s (x0 / x25% / x50%)
 *   [315s .. 345s)  TAIL     - black hole at the head grows 1 -> 18 blocks, eating blocks on the way
 *   [345s]          TAIL     - black hole collapses and deals 3000 void damage within 18 blocks
 *   [355s]          END      - self removal + 12000 experience (same as the ender dragon)
 * </pre>
 *
 * <p>The entity is registered through {@code ModEntities.registerMobAttributes} (no GeckoLib model) and
 * is rendered by an empty renderer; the "shadow" it overlaps with itself is a separate
 * {@link IllusoryShadowEntity} that the client draws as a coloured cage.</p>
 */
public class IllusoryButterflyEntity extends Monster {

    // ------------------------------------------------------------------------------------------------
    // timeline
    // ------------------------------------------------------------------------------------------------

    /** Total fight duration: 355 seconds. */
    public static final int TOTAL_DURATION_TICKS = 7100;

    public static final int PHASE_PRESENT = 0;
    public static final int PHASE_PAST_RECORD = 1;
    public static final int PHASE_PAST_REPLAY = 2;
    public static final int PHASE_FUTURE = 3;
    public static final int PHASE_TAIL = 4;
    public static final int PHASE_END = 5;

    /** 115 s */
    private static final int T_PRESENT_END = 2300;
    /** 115 s — the first tick that is recorded. Same instant as {@link #T_PRESENT_END}, named for the log. */
    private static final int T_RECORD_START = T_PRESENT_END;
    /** 160 s */
    private static final int T_RECORD_END = 3200;
    /** 205 s */
    private static final int T_PAST_END = 4100;
    /** 315 s */
    private static final int T_FUTURE_END = 6300;
    /** 345 s */
    private static final int T_TAIL_END = 6900;

    // ------------------------------------------------------------------------------------------------
    // tuning
    // ------------------------------------------------------------------------------------------------

    /** "半径 4 个区块" == 64 blocks, the radius of the rewind sphere (shared with the event recorder). */
    private static final double RANGE = IllusionRewindRecorder.RECORD_RADIUS;
    private static final double RANGE_SQR = RANGE * RANGE;

    private static final float BASE_HEALTH = 3000.0F;
    private static final double BASE_ATTACK_DAMAGE = 3.0D;
    private static final double BASE_MOVEMENT_SPEED = 0.30D;
    /** The butterfly's hits never land for less than this (YNA minimum damage channel). */
    private static final float MIN_DAMAGE = 2.0F;
    private static final double FOLLOW_RANGE = 64.0D;

    /** How far above its feet the butterfly's "head" sits (used by the tail phase black hole). */
    private static final float HEAD_HEIGHT = 6.5F;

    /** One sample per tick: the 45 s record window [2300, 3200) produces exactly 900 sampled ticks. */
    private static final int RECORD_INTERVAL = 1;
    /** The rewind undoes one recorded tick per tick, so 45 s of recording takes 45 s to replay. */
    private static final int REPLAY_INTERVAL = 1;
    /** 45 s at 1 tick resolution: T_RECORD_END - T_RECORD_START == 900 ticks of rewind log. */
    private static final int MAX_RECORD_TICKS = T_RECORD_END - T_RECORD_START;
    private static final int MAX_ENTITIES_PER_TICK = 256;
    /**
     * Safety valves for the change log. A normal fight stays two orders of magnitude below them (a few
     * hundred to a few thousand block changes over the whole window); they only exist so a world edit
     * command, a huge modded explosion loop or a pathological spawner cannot eat the heap inside 45 s.
     */
    private static final int MAX_BLOCK_CHANGES_PER_TICK = 4096;
    private static final int MAX_BLOCK_CHANGES_TOTAL = 262144;
    private static final int MAX_ENTITY_EVENTS_TOTAL = 16384;
    /**
     * Replay diagnostics are throttled: the first {@code REWIND_LOG_LIMIT} block restores / discards /
     * revives of a replay are logged, everything after them is silent. The per tick entity position and
     * health restore is never logged (one line per entity per tick would flood the log).
     */
    private static final int REWIND_LOG_LIMIT = 20;

    private static final int FUTURE_SLOW_INTERVAL = 200;
    private static final int FUTURE_SLOW_DURATION = 60;
    private static final double[] FUTURE_SLOW_FACTORS = {0.0D, 0.25D, 0.50D};

    private static final float TAIL_BLACK_HOLE_START_RADIUS = 1.0F;
    private static final float TAIL_BLACK_HOLE_MAX_RADIUS = 18.0F;
    private static final int TAIL_BLOCKS_PER_TICK = 48;
    private static final int TAIL_COLLAPSE_TICKS = 60;
    private static final float VOID_DAMAGE = 3000.0F;
    private static final float TAIL_DAMAGE_RADIUS = 18.0F;

    private static final int TARGET_DEATH_BLACK_HOLE_TICKS = 200;
    private static final float TARGET_DEATH_BLACK_HOLE_RADIUS = 4.0F;

    private static final int XP_REWARD = 12000;

    private static final int ATTACK_INTERVAL = 20;

    private static final UUID SLOW_PRESENT_UUID = UUID.fromString("6f5f4a3a-8a4b-4c0e-9a3e-1f2d3c4b5a60");
    private static final UUID SPEED_FUTURE_UUID = UUID.fromString("6f5f4a3a-8a4b-4c0e-9a3e-1f2d3c4b5a61");
    private static final UUID ATTACK_SPEED_FUTURE_UUID = UUID.fromString("6f5f4a3a-8a4b-4c0e-9a3e-1f2d3c4b5a62");
    private static final UUID SLOW_RANDOM_UUID = UUID.fromString("6f5f4a3a-8a4b-4c0e-9a3e-1f2d3c4b5a63");

    private static final ResourceKey<DamageType> VOID_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(epca.MODID, "illusory_void"));

    // ------------------------------------------------------------------------------------------------
    // state
    // ------------------------------------------------------------------------------------------------

    /**
     * -1 on purpose: the very first {@link #tick()} then performs the {@link #PHASE_PRESENT} entry
     * (spawn sound + the 50% slow) through the normal transition path instead of needing a special case.
     */
    private int phase = -1;

    private int nextRecordTick = T_PRESENT_END;
    private int nextReplayTick = T_PAST_END;

    /**
     * The rewind log, keyed by the butterfly's {@link #tickCount} at the moment the change was observed.
     *
     * <p>Nothing here scales with the 64 block radius: the block / entity events feed these buckets and a
     * quiet tick simply has no bucket at all. The butterfly is frozen during the record phase, so the
     * entries never have to be re-anchored.</p>
     *
     * <p>Replaying walks the buckets newest first (tick {@code 3199} down to {@code 2300}) and applies every
     * stored {@code oldState} once, which restores exactly the state the world had when the record window
     * opened.</p>
     */
    private final NavigableMap<Integer, List<EntitySample>> entitySamples = new TreeMap<>();
    private final NavigableMap<Integer, List<BlockChange>> blockChanges = new TreeMap<>();
    private final NavigableMap<Integer, List<EntitySpawn>> entitySpawns = new TreeMap<>();
    private final NavigableMap<Integer, List<RecordedRemoval>> entityRemovals = new TreeMap<>();
    /** Only used for the "how big was the log" log line and for the global safety valves. */
    private int blockChangeCount;
    private int entityEventCount;

    // diagnostics only: log line budgets and the totals of what the replay actually did
    private int rewindBlockLogs;
    private int rewindRemoveLogs;
    private int rewindSpawnSkipLogs;
    private int rewindReviveLogs;
    private int rewindBlocksApplied;
    private int rewindEntitiesRemoved;
    private int rewindEntitiesRevived;

    private int nextFutureSlowTick = T_PAST_END;
    private final Map<UUID, Integer> randomSlowExpiry = new HashMap<>();

    private final Set<UUID> presentSlowed = new HashSet<>();

    @Nullable
    private UUID shadowId;
    private int shadowKind;

    @Nullable
    private BlackHoleHolder tailHole;

    private int attackCooldown;
    private int targetScanCooldown;

    private int deathHoleTicks;
    @Nullable
    private BlackHoleHolder deathHole;

    private boolean tailBlastDone;

    public IllusoryButterflyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build();
    }

    // ------------------------------------------------------------------------------------------------
    // immunity
    // ------------------------------------------------------------------------------------------------

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        // LivingEntity#addEffect(MobEffectInstance) is final and funnels into this overload, so this is
        // the single choke point for every positive and negative effect.
        return false;
    }

    @Override
    public void forceAddEffect(MobEffectInstance effect, @Nullable Entity source) {
        // 虚妄蝶 is immune to every buff and every debuff, including force applied ones.
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isDeadOrDying() {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        // never dies from damage; it only leaves on its own timeline
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void checkDespawn() {
        this.noActionTime = 0;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return isValidTarget(target) && super.canAttack(target);
    }

    /** The whole encounter is scripted in {@link #tick()}; no vanilla goals are wanted. */
    @Override
    protected void registerGoals() {
    }

    // ------------------------------------------------------------------------------------------------
    // main loop
    // ------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        this.purgeEffects();

        // a dead target spawns a 10 second black hole and then takes the butterfly with it: the whole
        // time line is suspended for those 10 seconds instead of running into the next phase
        if (this.deathHoleTicks > 0) {
            tickDeathBlackHole(level);
            return;
        }

        int tick = this.tickCount;
        int newPhase = phaseFor(tick);
        if (newPhase != this.phase) {
            exitPhase(level, this.phase, tick);
            this.phase = newPhase;
            enterPhase(level, newPhase, tick);
        }

        tickPhase(level, tick);
        tickTarget(level, tick);
        tickShadow(level);
        tickExpiringSlows(level, tick);
        tickDeathBlackHole(level);
    }

    private static int phaseFor(int tick) {
        if (tick < T_PRESENT_END) {
            return PHASE_PRESENT;
        }
        if (tick < T_RECORD_END) {
            return PHASE_PAST_RECORD;
        }
        if (tick < T_PAST_END) {
            return PHASE_PAST_REPLAY;
        }
        if (tick < T_FUTURE_END) {
            return PHASE_FUTURE;
        }
        if (tick < T_TAIL_END) {
            return PHASE_TAIL;
        }
        return PHASE_END;
    }

    private void purgeEffects() {
        Collection<MobEffectInstance> active = this.getActiveEffects();
        if (active.isEmpty()) {
            return;
        }
        List<MobEffect> toRemove = new ArrayList<>(active.size());
        for (MobEffectInstance instance : active) {
            toRemove.add(instance.getEffect());
        }
        for (MobEffect effect : toRemove) {
            this.removeEffect(effect);
        }
    }

    private void enterPhase(ServerLevel level, int newPhase, int tick) {
        switch (newPhase) {
            case PHASE_PRESENT -> {
                playSpawnSound(level);
                applyPresentSlow(level);
            }
            case PHASE_PAST_RECORD -> {
                clearRewindLog();
                this.nextRecordTick = tick;
                // from now on the recorder forwards every block / entity change inside the sphere to us
                IllusionRewindRecorder.begin(this);
                spawnShadow(level, IllusoryShadowEntity.KIND_PAST, IllusoryShadowEntity.COLOR_PAST);
            }
            case PHASE_PAST_REPLAY -> {
                this.nextReplayTick = tick;
                this.resetRewindDiagnostics();
                logRewindSummary();
            }
            case PHASE_FUTURE -> {
                // the rewind is over, nothing will ever read the log again
                clearRewindLog();
                spawnShadow(level, IllusoryShadowEntity.KIND_FUTURE, IllusoryShadowEntity.COLOR_FUTURE);
                applyFutureBuffs();
                this.nextFutureSlowTick = tick;
            }
            case PHASE_TAIL -> {
                removeShadow(level);
                spawnTailBlackHole(level);
            }
            case PHASE_END -> runTailBlast(level);
            default -> {
            }
        }
    }

    private void exitPhase(ServerLevel level, int oldPhase, int tick) {
        switch (oldPhase) {
            case PHASE_PRESENT -> removePresentSlow(level);
            case PHASE_PAST_RECORD -> IllusionRewindRecorder.end(this);
            case PHASE_PAST_REPLAY -> epca.LOGGER.info(
                    "[butterfly] rewind done at tick {}: {} blocks restored, {} entities discarded, {} entities revived",
                    tick, this.rewindBlocksApplied, this.rewindEntitiesRemoved, this.rewindEntitiesRevived);
            case PHASE_FUTURE -> {
                removeFutureBuffs();
                clearRandomSlows(level);
            }
            default -> {
            }
        }
    }

    private void tickPhase(ServerLevel level, int tick) {
        switch (this.phase) {
            case PHASE_PRESENT -> {
                moveTowardsTarget();
                tickAttack();
            }
            case PHASE_PAST_RECORD -> {
                // the butterfly is frozen in the past
                stopMoving();
                if (tick >= this.nextRecordTick) {
                    sampleEntities(level, tick);
                    this.nextRecordTick = tick + RECORD_INTERVAL;
                }
            }
            case PHASE_PAST_REPLAY -> {
                stopMoving();
                if (tick >= this.nextReplayTick) {
                    replayRecordedTick(level, tick);
                    this.nextReplayTick = tick + REPLAY_INTERVAL;
                }
            }
            case PHASE_FUTURE -> {
                moveTowardsTarget();
                tickAttack();
                if (tick >= this.nextFutureSlowTick) {
                    applyRandomFutureSlow(level);
                    this.nextFutureSlowTick = tick + FUTURE_SLOW_INTERVAL;
                }
            }
            case PHASE_TAIL -> {
                stopMoving();
                tickTailBlackHole(level, tick);
            }
            case PHASE_END -> {
                stopMoving();
                tickTailCollapse(level, tick);
                if (tick >= TOTAL_DURATION_TICKS) {
                    finish(level);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // movement & attack
    // ------------------------------------------------------------------------------------------------

    private void stopMoving() {
        this.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Flies at whatever {@code Attributes.MOVEMENT_SPEED} currently resolves to, so the x3 future phase
     * modifier and the random future slows (including the one it puts on itself) actually apply.
     */
    private void moveTowardsTarget() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            // drift slowly in place so the entity is still "alive" visually
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            return;
        }
        Vec3 aim = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        Vec3 delta = aim.subtract(this.position());
        double distance = delta.length();
        if (distance < 1.0E-3D) {
            return;
        }
        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.1D;
        if (distance < speed) {
            this.setDeltaMovement(delta);
        } else {
            this.setDeltaMovement(delta.scale(speed / distance));
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    @Override
    public void travel(Vec3 travelVector) {
        // flying, frictionless movement: the delta movement set by moveTowardsTarget() is the velocity
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
    }

    /** Attack cadence follows {@code Attributes.ATTACK_SPEED}, which the future phase triples. */
    private void tickAttack() {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double reach = 6.0D + this.getBbWidth() * 0.5D;
        if (this.distanceToSqr(target) > reach * reach) {
            return;
        }
        this.doHurtTarget(target);
        double attackSpeed = Math.max(0.5D, this.getAttributeValue(Attributes.ATTACK_SPEED));
        this.attackCooldown = Math.max(4, (int) Math.round(ATTACK_INTERVAL * 4.0D / attackSpeed));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        float healthBefore = living.getHealth();
        boolean hit = super.doHurtTarget(target);
        if (!hit) {
            return false;
        }
        float dealt = healthBefore - living.getHealth();
        if (dealt < MIN_DAMAGE) {
            // top the hit up through the YNA minimum damage channel so it can never be mitigated below 2
            MinimumDamage.apply(living, this.damageSources().mobAttack(this), MIN_DAMAGE - Math.max(0.0F, dealt));
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // target selection
    // ------------------------------------------------------------------------------------------------

    private void tickTarget(ServerLevel level, int tick) {
        LivingEntity current = this.getTarget();
        if (current != null && (!current.isAlive() || current.isRemoved())) {
            onTargetDeath(level);
            return;
        }
        if (current != null && !isValidTarget(current)) {
            this.setTarget(null);
            current = null;
        }
        if (current != null) {
            return;
        }
        if (this.targetScanCooldown-- > 0) {
            return;
        }
        this.targetScanCooldown = 20;
        LivingEntity found = findTarget(level);
        if (found != null) {
            this.setTarget(found);
        }
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel level) {
        AABB area = new AABB(
                this.getX() - RANGE, this.getY() - RANGE, this.getZ() - RANGE,
                this.getX() + RANGE, this.getY() + RANGE, this.getZ() + RANGE);

        LivingEntity bestPlayer = null;
        double bestPlayerDistance = Double.MAX_VALUE;
        LivingEntity bestOther = null;
        double bestOtherDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(this, area, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || !isValidTarget(living)) {
                continue;
            }
            double distance = this.distanceToSqr(living);
            if (distance > RANGE_SQR) {
                continue;
            }
            if (living instanceof Player) {
                if (distance < bestPlayerDistance) {
                    bestPlayerDistance = distance;
                    bestPlayer = living;
                }
            } else if (distance < bestOtherDistance) {
                bestOtherDistance = distance;
                bestOther = living;
            }
        }
        // players always win, players only lose when there is no player at all in range
        return bestPlayer != null ? bestPlayer : bestOther;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == this || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof IllusoryButterflyEntity) {
            return false;
        }
        if (entity instanceof Player player) {
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
                return false;
            }
            // 领巢者 (hive leaders) are excluded, reusing the very same data the /epca_hiveleader command writes
            return !HiveLeaderManager.isHiveLeader(player.getUUID());
        }
        return true;
    }

    private void onTargetDeath(ServerLevel level) {
        this.setTarget(null);
        if (this.deathHoleTicks > 0) {
            return;
        }
        BlackHoleHolder hole = spawnBlackHole(level, this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                TARGET_DEATH_BLACK_HOLE_RADIUS);
        this.deathHole = hole;
        this.deathHoleTicks = TARGET_DEATH_BLACK_HOLE_TICKS;
    }

    private void tickDeathBlackHole(ServerLevel level) {
        if (this.deathHoleTicks <= 0) {
            return;
        }
        this.deathHoleTicks--;
        if (this.deathHole != null) {
            this.deathHole.tick(level, TARGET_DEATH_BLACK_HOLE_RADIUS);
        }
        if (this.deathHoleTicks <= 0) {
            if (this.deathHole != null) {
                this.deathHole.discard();
                this.deathHole = null;
            }
            cleanupChildren(level);
            this.discard();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // present phase: 50% slow
    // ------------------------------------------------------------------------------------------------

    private void applyPresentSlow(ServerLevel level) {
        AABB area = new AABB(
                this.getX() - RANGE, this.getY() - RANGE, this.getZ() - RANGE,
                this.getX() + RANGE, this.getY() + RANGE, this.getZ() + RANGE);
        for (Entity entity : level.getEntities(this, area, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || living == this) {
                continue;
            }
            if (this.distanceToSqr(living) > RANGE_SQR) {
                continue;
            }
            AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) {
                continue;
            }
            if (speed.getModifier(SLOW_PRESENT_UUID) == null) {
                speed.addTransientModifier(new AttributeModifier(
                        SLOW_PRESENT_UUID, "epca_illusory_present_slow", -0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            this.presentSlowed.add(living.getUUID());
        }
    }

    private void removePresentSlow(ServerLevel level) {
        for (UUID id : this.presentSlowed) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(SLOW_PRESENT_UUID);
                }
            }
        }
        this.presentSlowed.clear();
    }

    // ------------------------------------------------------------------------------------------------
    // past phase: record / replay
    // ------------------------------------------------------------------------------------------------

    /**
     * True while block / entity changes around this butterfly have to be logged: the past record phase,
     * {@code tickCount} inside {@code [2300, 3200)}, on the server and still in the world.
     */
    boolean isRecordWindow() {
        if (this.isRemoved() || this.level().isClientSide()) {
            return false;
        }
        int tick = this.tickCount;
        return this.phase == PHASE_PAST_RECORD && tick >= T_RECORD_START && tick < T_RECORD_END;
    }

    // ---- block changes, forwarded by IllusionRewindRecorder ------------------------------------------

    /** A player broke a block inside the sphere: remember what it was so the rewind can put it back. */
    void onBlockBroken(ServerLevel level, BlockPos pos, BlockState destroyed) {
        if (level != this.level() || !this.isRecordWindow() || !this.inRange(pos)) {
            return;
        }
        // the replacement of a broken block is air (or whatever fluid flows in); only oldState is replayed
        this.addBlockChange(pos, destroyed, Blocks.AIR.defaultBlockState(), this.tickCount);
    }

    /** A player or an entity placed / replaced a block inside the sphere. */
    void onBlockPlaced(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level != this.level() || !this.isRecordWindow() || !this.inRange(pos)) {
            return;
        }
        if (oldState == newState) {
            return; // nothing changed (e.g. the same block placed over itself)
        }
        this.addBlockChange(pos, oldState, newState, this.tickCount);
    }

    private void addBlockChange(BlockPos pos, BlockState oldState, BlockState newState, int tick) {
        if (this.blockChangeCount >= MAX_BLOCK_CHANGES_TOTAL) {
            return;
        }
        List<BlockChange> bucket = this.blockChanges.computeIfAbsent(tick, key -> new ArrayList<>(8));
        if (bucket.size() >= MAX_BLOCK_CHANGES_PER_TICK) {
            return;
        }
        // BlockPos is immutable already, but a fresh instance keeps the log independent of the caller's pos
        BlockPos storedPos = pos.immutable();
        bucket.add(new BlockChange(storedPos, oldState, newState));
        this.blockChangeCount++;
        // every accepted block change is logged (there are only a handful per fight); filtered events stay silent
        epca.LOGGER.info("[butterfly] rec block {} -> {} at {} tick={}", oldState, newState, storedPos, tick);
    }

    // ---- entity spawns / deaths, forwarded by IllusionRewindRecorder ----------------------------------

    /** A non-disk entity joined the level: if it appeared inside the sphere, the rewind removes it again. */
    void onEntitySpawned(ServerLevel level, Entity entity) {
        if (!this.tracksEntity(level, entity)) {
            return;
        }
        if (!this.reserveEntityEvent()) {
            return;
        }
        // the bucket key is the spawn tick, so EntitySpawn does not have to repeat it
        this.entitySpawns.computeIfAbsent(this.tickCount, key -> new ArrayList<>(4))
                .add(new EntitySpawn(entity.getUUID(), entity.getType()));
        epca.LOGGER.info("[butterfly] rec spawn {} uuid={} tick={}", entity.getType(), entity.getUUID(), this.tickCount);
    }

    /** A living entity died inside the sphere: remember enough to bring it back on the rewind. */
    void onEntityDied(ServerLevel level, LivingEntity entity) {
        if (!this.tracksEntity(level, entity)) {
            return;
        }
        // health is already 0 here, so take the last sampled value (or the max health for an unsampled one)
        if (this.addRemoval(entity, this.lastRecordedHealth(entity.getUUID(), entity.getMaxHealth()))) {
            epca.LOGGER.info("[butterfly] rec death {} uuid={} tick={}", entity.getType(), entity.getUUID(), this.tickCount);
        }
    }

    /**
     * A living entity despawned / left inside the sphere.
     *
     * <p>A dead entity fires this event too when it is finally removed, but it is not alive any more at that
     * point, so it is skipped here: its {@link #onEntityDied} record already covers it and it can never be
     * resurrected twice. Non living entities (items, arrows, experience orbs) are never brought back,
     * respawning a picked up item stack would duplicate it.</p>
     */
    void onEntityLeft(ServerLevel level, Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (!this.tracksEntity(level, living)) {
            return;
        }
        if (this.addRemoval(living, this.lastRecordedHealth(living.getUUID(), living.getMaxHealth()))) {
            epca.LOGGER.info("[butterfly] rec leave {} uuid={} tick={}", living.getType(), living.getUUID(), this.tickCount);
        }
    }

    /** @return true when the removal was accepted into the log (the safety valve may reject it) */
    private boolean addRemoval(Entity entity, float health) {
        if (!this.reserveEntityEvent()) {
            return false;
        }
        this.entityRemovals.computeIfAbsent(this.tickCount, key -> new ArrayList<>(4))
                .add(new RecordedRemoval(entity.getUUID(), entity.getType(),
                        entity.getX(), entity.getY(), entity.getZ(), health));
        return true;
    }

    private boolean reserveEntityEvent() {
        if (this.entityEventCount >= MAX_ENTITY_EVENTS_TOTAL) {
            return false;
        }
        this.entityEventCount++;
        return true;
    }

    /** Health of the newest sample of that entity, or {@code fallback} when it was never sampled. */
    private float lastRecordedHealth(UUID uuid, float fallback) {
        for (List<EntitySample> samples : this.entitySamples.descendingMap().values()) {
            for (EntitySample sample : samples) {
                if (sample.uuid().equals(uuid)) {
                    return sample.health();
                }
            }
        }
        return fallback;
    }

    private boolean tracksEntity(ServerLevel level, Entity entity) {
        if (level != this.level() || entity == this || !this.isRecordWindow()) {
            return false;
        }
        if (entity instanceof Player) {
            // players are never removed / recreated, their position and health are rewound from the samples
            return false;
        }
        if (this.isOwnEntity(entity)) {
            return false;
        }
        return this.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) <= RANGE_SQR;
    }

    /** The butterfly and its scripted helpers are timeline driven and must never be rewound as mobs. */
    private boolean isOwnEntity(Entity entity) {
        return entity instanceof IllusoryButterflyEntity
                || entity instanceof IllusoryShadowEntity
                || entity instanceof BlackHoleEntity;
    }

    private boolean inRange(BlockPos pos) {
        return this.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= RANGE_SQR;
    }

    /**
     * Samples every living entity in the sphere, once per tick. This is the only part of the rewind that is
     * recorded unconditionally: 900 ticks x a few dozen entities, far below anything that would hurt.
     *
     * <p>Blocks and entity spawns / deaths do not need a sample at all, they arrive through
     * {@link IllusionRewindRecorder}; a tick in which nobody built anything costs one entity sweep.</p>
     */
    private void sampleEntities(ServerLevel level, int tick) {
        AABB area = new AABB(
                this.getX() - RANGE, this.getY() - RANGE, this.getZ() - RANGE,
                this.getX() + RANGE, this.getY() + RANGE, this.getZ() + RANGE);
        List<EntitySample> samples = null;
        for (Entity entity : level.getEntities(this, area, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || this.isOwnEntity(living)) {
                continue;
            }
            if (this.distanceToSqr(living) > RANGE_SQR) {
                continue; // the AABB above is a cube, the rewind is a sphere
            }
            if (samples == null) {
                samples = new ArrayList<>(16);
            }
            if (samples.size() >= MAX_ENTITIES_PER_TICK) {
                break;
            }
            samples.add(new EntitySample(living.getUUID(),
                    living.getX(), living.getY(), living.getZ(), living.getHealth()));
        }
        if (samples != null && !samples.isEmpty()) {
            this.entitySamples.put(tick, samples);
        }
        this.pruneRewindLog(tick);
    }

    /**
     * Keeps only the newest 45 s of the log, and never anything older than the record window itself, so the
     * four maps can never outgrow {@code MAX_RECORD_TICKS} keys.
     */
    private void pruneRewindLog(int tick) {
        int oldest = Math.max(T_RECORD_START, tick - MAX_RECORD_TICKS);
        if (oldest <= T_RECORD_START) {
            return;
        }
        this.entitySamples.headMap(oldest, false).clear();
        this.blockChanges.headMap(oldest, false).clear();
        this.entitySpawns.headMap(oldest, false).clear();
        this.entityRemovals.headMap(oldest, false).clear();
    }

    /** Drops the whole log (phase entry and reload); the rewind then simply has nothing to do. */
    private void clearRewindLog() {
        this.entitySamples.clear();
        this.blockChanges.clear();
        this.entitySpawns.clear();
        this.entityRemovals.clear();
        this.blockChangeCount = 0;
        this.entityEventCount = 0;
    }

    private void logRewindSummary() {
        epca.LOGGER.info("[butterfly] rewind start: {} block changes on {} ticks, {} entity samples, {} spawns, {} removals",
                this.blockChangeCount, this.blockChanges.size(), this.entitySamples.size(),
                this.countEntries(this.entitySpawns), this.countEntries(this.entityRemovals));
        // the tick mapping is the part that silently produced "nothing happened" before: print both ends
        epca.LOGGER.info("[butterfly] rewind map: replay {}..{} -> record {}..{} (newest first)",
                T_RECORD_END, T_PAST_END - 1, T_RECORD_END - 1, T_RECORD_START);
    }

    /** Resets the per replay diagnostic budgets and counters (phase entry; safe to call more than once). */
    private void resetRewindDiagnostics() {
        this.rewindBlockLogs = 0;
        this.rewindRemoveLogs = 0;
        this.rewindSpawnSkipLogs = 0;
        this.rewindReviveLogs = 0;
        this.rewindBlocksApplied = 0;
        this.rewindEntitiesRemoved = 0;
        this.rewindEntitiesRevived = 0;
    }

    /**
     * Diagnostic snapshot for {@code /epca_butterfly_info}: timeline tick, phase, the block change total
     * with the first five tick buckets, and the number of recorded spawns / removals.
     */
    public String debugInfo() {
        StringBuilder info = new StringBuilder(192);
        info.append("tick=").append(this.tickCount)
                .append(" phase=").append(phaseName(this.phase)).append('(').append(this.phase).append(')')
                .append(" blockChanges=").append(this.blockChangeCount)
                .append(" in ").append(this.blockChanges.size()).append(" ticks, first buckets: ");
        int shown = 0;
        for (Map.Entry<Integer, List<BlockChange>> bucket : this.blockChanges.entrySet()) {
            if (shown >= 5) {
                break;
            }
            if (shown > 0) {
                info.append(", ");
            }
            info.append(bucket.getKey()).append("->").append(bucket.getValue().size());
            shown++;
        }
        if (shown == 0) {
            info.append("none");
        }
        info.append("; entitySpawns=").append(this.countEntries(this.entitySpawns))
                .append(" entityRemovals=").append(this.countEntries(this.entityRemovals))
                .append(" entitySamples=").append(this.countEntries(this.entitySamples));
        return info.toString();
    }

    private static String phaseName(int phase) {
        return switch (phase) {
            case PHASE_PRESENT -> "PRESENT";
            case PHASE_PAST_RECORD -> "PAST_RECORD";
            case PHASE_PAST_REPLAY -> "PAST_REPLAY";
            case PHASE_FUTURE -> "FUTURE";
            case PHASE_TAIL -> "TAIL";
            case PHASE_END -> "END";
            default -> "UNKNOWN";
        };
    }

    private int countEntries(NavigableMap<Integer, ? extends List<?>> map) {
        int total = 0;
        for (List<?> bucket : map.values()) {
            total += bucket.size();
        }
        return total;
    }

    /**
     * Rewinds exactly one recorded tick, newest recorded tick first.
     *
     * <p>Replay tick {@code 3200} undoes record tick {@code 3199} and replay tick {@code 4099} undoes record
     * tick {@code 2300}, i.e. the 900 ticks of the record window are undone by the 900 ticks of the replay
     * window, one bucket per tick. Within a bucket everything is applied in reverse insertion order, so a
     * position that changed several times walks back through its own history and ends up exactly at the
     * state it had when the window opened.</p>
     *
     * <p>Order inside one replayed tick: entities that died come back first (the entity samples of the same
     * tick can then position them), then the entity samples, then the blocks, and finally the entities that
     * spawned on that tick are removed because they did not exist before it.</p>
     */
    private void replayRecordedTick(ServerLevel level, int replayTick) {
        // replay starts on T_RECORD_END (3200) and ends on T_PAST_END - 1 (4099): replay tick 3200 undoes
        // record tick 3199 and replay tick 4099 undoes record tick 2300, so the 900 replay ticks walk the
        // 900 recorded buckets newest first. (The old formula subtracted T_PAST_END instead of T_RECORD_END,
        // which asked for ticks 4099..3200 - outside the recorded window - and replayed nothing.)
        int recordTick = T_RECORD_END - 1 - (replayTick - T_RECORD_END);
        if (recordTick < T_RECORD_START || recordTick >= T_RECORD_END) {
            // nothing recorded (e.g. the save was reloaded mid fight): skip the rewind instead of crashing
            return;
        }

        List<RecordedRemoval> removals = this.entityRemovals.get(recordTick);
        if (removals != null) {
            for (RecordedRemoval removal : removals) {
                this.respawnRecorded(level, removal);
            }
        }

        List<EntitySample> samples = this.entitySamples.get(recordTick);
        if (samples != null) {
            for (EntitySample sample : samples) {
                this.restoreEntity(level, sample);
            }
        }

        List<BlockChange> changes = this.blockChanges.get(recordTick);
        if (changes != null) {
            for (int i = changes.size() - 1; i >= 0; i--) {
                this.restoreBlock(level, recordTick, changes.get(i));
            }
        }

        List<EntitySpawn> spawns = this.entitySpawns.get(recordTick);
        if (spawns != null) {
            for (EntitySpawn spawn : spawns) {
                Entity spawned = level.getEntity(spawn.uuid());
                if (spawned == null) {
                    // the golem may already be gone (killed / despawned): nothing to discard, but say so
                    this.logSpawnSkip(spawn, recordTick, "not in level");
                    continue;
                }
                if (spawned == this || spawned instanceof Player) {
                    continue;
                }
                if (this.isOwnEntity(spawned) || spawned.getType() != spawn.type()) {
                    this.logSpawnSkip(spawn, recordTick, "type mismatch " + spawned.getType());
                    continue;
                }
                // it appeared on this very tick, so the world before this tick did not contain it
                if (this.rewindRemoveLogs < REWIND_LOG_LIMIT) {
                    this.rewindRemoveLogs++;
                    epca.LOGGER.info("[butterfly] rewind remove {} uuid={}", spawned.getType(), spawn.uuid());
                }
                this.rewindEntitiesRemoved++;
                spawned.discard();
            }
        }
    }

    /** Throttled "the recorded spawn is no longer there" diagnostic (same budget as the remove lines). */
    private void logSpawnSkip(EntitySpawn spawn, int recordTick, String reason) {
        if (this.rewindSpawnSkipLogs >= REWIND_LOG_LIMIT) {
            return;
        }
        this.rewindSpawnSkipLogs++;
        epca.LOGGER.info("[butterfly] rewind remove SKIP {} uuid={} tick {}: {}",
                spawn.type(), spawn.uuid(), recordTick, reason);
    }

    /** Puts one sampled entity back to its position and health at that recorded tick. */
    private void restoreEntity(ServerLevel level, EntitySample entitySample) {
        Entity entity = level.getEntity(entitySample.uuid());
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
            return;
        }
        BlockPos restorePos = new BlockPos(
                Mth.floor(entitySample.x()), Mth.floor(entitySample.y()), Mth.floor(entitySample.z()));
        if (!level.hasChunkAt(restorePos)) {
            return; // never touch entities whose chunk is no longer loaded
        }
        float yRot = living.getYRot();
        float xRot = living.getXRot();
        living.setPos(entitySample.x(), entitySample.y(), entitySample.z());
        // 1.20.1 only has the no-argument variant: it copies the position we just set into xo/yo/zo,
        // so the client interpolates from the new spot instead of streaking across the map
        living.setOldPosAndRot();
        if (living instanceof ServerPlayer serverPlayer) {
            // players additionally need the position packet, otherwise the client snaps them back
            serverPlayer.connection.teleport(entitySample.x(), entitySample.y(), entitySample.z(),
                    yRot, xRot);
        }
        // never rewind anybody into (or out of) the grave: keep the restored health within 1..max
        float restored = Mth.clamp(entitySample.health(), 1.0F, living.getMaxHealth());
        if (Math.abs(living.getHealth() - restored) > 0.01F) {
            living.setHealth(restored);
        }
    }

    /** Puts one block back to the state it had before that recorded tick. */
    private void restoreBlock(ServerLevel level, int recordTick, BlockChange change) {
        BlockPos pos = change.pos();
        if (!level.hasChunkAt(pos)) {
            return;
        }
        if (level.getBlockState(pos) == change.oldState()) {
            return;
        }
        if (this.rewindBlockLogs < REWIND_LOG_LIMIT) {
            this.rewindBlockLogs++;
            epca.LOGGER.info("[butterfly] rewind block {} tick {} -> {}", pos, recordTick, change.oldState());
        }
        this.rewindBlocksApplied++;
        // flag 3 = UPDATE_NEIGHBORS | UPDATE_CLIENTS: flag 2 alone already notifies clients, but it leaves
        // neighbour dependent models (fences, walls, redstone, plants) in their stale shape. Flag 3 makes
        // the restored state settle visually; the record phase is over, so these updates cannot be re-logged.
        level.setBlock(pos, change.oldState(), 3);
    }

    /**
     * Brings back an entity that died or despawned inside the window.
     *
     * <p>Two guards keep this from ever duplicating a mob: an entity with that uuid that is still present is
     * left alone (a chunk unload fires {@code EntityLeaveLevelEvent} too, and that entity comes back on its
     * own), and a chunk that is not loaded is never force loaded just to respawn something.</p>
     */
    private void respawnRecorded(ServerLevel level, RecordedRemoval removal) {
        if (removal.type() == null || level.getEntity(removal.uuid()) != null) {
            return;
        }
        BlockPos pos = new BlockPos(
                Mth.floor(removal.x()), Mth.floor(removal.y()), Mth.floor(removal.z()));
        if (!level.hasChunkAt(pos)) {
            return;
        }
        Entity created = removal.type().create(level);
        if (created == null) {
            return;
        }
        // the uuid is restored on purpose: the entity samples of the earlier ticks refer to it, so the
        // rewind can keep moving the resurrected mob, and the "already present" guard above keeps working
        created.setUUID(removal.uuid());
        created.moveTo(removal.x(), removal.y(), removal.z(), 0.0F, 0.0F);
        if (created instanceof LivingEntity living) {
            living.setHealth(Mth.clamp(removal.health(), 1.0F, living.getMaxHealth()));
        }
        if (this.rewindReviveLogs < REWIND_LOG_LIMIT) {
            this.rewindReviveLogs++;
            epca.LOGGER.info("[butterfly] rewind revive {} uuid={}", removal.type(), removal.uuid());
        }
        this.rewindEntitiesRevived++;
        level.addFreshEntity(created);
    }

    // ------------------------------------------------------------------------------------------------
    // future phase: x3 speed, random slows
    // ------------------------------------------------------------------------------------------------

    private void applyFutureBuffs() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_FUTURE_UUID) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_FUTURE_UUID, "epca_illusory_future_speed", 2.0D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        AttributeInstance attackSpeed = this.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null && attackSpeed.getModifier(ATTACK_SPEED_FUTURE_UUID) == null) {
            attackSpeed.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_FUTURE_UUID, "epca_illusory_future_attack_speed", 2.0D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private void removeFutureBuffs() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_FUTURE_UUID);
        }
        AttributeInstance attackSpeed = this.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_FUTURE_UUID);
        }
    }

    private void applyRandomFutureSlow(ServerLevel level) {
        double factor = FUTURE_SLOW_FACTORS[this.random.nextInt(FUTURE_SLOW_FACTORS.length)];
        int expiry = this.tickCount + FUTURE_SLOW_DURATION;

        AABB area = new AABB(
                this.getX() - RANGE, this.getY() - RANGE, this.getZ() - RANGE,
                this.getX() + RANGE, this.getY() + RANGE, this.getZ() + RANGE);
        for (Entity entity : level.getEntities(this, area, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || living == this) {
                continue;
            }
            if (this.distanceToSqr(living) > RANGE_SQR) {
                continue;
            }
            applyRandomSlow(living, factor);
            this.randomSlowExpiry.put(living.getUUID(), expiry);
        }
        // the butterfly slows itself as well, which is a lot more interesting than a free x3 window
        applyRandomSlow(this, factor);
        this.randomSlowExpiry.put(this.getUUID(), expiry);
    }

    private void applyRandomSlow(LivingEntity living, double factor) {
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SLOW_RANDOM_UUID);
        double amount = factor - 1.0D;
        if (Math.abs(amount) < 1.0E-4D) {
            return;
        }
        speed.addTransientModifier(new AttributeModifier(
                SLOW_RANDOM_UUID, "epca_illusory_future_slow", amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private void tickExpiringSlows(ServerLevel level, int tick) {
        if (this.randomSlowExpiry.isEmpty()) {
            return;
        }
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : this.randomSlowExpiry.entrySet()) {
            if (entry.getValue() <= tick) {
                expired.add(entry.getKey());
            }
        }
        for (UUID id : expired) {
            this.randomSlowExpiry.remove(id);
            Entity entity = id.equals(this.getUUID()) ? this : level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(SLOW_RANDOM_UUID);
                }
            }
        }
    }

    private void clearRandomSlows(ServerLevel level) {
        for (UUID id : this.randomSlowExpiry.keySet()) {
            Entity entity = id.equals(this.getUUID()) ? this : level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(SLOW_RANDOM_UUID);
                }
            }
        }
        this.randomSlowExpiry.clear();
    }

    // ------------------------------------------------------------------------------------------------
    // tail phase: the black hole that eats the world
    // ------------------------------------------------------------------------------------------------

    private void spawnTailBlackHole(ServerLevel level) {
        Vec3 head = this.headPosition();
        this.tailHole = spawnBlackHole(level, head.x, head.y, head.z, TAIL_BLACK_HOLE_START_RADIUS);
        this.tailBlastDone = false;
    }

    private void tickTailBlackHole(ServerLevel level, int tick) {
        if (this.tailHole == null || this.tailHole.isRemoved()) {
            return;
        }
        float progress = Mth.clamp((tick - T_FUTURE_END) / (float) (T_TAIL_END - T_FUTURE_END), 0.0F, 1.0F);
        float radius = TAIL_BLACK_HOLE_START_RADIUS
                + (TAIL_BLACK_HOLE_MAX_RADIUS - TAIL_BLACK_HOLE_START_RADIUS) * progress;
        Vec3 head = this.headPosition();
        this.tailHole.tick(level, radius, head);
        devourBlocks(level, head, radius);
    }

    private void tickTailCollapse(ServerLevel level, int tick) {
        // idempotent: the first collapse tick (or the phase entry, whichever comes first) fires the blast
        runTailBlast(level);
        if (this.tailHole == null || this.tailHole.isRemoved()) {
            return;
        }
        float progress = Mth.clamp((tick - T_TAIL_END) / (float) TAIL_COLLAPSE_TICKS, 0.0F, 1.0F);
        float radius = TAIL_BLACK_HOLE_MAX_RADIUS * (1.0F - progress);
        if (radius <= 0.05F) {
            this.tailHole.discard();
            this.tailHole = null;
            return;
        }
        Vec3 head = this.headPosition();
        this.tailHole.tick(level, radius, head);
        devourBlocks(level, head, radius);
    }

    private void runTailBlast(ServerLevel level) {
        if (this.tailBlastDone) {
            return;
        }
        this.tailBlastDone = true;
        Vec3 center = this.headPosition();
        DamageSource source = voidDamageSource(level);
        AABB area = new AABB(
                center.x - TAIL_DAMAGE_RADIUS, center.y - TAIL_DAMAGE_RADIUS, center.z - TAIL_DAMAGE_RADIUS,
                center.x + TAIL_DAMAGE_RADIUS, center.y + TAIL_DAMAGE_RADIUS, center.z + TAIL_DAMAGE_RADIUS);
        for (Entity entity : level.getEntities(this, area, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living) || living == this) {
                continue;
            }
            if (living.position().distanceToSqr(center) > TAIL_DAMAGE_RADIUS * TAIL_DAMAGE_RADIUS) {
                continue;
            }
            // the YNA minimum damage channel bypasses armour / enchantments / effects and respects
            // creative + spectator, which is exactly the "void damage" contract we want here
            MinimumDamage.apply(living, source, VOID_DAMAGE);
        }
    }

    private DamageSource voidDamageSource(ServerLevel level) {
        Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(VOID_DAMAGE_TYPE);
        return new DamageSource(holder, this);
    }

    /**
     * Eats blocks that the growing event horizon touches. Only a small number of positions is sampled
     * per tick (and only near the rim of the sphere) so a radius 18 hole does not freeze the server.
     */
    private void devourBlocks(ServerLevel level, Vec3 center, float radius) {
        if (radius < 1.0F) {
            return;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int destroyed = 0;
        int attempts = TAIL_BLOCKS_PER_TICK * 4;
        for (int i = 0; i < attempts && destroyed < TAIL_BLOCKS_PER_TICK; i++) {
            // sample a thin shell close to the current horizon
            double distance = radius * (0.85D + this.random.nextDouble() * 0.15D);
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double cosPhi = this.random.nextDouble() * 2.0D - 1.0D;
            double sinPhi = Math.sqrt(1.0D - cosPhi * cosPhi);
            double x = center.x + distance * sinPhi * Math.cos(theta);
            double y = center.y + distance * cosPhi;
            double z = center.z + distance * sinPhi * Math.sin(theta);
            mutable.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
            BlockState state = level.getBlockState(mutable);
            if (state.isAir()) {
                continue;
            }
            if (state.is(BlockTags.WITHER_IMMUNE) || state.getDestroySpeed(level, mutable) < 0.0F) {
                continue; // bedrock, barriers, end portal frames, ... must survive
            }
            level.destroyBlock(mutable, false);
            destroyed++;
        }
    }

    private void finish(ServerLevel level) {
        cleanupChildren(level);
        ExperienceOrb.award(level, this.position(), XP_REWARD);
        this.discard();
    }

    private Vec3 headPosition() {
        return new Vec3(this.getX(), this.getY() + HEAD_HEIGHT, this.getZ());
    }

    // ------------------------------------------------------------------------------------------------
    // shadow helper entity
    // ------------------------------------------------------------------------------------------------

    private void spawnShadow(ServerLevel level, int kind, int color) {
        removeShadow(level);
        IllusoryShadowEntity shadow = ModEntities.ILLUSORY_SHADOW.get().create(level);
        if (shadow == null) {
            return;
        }
        shadow.configure(kind, color, 1.1F);
        shadow.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(shadow);
        this.shadowId = shadow.getUUID();
        this.shadowKind = kind;
    }

    private void tickShadow(ServerLevel level) {
        if (this.shadowId == null) {
            return;
        }
        Entity shadow = level.getEntity(this.shadowId);
        if (shadow == null) {
            this.shadowId = null;
            this.shadowKind = 0;
            return;
        }
        shadow.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
    }

    private void removeShadow(ServerLevel level) {
        if (this.shadowId != null) {
            Entity shadow = level.getEntity(this.shadowId);
            if (shadow != null) {
                shadow.discard();
            }
        }
        this.shadowId = null;
        this.shadowKind = 0;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.ILLUSORY_BUTTERFLY.get(), SoundSource.HOSTILE, 8.0F, 1.0F);
            epca.LOGGER.info("[butterfly] spawn sound broadcast at {} {} {}", this.getX(), this.getY(), this.getZ());
        }
    }
    private void playSpawnSound(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.ILLUSORY_BUTTERFLY.get(), SoundSource.HOSTILE, 8.0F, 1.0F);
    }

    // ------------------------------------------------------------------------------------------------
    // black hole wrapper
    // ------------------------------------------------------------------------------------------------

    private BlackHoleHolder spawnBlackHole(ServerLevel level, double x, double y, double z, float radius) {
        var type = ModEntities.BLACK_HOLE.get();
        var hole = type.create(level);
        if (hole == null) {
            return new BlackHoleHolder(null, x, y, z);
        }
        hole.moveTo(x, y, z, 0.0F, 0.0F);
        hole.setRadius(radius);
        level.addFreshEntity(hole);
        return new BlackHoleHolder(hole, x, y, z);
    }

    /** Tiny holder so the butterfly can keep a handle on a black hole without hard depending on its type. */
    private static final class BlackHoleHolder {
        @Nullable
        private final Entity entity;
        private final double x;
        private final double y;
        private final double z;

        private BlackHoleHolder(@Nullable Entity entity, double x, double y, double z) {
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private boolean isRemoved() {
            return this.entity == null || this.entity.isRemoved();
        }

        private void tick(ServerLevel level, float radius) {
            this.tick(level, radius, new Vec3(this.x, this.y, this.z));
        }

        private void tick(ServerLevel level, float radius, Vec3 position) {
            if (!(this.entity instanceof BlackHoleEntity hole)) {
                return;
            }
            if (hole.isRemoved()) {
                return;
            }
            hole.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
            hole.setRadius(radius);
        }

        private void discard() {
            if (this.entity != null) {
                this.entity.discard();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // cleanup / persistence
    // ------------------------------------------------------------------------------------------------

    private void cleanupChildren(ServerLevel level) {
        removeShadow(level);
        if (this.tailHole != null) {
            this.tailHole.discard();
            this.tailHole = null;
        }
        if (this.deathHole != null) {
            this.deathHole.discard();
            this.deathHole = null;
        }
        removePresentSlow(level);
        clearRandomSlows(level);
        removeFutureBuffs();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel level) {
            cleanupChildren(level);
        }
        IllusionRewindRecorder.end(this);
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TimelineTick", this.tickCount);
        tag.putInt("Phase", this.phase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TimelineTick")) {
            this.tickCount = tag.getInt("TimelineTick");
        }
        this.phase = phaseFor(this.tickCount);
        // the rewind log is intentionally not persisted: after a reload there is nothing to rewind, so the
        // log is emptied and replayRecordedTick() no-ops until the next record window fills it again. This
        // also keeps a half filled (and possibly stale) log from ever being replayed.
        clearRewindLog();
        this.nextRecordTick = Math.max(T_RECORD_START, this.tickCount);
        this.nextReplayTick = Math.max(T_PAST_END, this.tickCount);
        // a reload inside the record window skips the phase transition, so the recorder has to be re-armed
        // by hand; outside of it the butterfly must not be registered at all
        if (this.phase == PHASE_PAST_RECORD) {
            IllusionRewindRecorder.begin(this);
        } else {
            IllusionRewindRecorder.end(this);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // rewind log records
    // ------------------------------------------------------------------------------------------------

    /** One living entity sample: its uuid, position and health at the recorded tick. */
    private record EntitySample(UUID uuid, double x, double y, double z, float health) {
    }

    /**
     * A single block change observed by {@link IllusionRewindRecorder}. {@code oldState} is what the rewind
     * puts back, {@code newState} is what the change installed (kept for diagnostics and logging only, the
     * replay never writes it).
     */
    private record BlockChange(BlockPos pos, BlockState oldState, BlockState newState) {
    }

    /** An entity that appeared inside the sphere; the bucket it lives in is its spawn tick. */
    private record EntitySpawn(UUID uuid, EntityType<?> type) {
    }

    /**
     * An entity that died / left inside the sphere, plus everything needed to recreate it. The bucket it
     * lives in is the tick it was removed on.
     */
    private record RecordedRemoval(UUID uuid, EntityType<?> type, double x, double y, double z, float health) {
    }
}
