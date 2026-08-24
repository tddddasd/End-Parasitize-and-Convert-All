package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;

public class Fins extends PathfinderMob implements GeoEntity, IParasite, IOnesent, Enemy {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final float SWIM_SPEED_MULTIPLIER = 0.5f; 
    private int waterSearchCooldown = 0;
    public enum Variant {
        DEFAULT,
        BLOOD
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        
        this.entityData.define(DATA_VARIANT, Fins.Variant.DEFAULT.ordinal());
    }
    
    private boolean isSprinting = false;
    private int sprintCooldown = 0;
    private int sprintDuration = 0;
    private int jumpAnimationTime = 0;
    private int jumpOutCooldown = 0;
    
    private static final int MIN_JUMP_OUT_COOLDOWN = 500;
    private static final int MAX_JUMP_OUT_COOLDOWN = 700;
    private boolean hasSprintDamaged = false;
    
    private int landMovementCooldown = 0;
    private boolean isOnLand = false;

    
    private int deepSneakCheckCooldown = 0;
    private static final int DEEP_SNEAK_CHECK_INTERVAL = 100; 

    public Fins(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);

        
        this.setMaxUpStep(0.3F);

        
        if (!level.isClientSide) {
            int roll = this.random.nextInt(100);
            if (roll < 50) {
                this.setVariant(Fins.Variant.DEFAULT);  
            } else {
                this.setVariant(Fins.Variant.BLOOD);
            }
        }

