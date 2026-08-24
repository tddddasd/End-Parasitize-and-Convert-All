package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class InfestedDrowned extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    public int tryApplyCothEffect(Mob mob, int currentCooldown) {
        return 0;
    }
    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(InfestedDrowned.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(InfestedDrowned.class, EntityDataSerializers.BOOLEAN);
    private static final UUID WANDER_SPEED_ID = UUID.fromString("A4766B59-7066-4402-AD81-0E3B7B6C2B9B");
    private static final AttributeModifier WANDER_SPEED_REDUCTION = new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
    
    private int breakCooldown = 0;
    private int jumpCooldown = 0;

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.level().isClientSide) {
            breakCooldown = tryBreakLightSources(this, breakCooldown);

            
            if (jumpCooldown > 0) {
                jumpCooldown--;
            }

            if (jumpCooldown == 0 && this.onGround() && !this.isInWater()) {
                LivingEntity target = this.getTarget();
                if (target != null) {
                    double dx = target.getX() - this.getX();
                    double dz = target.getZ() - this.getZ();
                    double horizontalDistSq = dx * dx + dz * dz;

                    
                    if (horizontalDistSq > 1.0) {
                        Vec3 dir = new Vec3(dx, 0, dz).normalize();

                        
                        double currentGroundY = getGroundHeightAt(this.blockPosition());

                        
                        double forward1Y = getGroundHeightAt(
                                BlockPos.containing(this.getX() + dir.x, this.getY(), this.getZ() + dir.z)
                        );

                        
                        double forward2Y = getGroundHeightAt(
                                BlockPos.containing(this.getX() + dir.x * 2, this.getY(), this.getZ() + dir.z * 2)
                        );

                        
                        
                        boolean isCliffToJump = (currentGroundY - forward1Y > 0.5) && (currentGroundY - forward2Y <= 0.5);

                        if (isCliffToJump) {
                            
                            double jumpPower = 0.45;       
                            double horizontalSpeed = 0.28;  

                            this.setDeltaMovement(
                                    dir.x * horizontalSpeed,
                                    jumpPower,
                                    dir.z * horizontalSpeed
                            );
                            this.hasImpulse = true;          
                            this.jumpCooldown = 10;           
                        }
                    }
                }
            }
            
            handleIceTargeting();
        }
    }

    
    private static final double WATER_ATTACK_RANGE = 4.5D; 
    private static final double WATER_ATTACK_RANGE_SQR = WATER_ATTACK_RANGE * WATER_ATTACK_RANGE; 

    
    private static final double CLOSE_ATTACK_RANGE_SQR = 2 * 2; 
    private static final double MAX_TRACKING_RANGE_SQR = 24 * 24;   

    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 12 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 16 * 20; 

    
    private int deepSneakCheckCooldown = 0;
    private static final int DEEP_SNEAK_CHECK_INTERVAL = 100; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedDrowned.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 30;
    private BlockPos deathPosition; 

    
    private int iceBreakCooldown = 0;
    private static final int ICE_BREAK_COOLDOWN_TICKS = 10; 

    public InfestedDrowned(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        
        this.moveControl = new InfestedMoveControl(this);
        this.setMaxUpStep(0.5F);
        
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    
    static class InfestedMoveControl extends MoveControl {
        private final InfestedDrowned drowned;
        private float targetPitch; 

        public InfestedMoveControl(InfestedDrowned drowned) {
            super(drowned);
            this.drowned = drowned;
        }

        @Override
        public void tick() {
            if (this.drowned.isInWater() && !this.drowned.isFakingDeath()) {
                LivingEntity target = this.drowned.getTarget();

                if (target != null) {
                    Vec3 toTarget = new Vec3(
                            target.getX() - drowned.getX(),
                            target.getEyeY() - drowned.getEyeY(),
                            target.getZ() - drowned.getZ()
                    );

                    
                    float targetYaw = (float) Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F;
                    drowned.setYRot(this.rotlerp(drowned.getYRot(), targetYaw, 90.0F));
                    drowned.yBodyRot = drowned.getYRot();

                    
                    double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
                    float rawPitch = (float) -Math.toDegrees(Math.atan2(toTarget.y, horizontalDist));
                    this.targetPitch = Mth.clamp(rawPitch, -75.0F, 75.0F);
                }

                
                drowned.setXRot(this.rotlerp(drowned.getXRot(), targetPitch, 20.0F));

                
                if (target != null) {
                    double dy = target.getY() - this.drowned.getY();
                    if (Math.abs(dy) > 0.5) {
                        
                        double ySpeed = Mth.clamp(dy * 0.1, -0.15, 0.2);
                        this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0, ySpeed, 0));
                    }
                }

                if (this.operation != Operation.MOVE_TO || this.drowned.getNavigation().isDone()) {
                    this.drowned.setSpeed(0.0F);
                    return;
                }

                
                double dx = this.wantedX - this.drowned.getX();
                double dy = this.wantedY - this.drowned.getY();
                double dz = this.wantedZ - this.drowned.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dy /= distance;

                
                float baseSpeed = (float)(this.speedModifier * this.drowned.getAttributeValue(Attributes.MOVEMENT_SPEED));
                
                if (this.drowned.isSwimming()) {
                    baseSpeed *= 2.75F;
                }
                float adjustedSpeed = Mth.lerp(0.125F, this.drowned.getSpeed(), baseSpeed);
                this.drowned.setSpeed(adjustedSpeed);

                
                this.drowned.setDeltaMovement(
                        this.drowned.getDeltaMovement().add(
                                (double)adjustedSpeed * dx * 0.005D,
                                (double)adjustedSpeed * dy * 0.1D,
                                (double)adjustedSpeed * dz * 0.005D
                        )
                );
            } else {
                
                super.tick();
            }
        }
    }

    
    private void handleIceTargeting() {
        if (iceBreakCooldown > 0) {
            iceBreakCooldown--;
        }
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB searchArea = this.getBoundingBox().inflate(followRange);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != this && entity.isAlive() && !IParasite.isParasiteByTagOrInterface(entity) && !(entity instanceof Creeper) &&
                        (!(entity instanceof Player) || (!((Player) entity).isCreative() && !((Player) entity).isSpectator()))
        );

        List<BlockPos> icePositionsToBreak = new ArrayList<>();
        LivingEntity targetToMove = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity target : candidates) {
            BlockPos targetFeetPos = target.blockPosition();
            BlockPos icePos = targetFeetPos.below();
            BlockState blockBelow = this.level().getBlockState(icePos);
            boolean isOnIce = blockBelow.is(Blocks.ICE) || blockBelow.is(Blocks.FROSTED_ICE);
            if (!isOnIce) continue;

            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horizontalDistSq = dx * dx + dz * dz;
            if (target.getY() <= this.getY() || horizontalDistSq > 4.0) continue; 

            
            double distToTarget = this.distanceToSqr(target);
            if (distToTarget < closestDist) {
                closestDist = distToTarget;
                targetToMove = target;
            }

            
            double distToIceSq = this.distanceToSqr(icePos.getX() + 0.5, icePos.getY(), icePos.getZ() + 0.5);
            if (distToIceSq <= 1.44) {
                icePositionsToBreak.add(icePos.immutable());
            }
        }

        
        if (targetToMove != null) {
            BlockPos targetBottomPos = targetToMove.blockPosition().below();
            this.getNavigation().moveTo(targetBottomPos.getX() + 0.5, targetBottomPos.getY(), targetBottomPos.getZ() + 0.5, 1.0);
        }

        
        if (!icePositionsToBreak.isEmpty() && iceBreakCooldown == 0) {
            int toBreak = this.random.nextInt(2) + 1; 
            Collections.shuffle(icePositionsToBreak);
            int broken = 0;
            for (BlockPos pos : icePositionsToBreak) {
                if (broken >= toBreak) break;
                if (breakIceAt(pos)) {
                    broken++;
                }
            }
            if (broken > 0) {
                iceBreakCooldown = ICE_BREAK_COOLDOWN_TICKS;
            }
        }
    }

    private boolean breakIceAt(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            this.level().destroyBlock(pos, false); 
            this.level().playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    
    private double getGroundHeightAt(BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int startY = Math.min((int) this.getY() + 5, level().getMaxBuildHeight());
        mutable.setY(startY);
        while (mutable.getY() > level().getMinBuildHeight()) {
            BlockState state = level().getBlockState(mutable);
            if (state.isSolid()) {
                return mutable.getY() + 1; 
            }
            mutable.setY(mutable.getY() - 1);
        }
        return level().getMinBuildHeight();
    }

    
    private boolean shouldJumpOverCliff(LivingEntity target) {
        if (target == null) return false;

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        if (horizontalDistSq <= 1.0) return false; 

        Vec3 dir = new Vec3(dx, 0, dz).normalize();

        
        double currentGround = getGroundHeightAt(this.blockPosition());

        int width = 0;
        for (int i = 1; i <= 3; i++) {
            double checkX = this.getX() + dir.x * i;
            double checkZ = this.getZ() + dir.z * i;
            BlockPos checkPos = BlockPos.containing(checkX, 0, checkZ);
            double ground = getGroundHeightAt(checkPos);

            double drop = currentGround - ground;
            if (drop > 0.5) { 
                width++;
                if (width > 1) return false;        
                if (i == 3) return false;            
            } else {
                
                if (width > 0) {
                    
                    return this.getNavigation().createPath(target, 0) == null;
                } else {
                    return false; 
                }
            }
        }
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level); 
    }

    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D) 
                .add(Attributes.MOVEMENT_SPEED, 0.29D) 
                .add(Attributes.ATTACK_DAMAGE, 5.0D) 
                .add(Attributes.FOLLOW_RANGE, 24.0D) 
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ARMOR, 4.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));

        
        this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 1.0D, 30));

        
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new TryFindWaterGoal(this));

        this.targetSelector.addGoal(0, new PriorityTargetGoal(this, 16.0D));
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
        }

        
        if (!this.level().isClientSide && !this.isFakingDeath()) {
            if (this.isInWater() && !this.isSwimming()) {
                this.setSwimming(true);
            } else if (!this.isInWater() && this.isSwimming()) {
                this.setSwimming(false);
            }
        }

        
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                
                double distSqr = this.distanceToSqr(target);
                if (distSqr > MAX_TRACKING_RANGE_SQR || !this.hasLineOfSight(target)) {
                    
                    this.setTarget(null);
                } else {
                    
                    this.getNavigation().moveTo(target, this.isInWater() ? 1.2 : 0.8);
                }
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
                                1,    
                                false, true
                        ));

                        cloud.addEffect(new MobEffectInstance(
                                ModEffects.DEEP_SNEAK.get(),
                                600, 
                                14,    
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

        if (!this.level().isClientSide) {

            
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

                    
                    playAmbientSound();
                }
            }

            
            if (deepSneakCheckCooldown > 0) {
                deepSneakCheckCooldown--;
            } else {
                
                deepSneakCheckCooldown = DEEP_SNEAK_CHECK_INTERVAL;
                checkAndApplyDeepSneak();
            }
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
        if (!this.isSilent() && this.random.nextInt(3) == 0) {
            
            this.playSound(SoundEvents.DROWNED_AMBIENT, 0.85F, 0.7F);
        }
    }

    

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return SoundEvents.DROWNED_DEATH_WATER;
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
    public boolean doHurtTarget(Entity target) {
        
        if (this.isInWater()) {
            double distSqr = this.distanceToSqr(target);

            
            if (distSqr <= CLOSE_ATTACK_RANGE_SQR) {
                return performCloseRangeAttack(target);
            }

            
            if (distSqr > WATER_ATTACK_RANGE_SQR) {
                return false;
            }
        }

        boolean attackSuccess = super.doHurtTarget(target);

        return attackSuccess;
    }

    
    private boolean performCloseRangeAttack(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

            
            float actualDamage = calculateActualDamage(baseDamage, livingTarget);

            
            boolean success = livingTarget.hurt(this.damageSources().mobAttack(this), actualDamage);

            if (success) {
                
                if (!livingTarget.isAlive()) {
                    this.onKillEntity(livingTarget);
                }
            }
            return success;
        }
        return false;
    }

    
    private float calculateActualDamage(float baseDamage, LivingEntity target) {
        float actualDamage = baseDamage;
        MobEffectInstance resistance = target.getEffect(MobEffects.DAMAGE_RESISTANCE);

        if (resistance != null) {
            int level = resistance.getAmplifier() + 1;
            float reduction = level * 0.2F;
            actualDamage *= Math.max(1.0F - reduction, 0.0F);
        }
        return actualDamage;
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
        controllers.add(new AnimationController<>(this, "controller", 4, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedDrowned> event) {
        
        boolean inWater = this.isInWater();

        if (inWater) {
            
            if (this.isFakingDeath()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("dead_water"));
            } else if (event.isMoving()) {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("swim"));
            } else {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_water"));
            }
        } else {
            
            if (this.isFakingDeath()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
            } else if (this.isRunning()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
            } else if (this.isWalking()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            } else {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
            }
        }
        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedDrownedSpawnRules(
            EntityType<InfestedDrowned> entityType,
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
        
        return level.getBlockState(pos).is(Blocks.WATER);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        
        return level.isUnobstructed(this) && level.getBlockState(this.blockPosition()).is(Blocks.WATER);
    }

    
    private final double baseSpeed = 0.27D; 
    private final double chaseSpeed = 0.38D; 

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
                    WalkingDrownedHead head = new WalkingDrownedHead(ModEntities.WALKING_DROWNED_HEAD.get(), this.level());
                    head.setPos(this.getX(), this.getY(), this.getZ());
                    head.setYRot(this.random.nextFloat() * 360.0F);
                    this.level().addFreshEntity(head);
                }
            }
        }
    }

    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedDrowned.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
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

    @Override
    public boolean canBreatheUnderwater() {
        return true; 
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isSwimming(); 
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            
            this.moveRelative(0.02F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
            
        } else {
            super.travel(travelVector);
        }
    }
}
