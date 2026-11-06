package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.client.entity.IAutoRenderableEntity;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.BoneArrow;
import org.tdddd.epca.impl.utils.EntityHealthUtils;
import org.tdddd.epca.impl.utils.entity.EntityBreakUtils;
import org.tdddd.epca.impl.utils.entity.EntityMovementUtils;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;

public class InfestedSkeleton extends PathfinderMob implements RangedAttackMob, IAutoRenderableEntity, Enemy, IHeadRotatable {

public enum Variant { DEFAULT, FIRED }
private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<String> DATA_POSE_ANIM =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.STRING);
private static final EntityDataAccessor<Boolean> DATA_SHOOT_ANIM =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.BOOLEAN);
private static final EntityDataAccessor<Integer> DATA_SHOOT_ANIM_TIMER =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<Float> DATA_TARGET_PITCH =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.FLOAT);
private static final EntityDataAccessor<Float> DATA_SHOOT_THETA =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.FLOAT);
private static final int ATTACK_COOLDOWN = 40;
private static final int MAX_RANGE = 21;
private static final float ARROW_SPEED = 1.6F;
private static final Vec3 LOCATOR_OFFSET = new Vec3(0.0, 1.2, 0.5);
private static final double SAFE_DISTANCE = 0.0D;
private static final double MIN_DISTANCE = 0.0D;
private boolean isRetreating = false;
private int retreatCooldown = 0;
private static final int RETREAT_COOLDOWN = 5;
private static final ThreadLocal<EntitySpawnReason> SPAWN_TYPE = new ThreadLocal<>();
public InfestedSkeleton(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        EpcaEntityManager.track(this);
        this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
        var stepHeight = this.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) stepHeight.setBaseValue(0.5F);
        this.fakeDeathEnabled = true;
        this.fakeDeathChance = 40;
        this.xpReward = 8;
        this.baseSpeed = 0.21D;
        this.chaseSpeed = 0.29D;

        if (!level.isClientSide()) {
            EntitySpawnReason spawnType = SPAWN_TYPE.get();
            SPAWN_TYPE.remove();
            int stage = EvolutionManager.getStageForDimension((ServerLevel) level);
            boolean allowFireVariant = (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION)
                    ? (stage >= 4) : true;
            if (allowFireVariant && this.random.nextFloat() < 0.3f) {
                this.setVariant(Variant.FIRED);
            } else {
                this.setVariant(Variant.DEFAULT);
            }
        }
        this.navigation = new GroundPathNavigation(this, level);
    }
public Variant getVariant() {
        Integer ordinal = this.entityData.get(DATA_VARIANT);
        if (ordinal == null) return Variant.DEFAULT;
        int index = Mth.clamp(ordinal, 0, Variant.values().length - 1);
        return Variant.values()[index];
    }
public void setVariant(Variant variant) { this.entityData.set(DATA_VARIANT, variant.ordinal()); }
public Identifier getTextureResource() {
        return switch (getVariant()) {
            case FIRED -> Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_skeleton_fired.png");
            default -> Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_skeleton.png");
        };
    }
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", this.getVariant().name());
    }
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getString("Variant").isPresent()) {
            try { this.setVariant(Variant.valueOf(tag.getStringOr("Variant", ""))); }
            catch (IllegalArgumentException e) { this.setVariant(Variant.DEFAULT); }
        }
    }
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        defineSynchedData_b1(entityData);
        entityData.define(DATA_POSE_ANIM, "none");
        entityData.define(DATA_SHOOT_ANIM, false);
        entityData.define(DATA_SHOOT_ANIM_TIMER, 0);
        entityData.define(DATA_TARGET_PITCH, 0.0F);
        entityData.define(DATA_SHOOT_THETA, 0.0F);
        entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
    }
