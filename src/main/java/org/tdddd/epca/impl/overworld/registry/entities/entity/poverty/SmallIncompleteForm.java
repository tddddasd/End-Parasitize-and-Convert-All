package org.tdddd.epca.impl.overworld.registry.entities.entity.poverty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.entities.IPoverty;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
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

public class SmallIncompleteForm extends PathfinderMob implements GeoEntity, IParasite, IPoverty, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private int particleCooldown = 0;
    private static final int MIN_PARTICLE_INTERVAL = 20;
    private static final int MAX_PARTICLE_INTERVAL = 60;

    public SmallIncompleteForm(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setMaxUpStep(0.5F);
        
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true; 
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9.0D) 
                .add(Attributes.MOVEMENT_SPEED, 0.15D) 
                .add(Attributes.ATTACK_DAMAGE, 5.0D) 
                .add(Attributes.FOLLOW_RANGE, 16.0D) 
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
        this.goalSelector.addGoal(6, new SmallIncompleteForm.RandomSoundGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
    }

    @Override
    public void tick() {
        super.tick();

        
        if (this.level().isClientSide && this.isAlive()) {
            if (particleCooldown > 0) {
                particleCooldown--;
            } else {
                generateParticles();
                
                particleCooldown = this.random.nextInt(MAX_PARTICLE_INTERVAL - MIN_PARTICLE_INTERVAL + 1) + MIN_PARTICLE_INTERVAL;
            }
        }
        if (!this.level().isClientSide) {
            
            updateFloating();
        }
    }

    static class RandomSoundGoal extends Goal {
        private final SmallIncompleteForm smallIncompleteForm;
        private int nextSoundTick;

        RandomSoundGoal(SmallIncompleteForm smallIncompleteForm) {
            this.smallIncompleteForm = smallIncompleteForm;
        }

        

        @Override
        public boolean canUse() {
            return smallIncompleteForm.isAlive() && !smallIncompleteForm.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = smallIncompleteForm.getRandom().nextInt(140) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                playRandomLivingSound();
                this.nextSoundTick = smallIncompleteForm.getRandom().nextInt(140) + 80;
            }
        }

        private void playRandomLivingSound() {
            smallIncompleteForm.playSound(ModSoundEvents.INCOMPLETE_FORM_IDLE.get(), 1.0F, 1.0F);
        }
    }

    
    private void generateParticles() {
        
        if (!this.level().isClientSide) return;

        ParticleOptions particle = ModParticles.SPLASHI.get();
        if (particle == null) return;

        int particleCount = this.random.nextInt(3) + 1;
        for (int i = 0; i < particleCount; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 0.05;
            double y = this.getY() + this.random.nextDouble() * this.getBbHeight();
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 0.05;

            
            this.level().addParticle(
                    particle,
                    x, y, z,
                    0, -0.15, 0  
            );
        }
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
        controllers.add(new AnimationController<SmallIncompleteForm>(this, "controller", 4, this::predicate));
    }

    
    private PlayState predicate(AnimationState<SmallIncompleteForm> event) {
        AnimationController<SmallIncompleteForm> controller = event.getController();
        SmallIncompleteForm smallIncompleteForm = event.getAnimatable();

        if (smallIncompleteForm.isMoving()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            controller.setAnimation(RawAnimation.begin().thenLoop("idle"));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INCOMPLETE_FORM_HUNT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        
        return ModSoundEvents.INCOMPLETE_FORM_DEATH.get();
    }

    
    public static boolean checkSmallIncompleteFormSpawnRules(
            EntityType<SmallIncompleteForm> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        
        return level.getMaxLocalRawBrightness(pos) < 0;
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
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