package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import software.bernie.geckolib.util.GeckoLibUtil;

public class InfestedSlimeSize0 extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int currentSize = 0; 
    private float targetSquish;
    private float squish;
    private float oSquish;
    private boolean wasOnGround;
    private int attackCooldown = 0; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedSlimeSize0.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedSlimeSize0.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 10;
    private BlockPos deathPosition;

    
    private static final SizeAttributes SIZE_0_ATTRIBUTES = new SizeAttributes(
            2.0D,   
            0.0D,   
            0.2D,   
            0.4F,   
            1.2F,   
            10, 30, 
            1.0F    
    );

    
    private static class SizeAttributes {
        public final double maxHealth;
        public final double attackDamage;
        public final double movementSpeed;
        public final float jumpPower;
        public final float horizontalMultiplier;
        public final int minJumpDelay;
        public final int maxJumpDelay;
        public final float scale;

        public SizeAttributes(double maxHealth, double attackDamage, double movementSpeed,
                              float jumpPower, float horizontalMultiplier,
                              int minJumpDelay, int maxJumpDelay, float scale) {
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.jumpPower = jumpPower;
            this.horizontalMultiplier = horizontalMultiplier;
            this.minJumpDelay = minJumpDelay;
            this.maxJumpDelay = maxJumpDelay;
            this.scale = scale;
        }
    }

    
    private SizeAttributes getCurrentSizeAttributes() {
        return SIZE_0_ATTRIBUTES;
    }

    public int tryApplyCothEffect(Mob mob, int currentCooldown) {
        return 0;
    }

    public InfestedSlimeSize0(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 4; 
        
        this.navigation = new GroundPathNavigation(this, level);
        
        this.moveControl = new SlimeMoveControl(this);

        
        this.currentSize = 0;

        
        this.wasOnGround = this.onGround();
        
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SIZE_0_ATTRIBUTES.maxHealth)
                .add(Attributes.MOVEMENT_SPEED, SIZE_0_ATTRIBUTES.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, 16.0D); 
    }

    @Override
    public void tick() {

        
        if (isFakingDeath()) {
            super.tick();
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                
                if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                    
                    int particleCount = 8; 
                    for (int i = 0; i < particleCount; i++) {
                        double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                        double d1 = this.getY() + this.random.nextDouble() * this.getBbHeight();
                        double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                        serverLevel.sendParticles(
                                new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get().getDefaultInstance()),
                                d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.1D
                        );
                    }

                    
                    DamageSource damageSource = this.getLastDamageSource();
                    if (damageSource == null) {
                        
                        damageSource = this.damageSources().generic();
                    }
                    this.dropFromLootTable(damageSource, false);
                }

                
                this.discard();
                return; 
            }
            
            return;
        }

        
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        
        this.oSquish = this.squish;

        boolean isOnGround = this.onGround();

        
        if (isOnGround && !this.wasOnGround) {
            
            this.targetSquish = -0.5F;
        } else if (!isOnGround && this.wasOnGround) {
            
            this.targetSquish = 1.0F;
        }

        this.targetSquish *= 0.6F; 
        this.squish += (this.targetSquish - this.squish) * 0.5F; 

        super.tick();

        
        isOnGround = this.onGround();

        
        if (isOnGround && !this.wasOnGround) {
            
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                int particleCount = 4; 
                for (int i = 0; i < particleCount; i++) {
                    double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                    double d1 = this.getY();
                    double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                    serverLevel.sendParticles(
                            new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get().getDefaultInstance()),
                            d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.0D
                    );
                }
            }
            
            this.playSound(getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
        }
        
        this.wasOnGround = isOnGround;
        
        updateFloating();
    }

    
    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
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
        fakeDeathTimer = 10; 
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

        
        if (this.isOnFire()) {
            super.die(source);
            return;
        }

        
        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < 1.0f) {
            triggerFakeDeath(source);
        } else {
            
            super.die(source);
        }
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (isFakingDeath() || isInvulnerable()) {
            return false;
        }

        
        if (source.getEntity() instanceof LivingEntity attacker) {
            
            if (shouldIgnoreDamageFrom(attacker)) {
                return false; 
            }
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        
        float adjustedAmount = ((IParasite) this).onHurt(source, amount);

        
        boolean result = super.hurt(source, adjustedAmount);

        return result;
    }

    
    static class SlimeMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final InfestedSlimeSize0 slime;
        private boolean isAggressive;

        public SlimeMoveControl(InfestedSlimeSize0 slime) {
            super(slime);
            this.slime = slime;
            this.yRot = 180.0F * slime.getYRot() / (float)Math.PI;
        }

        public void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.isAggressive = aggressive;
        }

        public void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
                return;
            }

            this.operation = MoveControl.Operation.WAIT;

            if (this.mob.onGround()) {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = this.slime.getJumpDelay();

                    if (this.isAggressive) {
                        this.jumpDelay /= 3;
                    }

                    ((InfestedSlimeSize0)this.mob).getJumpControl().jump();

                    
                    this.slime.playSound(this.slime.getJumpSound(), this.slime.getSoundVolume(),
                            ((this.slime.getRandom().nextFloat() - this.slime.getRandom().nextFloat()) * 0.2F + 1.0F) * 0.8F);
                } else {
                    this.slime.xxa = 0.0F;
                    this.slime.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            } else {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }

    protected int getJumpDelay() {
        SizeAttributes attributes = getCurrentSizeAttributes();
        return this.random.nextInt(attributes.maxJumpDelay - attributes.minJumpDelay + 1) + attributes.minJumpDelay;
    }

    @Override
    protected void jumpFromGround() {
        Vec3 vec3 = this.getDeltaMovement();
        SizeAttributes attributes = getCurrentSizeAttributes();

        
        this.setDeltaMovement(vec3.x * attributes.horizontalMultiplier, attributes.jumpPower, vec3.z * attributes.horizontalMultiplier);
        this.hasImpulse = true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        
        if (!this.onGround()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.x * 0.95D, vec3.y, vec3.z * 0.95D);
        }

        
        if (this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() < 0.01D) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        
        if (!this.level().isClientSide && this.attackCooldown <= 0) {
            this.checkContactDamage();
        }
    }

    
    private void checkContactDamage() {
        
        AABB collisionBox = this.getBoundingBox().inflate(0.0D); 
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, collisionBox)) {
            
            if (target == this || IParasite.isParasiteByTagOrInterface(target)) {
                continue;
            }

            
            if (this.getBoundingBox().intersects(target.getBoundingBox())) {
                
                DamageSource magicDamage = this.damageSources().magic();
                boolean hurt = target.hurt(magicDamage, 2.0F);

                if (hurt) {
                    MobEffectInstance solidifyEffect = new MobEffectInstance(
                            ModEffects.SOLIDIFY.get(), 
                            40, 
                            0,  
                            false, 
                            true  
                    );
                    target.addEffect(solidifyEffect);

                    MobEffectInstance fearEffect = new MobEffectInstance(
                            ModEffects.FEAR.get(), 
                            400, 
                            0,  
                            false, 
                            true  
                    );
                    target.addEffect(fearEffect);


                    this.attackCooldown = 10;

                    
                    this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, this.getVoicePitch());

                    break; 
                }
            }
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    protected SoundEvent getJumpSound() {
        return SoundEvents.SLIME_JUMP;
    }

    protected SoundEvent getSquishSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F; 
    }

    @Override
    public float getVoicePitch() {
        return 1.4F; 
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        SizeAttributes attributes = getCurrentSizeAttributes();
        return super.getDimensions(pose).scale(attributes.scale);
    }

    public int getCurrentSize() {
        return currentSize;
    }

    
    public float getSquish() {
        return this.squish;
    }

    public float getoSquish() {
        return this.oSquish;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::playState));
        controllers.add(new AnimationController<>(this, "attack_controller", 4, this::attackPredicate));
    }

    private PlayState playState(AnimationState<InfestedSlimeSize0> event) {
        String sizeSuffix = getSizeSuffix();

        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("dead" + sizeSuffix));
        } else if (!this.onGround() || this.getDeltaMovement().y > 0.0D) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("jump" + sizeSuffix));
        } else if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle" + sizeSuffix));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle" + sizeSuffix));
        }

        return PlayState.CONTINUE;
    }

    private PlayState attackPredicate(AnimationState<InfestedSlimeSize0> event) {
        if (this.swinging) {
            String sizeSuffix = getSizeSuffix();
            
            return PlayState.CONTINUE;
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private String getSizeSuffix() {
        return "_size0"; 
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Size", this.currentSize);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        
        this.currentSize = 0;
    }

    public void setSize(int size) {
        
    }

    
    public static InfestedSlimeSize0 create(Level level) {
        return new InfestedSlimeSize0((EntityType<? extends PathfinderMob>) ModEntities.INFESTED_SLIME_SIZE0.get(), level);
    }

    
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
    }

    
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
    }

    public static boolean checkInfestedSlimeSize0SpawnRules(
            EntityType<InfestedSlimeSize0> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            
            int stage = EvolutionManager.getStageForDimension(level.getLevel());

            
            if (stage < 2 || stage > 4) {
                return false;
            }
        }

        
        return level.getMaxLocalRawBrightness(pos) < 8;
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
    public void onKillEntity(LivingEntity killedEntity) {
        
        if (!this.level().isClientSide) {
            
            IParasite.super.onKillEntity(killedEntity);
        }
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