public String getPoseAnim() { return this.entityData.get(DATA_POSE_ANIM); }
public int getShootAnimationTimer() { return this.entityData.get(DATA_SHOOT_ANIM_TIMER); }
private void setShootAnimationTimer(int timer) { this.entityData.set(DATA_SHOOT_ANIM_TIMER, timer); }
public float getShootTheta() { return this.entityData.get(DATA_SHOOT_THETA); }
public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ARMOR, 2.0D)
                .build();
    }
    protected void registerGoals() {
        registerGoals_b1();
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, ATTACK_COOLDOWN, MAX_RANGE) {
            public boolean canUse() {
                LivingEntity target = InfestedSkeleton.this.getTarget();
                if (target == null || !target.isAlive()) return false;
                return super.canUse() && InfestedSkeleton.this.distanceToSqr(target) <= MAX_RANGE * MAX_RANGE;
            }
        });
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 48.0D));
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0, 16));
    }
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (isFakingDeath()) return;
        Vec3 worldLocatorPos = calculateWorldLocatorPosition();
        BoneArrow arrow = new BoneArrow(this.level(), this);
        arrow.setPos(worldLocatorPos.x, worldLocatorPos.y, worldLocatorPos.z);

        double d0 = target.getX() - worldLocatorPos.x;
        double d1 = target.getY(0.3333333333333333D) - worldLocatorPos.y;
        double d2 = target.getZ() - worldLocatorPos.z;
        double horizontalDist = Math.sqrt(d0 * d0 + d2 * d2);
        arrow.shoot(d0, d1 + horizontalDist * 0.2D, d2, ARROW_SPEED, 0.5F);
        arrow.setYRot(this.getYRot());
        arrow.yRotO = this.getYRot();

        if (getVariant() == Variant.FIRED) {
            arrow.setRemainingFireTicks(600);
            arrow.getPersistentData().putBoolean("InfestedFireArrow", true);
        }

        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
        this.level().addFreshEntity(arrow);
        setShootAnimationTimer(5);
    }
private Vec3 calculateWorldLocatorPosition() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double dx = LOCATOR_OFFSET.x * cos - LOCATOR_OFFSET.z * sin;
        double dz = LOCATOR_OFFSET.x * sin + LOCATOR_OFFSET.z * cos;
        return this.position().add(dx, LOCATOR_OFFSET.y, dz);
    }
    public void tick() {
        tick_b1();

        if (isFakingDeath()) return;

        if (!this.level().isClientSide()) {
            if (getShootAnimationTimer() > 0) setShootAnimationTimer(getShootAnimationTimer() - 1);
            this.entityData.set(DATA_TARGET_PITCH, getTargetPitchDeg());

            boolean isMoving = isMoving();
            boolean hasTarget = this.getTarget() != null;
            if (isRetreating) {
                setWalking(true); setRunning(false);
            } else {
                setRunning(isMoving && hasTarget);
                setWalking(isMoving && !hasTarget);
            }

            applyInfestedMovementSpeed();

            LivingEntity target = this.getTarget();
            if (target != null && !isFakingDeath()) {
                if (retreatCooldown > 0) retreatCooldown--;
                if (this.distanceTo(target) < MIN_DISTANCE && retreatCooldown <= 0) {
                    startRetreating(target);
                    retreatCooldown = RETREAT_COOLDOWN;
                }
                if (isRetreating) {
                    updateRetreatMovement(target);
                    if (this.distanceTo(target) >= SAFE_DISTANCE) stopRetreating();
                }
            } else if (isRetreating) stopRetreating();

            tickStepSounds(ModSoundEvents.INFESTED_SKELETON_STEP.get());
        }
    }
private float getTargetPitchDeg() {
        LivingEntity target = this.getTarget();
        if (target == null) return 0.0F;
        double dx = target.getX() - this.getX();
        double dy = target.getY() + target.getEyeHeight() - (this.getY() + this.getEyeHeight());
        double dz = target.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        return (float) Mth.clamp(Math.toDegrees(Math.atan2(dy, horizontalDist)), -30.0, 90.0);
    }
