package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;


import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.EpcaGeoAnimations;
import org.tdddd.epca.impl.client.entity.IAutoRenderableEntity;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.utils.EntityHealthUtils;
import org.tdddd.epca.impl.utils.entity.EntityBreakUtils;
import org.tdddd.epca.impl.utils.entity.EntityMovementUtils;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

public class WalkingZombieHead extends PathfinderMob implements IAutoRenderableEntity, IHeadRotatable, Enemy {

    public ResourceLocation model, texture, animation;
    protected static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ANIM_RUN  = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation ANIM_DIE  = RawAnimation.begin().thenPlay("die");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final EntityDataAccessor<Boolean> DATA_IS_RUNNING =
            SynchedEntityData.defineId(WalkingZombieHead.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(WalkingZombieHead.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(WalkingZombieHead.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(WalkingZombieHead.class, EntityDataSerializers.BOOLEAN);

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

    protected static final UUID WANDER_SPEED_ID = UUID.fromString("A3766B59-7066-4402-AD81-0E3B7B6C2B9B");
    protected static final AttributeModifier WANDER_SPEED_REDUCTION =
            new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);

    protected double baseSpeed = 0.27D;
    protected double chaseSpeed = 0.38D;

    protected float fakeDeathBurstChance = 0.4f;

    public WalkingZombieHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);

        EpcaEntityManager.track(this);
        this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
        this.setMaxUpStep(0.5F);
        this.fakeDeathEnabled = true;
        this.fakeDeathChance = 40;

        this.xpReward = 8;
        this.fakeDeathEnabled = false;
        this.navigation = new GroundPathNavigation(this, level);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.8D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    @Override public ResourceLocation model()     { return this.model; }
    @Override public ResourceLocation texture()   { return this.texture; }
    @Override public ResourceLocation animation() { return this.animation; }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<WalkingZombieHead> event) {
        boolean inWater = this.isInWater();
        if (inWater) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(event.isMoving() ? "walk_water" : "idle_water"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(event.isMoving() ? "walk" : "idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
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

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
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
            if (!this.level().isClientSide) {
                fakeDeathTimer--;
                if (fakeDeathTimer <= 0) {
                    onFakeDeathEnd();
                }
            }
        }
    }

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
        if (!this.level().isClientSide) {
            onFakeDeathBurst();
        }
        triggerDismemberment();
        this.discard();
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
        serverLevel.getServer().tell(new TickTask(
                serverLevel.getServer().getTickCount() + delay,
                () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))
        ));

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
        cloud.setRadius(1.5F);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 1200, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
        serverLevel.addFreshEntity(cloud);
    }

    public BlockPos getDeathPosition() { return deathPosition; }

    protected static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide || deathPos == null) return;
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

    protected void applyMovementSpeed() {
        var attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double target = this.getTarget() != null ? chaseMoveSpeed : baseMoveSpeed;
            if (attr.getBaseValue() != target) attr.setBaseValue(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerable()) return false;
        return epcaHurt(source, amount);
    }

    protected boolean epcaHurt(DamageSource source, float amount) {
        if (isFakingDeath()) return false;

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (ParasiteEvents.shouldIgnoreDamageFrom(this, attacker)) return false;
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            ParasiteEvents.onAttacked(this, attacker);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {

        if (isFakingDeath()) {
            epcaDie(source);
            return;
        }

        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = 0;
            if (minKills > 0) {
                int currentDeaths = 0;
                if (false) {
                    ;
                    this.deathTime = 0;
                    this.setHealth(this.getMaxHealth());
                    this.clearFire();
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            epcaDie(source);
            return;
        }

        if (!this.level().isClientSide && this.fakeDeathEnabled && this.getHealth() <= 0.0F && this.random.nextFloat() < fakeDeathBurstChance) {
            onTriggerFakeDeath(source);
        } else {
            onNormalDeathActions(source);
            epcaDie(source);
        }

    }

    protected void epcaDie(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }
        if (!this.level().isClientSide && tryFakeDeath()) {
            return;
        }
        super.die(source);
    }

    protected void onNormalDeathActions(DamageSource source) {

    }

    protected void onTriggerFakeDeath(DamageSource source) {
        startFakeDeath();
    }

        public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
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

        public boolean canPassThroughInfestedLeaves() { return true; }

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

    protected int stepSoundDelay = 0;

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

    protected void tickStepSounds(SoundEvent stepSound) {
        tickStepSounds(stepSound, 20, 30, 1.0F);
    }

    protected boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.WALKING_HEAD_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.WALKING_HEAD_SAY.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.WALKING_HEAD_DEATH.get();
    }

    private void triggerDismemberment() {
        try {
            Class.forName("me.ichun.mods.mobdismemberment.common.MobDismemberment")
                    .getMethod("trigger", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, this);
        } catch (Throwable ignored) {

        }
    }

    @SuppressWarnings("unused")
    private static Consumer<WalkingZombieHead> unusedConfigurerHook() { return null; }

    public static boolean checkWalkingZombieHeadSpawnRules(
            EntityType<WalkingZombieHead> entityType, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 4) return false;
        }
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}
