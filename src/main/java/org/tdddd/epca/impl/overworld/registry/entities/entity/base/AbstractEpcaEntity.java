package org.tdddd.epca.impl.overworld.registry.entities.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.IAutoRenderableEntity;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.utils.entity.EntityBreakUtils;
import org.tdddd.epca.impl.utils.entity.EntityMovementUtils;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Abstract base class for all EPCA entities.
 * Provides common logic extracted from entity implementations:
 * <ul>
 *   <li>GeoEntity + IAutoRenderableEntity (auto model/texture/animation + auto renderer)</li>
 *   <li>Walking/running/idle/death animation state machine</li>
 *   <li>Fake death system with configurable burst</li>
 *   <li>Water floating and travel</li>
 *   <li>Boat/minecart riding prevention</li>
 *   <li>Light source breaking</li>
 *   <li>Head rotation via IHeadRotatable</li>
 *   <li>Common data sync for movement states</li>
 * </ul>
 */
public abstract class AbstractEpcaEntity extends PathfinderMob
        implements IAutoRenderableEntity, IParasite, Enemy, IHeadRotatable {

    // ────────── Geo resources ──────────
    public ResourceLocation model, texture, animation;
    protected static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ANIM_RUN  = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation ANIM_DIE  = RawAnimation.begin().thenPlay("die");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ────────── Sync data keys ──────────
    protected static final EntityDataAccessor<Boolean> DATA_IS_RUNNING =
            SynchedEntityData.defineId(AbstractEpcaEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(AbstractEpcaEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(AbstractEpcaEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(AbstractEpcaEntity.class, EntityDataSerializers.BOOLEAN);

    // ────────── Movement config ──────────
    protected double baseMoveSpeed = 0.25D;
    protected double chaseMoveSpeed = 0.38D;

    // ────────── Fake death config ──────────
    protected boolean fakeDeathEnabled = false;
    protected int fakeDeathChance = 40;        // percentage chance on death
    protected int fakeDeathDuration = 30;      // ticks
    protected float fakeDeathHealth = 0.02F;   // health to set during fake death

    // ────────── Floating ──────────
    protected final EntityMovementUtils.FloatingState floatingState = new EntityMovementUtils.FloatingState();

    // ────────── Break cooldown ──────────
    protected int breakCooldown = 0;

    // ────────── Jump cooldown ──────────
    protected int jumpCooldown = 0;

    // ────────── Ambient sound ──────────
    protected int ambientSoundTime;
    protected int minAmbientSoundDelay = 12 * 20;
    protected int maxAmbientSoundDelay = 16 * 20;

    // ────────── Constructors ──────────

    protected AbstractEpcaEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        EpcaEntityManager.track(this);
        this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
        this.setMaxUpStep(0.5F);
    }

    /**
     * Constructor with a consumer for post-init configuration (e.g. setting resources).
     */
    protected AbstractEpcaEntity(EntityType<? extends PathfinderMob> entityType, Level level,
                                  Consumer<AbstractEpcaEntity> configurer) {
        this(entityType, level);
        configurer.accept(this);
    }

    // ────────── IGeoResources ──────────

    @Override public ResourceLocation model()     { return this.model; }
    @Override public ResourceLocation texture()   { return this.texture; }
    @Override public ResourceLocation animation() { return this.animation; }

    // ────────── GeoEntity ──────────

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 3, state -> {
            if (isFakingDeath()) return PlayState.STOP;
            if (!state.isMoving()) return state.setAndContinue(ANIM_IDLE);
            if (isRunning()) return state.setAndContinue(ANIM_RUN);
            if (isWalking()) return state.setAndContinue(ANIM_WALK);
            return state.setAndContinue(ANIM_IDLE);
        }));
    }

    // ────────── Data sync ──────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
    }

    // ────────── State accessors ──────────

    public boolean isRunning() { return this.entityData.get(DATA_IS_RUNNING); }
    public void setRunning(boolean running) { this.entityData.set(DATA_IS_RUNNING, running); }

    public boolean isWalking() { return this.entityData.get(DATA_IS_WALKING); }
    public void setWalking(boolean walking) { this.entityData.set(DATA_IS_WALKING, walking); }

    public boolean isFakingDeath() { return this.entityData.get(DATA_IS_FAKING_DEATH); }
    protected void setFakingDeath(boolean faking) { this.entityData.set(DATA_IS_FAKING_DEATH, faking); }

    public boolean isInvulnerable() { return this.entityData.get(DATA_IS_INVULNERABLE); }
    public void setInvulnerable(boolean invulnerable) { this.entityData.set(DATA_IS_INVULNERABLE, invulnerable); }

    // ────────── Base attributes ──────────

    public static AttributeSupplier.Builder createBaseAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    // ────────── AI goals ──────────

    @Override
    protected void registerGoals() {
        // Subclasses should override and call super, then add their own goals
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ────────── Tick ──────────

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Update movement animation state
            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean hasTarget = this.getTarget() != null;
            setRunning(isMoving && hasTarget);
            setWalking(isMoving && !hasTarget);
            if (!isMoving) { setRunning(false); setWalking(false); }

            // Ambient sound
            if (this.getTarget() == null && --this.ambientSoundTime <= 0) {
                this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
                SoundEvent ambient = getAmbientSound();
                if (ambient != null) this.playSound(ambient, 1.0F, 1.0F);
            }

            // Light breaking
            breakCooldown = EntityBreakUtils.tryBreakLightSources(this, breakCooldown);

            // Water floating
            EntityMovementUtils.updateWaterFloating(this, floatingState);
        }

        // Fake death timer
        if (isFakingDeath()) {
            if (!this.level().isClientSide) {
                fakeDeathTimer--;
                if (fakeDeathTimer <= 0) {
                    onFakeDeathEnd();
                }
            }
        }
    }

    // ────────── Fake death ──────────

    protected int fakeDeathTimer = 0;
    protected BlockPos deathPosition;

    protected boolean tryFakeDeath() {
        if (!fakeDeathEnabled || isFakingDeath()) return false;
        if (fakeDeathChance > 0 && this.getRandom().nextInt(100) >= fakeDeathChance) return false;
        startFakeDeath();
        return true;
    }

    protected void startFakeDeath() {
        if (isFakingDeath()) return;
        setFakingDeath(true);
        setInvulnerable(true);
        fakeDeathTimer = fakeDeathDuration;
        deathPosition = this.blockPosition();
        this.setHealth(fakeDeathHealth);
        this.setNoAi(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
    }

    protected void onFakeDeathEnd() {
        setFakingDeath(false);
        if (!this.level().isClientSide) {
            onFakeDeathBurst();
        }
        this.discard();
    }

    /**
     * Override in subclasses to add custom burst effects (particles, sounds, remains blocks, etc.)
     * when the fake death timer expires.
     * Called server-side only, immediately before discard.
     */
    protected void onFakeDeathBurst() {
        // default: no burst
    }

    public BlockPos getDeathPosition() { return deathPosition; }

    // ────────── Hurt / Die ──────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFakingDeath()) return false;

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) return false;
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        float adjustedAmount = onHurt(source, amount);
        return super.hurt(source, adjustedAmount);
    }

    @Override
    public void die(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }
        if (!this.level().isClientSide && tryFakeDeath()) {
            this.onDeath(source);
            return;
        }
        super.die(source);
        this.onDeath(source);
    }

    // ────────── IParasite ──────────

    @Override
    public boolean canPassThroughInfestedLeaves() { return true; }

    // ────────── Boat/Minecart prevention ──────────

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) return false;
        return super.startRiding(vehicle, force);
    }

    @Override
    protected boolean canRide(Entity entity) {
        if (entity instanceof Boat || entity instanceof Minecart) return false;
        return super.canRide(entity);
    }

    // ────────── Water travel ──────────

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            EntityMovementUtils.applyWaterTravel(this, travelVector);
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected boolean isAffectedByFluids() { return true; }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) { return false; }

    // ────────── Movement speed ──────────

    protected void applyMovementSpeed() {
        var attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double target = this.getTarget() != null ? chaseMoveSpeed : baseMoveSpeed;
            if (attr.getBaseValue() != target) attr.setBaseValue(target);
        }
    }

    // ────────── Step sounds ──────────

    protected int stepSoundDelay = 0;

    /**
     * Tick step footstep sounds. Call from tick() or customServerAiStep().
     * Plays the given sound when on ground and moving, with a cooldown.
     */
    protected void tickStepSounds(SoundEvent stepSound, int minDelay, int maxDelay, float volume) {
        if (!this.level().isClientSide && this.onGround() && isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(stepSound, volume, 1.0F);
                this.stepSoundDelay = minDelay + this.random.nextInt(maxDelay - minDelay + 1);
            } else {
                this.stepSoundDelay--;
            }
        } else {
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }
    }

    // ────────── Movement utility ──────────

    /**
     * Check if entity is actually moving horizontally.
     */
    protected boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    // ────────── Misc overrides ──────────

    @Override public boolean removeWhenFarAway(double d) { return false; }

    // ────────── Sound stub ──────────

    /** Override to provide custom ambient sound. */
    protected SoundEvent getAmbientSound() { return null; }
}
