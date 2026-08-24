package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
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

public class WalkingDrownedHead extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 5 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 8 * 20; 

    
    private int deepSneakCheckCooldown = 0;
    private static final int DEEP_SNEAK_CHECK_INTERVAL = 100; 

    
    private int iceBreakCooldown = 0;
    private static final int ICE_BREAK_COOLDOWN_TICKS = 10; 

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.level().isClientSide) {
            
            handleIceTargeting();
        }
    }

    public WalkingDrownedHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
        this.moveControl = new WalkingHeadMoveControl(this);

        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.8D) 
                .add(Attributes.MOVEMENT_SPEED, 0.28D) 
                .add(Attributes.ATTACK_DAMAGE, 3.5D) 
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));                
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 30));   

        this.targetSelector.addGoal(0, new PriorityTargetGoal(this, 16.0D));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.isInWater() && !this.isSwimming()) {
                this.setSwimming(true);
            } else if (!this.isInWater() && this.isSwimming()) {
                this.setSwimming(false);
            }

            
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

    
    public void playAmbientSound() {
        if (!this.isSilent()) {
            this.playSound(ModSoundEvents.WALKING_HEAD_SAY.get(), 1.0F, 1.0F);
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.WALKING_HEAD_SAY.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return ModSoundEvents.WALKING_HEAD_DEATH.get();
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<WalkingDrownedHead> event) {
        
        boolean inWater = this.isInWater();

        if (inWater) {
            if (event.isMoving()) {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk_water"));
            } else {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_water"));
            }
        } else {
            if (event.isMoving()) {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            } else {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
            }
        }

        return PlayState.CONTINUE;
    }

    public static boolean checkWalkingDrownedHeadSpawnRules(
            EntityType<WalkingDrownedHead> entityType,
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

        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source); 
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

    
    static class WalkingHeadMoveControl extends MoveControl {
        private final WalkingDrownedHead head;
        private float targetPitch;

        public WalkingHeadMoveControl(WalkingDrownedHead head) {
            super(head);
            this.head = head;
        }

        @Override
        public void tick() {
            if (this.head.isInWater()) {
                LivingEntity target = this.head.getTarget();

                if (target != null) {
                    Vec3 toTarget = new Vec3(
                            target.getX() - head.getX(),
                            target.getEyeY() - head.getEyeY(),
                            target.getZ() - head.getZ()
                    );

                    
                    float targetYaw = (float) Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F;
                    head.setYRot(this.rotlerp(head.getYRot(), targetYaw, 90.0F));
                    head.yBodyRot = head.getYRot();

                    
                    double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
                    float rawPitch = (float) -Math.toDegrees(Math.atan2(toTarget.y, horizontalDist));
                    this.targetPitch = Mth.clamp(rawPitch, -75.0F, 75.0F);
                }

                
                head.setXRot(this.rotlerp(head.getXRot(), targetPitch, 20.0F));

                
                if (target != null) {
                    double dy = target.getY() - this.head.getY();
                    if (Math.abs(dy) > 0.5) {
                        
                        double ySpeed = Mth.clamp(dy * 0.1, -0.05, 0.2);
                        this.head.setDeltaMovement(this.head.getDeltaMovement().add(0, ySpeed, 0));
                    }
                }

                if (this.operation != Operation.MOVE_TO || this.head.getNavigation().isDone()) {
                    this.head.setSpeed(0.0F);
                    return;
                }

                
                double dx = this.wantedX - this.head.getX();
                double dy = this.wantedY - this.head.getY();
                double dz = this.wantedZ - this.head.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dy /= distance;

                float baseSpeed = (float) (this.speedModifier * this.head.getAttributeValue(Attributes.MOVEMENT_SPEED));
                if (this.head.isSwimming()) {
                    baseSpeed *= 2.75F; 
                }
                float adjustedSpeed = Mth.lerp(0.125F, this.head.getSpeed(), baseSpeed);
                this.head.setSpeed(adjustedSpeed);

                
                this.head.setDeltaMovement(
                        this.head.getDeltaMovement().add(
                                (double) adjustedSpeed * dx * 0.005D,
                                (double) adjustedSpeed * dy * 0.1D,
                                (double) adjustedSpeed * dz * 0.005D
                        )
                );
            } else {
                
                super.tick();
            }
        }
    }

    
    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
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
