package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class InfestedFox extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy , IHeadRotatable {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.BOOLEAN);
    private static final UUID WANDER_SPEED_ID = UUID.fromString("A3766B59-7066-4402-AD81-0E3B7B6C2B9B");
    private static final AttributeModifier WANDER_SPEED_REDUCTION = new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 30;
    private BlockPos deathPosition; 
    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.BOOLEAN);

    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 5 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 8 * 20; 

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            InfestedFox.class, EntityDataSerializers.INT
    );
    
    private static final EntityDataAccessor<Boolean> DATA_IS_POUNCING = SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_POUNCE_TARGET_ID = SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.INT);

    private int attackCooldown = 0;                 
    private LivingEntity pendingAttackTarget;       
    private boolean isPouncing = false;
    public double pounceStartY;
    private double pounceDamageBonus;               

    
    private static final int MAX_POUNCE_HEIGHT = 8; 
    private static final int POUNCE_CHECK_HEIGHT = 3; 
    
    public enum IdleAnimType { IDLE, SIT, LAYING }

    private static final EntityDataAccessor<Integer> DATA_IDLE_ANIM_TYPE =
            SynchedEntityData.defineId(InfestedFox.class, EntityDataSerializers.INT);

    private int idleAnimTimer = 0;           
    private int sitToLayingDelay = 0;        

    public IdleAnimType getIdleAnimType() {
        return IdleAnimType.values()[this.entityData.get(DATA_IDLE_ANIM_TYPE)];
    }

    private void setIdleAnimType(IdleAnimType type) {
        this.entityData.set(DATA_IDLE_ANIM_TYPE, type.ordinal());
    }

    
    private static final UUID LAYING_RANGE_ID = UUID.fromString("B7D8C1E2-3F4A-5B6C-7D8E-9F0A1B2C3D4E");
    private static final AttributeModifier LAYING_RANGE_REDUCTION =
            new AttributeModifier(LAYING_RANGE_ID, "Laying range reduction", -0.9,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
    private int attackCooldownTimer = 0;  

    public InfestedFox(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;

        
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

        
        this.navigation = new GroundPathNavigation(this, level);
    }

    public enum Variant {
        DEFAULT,
        SNOW
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        
        this.goalSelector.addGoal(1, new PounceGoal(this));
        
        this.goalSelector.addGoal(2, new InfestedFoxAttackGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                
                return super.canUse() && getIdleAnimType() == IdleAnimType.IDLE;
            }
        });
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    @Override
    public void tick() {
        super.tick();

        
        if (!this.level().isClientSide) {
            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001; 
            boolean hasTarget = this.getTarget() != null;

            
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

            
            if (this.getTarget() == null &&
                    this.getDeltaMovement().horizontalDistanceSqr() < 0.001 &&
                    !this.isPouncing() &&
                    !this.isFakingDeath()) {

                if (idleAnimTimer <= 0) {
                    
                    if (this.random.nextFloat() < 0.4f) { 
                        setIdleAnimType(IdleAnimType.SIT);
                        sitToLayingDelay = 280 + this.random.nextInt(120); 
                        idleAnimTimer = sitToLayingDelay;
                    } else { 
                        setIdleAnimType(IdleAnimType.IDLE);
                        idleAnimTimer = 100 + this.random.nextInt(100); 
                    }
                } else {
                    idleAnimTimer--;
                    
                    if (getIdleAnimType() == IdleAnimType.SIT && idleAnimTimer == 0) {
                        setIdleAnimType(IdleAnimType.LAYING);
                        idleAnimTimer = Integer.MAX_VALUE; 
                    }
                }
            } else {
                
                if (getIdleAnimType() != IdleAnimType.IDLE) {
                    setIdleAnimType(IdleAnimType.IDLE);
                }
                idleAnimTimer = 0;
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
                                10, 
                                0.3, 0.45, 0.3, 
                                0.0 
                        );

                        
                        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                7, 
                                0.4, 0.3, 0.4, 
                                0.2); 

                        
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

                        
                        cloud.addEffect(new MobEffectInstance(
                                ModEffects.COTH.get(),
                                1200, 
                                0,    
                                false, true
                        ));

                        
                        cloud.addEffect(new MobEffectInstance(
                                MobEffects.POISON,
                                200,  
                                0,    
                                false, true
                        ));

                        serverLevel.addFreshEntity(cloud);
                    }
                }

                
                this.discard();
                return; 
            }
            
            return;
        }

        if (!this.level().isClientSide) {

            
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

                    
                    playAmbientSound();
                }
            }
            
            updateFloating();

            
            if (attackCooldownTimer > 0) {
                attackCooldownTimer--;
            }
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

                this.setRunning(isActuallyMoving && hasTarget);
                this.setWalking(isActuallyMoving && !hasTarget);

                
                if (this.isRunning()) {
                    this.setWalking(false);
                }
            }
        }

        
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            if (getIdleAnimType() == IdleAnimType.LAYING) {
                if (!followRange.hasModifier(LAYING_RANGE_REDUCTION)) {
                    followRange.addPermanentModifier(LAYING_RANGE_REDUCTION);
                }
            } else {
                if (followRange.hasModifier(LAYING_RANGE_REDUCTION)) {
                    followRange.removeModifier(LAYING_RANGE_REDUCTION);
                }
            }
        }
    }

    private double baseSpeed = 0.27D; 
    private double chaseSpeed = 0.38D; 

    
    public InfestedFox.Variant getVariant() {
        
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) {
            
            return InfestedFox.Variant.DEFAULT;
        }

        
        int index = Mth.clamp(variantOrdinal, 0, InfestedFox.Variant.values().length - 1);
        return InfestedFox.Variant.values()[index];
    }

    public void setVariant(InfestedFox.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        if (!this.level().isClientSide) {
        }
    }

    
    public void playAmbientSound() {
        if (this.getTarget() != null) {
            this.playSound(ModSoundEvents.INFESTED_FOX_AGGRO.get());
        } else if (!this.isSilent()) {
            this.playSound(ModSoundEvents.INFESTED_FOX_SCREECH.get());
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        
        return ModSoundEvents.INFESTED_FOX_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return ModSoundEvents.INFESTED_FOX_DEATH.get();
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

        
        boolean result = super.hurt(source, adjustedAmount);

        return result;
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
                    WalkingFoxHead head = ModEntities.WALKING_FOX_HEAD.get().create(this.level());
                    if (head != null) {
                        head.setPos(this.getX(), this.getY(), this.getZ());
                        head.setYRot(this.random.nextFloat() * 360.0F);
                        
                        InfestedFox.Variant foxVariant = this.getVariant();
                        WalkingFoxHead.Variant headVariant = foxVariant == InfestedFox.Variant.SNOW ?
                                WalkingFoxHead.Variant.SNOW : WalkingFoxHead.Variant.DEFAULT;
                        head.setVariant(headVariant);
                        this.level().addFreshEntity(head);
                    }
                }
            }
        }
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedFox> event) {

        
        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        }else if (this.isPouncing() || !this.onGround()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_air"));
        } else if (this.isRunning()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        } else if (this.isWalking()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else if (this.isPouncing) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("attack"));
        } else {
            
            switch (getIdleAnimType()) {
                case IDLE:
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                    break;
                case SIT:
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("sit"));
                    break;
                case LAYING:
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("laying"));
                    break;
            }
        }

        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedFoxSpawnRules(
            EntityType<InfestedFox> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            
            int stage = EvolutionManager.getStageForDimension(level.getLevel());

            
            if (stage < 2 || stage > 5) {
                return false;
            }
        }

        
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                        @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);

        
        if (level.getBiome(this.blockPosition()).is(Tags.Biomes.IS_SNOWY)) {
            this.setVariant(Variant.SNOW);
        }

        return spawnGroupData;
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
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

    
    private static void spawnBuglins(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            
            EntityType<?> buglinType = ModEntities.CURBUG.get(); 
            Entity buglin = buglinType.create(level);

            if (buglin != null) {
                
                double offsetX = random.nextDouble() - 0.5;
                double offsetY = random.nextDouble() * 0.5;
                double offsetZ = random.nextDouble() - 0.5;

                buglin.setPos(
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + offsetY,
                        pos.getZ() + 0.5 + offsetZ
                );

                
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

            pos.set(
                    deathPos.getX() + offsetX,
                    deathPos.getY()+1 + offsetY,
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, InfestedFox.Variant.DEFAULT.ordinal());
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IS_POUNCING, false);
        this.entityData.define(DATA_POUNCE_TARGET_ID, -1);
        this.entityData.define(DATA_IDLE_ANIM_TYPE, IdleAnimType.IDLE.ordinal());
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
    public boolean isPouncing() { return this.entityData.get(DATA_IS_POUNCING); }
    private void setPouncing(boolean pouncing) { this.entityData.set(DATA_IS_POUNCING, pouncing); }

    public int getPounceTargetId() { return this.entityData.get(DATA_POUNCE_TARGET_ID); }
    private void setPounceTargetId(int id) { this.entityData.set(DATA_POUNCE_TARGET_ID, id); }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant", 8)) {
            String variantName = tag.getString("Variant");
            try {
                this.setVariant(InfestedFox.Variant.valueOf(variantName));
            } catch (IllegalArgumentException e) {
                
                this.setVariant(InfestedFox.Variant.DEFAULT);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        
        tag.putString("Variant", this.getVariant().name());
    }

    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case SNOW:
                return new ResourceLocation("epca", "textures/entity/infested_snow_fox.png");
            default:
                return new ResourceLocation("epca", "textures/entity/infested_fox.png");
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

    public class PounceGoal extends Goal {
        private final InfestedFox fox;
        private LivingEntity target;
        private int jumpCooldown = 0;
        private boolean forceMaxHeight = false; 

        public PounceGoal(InfestedFox fox) { this.fox = fox; }

        @Override
        public boolean canUse() {
            target = fox.getTarget();
            if (target == null || !target.isAlive()) return false;

            
            
            BlockPos belowPos = fox.blockPosition().below();
            BlockState belowState = fox.level().getBlockState(belowPos);
            if (!belowState.isCollisionShapeFullBlock(fox.level(), belowPos)) {
                return false;
            }

            
            double dx = fox.getX() - target.getX();
            double dz = fox.getZ() - target.getZ();
            double horizDistSq = dx * dx + dz * dz;
            if (horizDistSq > 3.5 * 3.5) return false; 

            double dy = target.getY() - fox.getY();

            
            if (dy > 1.0 && dy < 7.0) {
                boolean blocked = false;
                for (int y = 1; y <= 8; y++) {
                    BlockPos checkPos = fox.blockPosition().above(y);
                    BlockState state = fox.level().getBlockState(checkPos);
                    if (state.isSolid()) { 
                        blocked = true;
                        break;
                    }
                }
                if (!blocked) {
                    forceMaxHeight = true;
                    return true; 
                }
            }

            
            forceMaxHeight = false; 

            
            for (int y = 1; y <= POUNCE_CHECK_HEIGHT; y++) {
                BlockPos checkPos = fox.blockPosition().above(y);
                BlockState state = fox.level().getBlockState(checkPos);
                if (!state.isAir() && !state.is(ModBlocks.INFESTED_FLOWERING_LEAVES.get())
                        && !state.is(ModBlocks.INFESTED_LEAVES.get())
                        && !state.is(BlockTags.FLOWERS) && !state.is(BlockTags.SAPLINGS)) {
                    return false;
                }
            }

            
            if (dy > 2.0 && horizDistSq < 100.0) {
                if (fox.random.nextFloat() < 0.6f) {
                    return fox.getSensing().hasLineOfSight(target);
                } else {
                    return false;
                }
            }

            
            return fox.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return fox.isPouncing() || (target != null && target.isAlive() &&
                    fox.distanceToSqr(target) < 144.0); 
        }

        @Override
        public void start() {
            fox.setPouncing(true);
            fox.setNoActionTime(0);
            fox.pounceStartY = fox.getY();

            if (target != null) {
                
                double dx = target.getX() - fox.getX();
                double dz = target.getZ() - fox.getZ();
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                if (horizontalDist > 0.1) {
                    double horizontalSpeed = Math.min(horizontalDist * 0.08, 0.6);
                    double vx = (dx / horizontalDist) * horizontalSpeed;
                    double vz = (dz / horizontalDist) * horizontalSpeed;
                    fox.setDeltaMovement(vx, 0, vz);
                }

                
                double height;
                if (forceMaxHeight) {
                    height = 8.0; 
                } else {
                    double dy = target.getY() - fox.getY();
                    if (dy > 1.0) {
                        height = Math.min(dy + 1.5, MAX_POUNCE_HEIGHT);
                    } else {
                        height = Math.min(4.0 + horizontalDist * 0.3, MAX_POUNCE_HEIGHT);
                    }
                }
                double vy = Math.sqrt(2 * 0.08 * height);
                Vec3 currentMotion = fox.getDeltaMovement();
                fox.setDeltaMovement(currentMotion.x, vy, currentMotion.z);
                fox.hasImpulse = true;
                fox.setPounceTargetId(target.getId());
            }
        }

        @Override
        public void stop() {
            fox.setPouncing(false);
            fox.setPounceTargetId(-1);
            fox.pounceDamageBonus = 0;
            forceMaxHeight = false; 
        }

        @Override
        public void tick() {
            if (fox.isPouncing() && fox.onGround()) {
                LivingEntity target = (LivingEntity) fox.level().getEntity(fox.getPounceTargetId());
                if (target != null && target.isAlive()) {
                    double distSq = fox.distanceToSqr(target.getX(), fox.getY(), target.getZ());
                    if (distSq < 1.25 * 1.25) {
                        double fallDistance = fox.pounceStartY - fox.getY();
                        if (fallDistance < 0) fallDistance = 0;
                        float damage = 4.0f + (float) fallDistance;
                        fox.doPounceAttack(target, damage);
                        fox.attackCooldownTimer = 10;
                    }
                }
                fox.setPouncing(false);
                fox.setPounceTargetId(-1);
            }
        }
    }

    public class InfestedFoxAttackGoal extends MeleeAttackGoal {
        private final InfestedFox fox;
        private int attackDelay = 0;

        public InfestedFoxAttackGoal(InfestedFox fox) {
            super(fox, 1.0, true);
            this.fox = fox;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distance) {
            double d0 = this.getAttackReachSqr(target);
            if (distance <= d0 && this.isTimeToAttack() && fox.attackCooldownTimer == 0) {
                if (attackDelay == 0) {
                    attackDelay = 3; 
                    fox.pendingAttackTarget = target;
                    
                    fox.setPouncing(true);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (attackDelay > 0) {
                attackDelay--;
                if (attackDelay == 0 && fox.pendingAttackTarget != null) {
                    fox.doNormalAttack(fox.pendingAttackTarget);
                    fox.pendingAttackTarget = null;
                    

                    fox.setPouncing(false);
                    fox.attackCooldownTimer = 12;
                }
            }
        }
    }

    public void doNormalAttack(LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        float damage = 4.0f;
        if (this.getVariant() == Variant.SNOW) {
            
            target.setTicksFrozen(180);
        }
        target.hurt(damageSources().mobAttack(this), damage);
        this.playSound(ModSoundEvents.INFESTED_FOX_BITE.get());
    }

    public void doPounceAttack(LivingEntity target, float damage) {
        if (target == null || !target.isAlive()) return;
        if (this.getVariant() == Variant.SNOW) {
            target.setTicksFrozen(180);
        }
        target.hurt(damageSources().mobAttack(this), damage);
        this.playSound(ModSoundEvents.INFESTED_FOX_BITE.get());
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        if (fallDistance <= 9.0f) {
            return false; 
        }
        
        float reducedDamage = (fallDistance - 9.0f) * 0.05f;
        if (reducedDamage > 0) {
            this.hurt(source, reducedDamage);
        }
        return false; 
    }
}
