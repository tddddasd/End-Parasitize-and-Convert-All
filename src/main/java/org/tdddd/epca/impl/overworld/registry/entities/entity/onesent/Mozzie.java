package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Mozzie extends PathfinderMob implements GeoEntity, IParasite, IOnesent, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private int attackCooldown = 0; 
    private static final int ATTACK_COOLDOWN_TICKS = 20; 

    
    private int idleTimer = 0;
    private static final int MAX_IDLE_TIME = 1200; 

    
    private boolean isFlying = false;

    
    private static final double ATTACK_RANGE = 2.0D; 

    public Mozzie(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.moveControl = new FlyingMoveControl(this, 20, true); 

        
        this.navigation = new FlyingPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D) 
                .add(Attributes.MOVEMENT_SPEED, 0.38D) 
                .add(Attributes.ATTACK_DAMAGE, 5.0D) 
                .add(Attributes.FOLLOW_RANGE, 32.0D) 
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FLYING_SPEED, 0.28D) 
                .build();
    }

    
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level);
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(true);
        flyingPathNavigation.setCanPassDoors(true);
        return flyingPathNavigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));

        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GnatAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));

        this.goalSelector.addGoal(6, new Mozzie.RandomSoundGoal(this));
    }

    
    @Override
    public void tick() {
        super.tick();

        
        if (!this.level().isClientSide) {
            
            if (attackCooldown > 0) {
                attackCooldown--;
            }

            
            idleTimer++;

            
            if (idleTimer >= MAX_IDLE_TIME) {
                this.discard(); 
                return;
            }

            
            updateFlyingState();

            
            LivingEntity target = this.getTarget();
            if (target != null && this.isAlive()) {
                
                if (this.hasLineOfSight(target)) {
                    applyLimitedLookAt(target);
                }
                
            }

            
            checkGroundBelow();
        }
    }

    
    private void applyLimitedLookAt(LivingEntity target) {
        Vec3 eyePos = this.getEyePosition(1.0F);
        Vec3 targetPos = target.position();

        double dx = targetPos.x - eyePos.x;
        double dz = targetPos.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        
        if (horizontalDist < 0.01) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
        }

        double dy = targetPos.y - eyePos.y;
        double angle = Math.atan2(Math.abs(dy), horizontalDist); 
        double maxAngle = Math.toRadians(45.0);

        
        if (angle > maxAngle) {
            double sign = dy > 0 ? 1 : -1;
            double limitedDy = horizontalDist * Math.tan(maxAngle) * sign;
            
            Vec3 adjustedTarget = new Vec3(targetPos.x, eyePos.y + limitedDy, targetPos.z);
            this.getLookControl().setLookAt(adjustedTarget);
        } else {
            
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    
    private void checkGroundBelow() {
        
        if (this.getTarget() == null && this.isFlying) {
            BlockPos belowPos = this.blockPosition().below();

            
            if (isSolidGround(belowPos)) {
                
                this.isFlying = false;
                this.setNoGravity(false);
            }
        }
    }

    
    private boolean isSolidGround(BlockPos pos) {
        
        return this.level().getBlockState(pos).getBlock() != Blocks.AIR &&
                this.level().getBlockState(pos).getBlock() != Blocks.CAVE_AIR &&
                this.level().getBlockState(pos).getBlock() != Blocks.VOID_AIR;
    }

    
    private void updateFlyingState() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            
            isFlying = true;
            this.setNoGravity(true);
        } else {
            
            boolean hasGround = hasSolidGroundBelow();
            isFlying = !hasGround;
            this.setNoGravity(!hasGround);
        }
    }

    
    private boolean hasSolidGroundBelow() {
        BlockPos belowPos = this.blockPosition().below();
        return isSolidGround(belowPos);
    }

    
    private boolean canFlyAtCurrentPosition() {
        
        if (this.getTarget() != null) {
            BlockPos currentPos = this.blockPosition();

            
            if (!this.level().getBlockState(currentPos).isAir()) {
                return false;
            }

            
            if (this.level().getFluidState(currentPos).getType() != Fluids.EMPTY) {
                return false;
            }
        }

        
        return true;
    }

    
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false; 
    }

    @Override
    public void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
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

    
    @Override
    public boolean doHurtTarget(Entity target) {
        
        if (attackCooldown > 0) {
            return false; 
        }

        
        if (!isInAttackRange(target)) {
            return false;
        }

        boolean attackSuccess = super.doHurtTarget(target);

        if (attackSuccess) {
            
            idleTimer = 0;

            
            attackCooldown = ATTACK_COOLDOWN_TICKS;

            if (target instanceof LivingEntity livingTarget) {
                
                spawnAttackParticles(livingTarget);
                
                this.discard();
            }
        }

        return attackSuccess;
    }

    
    private boolean isInAttackRange(Entity target) {
        
        AABB attackRange = new AABB(
                this.getX() - ATTACK_RANGE,
                this.getY() - ATTACK_RANGE,
                this.getZ() - ATTACK_RANGE,
                this.getX() + ATTACK_RANGE,
                this.getY() + ATTACK_RANGE,
                this.getZ() + ATTACK_RANGE
        );

        return attackRange.intersects(target.getBoundingBox());
    }

    
    private void spawnAttackParticles(LivingEntity target) {
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            BlockPos targetPos = target.blockPosition();
            RandomSource random = this.getRandom();

            
            int particleCount = 5 + random.nextInt(4); 

            for (int i = 0; i < particleCount; i++) {
                double x = targetPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                double y = targetPos.getY() + 0.5 + random.nextDouble() * 2.0;
                double z = targetPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;

                serverLevel.sendParticles(
                        ModParticles.SPLASHI.get(),
                        x, y, z,
                        1, 
                        0.0, 0.0, 0.0, 
                        0.0 
                );
            }
        }
    }

    static class RandomSoundGoal extends Goal {
        private final Mozzie mozzie;
        private int nextSoundTick;

        RandomSoundGoal(Mozzie mozzie) {
            this.mozzie = mozzie;
        }

        

        @Override
        public boolean canUse() {
            return mozzie.isAlive() && !mozzie.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = mozzie.getRandom().nextInt(140) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                playRandomLivingSound();
                this.nextSoundTick = mozzie.getRandom().nextInt(140) + 80;
            }
        }

        private void playRandomLivingSound() {
            mozzie.playSound(ModSoundEvents.MOZZIE_IDLE.get(), 1.0F, 1.0F);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Mozzie>(this, "controller", 4, this::predicate));
    }

    
    private PlayState predicate(AnimationState<Mozzie> event) {
        AnimationController<Mozzie> controller = event.getController();
        Mozzie gnat = event.getAnimatable();

        
        if (gnat.isFlying()) {
            
            controller.setAnimation(RawAnimation.begin().thenLoop("fly"));
        } else if (gnat.isMoving()) {
            
            controller.setAnimation(RawAnimation.begin().thenLoop("fly"));
        } else {
            
            controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    
    static class GnatAttackGoal extends MeleeAttackGoal {
        public GnatAttackGoal(Mozzie gnat, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(gnat, speedModifier, followingTargetEvenIfNotSeen);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && ((Mozzie)this.mob).canFlyAtCurrentPosition();
        }

        @Override
        protected double getAttackReachSqr(LivingEntity attackTarget) {
            
            return 4.0D; 
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.MOZZIE_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        
        return ModSoundEvents.MOZZIE_HURT.get();
    }

    public static boolean checkGnatSpawnRules(
            EntityType<Mozzie> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source); 
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        
        if (!this.level().isClientSide) {
            
            IParasite.super.onKillEntity(killedEntity);
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

    
    public boolean isFlying() {
        return this.isFlying;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isFlying) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            } else {
                this.moveRelative(this.getSpeed(), travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
            }
        } else {
            super.travel(travelVector);
        }
    }
}