private void startRetreating(LivingEntity target) {
        if (target == null) return;
        isRetreating = true;
        this.getNavigation().stop();
    }
private void updateRetreatMovement(LivingEntity target) {
        if (target == null) return;
        Vec3 awayDir = this.position().subtract(target.position()).normalize();
        this.getNavigation().moveTo(awayDir.scale(8.0).add(this.position()).x,
                this.getY(), awayDir.scale(8.0).add(this.position()).z, 1.0);
    }
private void stopRetreating() {
        isRetreating = false;
        this.getNavigation().stop();
    }
    protected void onNormalDeathActions(DamageSource source) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            WalkingSkeletonHead head = ModEntities.WALKING_SKELETON_HEAD.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (head != null) {
                head.setPos(this.getX(), this.getY(), this.getZ());
                head.setYRot(this.random.nextFloat() * 360.0F);
                head.setVariant(WalkingSkeletonHead.Variant.valueOf(getVariant().name()));
                serverLevel.addFreshEntity(head);
            }
        }
    }
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.INFESTED_SKELETON_IDLE.get();
    }
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_SKELETON_HURT.get();
    }
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INFESTED_SKELETON_DEATH.get();
    }
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", EpcaAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));
    }
private PlayState animationPredicate(AnimationTest<InfestedSkeleton> event) {
        if (this.isFakingDeath())
            event.setAnimation(RawAnimation.begin().thenLoop("dead"));
        else if (this.isRunning())
            event.setAnimation(RawAnimation.begin().thenLoop("run"));
        else if (this.isWalking())
            event.setAnimation(RawAnimation.begin().thenLoop("walk"));
        else
            event.setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }
public static boolean checkInfestedSkeletonSpawnRules(
            EntityType<InfestedSkeleton> entityType, ServerLevelAccessor level,
            EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 5) return false;
        }
        SPAWN_TYPE.set(spawnType);
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

public Identifier model, texture, animation;

protected static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

protected static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");

protected static final RawAnimation ANIM_RUN  = RawAnimation.begin().thenLoop("run");

protected static final RawAnimation ANIM_DIE  = RawAnimation.begin().thenPlay("die");

private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

protected static final EntityDataAccessor<Boolean> DATA_IS_RUNNING =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.BOOLEAN);

protected static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.BOOLEAN);

protected static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.BOOLEAN);

protected static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.BOOLEAN);

protected double baseMoveSpeed = 0.25D;

protected double chaseMoveSpeed = 0.38D;

protected boolean fakeDeathEnabled = false;

protected int fakeDeathChance = 40;

protected int fakeDeathDuration = 30;

protected float fakeDeathHealth = 0.02F;

protected final EntityMovementUtils.FloatingState floatingState = new EntityMovementUtils.FloatingState();

protected int breakCooldown = 0;

protected int jumpCooldown = 0;

protected int ambientSoundTime;

protected int minAmbientSoundDelay = 12 * 20;

protected int maxAmbientSoundDelay = 16 * 20;

protected int fakeDeathTimer = 0;

protected BlockPos deathPosition;

protected int stepSoundDelay = 0;

protected static final Identifier WANDER_SPEED_ID = Identifier.fromNamespaceAndPath(epca.MODID, "wander_speed_reduction");

protected static final AttributeModifier WANDER_SPEED_REDUCTION =
            new AttributeModifier(WANDER_SPEED_ID, -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

protected double baseSpeed = 0.27D;

protected double chaseSpeed = 0.38D;

protected float fakeDeathBurstChance = 0.4f;

@Override public Identifier model()     { return this.model; }

@Override public Identifier texture()   { return this.texture; }

@Override public Identifier animation() { return this.animation; }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void registerControllers_b1(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>("controller", EpcaAnimations.GEO_TRANSITION_TICKS, state -> {
            if (isFakingDeath()) return PlayState.STOP;
            if (!state.isMoving()) return state.setAndContinue(ANIM_IDLE);
            if (isRunning()) return state.setAndContinue(ANIM_RUN);
            if (isWalking()) return state.setAndContinue(ANIM_WALK);
            return state.setAndContinue(ANIM_IDLE);
        }));
    }

    protected void defineSynchedData_b1(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_IS_RUNNING, false);
        entityData.define(DATA_IS_WALKING, false);
        entityData.define(DATA_IS_FAKING_DEATH, false);
        entityData.define(DATA_IS_INVULNERABLE, false);
    }

