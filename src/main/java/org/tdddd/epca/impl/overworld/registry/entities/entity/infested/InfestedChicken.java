package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.BiomassEgg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
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

import java.util.UUID;

public class InfestedChicken extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    public int tryApplyCothEffect(Mob mob, int currentCooldown) {
        return 0;
    }
    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(InfestedChicken.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(InfestedChicken.class, EntityDataSerializers.BOOLEAN);
    
    private static final EntityDataAccessor<Boolean> DATA_IS_FALLING = SynchedEntityData.defineId(InfestedChicken.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_SHOOTING = SynchedEntityData.defineId(InfestedChicken.class, EntityDataSerializers.BOOLEAN);

    private static final UUID WANDER_SPEED_ID = UUID.fromString("A2766B59-7066-4402-AD81-0E3B7B6C2B9B");
    private static final AttributeModifier WANDER_SPEED_REDUCTION = new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 12 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 16 * 20; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedChicken.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 20;
    private BlockPos deathPosition; 

    
    private int shootCooldown = 0; 
    private int meleeCooldown = 0; 
    private static final int NORMAL_SHOOT_COOLDOWN = 20; 
    private static final int BUFFED_SHOOT_COOLDOWN = 10; 
    private static final int MELEE_COOLDOWN = 20; 
    private float bodyRotationX = 0.0F; 
    
    private int shootAnimationTimer = 0;
    
    private static final double SAFE_DISTANCE = 8.0D; 
    private static final double MAX_CHASE_DISTANCE = 16.0D; 
    private int retreatCooldown = 0; 
    private static final int RETREAT_COOLDOWN = 5; 
    private boolean isRetreating = false; 
    private Vec3 retreatDirection = Vec3.ZERO; 

    public InfestedChicken(EntityType<? extends PathfinderMob> type, Level level) {
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
        }
    }

    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.21D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D) 
                .add(Attributes.FOLLOW_RANGE, 16.0D) 
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ARMOR, 1.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));

        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
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
            boolean isFalling = this.getDeltaMovement().y < -0.05 && !this.onGround(); 

            
            this.setRunning(isMoving && hasTarget && !isRetreating);
            this.setWalking(isMoving && !hasTarget);
            this.setFalling(isFalling);

            
            if (isMoving && hasTarget && !isRetreating) {
                this.setWalking(false);
            } else if (isMoving && !hasTarget) {
                this.setRunning(false);
            }

            
            if (!isMoving) {
                this.setRunning(false);
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

                        
                        spawnChickenHeads(serverLevel, deathPos, 2);
                    }
                }

                
                this.discard();
                return; 
            }
            
            return;
        }

        
        if (retreatCooldown > 0) {
            retreatCooldown--;
        }

        
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();

            if (target != null) {
                
                if (shootCooldown > 0) shootCooldown--;
                if (meleeCooldown > 0) meleeCooldown--;

                
                double distance = this.distanceTo(target);

                
                if (distance < SAFE_DISTANCE) {
                    
                    if (retreatCooldown <= 0) {
                        startRetreating(target);
                        retreatCooldown = RETREAT_COOLDOWN;
                    }

                    
                    if (shootCooldown <= 0 && this.hasLineOfSight(target)) {
                        shootBiomassEgg(target);

                        
                        if (this.hasEffect(ModEffects.RAGE.get())) {
                            shootCooldown = BUFFED_SHOOT_COOLDOWN;
                        } else {
                            shootCooldown = NORMAL_SHOOT_COOLDOWN;
                        }
                    }

                    
                    if (isRetreating) {
                        updateRetreatMovement();
                    }
                } else {
                    
                    stopRetreating();

                    
                    
                    if (distance < 1.25 && meleeCooldown <= 0) {
                        performMeleeAttack(target);
                        meleeCooldown = MELEE_COOLDOWN;
                    }

                    
                    if (shootCooldown <= 0 && this.hasLineOfSight(target)) {
                        shootBiomassEgg(target);

                        
                        if (this.hasEffect(ModEffects.RAGE.get())) {
                            shootCooldown = BUFFED_SHOOT_COOLDOWN;
                        } else {
                            shootCooldown = NORMAL_SHOOT_COOLDOWN;
                        }
                    }
                }

                
                calculateBodyRotation(target);

                
                this.lookControl.setLookAt(target, 30.0F, 30.0F);

                
                if (isRetreating && distance >= SAFE_DISTANCE + 2.0D) {
                    stopRetreating();
                }
            } else {
                
                stopRetreating();
            }
        }

        
        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null) {
            boolean hasTarget = this.getTarget() != null;

            
            movementAttribute.removeModifier(WANDER_SPEED_ID);

            if (isRetreating) {
                
                movementAttribute.setBaseValue(baseSpeed * 0.7D);
            } else if (hasTarget) {
                
                movementAttribute.setBaseValue(chaseSpeed);
            } else {
                
                movementAttribute.setBaseValue(baseSpeed);
                movementAttribute.addTransientModifier(WANDER_SPEED_REDUCTION);
            }

            
            if (!this.level().isClientSide) {
                double horizontalMovement = this.getDeltaMovement().horizontalDistance();
                boolean isActuallyMoving = horizontalMovement > 0.01; 
                boolean isFalling = this.getDeltaMovement().y < -0.05;

                
                if (isRetreating) {
                    this.setWalking(true);
                    this.setRunning(false);
                } else {
                    this.setRunning(isActuallyMoving && hasTarget);
                    this.setWalking(isActuallyMoving && !hasTarget);
                }

                this.setFalling(isFalling);

                
                if (this.isRunning()) {
                    this.setWalking(false);
                }
            }
        }

        if (!this.level().isClientSide) {

            
            if (this.getTarget() == null && !isRetreating) {
                if (--this.ambientSoundTime <= 0) {
                    
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

                    
                    playAmbientSound();
                }
            }
            
            updateFloating();
        }
    }

    
    private void startRetreating(LivingEntity target) {
        if (target == null) return;

        isRetreating = true;

        
        Vec3 toTarget = target.position().subtract(this.position());
        retreatDirection = toTarget.normalize().scale(-0.6); 

        
        this.getNavigation().stop();
    }

    
    private void updateRetreatMovement() {
        if (!isRetreating || retreatDirection.equals(Vec3.ZERO)) return;

        
        Vec3 movement = retreatDirection.scale(0.20D); 

        
        BlockPos checkPos = this.blockPosition().offset(
                (int)Math.signum(movement.x),
                0,
                (int)Math.signum(movement.z)
        );

        
        if (this.level().getBlockState(checkPos).isSolid()) {
            
            Vec3 sideDirection = retreatDirection.yRot((float)Math.PI / 2); 
            if (this.random.nextBoolean()) {
                sideDirection = sideDirection.yRot((float)Math.PI); 
            }
            movement = sideDirection.scale(0.20D);
        }

        
        this.setDeltaMovement(
                movement.x,
                this.getDeltaMovement().y,
                movement.z
        );

        
        LivingEntity target = this.getTarget();
        if (target != null && this.distanceTo(target) > MAX_CHASE_DISTANCE) {
            stopRetreating();
        }
    }

    
    private void stopRetreating() {
        if (isRetreating) {
            isRetreating = false;
            retreatDirection = Vec3.ZERO;

            
            this.getNavigation().recomputePath();
        }
    }

    
    private void shootBiomassEgg(LivingEntity target) {
        if (!this.level().isClientSide) {
            
            this.triggerAnim("shoot_controller", "shoot");
            
            this.setShooting(true);

            
            this.playSound(SoundEvents.EGG_THROW, 1.0F, 0.8F);

            
            Vec3 eyePos = this.getEyePosition();
            Vec3 targetPos = target.getEyePosition();
            Vec3 direction = targetPos.subtract(eyePos).normalize();

            
            double distance = eyePos.distanceTo(targetPos);
            double heightDifference = targetPos.y - eyePos.y;
            double pitchAngle = -Math.asin(heightDifference / distance) * (180.0 / Math.PI);

            
            pitchAngle = Mth.clamp(pitchAngle, -30.0, 72.5);

            
            BiomassEgg egg = new BiomassEgg(this.level(), this);

            
            Vec3 shootPos = eyePos.add(direction.scale(0.5));
            egg.setPos(shootPos.x, shootPos.y - 0.1, shootPos.z);

            
            double speed = 1.0; 
            double gravity = 0.05; 

            
            double requiredY = (targetPos.y - shootPos.y) + (gravity * distance * distance) / (2 * speed * speed);
            double requiredYVelocity = requiredY / (distance / speed);

            
            double maxYVelocity = speed * Math.tan(72.5 * Math.PI / 180.0);
            requiredYVelocity = Mth.clamp(requiredYVelocity, -speed * Math.tan(30 * Math.PI / 180.0), maxYVelocity);

            
            double horizontalDistance = Math.sqrt(distance * distance - heightDifference * heightDifference);
            double horizontalSpeed = Math.sqrt(speed * speed - requiredYVelocity * requiredYVelocity);

            Vec3 horizontalDirection = new Vec3(targetPos.x - shootPos.x, 0, targetPos.z - shootPos.z).normalize();
            Vec3 velocity = new Vec3(
                    horizontalDirection.x * horizontalSpeed,
                    requiredYVelocity,
                    horizontalDirection.z * horizontalSpeed
            );

            egg.shoot(velocity.x, velocity.y, velocity.z, 1.0F, 1.0F);

            
            this.level().addFreshEntity(egg);

            
            this.level().getServer().tell(new TickTask(
                    this.level().getServer().getTickCount() + 8,
                    () -> this.setShooting(false)
            ));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DATA_IS_SHOOTING)) {
            if (this.level().isClientSide && this.isShooting()) {
                this.shootAnimationTimer = 8;
            }
        }
    }

    
    private void performMeleeAttack(LivingEntity target) {
        
        if (target.hurt(this.damageSources().mobAttack(this), 2.0F)) {
            
            this.playSound(SoundEvents.CHICKEN_HURT, 0.85F, 0.7F);
        }
    }



    
    private void calculateBodyRotation(LivingEntity target) {
        if (target != null) {
            Vec3 eyePos = this.getEyePosition();
            Vec3 targetPos = target.getEyePosition();
            double distance = eyePos.distanceTo(targetPos);
            double heightDifference = targetPos.y - eyePos.y;

            
            double pitchAngle = -Math.asin(heightDifference / distance) * (180.0 / Math.PI);

            
            pitchAngle = Mth.clamp(pitchAngle, -30.0, 72.5);

            
            this.bodyRotationX = (float) Math.toRadians(pitchAngle);
        } else {
            this.bodyRotationX = 0.0F;
        }
    }

    
    public float getBodyRotationX() {
        return this.bodyRotationX;
    }

    
    private static void spawnChickenHeads(ServerLevel level, BlockPos pos, int count) {
        for (int i = 0; i < count; i++) {
            
            EntityType<?> chickenHeadType = ModEntities.WALKING_CHICKEN_HEAD.get();
            Entity chickenHead = chickenHeadType.create(level);

            if (chickenHead != null) {
                
                double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
                double offsetY = level.random.nextDouble() * 0.5;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

                chickenHead.setPos(
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + offsetY,
                        pos.getZ() + 0.5 + offsetZ
                );

                
                if (chickenHead instanceof LivingEntity) {
                    ((LivingEntity) chickenHead).setDeltaMovement(
                            (level.random.nextDouble() - 0.5) * 0.1,
                            level.random.nextDouble() * 0.1,
                            (level.random.nextDouble() - 0.5) * 0.1
                    );
                }

                
                level.addFreshEntity(chickenHead);
            }
        }
    }

    
    public void playAmbientSound() {
        if (!this.isSilent() && this.random.nextInt(3) == 0) {
            
            this.playSound(SoundEvents.CHICKEN_AMBIENT, 0.85F, 0.7F);
        }
    }

    

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        
        SoundEvent soundevent = this.getHurtSound(source);
        if (soundevent != null) {
            this.playSound(soundevent, 0.85F, 0.7F); 
        }

        SoundEvent deathSound = this.getDeathSound();
        if (deathSound != null) {
            this.playSound(deathSound, 0.85F, 0.7F); 
        }
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source.is(DamageTypes.FALL)) {
            return false; 
        }

        
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
        
        controllers.add(new AnimationController<>(this, "main_controller", 4, this::mainAnimationPredicate));

        
        AnimationController<InfestedChicken> shootController = new AnimationController<>(this, "shoot_controller", 3, state -> PlayState.STOP);
        shootController.triggerableAnim("shoot", RawAnimation.begin().thenPlay("shoot"));
        controllers.add(shootController);
    }

    
    private PlayState mainAnimationPredicate(AnimationState<InfestedChicken> event) {
        
        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
            return PlayState.CONTINUE;
        }

        
        if (this.isFalling() && !this.onGround()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("fall"));
            return PlayState.CONTINUE;
        }

        
        if (this.isRunning() || this.isRetreating) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        } else if (this.isWalking()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }

        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedChickenSpawnRules(
            EntityType<InfestedChicken> entityType,
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

    
    public double getGravity() {
        
        return 0.08D; 
    }

    
    private double baseSpeed = 0.21D; 
    private double chaseSpeed = 0.32D; 

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
                    if (this.level() instanceof ServerLevel serverLevel) {
                        spawnChickenHeads(serverLevel, this.blockPosition(), 1);
                    }
                }
            }
        }
    }

    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedChicken.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IS_FALLING, false);
        this.entityData.define(DATA_IS_SHOOTING, false);
    }

    
    public boolean isRunning() {
        return this.entityData.get(DATA_IS_RUNNING);
    }

    public boolean isWalking() {
        return this.entityData.get(DATA_IS_WALKING);
    }

    public boolean isFalling() {
        return this.entityData.get(DATA_IS_FALLING);
    }

    public boolean isShooting() {
        return this.entityData.get(DATA_IS_SHOOTING);
    }

    
    private void setRunning(boolean running) {
        this.entityData.set(DATA_IS_RUNNING, running);
    }

    private void setWalking(boolean walking) {
        this.entityData.set(DATA_IS_WALKING, walking);
    }

    private void setFalling(boolean falling) {
        this.entityData.set(DATA_IS_FALLING, falling);
    }

    private void setShooting(boolean shooting) {
        this.entityData.set(DATA_IS_SHOOTING, shooting);
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
        fakeDeathTimer = 20;
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
}