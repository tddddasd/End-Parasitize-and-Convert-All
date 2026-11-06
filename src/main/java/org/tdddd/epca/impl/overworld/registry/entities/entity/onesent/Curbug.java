package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;


import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.client.entity.EpcaGeoAnimations;
import org.tdddd.epca.impl.client.entity.IAutoRenderableEntity;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Curbug extends PathfinderMob implements IAutoRenderableEntity, IHeadRotatable, Enemy {

    private final Map<Integer, Integer> touchingEntities = new HashMap<>();
    private static final int TOUCH_THRESHOLD = 2;
    private int growTimer = -1;
    private int spawnAnimationTimer = 0;
    private boolean isHiding = false;
    private BlockPos hidingPos = null;
    private int hideTimer = 0;
    private static final int HIDE_DURATION = 200;
    private int hideSearchCooldown = 0;

    public ResourceLocation model, texture, animation;
    protected static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ANIM_RUN  = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation ANIM_DIE  = RawAnimation.begin().thenPlay("die");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final EntityDataAccessor<Boolean> DATA_IS_RUNNING =
            SynchedEntityData.defineId(Curbug.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(Curbug.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(Curbug.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(Curbug.class, EntityDataSerializers.BOOLEAN);

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

    public Curbug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);

        EpcaEntityManager.track(this);
        this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
        this.setMaxUpStep(0.5F);
        this.setMaxUpStep(0.5F);

        this.xpReward = 2;
        if (!level.isClientSide) {
            this.growTimer = this.random.nextInt(1200) + 1200;
        }
        this.navigation = new GroundPathNavigation(this, level);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ARMOR, 1.5D)
                .build();
    }

    @Override
    protected void registerGoals() {
        epcaRegisterGoals();
        this.goalSelector.addGoal(0, new FleeFromTargetersGoal(this, 16.0F, 1.2D, 1.4D));
        this.goalSelector.addGoal(3, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<Player>(this, Player.class, 7.0F, 1.0D, 1.2D) {
            @Override
            public boolean canUse() {
                Player nearestPlayer = this.mob.level().getNearestPlayer(this.mob, this.maxDist);
                if (nearestPlayer != null) { this.toAvoid = nearestPlayer; return true; }
                return false;
            }
        });
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0, 16));
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(2, new HideInFoliageGoal(this, 12.0D, 1.0D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.008F) {
            @Override
            public boolean canUse() {
                if (Curbug.this.isHiding) return false;
                return super.canUse();
            }
            @Override
            public boolean canContinueToUse() {
                if (Curbug.this.isHiding) return false;
                return super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new RandomSoundGoal(this, ModSoundEvents.CURBUG_SAY.get()));
    }

    protected void epcaRegisterGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        epcaTick();

        if (spawnAnimationTimer > 0) spawnAnimationTimer--;

        if (hideTimer > 0) {
            hideTimer--;
            if (hideTimer <= 0) {
                isHiding = false;
                hidingPos = null;
            }
        }

        if (!this.level().isClientSide) {
            handleEntityContact();

            if (this.isAlive()) {
                if (growTimer > 0) {
                    growTimer--;
                    if (growTimer <= 0) {
                        if (this.isInWater()) growIntoFins();
                        else growIntoRupter();
                    }
                }
            }
        }
    }

    protected void epcaTick() {
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

    static class FleeFromTargetersGoal extends AvoidEntityGoal<LivingEntity> {
        private final Curbug curbug;
        private final double radius;
        private int searchCooldown = 0;
        private static final int SEARCH_INTERVAL = 5;

        public FleeFromTargetersGoal(Curbug curbug, float maxDist, double walkSpeed, double sprintSpeed) {
            super(curbug, LivingEntity.class, maxDist, walkSpeed, sprintSpeed, e -> false);
            this.curbug = curbug;
            this.radius = maxDist;
        }

        @Override
        public boolean canUse() {
            if (--searchCooldown > 0) return false;
            searchCooldown = SEARCH_INTERVAL;

            LivingEntity nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Mob mob : curbug.level().getEntitiesOfClass(
                    Mob.class,
                    curbug.getBoundingBox().inflate(radius, 4.0D, radius),
                    m -> m != null && m.isAlive() && m.getTarget() == curbug)) {
                double d = curbug.distanceToSqr(mob);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = mob;
                }
            }
            if (nearest != null) {
                this.toAvoid = nearest;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.toAvoid == null || !this.toAvoid.isAlive()) return false;
            if (this.toAvoid instanceof Mob mob && mob.getTarget() != curbug) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            this.toAvoid = null;
        }
    }

    @Override
    public void die(DamageSource source) {
        epcaDie(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = epcaHurt(source, amount);
        if (result && !this.level().isClientSide && this.isHiding) {
            this.isHiding = false;
            this.hideTimer = 0;
            this.hidingPos = null;
        }
        return result;
    }

    @Override public ResourceLocation model()     { return this.model; }
    @Override public ResourceLocation texture()   { return this.texture; }
    @Override public ResourceLocation animation() { return this.animation; }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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

    }

    public BlockPos getDeathPosition() { return deathPosition; }

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

    protected void applyMovementSpeed() {
        var attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double target = this.getTarget() != null ? chaseMoveSpeed : baseMoveSpeed;
            if (attr.getBaseValue() != target) attr.setBaseValue(target);
        }
    }

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

    protected boolean tickStepSounds(SoundEvent stepSound) {
        if (!this.level().isClientSide && this.onGround() && isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(stepSound, 0.8F, 1.0F);
                this.stepSoundDelay = 10 + this.random.nextInt(6);
                return true;
            } else {
                this.stepSoundDelay--;
            }
        } else {
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }
        return false;
    }

    protected boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    protected SoundEvent getAmbientSound() { return null; }

    private void triggerDismemberment() {
        try {
            Class.forName("me.ichun.mods.mobdismemberment.common.MobDismemberment")
                    .getMethod("trigger", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, this);
        } catch (Throwable ignored) {

        }
    }

    protected int getEvolutionStage() {
        if (this.level() instanceof ServerLevel serverLevel) {
            return EvolutionManager.getStageForDimension(serverLevel);
        }
        return 0;
    }

    protected static class RandomSoundGoal extends Goal {
        private final Mob entity;
        private final SoundEvent sound;
        private int nextSoundTick;

        public RandomSoundGoal(Mob entity, SoundEvent sound) {
            this.entity = entity;
            this.sound = sound;
            this.setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return entity.isAlive() && !entity.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = entity.getRandom().nextInt(120) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                entity.playSound(sound, 1.0F, 1.0F);
                this.nextSoundTick = entity.getRandom().nextInt(120) + 80;
            }
        }
    }

    private void handleEntityContact() {
        touchingEntities.keySet().removeIf(id -> {
            Entity entity = this.level().getEntity(id);
            return entity == null || !entity.isAlive() ||
                    !this.getBoundingBox().intersects(entity.getBoundingBox());
        });

        for (Entity entity : this.level().getEntities(this, this.getBoundingBox())) {
            if (entity instanceof LivingEntity living && entity.isAlive() && !ParasiteEvents.isParasiteByTagOrInterface(living)) {
                int entityId = entity.getId();
                int touchTime = touchingEntities.getOrDefault(entityId, 0) + 1;
                touchingEntities.put(entityId, touchTime);
                if (touchTime >= TOUCH_THRESHOLD) {
                    applyParaCurseEffect(living);
                    touchingEntities.remove(entityId);
                }
            }
        }
    }

    private void applyParaCurseEffect(LivingEntity target) {
        MobEffectInstance current = target.getEffect(ModEffects.PARA_CURSE.get());
        int newAmplifier = current != null ? Math.min(current.getAmplifier() + 1, 2) : 0;
        target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 400, newAmplifier, false, true, true));
    }

    private void growIntoFins() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Fins fins = ModEntities.FINS.get().create(serverLevel);
        if (fins != null) {
            fins.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            serverLevel.addFreshEntity(fins);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.CURBUG_EVOLVE.get(), this.getSoundSource(), 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            this.discard();
        }
    }

    private void growIntoRupter() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Ripper rupter = ModEntities.RIPPER.get().create(serverLevel);
        if (rupter != null) {
            rupter.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            serverLevel.addFreshEntity(rupter);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.CURBUG_EVOLVE.get(), this.getSoundSource(), 1.0F, 1.0F);
            ModParticles.spawnConversion(serverLevel,
                    this.getX(), this.getY() + 0.5, this.getZ(), 2, 0xFFFFFF);
            this.discard();
        }
    }

    @Override
    protected SoundEvent getDeathSound() { return ModSoundEvents.RIPPER_DEATH.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSoundEvents.RIPPER_HUNT.get(); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::predicate));
        controllers.add(new AnimationController<>(this, "spawn_controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::spawnPredicate));
    }

    private PlayState spawnPredicate(AnimationState<Curbug> event) {
        if (this.spawnAnimationTimer > 0) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("spawn"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private PlayState predicate(AnimationState<Curbug> event) {
        if (this.spawnAnimationTimer > 0) return PlayState.STOP;
        event.getController().setAnimation(RawAnimation.begin().thenLoop(isMoving() ? "walk" : "idle"));
        return PlayState.CONTINUE;
    }

    public static boolean checkBuglinSpawnRules(
            EntityType<Curbug> entityType, ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        int blockLight = levelAccessor.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = levelAccessor.getBrightness(LightLayer.SKY, pos);
        long dayTime = levelAccessor.getLevelData().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime <= 23000;

        if (isNight) { if (blockLight > 8) return false; }
        else { if (skyLight > 8 || blockLight > 8) return false; }

        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 0 || stage > 2) return false;
        }
        return levelAccessor.getMaxLocalRawBrightness(pos) < 8;
    }

    static class HideInFoliageGoal extends Goal {
        private final Curbug curbug;
        private final double searchRadius;
        private final double speed;
        private BlockPos targetPos;
        private int searchCooldown = 0;
        private static final int SEARCH_INTERVAL = 20;

        public HideInFoliageGoal(Curbug curbug, double searchRadius, double speed) {
            this.curbug = curbug;
            this.searchRadius = searchRadius;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (curbug.getTarget() != null) return false;
            if (curbug.isHiding) return false;
            if (curbug.isInWater()) return false;

            if (--searchCooldown > 0) return false;
            searchCooldown = SEARCH_INTERVAL;

            targetPos = findFoliage();
            return targetPos != null;
        }

        private BlockPos findFoliage() {
            BlockPos origin = curbug.blockPosition();
            int r = (int) searchRadius;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        mp.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!isFoliage(mp)) continue;
                        double d = origin.distSqr(mp);
                        if (d < bestDist) {
                            bestDist = d;
                            best = mp.immutable();
                        }
                    }
                }
            }
            return best;
        }

        private boolean isFoliage(BlockPos pos) {
            BlockState state = curbug.level().getBlockState(pos);
            if (state.isAir()) return false;
            if (!state.getFluidState().isEmpty()) return false;

            VoxelShape shape = state.getCollisionShape(curbug.level(), pos);
            if (!shape.isEmpty()) return false;

            Block block = state.getBlock();
            if (block instanceof BaseRailBlock) return false;
            if (block instanceof RedStoneWireBlock) return false;
            if (block instanceof TripWireBlock) return false;

            if (block instanceof BushBlock
                    || block instanceof TallGrassBlock
                    || block instanceof FlowerBlock
                    || block instanceof DoublePlantBlock) {
                return true;
            }
            if (state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SMALL_FLOWERS)
                    || state.is(BlockTags.TALL_FLOWERS)) {
                return true;
            }
            return !state.isSolid();
        }

        @Override
        public void start() {
            if (targetPos != null) {
                curbug.getNavigation().moveTo(
                        targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                        speed);
            }
        }

        @Override
        public void tick() {
            if (targetPos == null) return;
            double distSqr = curbug.distanceToSqr(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5);
            if (distSqr < 1.0) {
                curbug.isHiding = true;
                curbug.hidingPos = targetPos.immutable();
                curbug.hideTimer = HIDE_DURATION;
                curbug.getNavigation().stop();
                curbug.getLookControl().setLookAt(
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5);
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (curbug.isHiding) return false;
            if (targetPos == null) return false;
            return !curbug.getNavigation().isDone();
        }

        @Override
        public void stop() {
            targetPos = null;
            if (!curbug.isHiding) {
                curbug.getNavigation().stop();
            }
        }
    }
}