public boolean isRunning() { return this.entityData.get(DATA_IS_RUNNING); }

public void setRunning(boolean running) { this.entityData.set(DATA_IS_RUNNING, running); }

public boolean isWalking() { return this.entityData.get(DATA_IS_WALKING); }

public void setWalking(boolean walking) { this.entityData.set(DATA_IS_WALKING, walking); }

public boolean isFakingDeath() { return this.entityData.get(DATA_IS_FAKING_DEATH); }

protected void setFakingDeath(boolean faking) { this.entityData.set(DATA_IS_FAKING_DEATH, faking); }

public boolean isInvulnerable() { return this.entityData.get(DATA_IS_INVULNERABLE); }

public void setInvulnerable(boolean invulnerable) { this.entityData.set(DATA_IS_INVULNERABLE, invulnerable); }

public static AttributeSupplier.Builder createBaseAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    protected void registerGoals_b1() {

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public void tick_b1() {
        super.tick();

        if (!this.level().isClientSide()) {

            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean hasTarget = this.getTarget() != null;
            setRunning(isMoving && hasTarget);
            setWalking(isMoving && !hasTarget);
            if (!isMoving) { setRunning(false); setWalking(false); }

            if (this.getTarget() == null && --this.ambientSoundTime <= 0) {
                this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
                SoundEvent ambient = getAmbientSound();
                if (ambient != null) this.playSound(ambient, 1.0F, 1.0F);
            }

            breakCooldown = EntityBreakUtils.tryBreakLightSources(this, breakCooldown);

            EntityMovementUtils.updateWaterFloating(this, floatingState);
        }

        if (isFakingDeath()) {
            if (!this.level().isClientSide()) {
                fakeDeathTimer--;
                if (fakeDeathTimer <= 0) {
                    onFakeDeathEnd();
                }
            }
        }
    }

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
        this.setBurstHealth(fakeDeathHealth);
        this.setNoAi(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
    }

protected void setBurstHealth(float requested) {
        this.setHealth(EntityHealthUtils.burstHealth(this, requested));
    }

protected void onFakeDeathEnd() {
        setFakingDeath(false);
        if (!this.level().isClientSide()) {
            onFakeDeathBurst();
        }
        triggerDismemberment();
        this.discard();
    }

protected void onFakeDeathBurst_b1() {

    }

public BlockPos getDeathPosition() { return deathPosition; }

    public boolean hurtServer_b1(ServerLevel level, DamageSource source, float amount) {
        if (isFakingDeath()) return false;

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (ParasiteEvents.shouldIgnoreDamageFrom(this, attacker)) return false;
            ParasiteEvents.onAttacked(this, attacker);
        }

        float adjustedAmount = ParasiteEvents.handleParasiteDamage(this, source, amount);
        return super.hurtServer(level, source, adjustedAmount);
    }

    public void die_b1(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }
        if (!this.level().isClientSide() && tryFakeDeath()) {
            return;
        }
        super.die(source);
    }

public boolean canPassThroughInfestedLeaves() { return true; }

    public boolean startRiding(Entity vehicle, boolean force, boolean sendEventAndTriggers) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) return false;
        return super.startRiding(vehicle, force, sendEventAndTriggers);
    }

    protected boolean canRide(Entity entity) {
        if (entity instanceof Boat || entity instanceof Minecart) return false;
        return super.canRide(entity);
    }

    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            EntityMovementUtils.applyWaterTravel(this, travelVector);
        } else {
            super.travel(travelVector);
        }
    }

    public boolean isAffectedByFluids() { return true; }

    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) { return false; }

