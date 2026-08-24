package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import org.tdddd.epca.impl.overworld.registry.blocks.BlockConversionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
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

import java.util.List;

public class FlyingCarrier extends PathfinderMob implements GeoEntity, IParasite, IOnesent, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_EXPLODING = SynchedEntityData.defineId(FlyingCarrier.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_BOOMING = SynchedEntityData.defineId(FlyingCarrier.class, EntityDataSerializers.BOOLEAN);

    
    private int explosionTimer = 20;
    private BlockPos deathPosition;

    
    private int boomAnimationTimer = 0;          
    private boolean isBoomTriggered = false;     

    
    private int clientBoomTimer = 0;
    private int clientSpawnTimer = 0;

    
    private int gnatSpawnTimer = 0;

    
    private int particleTimer = 0;

    
    private int dodgeCooldown = 0;
    private boolean isDodging = false;
    
    private boolean isRetreating = false;
    private int retreatCooldown = 0;
    private Vec3 retreatDirection = Vec3.ZERO;
    private static final double SAFE_DISTANCE = 8.0D;           
    private static final int RETREAT_COOLDOWN = 5;              
    private static final double RETREAT_SPEED = 0.25D;          

    public FlyingCarrier(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        this.moveControl = new FlyingMoveControl(this, 20, true);

        if (level.isClientSide) {
            clientSpawnTimer = 15; 
        }
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D)
                .add(Attributes.FLYING_SPEED, 0.35D)
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
        
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_EXPLODING, false);
        this.entityData.define(DATA_IS_BOOMING, false);
    }

    
    public boolean isExploding() {
        return this.entityData.get(DATA_IS_EXPLODING);
    }

    private void setExploding(boolean exploding) {
        this.entityData.set(DATA_IS_EXPLODING, exploding);
    }

    private void triggerExplosionDeath(DamageSource source) {
        setExploding(true);
        explosionTimer = 20;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.entityData.set(DATA_IS_BOOMING, false); 
    }

    private void executeExplosionEffects() {
        if (this.level() instanceof ServerLevel serverLevel) {
            
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.BIG_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);

            
            AABB explosionArea = this.getBoundingBox().inflate(4.0);
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, explosionArea)) {
                if (entity != null && entity.isAlive() && entity != this && !IParasite.isParasiteByTagOrInterface(entity)) {
                    double distance = this.distanceTo(entity);
                    if (distance <= 4.0) {
                        float damage = 35.0F * (float)(1.0 - distance / 4.0);
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

            
            int rupterCount = 2 + this.random.nextInt(2);
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

            
            int buglinCount = 1 + this.random.nextInt(2);
            for (int i = 0; i < buglinCount; i++) {
                EntityType<?> buglinType = ModEntities.CURBUG.get();
                Entity buglin = buglinType.create(this.level());
                if (buglin != null) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
                    buglin.setPos(
                            this.getX() + offsetX,
                            this.getY(),
                            this.getZ() + offsetZ
                    );
                    this.level().addFreshEntity(buglin);
                }
            }

            
            BlockPos center = this.blockPosition();
            BlockConversionManager manager = BlockConversionManager.getInstance();

            
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        double distSqr = center.distSqr(pos);
                        
                        if (distSqr > 9.0) continue;

                        BlockState state = serverLevel.getBlockState(pos);
                        if (!manager.isExposed(serverLevel, pos)) continue; 

                        
                        if (distSqr <= 4.0) {
                            
                            if (this.random.nextFloat() < 0.8f) {
                                manager.convertBlockUsingGeneralConfig(serverLevel, pos, state);
                                manager.convertPlantsInRangeForGeneral(serverLevel, pos);
                            }
                        } else {
                            
                            if (this.random.nextFloat() < 0.15f) {
                                manager.convertBlockUsingGeneralConfig(serverLevel, pos, state);
                                manager.convertPlantsInRangeForGeneral(serverLevel, pos);
                            }
                        }
                    }
                }
            }
        }
    }

    
    private void startRetreating(LivingEntity target) {
        if (target == null) return;
        isRetreating = true;
        
        retreatDirection = this.position().subtract(target.position()).normalize();
        this.getNavigation().stop(); 
    }

    
    private void updateRetreatMovement() {
        if (!isRetreating || retreatDirection == Vec3.ZERO) return;
        
        this.setDeltaMovement(retreatDirection.scale(RETREAT_SPEED));
    }

    
    private void stopRetreating() {
        if (isRetreating) {
            isRetreating = false;
            retreatDirection = Vec3.ZERO;
            
            if (this.getTarget() != null) {
                this.getNavigation().moveTo(this.getTarget(), 1.0);
            }
        }
    }

    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)) {
            return false;
        }

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
        return super.hurt(source, adjustedAmount);
    }

    
    @Override
    public void die(DamageSource source) {
        if (this.isOnFire()) {
            
            super.die(source);
            this.onDeath(source);
            return;
        }

        
        if (!this.level().isClientSide && !this.isExploding()) {
            triggerExplosionDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
        }
    }

    
    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    
    @Override
    public void tick() {
        super.tick();

        
        if (this.level().isClientSide) {
            
            if (this.entityData.get(DATA_IS_BOOMING)) {
                if (clientBoomTimer <= 0) {
                    clientBoomTimer = 20;
                }
                if (clientBoomTimer > 0) {
                    clientBoomTimer--;
                }
            } else {
                clientBoomTimer = 0;
            }
            
            if (clientSpawnTimer > 0) {
                clientSpawnTimer--;
            }

            
            if (particleTimer <= 0) {
                particleTimer = 4;
                int count = 1 + this.random.nextInt(4); 
                for (int i = 0; i < count; i++) {
                    double x = this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth();
                    double y = this.getY(); 
                    double z = this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth();
                    this.level().addParticle(ModParticles.BIOMASS.get(), x, y, z, 0, -0.25, 0);
                }
            } else {
                particleTimer--;
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

        
        if (boomAnimationTimer > 0) {
            boomAnimationTimer--;
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && !this.level().isClientSide) {
                this.getNavigation().moveTo(target, 1.2);
            }
            if (boomAnimationTimer <= 0) {
                this.entityData.set(DATA_IS_BOOMING, false); 
                if (!this.level().isClientSide) {
                    triggerExplosionDeath(null);
                }
            }
        }

        
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();

            
            if (target == null || !target.isAlive()) {
                if (boomAnimationTimer == 0 && !isExploding()) {
                    isBoomTriggered = false;
                    this.entityData.set(DATA_IS_BOOMING, false);
                }
                
            }

            
            if (target != null && target.isAlive() && !isBoomTriggered && boomAnimationTimer == 0) {
                double distance = this.distanceTo(target);
                boolean healthLow = this.getHealth() / this.getMaxHealth() < 0.25F;
                boolean tooClose = distance <= 5.0;
                if (healthLow || tooClose) {
                    isBoomTriggered = true;
                    boomAnimationTimer = 20; 
                    this.entityData.set(DATA_IS_BOOMING, true);
                    this.getNavigation().moveTo(target, 1.2);
                }
            }

            
            if (boomAnimationTimer == 0 && !isExploding()) {
                if (dodgeCooldown <= 0) {
                    dodgeCooldown = 5; 
                    
                    List<Projectile> projectiles = this.level().getEntitiesOfClass(Projectile.class,
                            this.getBoundingBox().inflate(16.0),
                            proj -> {
                                if (!proj.isAlive() || proj.getDeltaMovement().lengthSqr() < 0.001) return false;
                                Entity owner = proj.getOwner();
                                if (IParasite.isParasiteNoLivingByTagOrInterface(owner)) return false; 
                                
                                Vec3 toSelf = this.position().subtract(proj.position());
                                Vec3 motion = proj.getDeltaMovement();
                                if (toSelf.dot(motion) <= 0) return false; 
                                double distance = toSelf.length();
                                if (distance > 16.0) return false;
                                
                                double relativeSpeed = motion.length() - this.getDeltaMovement().dot(motion.normalize());
                                double timeToHit = distance / Math.max(relativeSpeed, 0.1);
                                return timeToHit < 5.0; 
                            });

                    if (!projectiles.isEmpty()) {
                        
                        Vec3 dodgeDir = Vec3.ZERO;
                        for (Projectile proj : projectiles) {
                            Vec3 away = this.position().subtract(proj.position()).normalize();
                            dodgeDir = dodgeDir.add(away);
                        }
                        dodgeDir = dodgeDir.normalize();
                        
                        Vec3 dodgeTarget = this.position().add(dodgeDir.scale(10.0));
                        this.getNavigation().moveTo(dodgeTarget.x, dodgeTarget.y, dodgeTarget.z, 1.5);
                        isDodging = true;
                    } else {
                        isDodging = false;
                    }
                } else {
                    dodgeCooldown--;
                }
            } else {
                isDodging = false; 
            }

            if (!isDodging && boomAnimationTimer == 0 && !isExploding()) {
                if (target != null && target.isAlive()) {
                    double distance = this.distanceTo(target);

                    
                    if (retreatCooldown > 0) retreatCooldown--;

                    
                    if (distance < SAFE_DISTANCE) {
                        if (retreatCooldown <= 0) {
                            startRetreating(target);
                            retreatCooldown = RETREAT_COOLDOWN;
                        }
                        if (isRetreating) {
                            updateRetreatMovement();
                        }
                        
                        if (gnatSpawnTimer > 0) {
                            gnatSpawnTimer--;
                        }
                    } else {
                        
                        if (isRetreating && distance >= SAFE_DISTANCE + 3.0) {
                            stopRetreating();
                        }

                        
                        if (!isRetreating) {
                            if (distance > 10.0) {
                                
                                Vec3 targetPos = new Vec3(this.getX(), target.getY() + 5.0, this.getZ());
                                this.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);

                                
                                if (gnatSpawnTimer <= 0) {
                                    int gnatCount = 1 + this.random.nextInt(2);
                                    for (int i = 0; i < gnatCount; i++) {
                                        EntityType<?> gnatType = ModEntities.MOZZIE.get();
                                        Entity gnat = gnatType.create(this.level());
                                        if (gnat != null) {
                                            gnat.setPos(this.getX(), this.getY(), this.getZ());
                                            this.level().addFreshEntity(gnat);
                                        }
                                    }
                                    gnatSpawnTimer = 240;
                                } else {
                                    gnatSpawnTimer--;
                                }
                            } else {
                                
                                this.getNavigation().moveTo(target, 1.0);
                            }
                        }
                    }
                } else {
                    
                    stopRetreating();
                }
            }
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.RIPPER_HUNT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.RIPPER_HUNT.get();
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FlyingCarrier>(this, "controller", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<FlyingCarrier> event) {
        AnimationController<FlyingCarrier> controller = event.getController();
        FlyingCarrier entity = event.getAnimatable();

        if (entity.isExploding()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("boom"));
        } else if (entity.clientSpawnTimer > 0) {
            controller.setAnimation(RawAnimation.begin().thenLoop("spawn"));
        } else if (entity.getTarget() != null || !entity.onGround()) {
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
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
        }
    }

    
    public static boolean checkFlyingCarrierSpawnRules(
            EntityType<FlyingCarrier> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getMaxLocalRawBrightness(pos) < 0;
    }
}