        this.xpReward = 5;

        
        this.navigation = new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            Fins.class, EntityDataSerializers.INT
    );

    
    public Fins.Variant getVariant() {
        
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) {
            
            return Fins.Variant.DEFAULT;
        }

        
        int index = Mth.clamp(variantOrdinal, 0, Fins.Variant.values().length - 1);
        return Fins.Variant.values()[index];
    }

    public void setVariant(Fins.Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public static AttributeSupplier setAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 5.0D)
                .build(); 
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.8D, true));
        this.goalSelector.addGoal(2, new Fins.FinsSwimGoal(this));
        this.goalSelector.addGoal(3, new Fins.FinsLandMoveGoal(this)); 
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.5D, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));

        
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                entity -> {
                    if (!isValidTarget(entity) || IParasite.isParasiteByTagOrInterface(entity) || entity instanceof Creeper) {
                        return false;
                    }

                    double range = this.isInWater() ? 32.0 : 2.0; 
                    return this.distanceToSqr(entity) <= range * range;
                }));
    }

    
    private boolean isValidTarget(Entity entity) {
        return entity != null &&
                entity.isAlive() &&
                !IParasite.isParasiteNoLivingByTagOrInterface(entity) &&
                !(entity instanceof Creeper);
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
        boolean attackSuccess = super.doHurtTarget(target);

        if (attackSuccess && target instanceof LivingEntity livingTarget) {
            
            

            
            if (Objects.requireNonNull(this.getVariant()) == Variant.BLOOD) {
                applyBleedingEffect(livingTarget);
            }
        }

        return attackSuccess;
    }

    private void applyBleedingEffect(LivingEntity target) {
        
        MobEffectInstance existingEffect = target.getEffect(ModEffects.BLEEDING.get());
        int newAmplifier = 0; 

        if (existingEffect != null) {
            
            newAmplifier = Math.min(existingEffect.getAmplifier() + 1, 4); 
        }

        
        target.addEffect(new MobEffectInstance(
                ModEffects.BLEEDING.get(),
                100,  
                newAmplifier,
                false, false, true
        ));
    }

    
    public static boolean checkFinsSpawnRules(
            EntityType<Fins> entityType,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());

            
            if (stage < 3 || stage > 5) {
                return false;
            }
        }

        
        return levelAccessor.getBlockState(pos).is(Blocks.WATER) && levelAccessor.getMaxLocalRawBrightness(pos) < 10;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        
        return level.isUnobstructed(this) && level.getBlockState(this.blockPosition()).is(Blocks.WATER);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.1F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));

            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.18D, 0.0D));
        } else {
            
            super.travel(travelVector);

            
            if (!this.isInWater()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D)); 
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            
            if (deepSneakCheckCooldown > 0) {
                deepSneakCheckCooldown--;
            } else {
                
                deepSneakCheckCooldown = DEEP_SNEAK_CHECK_INTERVAL;
                checkAndApplyDeepSneak();
            }
        }

        
        boolean wasOnLand = this.isOnLand;
        this.isOnLand = !this.isInWater() && this.onGround();

        
        if (!this.isInWater() && this.waterSearchCooldown-- <= 0) {
            this.waterSearchCooldown = 20; 
            this.findWater();
        }

        
        if (!this.isInWater() && this.getAirSupply() < this.getMaxAirSupply()) {
            this.setAirSupply(this.getMaxAirSupply());
        }

        
        if (this.sprintCooldown > 0) {
            this.sprintCooldown--;
        }

        if (this.isSprinting) {
            this.sprintDuration--;

            
            if (this.sprintDuration <= 0) {
                this.isSprinting = false;
                
                this.checkSprintCollision();
            }
        }

        
        if (this.jumpAnimationTime > 0) {
            this.jumpAnimationTime--;
        }

        
        if (this.jumpOutCooldown > 0) {
            this.jumpOutCooldown--;
        }

        
        if (!this.level().isClientSide && this.isInWater() && this.getDeltaMovement().y > 0.2 && this.jumpAnimationTime <= 0) {
            
            if (this.canJumpOutOfWater()) {
                this.jumpAnimationTime = 20; 
                
                this.jumpOutCooldown = MIN_JUMP_OUT_COOLDOWN + this.random.nextInt(MAX_JUMP_OUT_COOLDOWN - MIN_JUMP_OUT_COOLDOWN + 1);
            }
        }

        
        if (!this.level().isClientSide && this.isOnLand && this.getDeltaMovement().horizontalDistanceSqr() > 0.01 && this.landMovementCooldown-- <= 0) {
            this.landMovementCooldown = 10;
        }
    }

    
    private void checkAndApplyDeepSneak() {
        
        if (this.getEffect(ModEffects.DEEP_SNEAK.get()) == null) {
            
            this.addEffect(new MobEffectInstance(
                    ModEffects.DEEP_SNEAK.get(),
                    Integer.MAX_VALUE, 
                    0,                 
                    false, false, false
            ));
        }
    }

    
    private boolean canJumpOutOfWater() {
        
        if (this.jumpOutCooldown > 0) {
            return false;
        }

        
        if (this.getTarget() != null) {
            return false;
        }

        
        if (!this.isInWater()) {
            return false;
        }

        
        BlockPos pos = this.blockPosition();
        int surfaceY = this.findWaterSurface(pos);
        if (surfaceY == -1 || this.getY() < surfaceY - 2) {
            return false;
        }

        
        for (int i = 1; i <= 3; i++) {
            BlockPos abovePos = new BlockPos(pos.getX(), surfaceY + i, pos.getZ());
            if (!this.level().getBlockState(abovePos).isAir()) {
                return false;
            }
        }

        return true;
    }

    
    private int findWaterSurface(BlockPos pos) {
        int y = pos.getY();
        
        while (y < this.level().getMaxBuildHeight()) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!this.level().getBlockState(checkPos).is(Blocks.WATER)) {
                return y - 1; 
            }
            y++;
        }
        return -1; 
    }

    
    public void checkSprintCollision() {
        if ((!this.level().isClientSide) && !hasSprintDamaged) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && !IParasite.isParasiteByTagOrInterface(target) && !(target instanceof Creeper)) {
                
                if (this.distanceToSqr(target) < 4.0) { 
                    target.hurt(this.damageSources().mobAttack(this), 5.0F);
                    this.hasSprintDamaged = true; 
                }
            }
        }
    }

    
    public void startSprint() {
        if (this.sprintCooldown <= 0 && this.getTarget() != null && this.isValidTarget(this.getTarget())) {
            this.isSprinting = true;
            this.sprintDuration = 15; 
            this.sprintCooldown = 80; 
            this.hasSprintDamaged = false; 

            
            Vec3 targetDir = new Vec3(
                    this.getTarget().getX() - this.getX(),
                    0,
                    this.getTarget().getZ() - this.getZ()
            ).normalize();

            this.setDeltaMovement(targetDir.x * 2.0, 0.5, targetDir.z * 2.0);
        }
    }

    private void findWater() {
        BlockPos currentPos = this.blockPosition();

        for (int range = 1; range <= 8; range++) {
            for (BlockPos pos : BlockPos.withinManhattan(currentPos, range, range, range)) {
                if (this.level().getBlockState(pos).is(Blocks.WATER)) {
                    
                    this.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0D);
                    return;
                }
            }
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; 
    }

    @Override
    public boolean isPushedByFluid() {
        return false; 
    }

    @Override
    public int getMaxHeadXRot() {
        return 1; 
    }

    @Override
    public int getMaxHeadYRot() {
        return 1; 
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions size) {
        return size.height * 0.6F;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            
            if (this.isSprinting) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("sprint"));
                return PlayState.CONTINUE;
            }

            
            if (this.jumpAnimationTime > 0) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("jump_water"));
                return PlayState.CONTINUE;
            }

            
            if (!this.isInWater() && this.onGround()) {
                if (this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_land"));
                }
                return PlayState.CONTINUE;
            }

            
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run_water"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    
    static class FinsSwimGoal extends Goal {
        private final Fins fins;

        public FinsSwimGoal(Fins fins) {
            this.fins = fins;
            if (this.fins.getTarget() != null && this.fins.isValidTarget(this.fins.getTarget()) && this.fins.getRandom().nextFloat() < 0.1F) {
                this.fins.startSprint();
            }
        }

        @Override
        public boolean canUse() {
            return this.fins.isInWater();
        }

        @Override
        public void tick() {
            
            if (this.fins.getRandom().nextFloat() < 0.8F) {
                this.fins.getJumpControl().jump();
            }

            
            if (this.fins.getTarget() != null && this.fins.getRandom().nextFloat() < 0.1F) {
                this.fins.startSprint();
            }

            
            Vec3 movement = this.fins.getDeltaMovement();
            double speed = SWIM_SPEED_MULTIPLIER;

            
            if (this.fins.getTarget() != null && !this.fins.isSprinting) {
                Vec3 targetPos = new Vec3(
                        this.fins.getTarget().getX() - this.fins.getX(),
                        this.fins.getTarget().getY() - this.fins.getY(),
                        this.fins.getTarget().getZ() - this.fins.getZ()
                ).normalize();

                this.fins.setDeltaMovement(
                        Mth.lerp(0.2, movement.x, targetPos.x * speed),
                        Mth.lerp(0.2, movement.y, targetPos.y * speed),
                        Mth.lerp(0.2, movement.z, targetPos.z * speed)
                );
            } else if (!this.fins.isSprinting) {
                
                float yRot = this.fins.getYRot() * ((float)Math.PI / 180F);
                float xRot = this.fins.getXRot() * ((float)Math.PI / 180F);

                double x = Math.sin(yRot) * Math.cos(xRot) * speed;
                double z = Math.cos(yRot) * Math.cos(xRot) * speed;
                double y = -Math.sin(xRot) * speed;

                
                y = Mth.clamp(y, -0.05, 0.05);

                this.fins.setDeltaMovement(
                        Mth.lerp(0.2, movement.x, x),
                        Mth.lerp(0.2, movement.y, y),
                        Mth.lerp(0.2, movement.z, z)
                );
            }

            
            this.fins.setYRot(Mth.rotLerp(0.2F, this.fins.getYRot(), this.fins.yHeadRot));
            this.fins.yBodyRot = this.fins.getYRot();
        }
    }

    
    static class FinsLandMoveGoal extends Goal {
        private final Fins fins;

        public FinsLandMoveGoal(Fins fins) {
            this.fins = fins;
        }

        @Override
        public boolean canUse() {
            return !this.fins.isInWater() && this.fins.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.fins.getTarget();
            if (target != null && target.isAlive()) {
                
                this.fins.getNavigation().moveTo(target, 0.5); 

                
                if (this.fins.distanceToSqr(target) < 4.0) {
                    this.fins.doHurtTarget(target);
                }
            }
        }
    }

    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case BLOOD:
                return new ResourceLocation("epca", "textures/entity/fins_blood.png");
            default:
                return new ResourceLocation("epca", "textures/entity/fins.png");
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.RIPPER_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.RIPPER_HUNT.get();
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
}