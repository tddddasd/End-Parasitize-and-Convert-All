package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class InfestedCow extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy , IHeadRotatable {
    private int stepSoundDelay = 0;
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    public int tryApplyCothEffect(Mob mob, int currentCooldown) {
        return 0;
    }

    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(InfestedCow.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(InfestedCow.class, EntityDataSerializers.BOOLEAN);
    
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(InfestedCow.class, EntityDataSerializers.BOOLEAN);

    private static final UUID WANDER_SPEED_ID = UUID.fromString("A5766B59-7066-4402-AD81-0E3B7B6C2B9B");
    private static final AttributeModifier WANDER_SPEED_REDUCTION = new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);

    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 12 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 16 * 20; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedCow.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 30;
    private BlockPos deathPosition; 

    
    private int chargeCooldown = 0;                      
    private int chargeTicksRemaining = 0;                 
    private Vec3 chargeDirection;                          
    private LivingEntity chargeTarget;                     
    private List<Entity> chargedEntities = new ArrayList<>(); 
    private static final int CHARGE_COOLDOWN_TICKS = 200;          
    private static final double CHARGE_SPEED_MULTIPLIER = 1.2;     
    private static final int MAX_CHARGE_DISTANCE = 8;              
    private static final int STOP_AHEAD_TICKS = 20;                 

    public InfestedCow(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        this.setMaxUpStep(0.5F);
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    private int breakCooldown = 0;

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.level().isClientSide) {
            breakCooldown = tryBreakLightSources(this, breakCooldown);
            
            tickCharge();
        }
    }

    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    
    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
    }

    private void setCharging(boolean charging) {
        this.entityData.set(DATA_IS_CHARGING, charging);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.ARMOR, 5.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(0, new ChargeGoal());

        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new InfestedCow.RandomSoundGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    @Override
    public void tick() {
        super.tick();

        
        if (!this.level().isClientSide) {
            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean hasTarget = this.getTarget() != null;

            
            if (!isCharging()) {
                this.setRunning(isMoving && hasTarget);
                this.setWalking(isMoving && !hasTarget);

                if (isMoving && hasTarget) {
                    this.setWalking(false);
                } else if (isMoving && !hasTarget) {
                    this.setRunning(false);
                } else {
                    this.setRunning(false);
                    this.setWalking(false);
                }
            } else {
                
                this.setRunning(true);
                this.setWalking(false);
            }
        }

        
        if (isFakingDeath()) {
            super.tick();
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                if (!this.level().isClientSide) {
                    
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ModParticles.COTH.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                10, 0.3, 0.45, 0.3, 0.0);

                        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                7, 0.4, 0.3, 0.4, 0.2);

                        BlockPos deathPos = this.deathPosition;
                        long seed = this.random.nextLong();
                        int delay = this.random.nextInt(30) + 40;

                        serverLevel.getServer().tell(new TickTask(
                                serverLevel.getServer().getTickCount() + delay,
                                () -> {
                                    spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed));
                                    spawnBuglins(serverLevel, deathPos, RandomSource.create(seed));
                                }
                        ));

                        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
                        cloud.setRadius(1.5F);
                        cloud.setDuration(60);
                        cloud.setRadiusPerTick(0);
                        cloud.setWaitTime(0);
                        cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 1, false, true));
                        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
                        serverLevel.addFreshEntity(cloud);
                    }
                }
                this.discard();
                return;
            }
            return;
        }

        
        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            boolean hasTarget = this.getTarget() != null;
            movementAttribute.removeModifier(WANDER_SPEED_ID);

            if (hasTarget) {
                movementAttribute.setBaseValue(chaseSpeed);
            } else {
                movementAttribute.setBaseValue(baseSpeed);
                movementAttribute.addTransientModifier(WANDER_SPEED_REDUCTION);
            }

            if (!this.level().isClientSide) {
                double horizontalMovement = this.getDeltaMovement().horizontalDistance();
                boolean isActuallyMoving = horizontalMovement > 0.01;

                if (!isCharging()) {
                    this.setRunning(isActuallyMoving && hasTarget);
                    this.setWalking(isActuallyMoving && !hasTarget);
                    if (this.isRunning()) {
                        this.setWalking(false);
                    }
                }
            }
        }

        if (!this.level().isClientSide) {
            
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
                    playAmbientSound();
                }
            }
            
            updateFloating();

            
            if (chargeCooldown > 0) {
                chargeCooldown--;
            }
        }

        
        if (!this.level().isClientSide && this.onGround() && this.isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(ModSoundEvents.INFESTED_COW_STEP.get(), 1.0F, 1.0F);
                
                this.stepSoundDelay = 15 + this.random.nextInt(6);
            } else {
                this.stepSoundDelay--;
            }
        } else {
            
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    static class RandomSoundGoal extends Goal {
        private final InfestedCow infestedCow;
        private int nextSoundTick;

        RandomSoundGoal(InfestedCow infestedCow) {
            this.infestedCow = infestedCow;
        }

        

        @Override
        public boolean canUse() {
            return infestedCow.isAlive() && !infestedCow.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = infestedCow.getRandom().nextInt(120) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                playRandomLivingSound();
                this.nextSoundTick = infestedCow.getRandom().nextInt(120) + 80;
            }
        }

        private void playRandomLivingSound() {
            infestedCow.playSound(ModSoundEvents.INFESTED_COW_IDLE.get(), 1.0F, 1.0F);
        }
    }

    
    private void tickCharge() {
        if (!isCharging()) {
            return;
        }

        
        if (chargeTarget == null || !chargeTarget.isAlive() || chargeTicksRemaining <= 0) {
            stopCharging();
            return;
        }

        
        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * CHARGE_SPEED_MULTIPLIER;
        if (speed <= 0) speed = 0.3; 

        
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(chargeDirection.scale(speed));

        
        BlockPos nextBlockPos = new BlockPos((int) Math.floor(nextPos.x), (int) Math.floor(nextPos.y), (int) Math.floor(nextPos.z));
        BlockPos belowNext = nextBlockPos.below();
        boolean canStand = this.level().getBlockState(belowNext).isSolid();

        
        if (!canStand && chargeTicksRemaining > STOP_AHEAD_TICKS) {
            stopCharging();
            return;
        }

        
        this.setDeltaMovement(chargeDirection.x * speed, 0, chargeDirection.z * speed);
        
        this.setOnGround(true);
        
        this.move(MoverType.SELF, new Vec3(chargeDirection.x * speed, 0, chargeDirection.z * speed));

        
        AABB chargeBox = this.getBoundingBox();
        List<Entity> nearby = this.level().getEntities(this, chargeBox, entity -> {
            if (entity == this || !entity.isAlive()) return false;
            if (IParasite.isParasiteNoLivingByTagOrInterface(entity) || entity instanceof Creeper) return false;
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
            return true;
        });

        for (Entity hit : nearby) {
            if (!chargedEntities.contains(hit)) {
                chargedEntities.add(hit);
                
                if (hit instanceof LivingEntity living) {
                    living.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.25f);
                }
            }
        }

        chargeTicksRemaining--;

        
        if (chargeTicksRemaining <= 0) {
            stopCharging();
        }
    }

    private void startCharging(LivingEntity target) {
        if (target == null) return;
        this.chargeTarget = target;
        this.chargeDirection = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ()).normalize();
        double distance = this.distanceTo(target);
        
        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * CHARGE_SPEED_MULTIPLIER;
        if (speed <= 0) speed = 0.3;
        int ticksNeeded = (int) Math.ceil(Math.min(distance, MAX_CHARGE_DISTANCE) / speed);
        this.chargeTicksRemaining = ticksNeeded;
        this.chargedEntities.clear();
        this.setCharging(true);
        this.getNavigation().stop(); 
        this.setTarget(target); 
    }

    private void stopCharging() {
        this.setCharging(false);
        this.chargeTarget = null;
        this.chargeDirection = null;
        this.chargedEntities.clear();
        this.chargeCooldown = CHARGE_COOLDOWN_TICKS;
        
        this.setDeltaMovement(Vec3.ZERO);
    }

    
    private static void spawnBuglins(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            EntityType<?> buglinType = ModEntities.CURBUG.get();
            Entity buglin = buglinType.create(level);
            if (buglin != null) {
                double offsetX = random.nextDouble() - 0.5;
                double offsetY = random.nextDouble() * 0.5;
                double offsetZ = random.nextDouble() - 0.5;
                buglin.setPos(pos.getX() + 0.5 + offsetX, pos.getY() + offsetY, pos.getZ() + 0.5 + offsetZ);
                if (buglin instanceof LivingEntity) {
                    ((LivingEntity) buglin).setDeltaMovement(
                            (random.nextDouble() - 0.5) * 0.1,
                            random.nextDouble() * 0.1,
                            (random.nextDouble() - 0.5) * 0.1
                    );
                }
                level.addFreshEntity(buglin);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_COW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INFESTED_COW_DEATH.get();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) {
                return false;
            }
        }

        if (isFakingDeath() || isInvulnerable()) {
            return false;
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        float adjustedAmount = ((IParasite) this).onHurt(source, amount);
        return super.hurt(source, adjustedAmount);
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::playState));
    }

    private PlayState playState(AnimationState<InfestedCow> event) {
        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        } else if (this.isRunning()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        } else if (this.isWalking()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedCowSpawnRules(
            EntityType<InfestedCow> entityType,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 2 || stage > 5) {
                return false;
            }
        }
        BlockPos groundPos = pos.below();
        BlockState groundState = levelAccessor.getBlockState(groundPos);
        return groundState.is(Blocks.GRASS_BLOCK);
    }

    private double baseSpeed = 0.32D;
    private double chaseSpeed = 0.36D;

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public void die(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }

        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = config.getMinimumKillCount();
            if (minKills > 0) {
                int currentDeaths = DamageAdaptation.getDeathCount(this);
                if (currentDeaths < minKills) {
                    DamageAdaptation.recordDeath(this);
                    this.setHealth(this.getMaxHealth());
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            
            super.die(source);
            this.onDeath(source);
            return;
        }

        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < 0.4f) {
            triggerFakeDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
            if (!this.level().isClientSide) {
                if (this.random.nextFloat() < 0.3f) {
                    WalkingCowHead head = ModEntities.WALKING_COW_HEAD.get().create(this.level());
                    if (head != null) {
                        head.setPos(this.getX(), this.getY(), this.getZ());
                        head.setYRot(this.random.nextFloat() * 360.0F);
                        this.level().addFreshEntity(head);
                    }
                }
            }
        }
    }

    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedCow.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        
        this.entityData.define(DATA_IS_CHARGING, false);
    }

    public boolean isRunning() {
        return this.entityData.get(DATA_IS_RUNNING);
    }

    public boolean isWalking() {
        return this.entityData.get(DATA_IS_WALKING);
    }

    private void setRunning(boolean running) {
        this.entityData.set(DATA_IS_RUNNING, running);
    }

    private void setWalking(boolean walking) {
        this.entityData.set(DATA_IS_WALKING, walking);
    }

    public boolean isInvulnerable() {
        return this.entityData.get(DATA_IS_INVULNERABLE);
    }

    public void setInvulnerable(boolean invulnerable) {
        this.entityData.set(DATA_IS_INVULNERABLE, invulnerable);
    }

    private void triggerFakeDeath(DamageSource source) {
        setFakingDeath(true);
        setInvulnerable(true);
        fakeDeathTimer = 30;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
        this.entityData.set(DATA_IS_FAKING_DEATH, true);
    }

    private static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide || deathPos == null) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_LARGE.get().defaultBlockState(), 1);
        int mediumCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_MEDIUM.get().defaultBlockState(), mediumCount);
        int smallCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_SMALL.get().defaultBlockState(), smallCount);
    }

    private static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos,
                                          RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5;
            double offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);
            pos.set(deathPos.getX() + offsetX, deathPos.getY() + 1 + offsetY, deathPos.getZ() + offsetZ);
            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (vehicle instanceof Entity && (vehicle instanceof Boat || vehicle instanceof Minecart)) {
            return false;
        }
        return super.startRiding(vehicle, force);
    }

    @Override
    protected boolean canRide(Entity entity) {
        if (entity instanceof Entity && (entity instanceof Boat || entity instanceof Minecart)) {
            return false;
        }
        return super.canRide(entity);
    }

    private int floatingTime;

    private void updateFloating() {
        if (this.isInWater()) {
            Vec3 vec3 = this.getDeltaMovement();
            if (vec3.y < 0.0D) {
                this.setDeltaMovement(vec3.x, Math.max(vec3.y * 0.8D, -0.05D), vec3.z);
            }
            this.floatingTime++;
            if (this.floatingTime > 10) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.4D, 0.0D));
                this.floatingTime = 0;
            }
        } else {
            this.floatingTime = 0;
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.01F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    
    class ChargeGoal extends Goal {
        public ChargeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (chargeCooldown > 0) return false;
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!hasLineOfSight(target)) return false;
            double distance = distanceToSqr(target);
            if (distance > MAX_CHARGE_DISTANCE * MAX_CHARGE_DISTANCE) return false; 
            
            if (Math.abs(target.getY() - getY()) > 1.0) return false;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return isCharging();
        }

        @Override
        public void start() {
            startCharging(getTarget());
        }

        @Override
        public void stop() {
            
            if (isCharging()) {
                stopCharging();
            }
        }
    }
}