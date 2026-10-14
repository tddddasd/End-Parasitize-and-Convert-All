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
import org.tdddd.epca.impl.client.entity.EpcaGeoAnimations;
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
import java.util.EnumSet;
import java.util.List;
import java.lang.ref.WeakReference;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class Fins extends PathfinderMob implements GeoEntity, IParasite, IOnesent, Enemy {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final float SWIM_SPEED_MULTIPLIER = 0.5f; 
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

    
    private int flopCooldown = 0;
    private int flopHopCooldown = 0;
    @Nullable
    private BlockPos waterTarget;

    
    @Nullable
    private WeakReference<Fins> schoolLeader;
    private int schoolSize = 1;

    
    private int deepSneakCheckCooldown = 0;
    private static final int DEEP_SNEAK_CHECK_INTERVAL = 100; 

    
    private int waterSearchCooldown = 0;
    /** Ticks between two 3x3x3 water searches while stranded on land. */
    private static final int WATER_SEARCH_INTERVAL = 20;
    /** Give up on a remembered water block past this distance. */
    private static final double WATER_TARGET_RANGE_SQR = 12.0D * 12.0D;
    /** Ticks between two flop sounds. */
    private static final int FLOP_MIN_INTERVAL = 12;
    private static final int FLOP_MAX_INTERVAL = 26;
    /** Ticks between two flop hops; also the window in which the hop is steered. */
    private static final int FLOP_HOP_INTERVAL = 12;
    /** Upward speed of the flop hop - a small toss, nowhere near the gap-jump leap. */
    private static final double FLOP_HOP_UP = 0.42D;
    /** Horizontal speed of a steered flop hop toward water. */
    private static final double FLOP_HOP_FORWARD = 0.18D;

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
        // 鳍兽 schooling, mirroring vanilla fish: same priority 4 as the random swim so it can take
        // the MOVE flag first, and above every lower-priority swim/look goal.
        this.goalSelector.addGoal(4, new Fins.FinsSchoolGoal(this));

        
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

        
        this.tickLandBehaviour();

        if (this.flopCooldown > 0) {
            this.flopCooldown--;
        }
        if (this.flopHopCooldown > 0) {
            this.flopHopCooldown--;
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
        BlockPos origin = this.blockPosition().below();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        // 3x3x3 = 27 position tests per call, and only on the call interval while on land.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!this.level().getBlockState(pos).is(Blocks.WATER)) {
                        continue;
                    }
                    double distance = this.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        this.waterTarget = best == null ? null : best.immutable();
    }

    /**
     * Land behaviour: a stranded 鳍兽 flops like a fish and crawls toward water.
     *
     * <p>Cost: while swimming, this is a handful of flag reads and two cooldown decrements - no block
     * lookups at all. While on land it runs the 3x3x3 water search once per {@link #WATER_SEARCH_INTERVAL}
     * ticks (27 block reads), and when a target is held it does two reads per {@link #FLOP_HOP_INTERVAL}
     * ticks to steer the hop. Everything else is arithmetic.</p>
     */
    private void tickLandBehaviour() {
        if (this.isInWater()) {
            this.waterTarget = null;
            return;
        }

        // 1. Water search: staggered, 3x3x3 around the block under the mob.
        if (--this.waterSearchCooldown <= 0) {
            this.waterSearchCooldown = WATER_SEARCH_INTERVAL + this.random.nextInt(WATER_SEARCH_INTERVAL);
            this.findWater();
        }

        // 2. Drop a target that has gone stale.
        if (this.waterTarget != null) {
            boolean stale;
            if (!this.level().getBlockState(this.waterTarget).is(Blocks.WATER)) {
                stale = true;
            } else {
                double dx = this.waterTarget.getX() + 0.5D - this.getX();
                double dz = this.waterTarget.getZ() + 0.5D - this.getZ();
                stale = dx * dx + dz * dz > WATER_TARGET_RANGE_SQR;
            }
            if (stale) {
                this.waterTarget = null;
            }
        }

        // 3. Flop: the vanilla fish-out-of-water sound, on a randomised interval.
        if (this.onGround() && this.flopCooldown <= 0) {
            this.flopCooldown = FLOP_MIN_INTERVAL + this.random.nextInt(FLOP_MAX_INTERVAL - FLOP_MIN_INTERVAL);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.COD_FLOP,
                    SoundSource.HOSTILE, 0.6F, 0.9F + this.random.nextFloat() * 0.2F);
        }

        // 4. A small dedicated hop, deliberately NOT the gap-jump leap.
        if (this.onGround() && this.flopHopCooldown <= 0) {
            this.flopHopCooldown = FLOP_HOP_INTERVAL;
            this.setDeltaMovement(this.getDeltaMovement().x, FLOP_HOP_UP, this.getDeltaMovement().z);
            this.hasImpulse = true;
        }

        // 5. Steer the flop toward the remembered water.
        if (this.waterTarget != null && this.onGround()) {
            double dx = this.waterTarget.getX() + 0.5D - this.getX();
            double dz = this.waterTarget.getZ() + 0.5D - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 0.5D && this.flopHopCooldown == FLOP_HOP_INTERVAL) {
                double vx = dx / length * FLOP_HOP_FORWARD;
                double vz = dz / length * FLOP_HOP_FORWARD;
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(vx, motion.y, vz);
                this.setYRot((float) (Mth.atan2(vz, vx) * 180.0D / Math.PI) - 90.0F);
                this.yBodyRot = this.getYRot();
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
        controllers.add(new AnimationController<>(this, "controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, event -> {
            
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

    /**
     * Server-side schooling for 鳍兽, mirroring vanilla fish flocking: same-type fish within a modest
     * radius form a school of at most {@link #MAX_SCHOOL_SIZE} members, a fish with no leader finds one
     * (or becomes one), and members follow the leader at normal fish spacing.
     *
     * <p><b>Why a goal and not entity data:</b> school membership is ephemeral and only ever read by the
     * server, so it is kept in transient fields on each {@code Fins} (a weak leader reference plus a
     * member count) rather than a synced datum - nothing new goes over the wire, and a save/load simply
     * re-forms the school.</p>
     *
     * <p><b>Cost.</b> The neighbourhood query runs at most once per
     * {@link #SEARCH_INTERVAL_MIN}..{@link #SEARCH_INTERVAL_MAX} ticks per instance, and never on the
     * ticks where the cooldown is still running (that path is one integer decrement). The query is a
     * radius filter over {@code Fins} entities in a {@link #SCHOOL_RADIUS}-block box with a cheap
     * predicate; no block lookups, no per-tick scanning.</p>
     */
    static class FinsSchoolGoal extends Goal {
        /** Same-type fish within this radius may share a school. */
        private static final double SCHOOL_RADIUS = 8.0D;
        /** Hard cap, as requested: at most 7 per school (leader included). */
        private static final int MAX_SCHOOL_SIZE = 7;
        /** Members follow the leader only once they drift past this distance. */
        private static final double FOLLOW_DISTANCE_SQR = 9.0D;
        /** Re-evaluation interval, staggered per instance. */
        private static final int SEARCH_INTERVAL_MIN = 40;
        private static final int SEARCH_INTERVAL_MAX = 80;
        /** Horizontal speed of the follow steering. */
        private static final double FOLLOW_SPEED = 0.45D;

        private final Fins fins;
        private int cooldown;

        FinsSchoolGoal(Fins fins) {
            this.fins = fins;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            // Stagger the first evaluation so a group spawned together does not scan on one tick.
            this.cooldown = SEARCH_INTERVAL_MIN + fins.getRandom().nextInt(SEARCH_INTERVAL_MAX - SEARCH_INTERVAL_MIN);
        }

        @Override
        public boolean canUse() {
            if (this.fins.level().isClientSide()) {
                return false;
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            this.cooldown = SEARCH_INTERVAL_MIN + this.fins.getRandom().nextInt(SEARCH_INTERVAL_MAX - SEARCH_INTERVAL_MIN);

            // A swimming fish with a live leader keeps it; otherwise look for a school to join.
            Fins leader = this.leader();
            if (leader == this.fins && this.fins.schoolLeader != null) {
                // The old leader is gone or dead: forget it before searching again.
                this.fins.schoolLeader = null;
            } else if (leader != this.fins && this.fins.distanceToSqr(leader) > (SCHOOL_RADIUS * 2.0D) * (SCHOOL_RADIUS * 2.0D)) {
                // Drifted too far from the old school: leave it, and give the count back so the leader's
                // capacity reflects reality rather than only ever growing.
                leader.schoolSize = Math.max(1, leader.schoolSize - 1);
                this.fins.schoolLeader = null;
                leader = null;
            }
            if (leader == null || leader == this.fins) {
                Fins found = this.findSchoolLeader();
                if (found != null) {
                    this.setLeader(found);
                } else {
                    this.fins.schoolLeader = null;
                    this.fins.schoolSize = Math.max(1, this.fins.schoolSize);
                }
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            Fins leader = this.leader();
            return leader == null || this.fins.distanceToSqr(leader) <= (SCHOOL_RADIUS * 4.0D) * (SCHOOL_RADIUS * 4.0D);
        }

        @Override
        public void tick() {
            if (!this.fins.isInWater() || this.fins.getTarget() != null) {
                return;
            }
            Fins leader = this.leader();
            if (leader == null || leader == this.fins) {
                return;
            }
            if (this.fins.distanceToSqr(leader) <= FOLLOW_DISTANCE_SQR) {
                return;
            }
            double dx = leader.getX() - this.fins.getX();
            double dy = leader.getY() - this.fins.getY();
            double dz = leader.getZ() - this.fins.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal < 1.0E-4D) {
                return;
            }
            Vec3 motion = this.fins.getDeltaMovement();
            this.fins.setDeltaMovement(
                    Mth.lerp(0.2D, motion.x, dx / horizontal * FOLLOW_SPEED),
                    Mth.lerp(0.2D, motion.y, Mth.clamp(dy * 0.1D, -0.05D, 0.05D)),
                    Mth.lerp(0.2D, motion.z, dz / horizontal * FOLLOW_SPEED));
            this.fins.setYRot(Mth.rotLerp(0.2F, this.fins.getYRot(),
                    (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F));
            this.fins.yBodyRot = this.fins.getYRot();
        }

        @Override
        public void stop() {
            // Nothing persistent to clear: the leader reference is only re-read on the next evaluation.
        }

        /** The leader reference, or this fish itself when it is a school leader. */
        @Nullable
        private Fins leader() {
            if (this.fins.schoolLeader == null) {
                return this.fins;
            }
            Fins leader = this.fins.schoolLeader.get();
            return leader == null || !leader.isAlive() ? this.fins : leader;
        }

        /** The nearest same-type leader whose school is not full; null when there is none in range. */
        @Nullable
        private Fins findSchoolLeader() {
            AABB area = this.fins.getBoundingBox().inflate(SCHOOL_RADIUS);
            List<Fins> candidates = this.fins.level().getEntitiesOfClass(Fins.class, area,
                    other -> other != this.fins
                            && other.isAlive()
                            && other.getVariant() == this.fins.getVariant()
                            && this.leaderOf(other).schoolSize < MAX_SCHOOL_SIZE);
            Fins best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Fins candidate : candidates) {
                double distance = this.fins.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            return best;
        }

        /** The school record a candidate belongs to: itself when it has no live leader. */
        private Fins leaderOf(Fins candidate) {
            if (candidate.schoolLeader == null) {
                return candidate;
            }
            Fins leader = candidate.schoolLeader.get();
            return leader == null || !leader.isAlive() ? candidate : leader;
        }

        private void setLeader(@Nullable Fins leader) {
            if (leader == null || leader == this.fins) {
                this.fins.schoolLeader = null;
                this.fins.schoolSize = Math.max(this.fins.schoolSize, 1);
                return;
            }
            this.fins.schoolLeader = new WeakReference<>(leader);
            leader.schoolSize = Math.min(MAX_SCHOOL_SIZE, Math.max(1, leader.schoolSize) + 1);
        }
    }
}