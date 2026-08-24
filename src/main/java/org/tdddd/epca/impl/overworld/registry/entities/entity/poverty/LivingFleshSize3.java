package org.tdddd.epca.impl.overworld.registry.entities.entity.poverty;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IPoverty;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
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

import java.util.EnumSet;
import java.util.List;

public class LivingFleshSize3 extends PathfinderMob implements GeoEntity, IParasite, IPoverty, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("size3_idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("size3_walk");
    private static final RawAnimation ANIM_IDLE_WATER = RawAnimation.begin().thenLoop("size3_idle_water");
    private static final RawAnimation ANIM_SWIM = RawAnimation.begin().thenLoop("size3_swim");
    private static final RawAnimation ANIM_GROW_LAND = RawAnimation.begin().thenPlay("size2_grow_to_size3");
    private static final RawAnimation ANIM_GROW_WATER = RawAnimation.begin().thenPlay("size2_grow_to_size3_water");

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.LIVING_FLESH_SPAWN_EGG.get());
    }
    
    private boolean hasPlayedGrowAnimation = false;
    private boolean isInWaterAtSpawn = false;

    public LivingFleshSize3(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setMaxUpStep(1.0F);
        
        this.moveControl = new WaterMoveControl(this);
        
        this.navigation = new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        
        this.goalSelector.addGoal(0, new FindGroupGoal(this, 1.2D, 32.0F));
        
        this.goalSelector.addGoal(1, new AvoidNonParasiteGoal(this, 1.0D, 8.0F));
        
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
    }

    @Override
    public void tick() {
        super.tick();

        
        if (this.isInWater()) {
            
            if (!(this.navigation instanceof WaterBoundPathNavigation)) {
                this.navigation = new WaterBoundPathNavigation(this, this.level());
            }

            
            Vec3 deltaMovement = this.getDeltaMovement();
            
            if (!this.isNoGravity()) {
                this.setDeltaMovement(deltaMovement.x * 0.9D, deltaMovement.y * 0.9D + 0.005D, deltaMovement.z * 0.9D);
            }

            
            if (this.getNavigation().isDone()) {
                
                this.setDeltaMovement(this.getDeltaMovement().add(
                        (this.random.nextFloat() - 0.5F) * 0.02F,
                        (this.random.nextFloat() - 0.5F) * 0.02F,
                        (this.random.nextFloat() - 0.5F) * 0.02F
                ));
            }
        } else {
            
            if (!(this.navigation instanceof GroundPathNavigation)) {
                this.navigation = new GroundPathNavigation(this, this.level());
            }
        }
    }

    
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 1, false, false));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        
        this.isInWaterAtSpawn = this.isInWater();
    }

    
    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();

            
            BlockPos deathPos = this.blockPosition();

            
            int particleCount = 6 + this.random.nextInt(6); 
            for (int i = 0; i < particleCount; i++) {
                double x = deathPos.getX() + 0.5 + (this.random.nextDouble() - 0.5) * 2.0;
                double y = deathPos.getY() + 0.5 + this.random.nextDouble();
                double z = deathPos.getZ() + 0.5 + (this.random.nextDouble() - 0.5) * 2.0;
                double dx = (this.random.nextDouble() - 0.5) * 0.5;
                double dy = (this.random.nextDouble() - 0.5) * 0.5;
                double dz = (this.random.nextDouble() - 0.5) * 0.5;

                serverLevel.sendParticles(ModParticles.LIVING_FLESH.get(), x, y, z, 1, dx, dy, dz, 0.1);
            }

            
            AreaEffectCloud areaEffectCloud = new AreaEffectCloud(this.level(), deathPos.getX(), deathPos.getY(), deathPos.getZ());
            areaEffectCloud.setRadius(3.0F); 
            areaEffectCloud.setDuration(100); 
            areaEffectCloud.setWaitTime(0);
            areaEffectCloud.setRadiusPerTick(0.0F);

            
            areaEffectCloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 1)); 
            areaEffectCloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); 
            areaEffectCloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1)); 

            
            this.level().addFreshEntity(areaEffectCloud);
        }

        
        this.remove(RemovalReason.KILLED);
    }

    
    static class FindGroupGoal extends Goal {
        private final LivingFleshSize3 entity;
        private final double speedModifier;
        private final float searchRange;
        private LivingEntity target;
        private int cooldown;

        public FindGroupGoal(LivingFleshSize3 entity, double speedModifier, float searchRange) {
            this.entity = entity;
            this.speedModifier = speedModifier;
            this.searchRange = searchRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }

            this.cooldown = 100; 

            
            this.target = findNearbyLivingFlesh();
            if (this.target == null) {
                
                this.target = findNearbyAssimilation();
            }

            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null &&
                    this.target.isAlive() &&
                    this.entity.distanceToSqr(this.target) < (double)(this.searchRange * this.searchRange);
        }

        @Override
        public void start() {
            
            this.entity.getNavigation().moveTo(this.target, this.speedModifier);
        }

        @Override
        public void stop() {
            this.target = null;
            this.entity.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.target != null) {
                
                this.entity.getNavigation().moveTo(this.target, this.speedModifier);

                
                this.entity.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            }
        }

        private LivingFleshSize3 findNearbyLivingFlesh() {
            List<LivingFleshSize3> nearby = this.entity.level().getEntitiesOfClass(
                    LivingFleshSize3.class,
                    this.entity.getBoundingBox().inflate(this.searchRange)
            );

            for (LivingFleshSize3 flesh : nearby) {
                if (flesh != this.entity &&
                        flesh.isAlive() &&
                        this.entity.hasLineOfSight(flesh)) {
                    return flesh;
                }
            }
            return null;
        }

        private LivingEntity findNearbyAssimilation() {
            List<LivingEntity> nearby = this.entity.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.entity.getBoundingBox().inflate(this.searchRange)
            );

            for (LivingEntity entity : nearby) {
                if (entity != this.entity &&
                        entity.isAlive() &&
                        entity instanceof IInfested ||
                        entity instanceof LivingFleshSize0 ||
                        entity instanceof LivingFleshSize1 ||
                        entity instanceof LivingFleshSize2 ||
                        entity instanceof LivingFleshSize3 ||
                        entity instanceof LivingFleshSize4 ||
                        entity instanceof LargeIncompleteForm ||
                        this.entity.hasLineOfSight(entity)) {
                    return entity;
                }
            }
            return null;
        }
    }

    
    static class AvoidNonParasiteGoal extends Goal {
        private final LivingFleshSize3 entity;
        private final double walkSpeedModifier;
        private final float maxDist;
        private LivingEntity toAvoid;
        private int panicTime;

        public AvoidNonParasiteGoal(LivingFleshSize3 entity, double walkSpeedModifier, float maxDist) {
            this.entity = entity;
            this.walkSpeedModifier = walkSpeedModifier;
            this.maxDist = maxDist;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            
            this.toAvoid = findEntityToAvoid();
            return this.toAvoid != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.toAvoid != null &&
                    this.toAvoid.isAlive() &&
                    this.entity.distanceToSqr(this.toAvoid) < (double)(this.maxDist * this.maxDist) &&
                    this.panicTime > 0;
        }

        @Override
        public void start() {
            this.panicTime = 100 + this.entity.getRandom().nextInt(100); 
        }

        @Override
        public void stop() {
            this.toAvoid = null;
            this.panicTime = 0;
        }

        @Override
        public void tick() {
            if (this.toAvoid != null) {
                
                Vec3 awayVector = this.entity.position().subtract(this.toAvoid.position()).normalize();
                Vec3 targetPos = this.entity.position().add(awayVector.scale(6.0D)); 

                
                this.entity.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.walkSpeedModifier);

                this.panicTime--;
            }
        }

        private LivingEntity findEntityToAvoid() {
            
            for (LivingEntity living : this.entity.level().getEntitiesOfClass(LivingEntity.class,
                    this.entity.getBoundingBox().inflate(this.maxDist))) {
                
                if (living != this.entity &&
                        living.isAlive() &&
                        !IParasite.isParasiteByTagOrInterface(living) &&
                        this.entity.hasLineOfSight(living)) {
                    return living;
                }
            }
            return null;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source == this.damageSources().fall()) {
            return false;
        }

        
        if (source == this.damageSources().inWall() || source == this.damageSources().drown()) {
            return false;
        }

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) {
                return false;
            }
        }

        float adjustedAmount = ((IParasite) this).onHurt(source, amount);
        boolean result = super.hurt(source, adjustedAmount);

        return result;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .build();
    }

    public static boolean checkLivingFleshSize3SpawnRules(
            EntityType<LivingFleshSize3> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LivingFleshSize3>(this, "controller", 4, this::predicate));
    }

    
    private PlayState predicate(AnimationState<LivingFleshSize3> event) {
        AnimationController<LivingFleshSize3> controller = event.getController();
        LivingFleshSize3 entity = event.getAnimatable();

        
        if (!entity.hasPlayedGrowAnimation) {
            entity.hasPlayedGrowAnimation = true;

            
            if (entity.isInWaterAtSpawn) {
                
                controller.setAnimation(ANIM_GROW_WATER);
            } else {
                
                controller.setAnimation(ANIM_GROW_LAND);
            }
            return PlayState.CONTINUE;
        }

        
        boolean inWater = entity.isInWater();
        
        boolean isMoving = event.isMoving();

        if (inWater) {
            
            if (isMoving) {
                
                controller.setAnimation(ANIM_SWIM);
            } else {
                
                controller.setAnimation(ANIM_IDLE_WATER);
            }
        } else {
            
            if (isMoving) {
                
                controller.setAnimation(ANIM_WALK);
            } else {
                
                controller.setAnimation(ANIM_IDLE);
            }
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    
    @Override
    public int getAirSupply() {
        return this.getMaxAirSupply(); 
    }

    
    static class WaterMoveControl extends MoveControl {
        private final LivingFleshSize3 entity;

        public WaterMoveControl(LivingFleshSize3 entity) {
            super(entity);
            this.entity = entity;
        }

        @Override
        public void tick() {
            if (this.operation == Operation.MOVE_TO && !this.entity.getNavigation().isDone()) {
                Vec3 vec3 = new Vec3(this.wantedX - this.entity.getX(), this.wantedY - this.entity.getY(), this.wantedZ - this.entity.getZ());
                double d0 = vec3.length();

                if (d0 < 0.01) {
                    this.mob.setSpeed(0.0F);
                    return;
                }

                
                float speed = (float)(this.speedModifier * this.entity.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.entity.setSpeed(speed);

                
                vec3 = vec3.normalize();
                this.entity.setDeltaMovement(this.entity.getDeltaMovement().add(vec3.scale(0.05D)));

                
                if (d0 > 0.05) {
                    Vec3 lookVec = this.entity.getDeltaMovement().normalize();
                    this.entity.setYRot(-((float)Math.atan2(lookVec.x, lookVec.z)) * (180F / (float)Math.PI));
                    this.entity.yBodyRot = this.entity.getYRot();
                }
            } else {
                this.entity.setSpeed(0.0F);
            }
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INCOMPLETE_FORM_HUNT.get();
    }

    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INCOMPLETE_FORM_HUNT.get();
    }
}