protected void applyMovementSpeed() {
        var attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double target = this.getTarget() != null ? chaseMoveSpeed : baseMoveSpeed;
            if (attr.getBaseValue() != target) attr.setBaseValue(target);
        }
    }

protected void tickStepSounds(SoundEvent stepSound, int minDelay, int maxDelay, float volume) {
        if (!this.level().isClientSide() && this.onGround() && isMoving()) {
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

protected boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

@Override public boolean removeWhenFarAway(double d) { return false; }

protected SoundEvent getAmbientSound_b1() { return null; }

private void triggerDismemberment() {
        try {
            org.tdddd.epca.impl.network.packet.s2c.DismemberS2CPacket.send(this);
        } catch (Throwable ignored) {

        }
    }

    protected void onFakeDeathBurst() {
        performFakeDeathBurst(deathPosition);
    }

protected void performFakeDeathBurst(BlockPos burstPos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (burstPos == null) burstPos = this.blockPosition();

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        serverLevel.sendParticles(ModParticles.PARA_CURSE.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                10, 0.3, 0.45, 0.3, 0.0);

        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                7, 0.4, 0.3, 0.4, 0.2);

        BlockPos deathPos = burstPos;
        long seed = this.random.nextLong();
        int delay = this.random.nextInt(30) + 40;

        serverLevel.getServer().schedule(new TickTask(
                serverLevel.getServer().getTickCount() + delay,
                () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))
        ));

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
        cloud.setRadius(1.5F);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, 1200, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
        serverLevel.addFreshEntity(cloud);
    }

protected static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide() || deathPos == null) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_LARGE.get().defaultBlockState(), 1);

        int mediumCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_MEDIUM.get().defaultBlockState(), mediumCount);

        int smallCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_SMALL.get().defaultBlockState(), smallCount);
    }

protected static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos,
                                             RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5;
            double offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);

            pos.set(
                    deathPos.getX() + offsetX,
                    deathPos.getY() + 1 + offsetY,
                    deathPos.getZ() + offsetZ
            );

            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }

protected void applyInfestedMovementSpeed() {
        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute == null) return;

        movementAttribute.removeModifier(WANDER_SPEED_ID);

        if (this.getTarget() != null) {
            movementAttribute.setBaseValue(chaseSpeed);
        } else {
            movementAttribute.setBaseValue(baseSpeed);
            movementAttribute.addTransientModifier(WANDER_SPEED_REDUCTION);
        }
    }

protected void tickStepSounds(SoundEvent stepSound) {
        tickStepSounds(stepSound, 20, 30, 1.0F);
    }

    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (isInvulnerable()) return false;
        return hurtServer_b1(serverLevel, source, amount);
    }

    public void die(DamageSource source) {

        if (isFakingDeath()) {
            die_b1(source);
            return;
        }

        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = 0;
            if (minKills > 0) {
                int currentDeaths = 0;
                if (false) {
                    ;
                    this.setHealth(this.getMaxHealth());
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            die_b1(source);
            return;
        }

        if (!this.level().isClientSide() && this.fakeDeathEnabled && this.getHealth() <= 0.0F && this.random.nextFloat() < fakeDeathBurstChance) {
            onTriggerFakeDeath(source);
        } else {
            onNormalDeathActions(source);
            die_b1(source);
        }
    }

protected void onNormalDeathActions_b2(DamageSource source) {

    }

protected void onTriggerFakeDeath(DamageSource source) {
        startFakeDeath();
    }

public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                EvolutionManager.forDimension(serverLevel).addPoints(1);
            }
            this.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    60,
                    0,
                    false, false, true
            ));
        }
    }
}
