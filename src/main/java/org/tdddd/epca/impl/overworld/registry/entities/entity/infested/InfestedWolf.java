package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;

import org.tdddd.epca.impl.client.entity.IOverlayRenderable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class InfestedWolf extends PathfinderMob implements IOverlayRenderable, GeoEntity, IParasite, IInfested, Enemy , IHeadRotatable {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID ownerUUID;
    public int tryApplyCothEffect(Mob mob, int currentCooldown) {
        return 0;
    }

    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.BOOLEAN);
    private static final UUID WANDER_SPEED_ID = UUID.fromString("A5766B59-7066-4402-AD81-0E3B7B6C2B9B");
    private static final AttributeModifier WANDER_SPEED_REDUCTION = new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);

    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 12 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 16 * 20; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 30;
    private BlockPos deathPosition; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_HOWLING = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HOWL_TARGET_ID = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.INT);
    private int howlCooldown = 0; 
    private static final int HOWL_COOLDOWN_TICKS = 13 * 20; 
    private int howlEffectTimer = 0; 
    private static final int HOWL_EFFECT_DURATION = 3 * 20; 
    private static final int HOWL_TARGETING_INTERVAL = 20; 
    private int howlTargetingTimer = 0; 

    
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.INT);
    private static final String COLLAR_COLOR_TAG = "CollarColor";
    private static final int NO_COLLAR_COLOR = -1; 

    
    private double baseSpeed = 0.30D; 
    private double chaseSpeed = 0.34D; 

    public enum IdleAnimType { IDLE, SIT }
    private static final EntityDataAccessor<Integer> DATA_IDLE_ANIM_TYPE = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.INT);
    private int idleAnimTimer = 0;
    public IdleAnimType getIdleAnimType() { return IdleAnimType.values()[this.entityData.get(DATA_IDLE_ANIM_TYPE)]; }
    private void setIdleAnimType(IdleAnimType type) { this.entityData.set(DATA_IDLE_ANIM_TYPE, type.ordinal()); }

    public InfestedWolf(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        this.entityData.define(DATA_IDLE_ANIM_TYPE, IdleAnimType.IDLE.ordinal());
        this.setMaxUpStep(0.5F);
        
        this.navigation = new GroundPathNavigation(this, level);

        
        this.howlCooldown = this.random.nextInt(HOWL_COOLDOWN_TICKS / 2, HOWL_COOLDOWN_TICKS);
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

            
            updateHowlLogic();
        }
    }

    
    private void updateHowlLogic() {
        
        if (howlCooldown > 0) {
            howlCooldown--;
        }

        
        if (isHowling()) {
            
            this.getNavigation().stop();

            
            if (howlEffectTimer <= 0) {
                setHowling(false);
                this.entityData.set(DATA_HOWL_TARGET_ID, -1);
                return;
            }

            howlEffectTimer--;

            
            if (howlTargetingTimer <= 0) {
                howlTargetingTimer = HOWL_TARGETING_INTERVAL;
                updateParasiteTargeting();
            } else {
                howlTargetingTimer--;
            }

            return;
        }

        
        if (howlCooldown <= 0 && this.getTarget() == null) {
            LivingEntity target = findHowlTarget();
            if (target != null) {
                triggerHowl(target);
            }
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.level().isClientSide) {
            super.setTarget(target);
            return;
        }

        if (target != null && isOwner(target)) {
            if (getIdleAnimType() == IdleAnimType.SIT) {
                super.setTarget(null);
                return;
            }

            double reducedRange = 1.6D;
            if (this.distanceToSqr(target) > reducedRange * reducedRange) {
                super.setTarget(null);
                return;
            }
        }

        // 其他情况正常设置目标
        super.setTarget(target);
    }
    
    private LivingEntity findHowlTarget() {
        if (this.level().isClientSide) return null;

        
        List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(32.0),
                entity -> {
                    if (entity == null || !entity.isAlive()) return false;
                    if (IParasite.isParasiteByTagOrInterface(entity)) return false;
                    if (entity instanceof Creeper) return false;
                    if (entity instanceof Player player) {
                        return !player.isCreative() && !player.isSpectator();
                    }
                    return true;
                }
        );

        if (entities.isEmpty()) return null;

        
        return entities.stream()
                .min((e1, e2) ->
                        Float.compare(
                                (float) this.distanceToSqr(e1),
                                (float) this.distanceToSqr(e2)
                        ))
                .orElse(null);
    }

    
    private void triggerHowl(LivingEntity target) {
        setHowling(true);
        howlCooldown = HOWL_COOLDOWN_TICKS;
        howlEffectTimer = HOWL_EFFECT_DURATION;
        howlTargetingTimer = 0; 

        
        this.entityData.set(DATA_HOWL_TARGET_ID, target.getId());
        
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.playSound(ModSoundEvents.INFESTED_WOLF_HOWL.get());
        
        updateParasiteTargeting();
    }

    
    private void updateParasiteTargeting() {
        if (this.level().isClientSide) {
            return;
        }

        
        int targetId = this.entityData.get(DATA_HOWL_TARGET_ID);
        if (targetId == -1) return;

        Entity targetEntity = this.level().getEntity(targetId);
        if (!(targetEntity instanceof LivingEntity) || !targetEntity.isAlive()) {
            return;
        }

        LivingEntity howlTarget = (LivingEntity) targetEntity;

        
        List<LivingEntity> parasites = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(32.0),
                entity -> entity != null &&
                        entity.isAlive() &&
                        entity != this &&
                        IParasite.isParasiteByTagOrInterface(entity) &&
                        !(entity instanceof Player)
        );

        for (LivingEntity parasite : parasites) {
            
            if (parasite instanceof Mob mob) {
                LivingEntity currentTarget = mob.getTarget();
                if (currentTarget == null || currentTarget != howlTarget) {
                    mob.setTarget(howlTarget);
                    
                    mob.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,
                            100, 
                            0,
                            false, false
                    ));
                }
            }
        }
    }

    
    public boolean isHowling() {
        return this.entityData.get(DATA_IS_HOWLING);
    }

    private void setHowling(boolean howling) {
        this.entityData.set(DATA_IS_HOWLING, howling);
    }

    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D) 
                .add(Attributes.FOLLOW_RANGE, 16.0D) 
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.ARMOR, 2.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
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
            
            if (!isHowling()) {
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
            }
        }

        
        if (this.getTarget() == null &&
                this.getDeltaMovement().horizontalDistanceSqr() < 0.001 &&
                !this.isHowling() &&
                !this.isFakingDeath()) {

            if (idleAnimTimer <= 0) {
                
                if (this.random.nextFloat() < 0.4f) { 
                    setIdleAnimType(IdleAnimType.SIT);
                    idleAnimTimer = 200 + this.random.nextInt(200); 
                } else { 
                    setIdleAnimType(IdleAnimType.IDLE);
                    idleAnimTimer = 100 + this.random.nextInt(100); 
                }
            } else {
                idleAnimTimer--;
            }
        } else {
            
            if (getIdleAnimType() != IdleAnimType.IDLE) {
                setIdleAnimType(IdleAnimType.IDLE);
            }
            idleAnimTimer = 0;
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
                                1,    
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

        
        if (!isHowling()) {
            
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
        }

        if (!this.level().isClientSide) {
            
            if (this.getTarget() == null && !isHowling()) {
                if (--this.ambientSoundTime <= 0) {
                    
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

                    
                    playAmbientSound();
                }
            }
            
            updateFloating();
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

    
    public void playAmbientSound() {
        if (this.getTarget() != null) {
            this.playSound(ModSoundEvents.INFESTED_WOLF_GROWL.get());
        } else if (!this.isSilent()) {
            this.playSound(ModSoundEvents.INFESTED_WOLF_WHINE.get());
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_WOLF_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INFESTED_WOLF_DEATH.get();
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

        
        if (isHowling()) {
            setHowling(false);
            howlEffectTimer = 0;
            this.entityData.set(DATA_HOWL_TARGET_ID, -1);
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        
        float adjustedAmount = ((IParasite) this).onHurt(source, amount);

        
        boolean result = super.hurt(source, adjustedAmount);

        return result;
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);

            
            this.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    60,     
                    0,      
                    false, false, true
            ));
        }
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::playState));
    }

    private PlayState playState(AnimationState<InfestedWolf> event) {
        
        boolean isMoving = event.isMoving();

        
        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        }
        
        else if (this.isHowling()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("howl"));
        }
        else if (this.isRunning()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        } else if (this.isWalking()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else if (getIdleAnimType() == IdleAnimType.SIT) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("sit"));
        } else {
            
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }

        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedWolfSpawnRules(
            EntityType<InfestedWolf> entityType,
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
                    WalkingWolfHead head = ModEntities.WALKING_WOLF_HEAD.get().create(this.level());
                    head.setPos(this.getX(), this.getY(), this.getZ());
                    head.setYRot(this.random.nextFloat() * 360.0F);
                    this.level().addFreshEntity(head);
                }
            }
        }
    }

    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedWolf.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        
        this.entityData.define(DATA_IS_HOWLING, false);
        this.entityData.define(DATA_HOWL_TARGET_ID, -1);
        
        this.entityData.define(DATA_COLLAR_COLOR, NO_COLLAR_COLOR);
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

    
    public boolean hasCollar() {
        return this.entityData.get(DATA_COLLAR_COLOR) != NO_COLLAR_COLOR;
    }

    public int getCollarColor() {
        return this.entityData.get(DATA_COLLAR_COLOR);
    }

    public void setCollarColor(int color) {
        this.entityData.set(DATA_COLLAR_COLOR, color);
    }

    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(COLLAR_COLOR_TAG, Tag.TAG_INT)) {
            int collarColor = tag.getInt(COLLAR_COLOR_TAG);
            this.setCollarColor(collarColor);
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasCollar()) {
            tag.putInt(COLLAR_COLOR_TAG, getCollarColor());
        }
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
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
    private static final net.minecraft.resources.ResourceLocation COLLAR_OVERLAY =
            new net.minecraft.resources.ResourceLocation("epca", "textures/entity/infested_wolf_collar.png");

    private static final float[][] COLLAR_COLORS = {
            {0.85F, 0.85F, 0.85F}, {0.3F, 0.3F, 0.3F}, {0.6F, 0.2F, 0.7F}, {0.4F, 0.5F, 0.8F},
            {0.9F, 0.7F, 0.2F}, {0.5F, 0.7F, 0.3F}, {0.95F, 0.5F, 0.65F}, {0.25F, 0.25F, 0.25F},
            {0.55F, 0.55F, 0.55F}, {0.2F, 0.4F, 0.6F}, {0.45F, 0.2F, 0.6F}, {0.1F, 0.1F, 0.6F},
            {0.4F, 0.3F, 0.2F}, {0.3F, 0.5F, 0.2F}, {0.6F, 0.2F, 0.2F}, {0.05F, 0.05F, 0.05F}
    };

    @Override
    public net.minecraft.resources.ResourceLocation getOverlayTexture() {
        return hasCollar() ? COLLAR_OVERLAY : null;
    }

    @Override
    public float[] getOverlayColor() {
        int c = getCollarColor();
        if (c < 0 || c >= COLLAR_COLORS.length) c = 0;
        return COLLAR_COLORS[c];
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public boolean isOwner(@Nullable LivingEntity entity) {
        return this.ownerUUID != null && entity != null && entity.getUUID().equals(this.ownerUUID);
    }
}
