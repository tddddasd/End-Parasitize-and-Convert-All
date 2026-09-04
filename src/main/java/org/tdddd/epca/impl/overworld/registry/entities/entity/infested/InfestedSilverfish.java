package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.blocks.InfestedBlockInterface;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
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
import software.bernie.geckolib.util.GeckoLibUtil;

public class InfestedSilverfish extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int ambientSoundTime;
    private static final int MIN_AMBIENT_SOUND_DELAY = 5 * 20; 
    private static final int MAX_AMBIENT_SOUND_DELAY = 8 * 20; 

    public InfestedSilverfish(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;

        
        this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);

        
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));

        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            
            if (this.getTarget() == null) {
                if (--this.ambientSoundTime <= 0) {
                    this.ambientSoundTime = this.random.nextInt(MIN_AMBIENT_SOUND_DELAY, MAX_AMBIENT_SOUND_DELAY);
                    playAmbientSound();
                }
            }

            
            LivingEntity target = this.getTarget();
            BlockPos footPos = this.blockPosition();
            boolean inInfestedBlock = this.level().getBlockState(footPos).getBlock() instanceof InfestedBlockInterface;
            boolean shouldHaveNoGravity = false;

            if (target == null) {
                
                BlockState footState = this.level().getBlockState(footPos);
                BlockState belowState = this.level().getBlockState(footPos.below());
                shouldHaveNoGravity = footState.getBlock() instanceof InfestedBlockInterface ||
                        belowState.getBlock() instanceof InfestedBlockInterface;
            } else {
                
                double dy = target.getY() - this.getY();
                double horizontalDistSq = this.distanceToSqr(target.getX(), this.getY(), target.getZ());

                
                if (dy > 0.5 && inInfestedBlock) {
                    shouldHaveNoGravity = true; 
                    Vec3 motion = this.getDeltaMovement();
                    
                    if (motion.y < 0.2) {
                        this.setDeltaMovement(motion.x, Math.min(motion.y + 0.05, 0.2), motion.z);
                    }
                } else {
                    shouldHaveNoGravity = false; 
                    
                    if (Math.abs(dy) < 1.5 && horizontalDistSq < 4.0) {
                        Vec3 motion = this.getDeltaMovement();
                        if (motion.y < 0) {
                            this.setDeltaMovement(motion.x, 0, motion.z);
                        }
                    }
                }

                
                if (this.navigation.isDone() && this.random.nextInt(20) == 0) {
                    this.navigation.moveTo(target, 1.0);
                }
            }

            
            if (this.isNoGravity() != shouldHaveNoGravity) {
                this.setNoGravity(shouldHaveNoGravity);
                
                if (shouldHaveNoGravity && this.getDeltaMovement().y < 0) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
                }
            }

            
            updateFloating();
        }
    }

    
    public void playAmbientSound() {
        if (!this.isSilent() && this.random.nextInt(3) == 0) {
            
            this.playSound(SoundEvents.SILVERFISH_AMBIENT, 0.95F, 0.8F);
        }
    }

    

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        
        SoundEvent soundevent = this.getHurtSound(source);
        if (soundevent != null) {
            this.playSound(soundevent, 0.95F, 0.8F); 
        }
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source == this.damageSources().fall() && this.isInInfestedBlock()) {
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

    private boolean isInInfestedBlock() {
        BlockPos pos = this.blockPosition();
        
        if (this.level().getBlockState(pos).getBlock() instanceof InfestedBlockInterface) {
            return true;
        }
        if (this.level().getBlockState(pos.above()).getBlock() instanceof InfestedBlockInterface) {
            return true;
        }
        
        return false;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedSilverfish> event) {

        if (event.isMoving()) {
            if (this.isInWall()) {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("mining"));
            } else {
                
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            }
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedSilverfishSpawnRules(
            EntityType<InfestedSilverfish> entityType,
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

        
        return level.getMaxLocalRawBrightness(pos) < 0;
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

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source); 
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
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
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

    
    
    @Override
    public boolean isInWall() {
        
        float f = this.getDimensions(this.getPose()).width * 0.8F;
        AABB aabb = AABB.ofSize(this.getEyePosition(), (double)f, 1.0E-6D, (double)f);
        return BlockPos.betweenClosedStream(aabb).anyMatch((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            
            if (blockstate.getBlock() instanceof InfestedBlockInterface) {
                return false;
            }
            return blockstate.isSuffocating(this.level(), pos) && blockstate.isRedstoneConductor(this.level(), pos);
        });
    }
}