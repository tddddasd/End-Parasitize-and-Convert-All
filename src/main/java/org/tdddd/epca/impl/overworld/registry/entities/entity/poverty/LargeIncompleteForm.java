package org.tdddd.epca.impl.overworld.registry.entities.entity.poverty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.tdddd.epca.impl.overworld.registry.entities.IPoverty;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
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

public class LargeIncompleteForm extends PathfinderMob implements GeoEntity, IParasite, IPoverty, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_EXPLODING = SynchedEntityData.defineId(LargeIncompleteForm.class, EntityDataSerializers.BOOLEAN);

    private int explosionTimer = 30;
    private BlockPos deathPosition;

    private int particleCooldown = 0;
    private static final int MIN_PARTICLE_INTERVAL = 20;
    private static final int MAX_PARTICLE_INTERVAL = 60;

    private int healCooldown = 0;
    private static final int HEAL_COOLDOWN = 20;

    private int contactDamageCooldown = 0;
    private static final int CONTACT_DAMAGE_INTERVAL = 10;

    public LargeIncompleteForm(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.setMaxUpStep(1.0F);
        this.navigation = new GroundPathNavigation(this, level);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 68.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 17.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.JUMP_STRENGTH, 1.5D)
                .build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_EXPLODING, false);
    }

    public boolean isExploding() {
        return this.entityData.get(DATA_IS_EXPLODING);
    }

    private void setExploding(boolean exploding) {
        this.entityData.set(DATA_IS_EXPLODING, exploding);
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
        this.goalSelector.addGoal(5, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(6, new LargeIncompleteForm.RandomSoundGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 24.0D));
    }

    @Override
    public void tick() {
        super.tick();

        
        if (!this.level().isClientSide) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 64.0D);
            if (nearestPlayer == null) {
                
                if (this.tickCount % 5 != 0) {
                    
                    updateFloating();
                    return;
                }
            }
        }

        
        if (isExploding()) {
            explosionTimer--;
            if (explosionTimer <= 0) {
                if (!this.level().isClientSide) {
                    executeExplosionEffects();
                }
                this.discard();
                return;
            }
            return;
        }

        
        if (this.level().isClientSide && this.isAlive()) {
            if (particleCooldown > 0) {
                particleCooldown--;
            } else {
                generateParticles();
                particleCooldown = this.random.nextInt(MAX_PARTICLE_INTERVAL - MIN_PARTICLE_INTERVAL + 1) + MIN_PARTICLE_INTERVAL;
            }
        }

        
        if (!this.level().isClientSide) {

            
            if (healCooldown > 0) {
                healCooldown--;
            }

            
            checkHealFromSmallForm();

            
            if (contactDamageCooldown > 0) {
                contactDamageCooldown--;
            } else {
                contactDamageCooldown = CONTACT_DAMAGE_INTERVAL;
                applyContactDamage();
            }

            
            updateFloating();
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

    
    private void executeExplosionEffects() {
        if (this.level() instanceof ServerLevel serverLevel) {
            
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.BIG_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);

            serverLevel.sendParticles(ModParticles.COTH.get(),
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    15, 0.5, 0.5, 0.5, 0.0);

            serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    10, 0.6, 0.4, 0.6, 0.3);

            
            AABB explosionArea = this.getBoundingBox().inflate(4.0);
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, explosionArea)) {
                if (entity != null && entity.isAlive() && entity != this && !IParasite.isParasiteByTagOrInterface(entity)) {
                    double distance = this.distanceTo(entity);
                    if (distance <= 4.0) {
                        float damage = 45.0F * (float)(1.0 - distance / 4.0);
                        entity.hurt(this.damageSources().explosion(this, null), damage);

                        double dx = entity.getX() - this.getX();
                        double dz = entity.getZ() - this.getZ();
                        double magnitude = Math.sqrt(dx * dx + dz * dz);
                        if (magnitude > 0) {
                            dx /= magnitude;
                            dz /= magnitude;
                            double knockbackStrength = (1.0 - distance / 4.0) * 2.0;
                            entity.setDeltaMovement(
                                    entity.getDeltaMovement().add(dx * knockbackStrength, 0.3, dz * knockbackStrength)
                            );
                        }
                    }
                }
            }

            
            int rupterCount = this.random.nextInt(2) + 1;
            for (int i = 0; i < rupterCount; i++) {
                EntityType<?> rupterType = ModEntities.RIPPER.get();
                Entity rupter = rupterType.create(this.level());

                if (rupter != null) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;

                    rupter.setPos(
                            this.getX() + offsetX,
                            this.getY(),
                            this.getZ() + offsetZ
                    );

                    this.level().addFreshEntity(rupter);
                }
            }
        }
    }

    static class RandomSoundGoal extends Goal {
        private final LargeIncompleteForm largeIncompleteForm;
        private int nextSoundTick;

        RandomSoundGoal(LargeIncompleteForm largeIncompleteForm) {
            this.largeIncompleteForm = largeIncompleteForm;
        }

        

        @Override
        public boolean canUse() {
            return largeIncompleteForm.isAlive() && !largeIncompleteForm.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = largeIncompleteForm.getRandom().nextInt(140) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                playRandomLivingSound();
                this.nextSoundTick = largeIncompleteForm.getRandom().nextInt(140) + 80;
            }
        }

        private void playRandomLivingSound() {
            largeIncompleteForm.playSound(ModSoundEvents.INCOMPLETE_FORM_IDLE.get(), 1.0F, 1.0F);
        }
    }

    
    private void applyContactDamage() {
        if (!this.isAlive()) return;

        
        AABB contactArea = this.getBoundingBox().inflate(1.5);
        double maxDistSq = 1.5 * 1.5; 

        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, contactArea)) {
            
            if (entity == this || !entity.isAlive() || IParasite.isParasiteByTagOrInterface(entity)) {
                continue;
            }
            
            if (this.distanceToSqr(entity) <= maxDistSq) {
                entity.hurt(this.damageSources().mobAttack(this), 3.0F);

                if (entity instanceof Player) {
                    double dx = entity.getX() - this.getX();
                    double dz = entity.getZ() - this.getZ();
                    double magnitude = Math.sqrt(dx * dx + dz * dz);
                    if (magnitude > 0) {
                        dx /= magnitude;
                        dz /= magnitude;
                        entity.setDeltaMovement(
                                entity.getDeltaMovement().add(dx * 0.1, 0.1, dz * 0.1)
                        );
                    }
                }
            }
        }
    }

    private void checkHealFromSmallForm() {
        if (healCooldown > 0 || this.getHealth() >= this.getMaxHealth() - 12.0F) return;

        AABB area = this.getBoundingBox().inflate(2.0);
        double maxDistSq = 2.2 * 2.2;

        for (SmallIncompleteForm smallForm : this.level().getEntitiesOfClass(SmallIncompleteForm.class, area)) {
            if (smallForm.isAlive() && this.distanceToSqr(smallForm) <= maxDistSq) {
                this.heal(12.0F);
                smallForm.discard();
                healCooldown = HEAL_COOLDOWN;
                break;
            }
        }
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isExploding()) {
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

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LargeIncompleteForm>(this, "controller", 6, this::predicate));
    }

    private PlayState predicate(AnimationState<LargeIncompleteForm> event) {
        AnimationController<LargeIncompleteForm> controller = event.getController();
        LargeIncompleteForm largeIncompleteForm = event.getAnimatable();

        if (largeIncompleteForm.isExploding()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("dead"));
        } else if (event.isMoving()) {
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

    
    @Override
    public void die(DamageSource source) {
        if (isExploding()) {
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

        if (!this.level().isClientSide && this.getHealth() <= 0.0F) {
            triggerExplosionDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
        }
    }

    private void triggerExplosionDeath(DamageSource source) {
        setExploding(true);
        explosionTimer = 30;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.entityData.set(DATA_IS_EXPLODING, true);
    }

    
    public static boolean checkLargeIncompleteFormSpawnRules(
            EntityType<LargeIncompleteForm> entityType,
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
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
        }
    }

    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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