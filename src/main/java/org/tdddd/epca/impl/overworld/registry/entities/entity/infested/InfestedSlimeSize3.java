package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.SlimeProjectile;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class InfestedSlimeSize3 extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int currentSize = 3; 
    private float targetSquish;
    private float squish;
    private float oSquish;
    private boolean wasOnGround;
    private int attackCooldown = 0; 

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(InfestedSlimeSize3.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(InfestedSlimeSize3.class, EntityDataSerializers.BOOLEAN);
    private int fakeDeathTimer = 10;
    private BlockPos deathPosition;

    
    private int rangedAttackCooldown = 0; 
    private int projectileCount = 0; 
    private int shootAnimationTime = 0; 
    private LivingEntity primaryTarget; 
    private List<LivingEntity> additionalTargets; 

    
    private static final EntityDataAccessor<Boolean> DATA_SHOOTING =
            SynchedEntityData.defineId(InfestedSlimeSize3.class, EntityDataSerializers.BOOLEAN);

    
    private static final SizeAttributes SIZE_3_ATTRIBUTES = new SizeAttributes(
            24.0D,  
            0.0D,   
            0.6D,   
            0.5F,   
            0.8F,   
            20, 60, 
            1.0F    
    );

    
    private static class SizeAttributes {
        public final double maxHealth;
        public final double attackDamage;
        public final double movementSpeed;
        public final float jumpPower;
        public final float horizontalMultiplier;
        public final int minJumpDelay;
        public final int maxJumpDelay;
        public final float scale;

        public SizeAttributes(double maxHealth, double attackDamage, double movementSpeed,
                              float jumpPower, float horizontalMultiplier,
                              int minJumpDelay, int maxJumpDelay, float scale) {
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.jumpPower = jumpPower;
            this.horizontalMultiplier = horizontalMultiplier;
            this.minJumpDelay = minJumpDelay;
            this.maxJumpDelay = maxJumpDelay;
            this.scale = scale;
        }
    }

    
    private SizeAttributes getCurrentSizeAttributes() {
        return SIZE_3_ATTRIBUTES;
    }

    public int tryApplyCothEffect(Mob mob, int currentCooldown) {
        return 0;
    }

    public InfestedSlimeSize3(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 12; 
        
        this.navigation = new GroundPathNavigation(this, level);
        
        this.moveControl = new SlimeMoveControl(this);

        
        this.currentSize = 3;

        
        this.wasOnGround = this.onGround();
        
        this.additionalTargets = new ArrayList<>(); 
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SlimeRangedAttackGoal(this)); 
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 32.0D));
    }

    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SIZE_3_ATTRIBUTES.maxHealth)
                .add(Attributes.MOVEMENT_SPEED, SIZE_3_ATTRIBUTES.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, 32.0D); 
    }

    @Override
    public void tick() {

        
        if (this.additionalTargets == null) {
            this.additionalTargets = new ArrayList<>();
        }
        
        if (isFakingDeath()) {
            super.tick();
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                
                if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                    
                    int particleCount = 24;
                    for (int i = 0; i < particleCount; i++) {
                        double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                        double d1 = this.getY() + this.random.nextDouble() * this.getBbHeight();
                        double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                        serverLevel.sendParticles(
                                new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get().getDefaultInstance()),
                                d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.1D
                        );
                    }

                    
                    int spawnCount = 2 + this.random.nextInt(3); 
                    for (int i = 0; i < spawnCount; i++) {
                        InfestedSlimeSize1 mediumSlime = InfestedSlimeSize1.create(this.level());
                        if (mediumSlime != null) {
                            
                            double offsetX = (this.random.nextDouble() - 0.5D) * 3.0D;
                            double offsetZ = (this.random.nextDouble() - 0.5D) * 3.0D;
                            mediumSlime.setPos(this.getX() + offsetX, this.getY() + 0.5D, this.getZ() + offsetZ);

                            
                            if (this.isOnFire()) {
                                mediumSlime.setSecondsOnFire(this.getRemainingFireTicks() / 20);
                            }

                            this.level().addFreshEntity(mediumSlime);
                        }
                    }
                }

                
                this.discard();
                return; 
            }
            
            return;
        }

        
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        
        if (this.rangedAttackCooldown > 0) {
            this.rangedAttackCooldown--;
        }

        
        if (this.shootAnimationTime > 0) {
            this.shootAnimationTime--;

            
            if (this.shootAnimationTime == 12 && !this.level().isClientSide) {
                if (this.additionalTargets.size() > 0) {
                    this.shootProjectileAtTarget(this.additionalTargets.get(0));
                }
            }
            
            else if (this.shootAnimationTime == 8 && !this.level().isClientSide) {
                if (this.additionalTargets.size() > 1) {
                    this.shootProjectileAtTarget(this.additionalTargets.get(1));
                }
            }
            
            else if (this.shootAnimationTime == 4 && !this.level().isClientSide) {
                if (this.additionalTargets.size() > 2) {
                    this.shootProjectileAtTarget(this.additionalTargets.get(2));
                }
            }
            
            else if (this.shootAnimationTime <= 0) {
                this.entityData.set(DATA_SHOOTING, false);
            }
        }

        
        this.oSquish = this.squish;

        boolean isOnGround = this.onGround();

        
        if (isOnGround && !this.wasOnGround) {
            
            this.targetSquish = -0.5F;
        } else if (!isOnGround && this.wasOnGround) {
            
            this.targetSquish = 1.0F;
        }

        this.targetSquish *= 0.6F; 
        this.squish += (this.targetSquish - this.squish) * 0.5F; 

        super.tick();

        
        isOnGround = this.onGround();

        
        if (isOnGround && !this.wasOnGround) {
            
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                int particleCount = this.currentSize * 4;
                for (int i = 0; i < particleCount; i++) {
                    double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                    double d1 = this.getY();
                    double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                    serverLevel.sendParticles(
                            new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get().getDefaultInstance()),
                            d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.0D
                    );
                }
            }
            
            this.playSound(getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
        }
        
        this.wasOnGround = isOnGround;
        
        updateFloating();
    }

    
    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
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
        fakeDeathTimer = 10; 
        deathPosition = this.blockPosition(); 

        
        this.setHealth(0.02F);

        
        this.setNoAi(true);

        
        this.setInvulnerable(true);

        
        this.setTarget(null);

        this.setPose(Pose.DYING); 

        
        this.entityData.set(DATA_IS_FAKING_DEATH, true);
    }

    @Override
    public void die(DamageSource source) {
        
        if (isFakingDeath()) {
            super.die(source);
            return;
        }

        
        if (this.isOnFire()) {
            
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                int spawnCount = 2 + this.random.nextInt(3); 
                for (int i = 0; i < spawnCount; i++) {
                    InfestedSlimeSize1 mediumSlime = InfestedSlimeSize1.create(this.level());
                    if (mediumSlime != null) {
                        
                        double offsetX = (this.random.nextDouble() - 0.5D) * 3.0D;
                        double offsetZ = (this.random.nextDouble() - 0.5D) * 3.0D;
                        mediumSlime.setPos(this.getX() + offsetX, this.getY() + 0.5D, this.getZ() + offsetZ);

                        
                        mediumSlime.setSecondsOnFire(this.getRemainingFireTicks() / 20);

                        this.level().addFreshEntity(mediumSlime);
                    }
                }
            }
            super.die(source);
            return;
        }

        
        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < 1.0f) {
            triggerFakeDeath(source);
        } else {
            
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                int spawnCount = 2 + this.random.nextInt(3); 
                for (int i = 0; i < spawnCount; i++) {
                    InfestedSlimeSize1 mediumSlime = InfestedSlimeSize1.create(this.level());
                    if (mediumSlime != null) {
                        
                        double offsetX = (this.random.nextDouble() - 0.5D) * 3.0D;
                        double offsetZ = (this.random.nextDouble() - 0.5D) * 3.0D;
                        mediumSlime.setPos(this.getX() + offsetX, this.getY() + 0.5D, this.getZ() + offsetZ);
                        this.level().addFreshEntity(mediumSlime);
                    }
                }
            }
            
            super.die(source);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        
        if (isFakingDeath() || isInvulnerable()) {
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



    
    static class SlimeRangedAttackGoal extends Goal {
        private final InfestedSlimeSize3 slime;
        private int seeTime;
        private final int attackInterval = 40; 

        public SlimeRangedAttackGoal(InfestedSlimeSize3 slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.slime.getTarget();
            return target != null && target.isAlive() && this.slime.rangedAttackCooldown <= 0;
        }

        @Override
        public void start() {
            this.seeTime = 0;
        }

        @Override
        public void stop() {
            this.slime.setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity target = this.slime.getTarget();
            if (target == null) return;

            
            boolean canSee = this.slime.getSensing().hasLineOfSight(target);
            if (canSee) {
                this.seeTime++;
            } else {
                this.seeTime = 0;
            }

            
            this.slime.getLookControl().setLookAt(target, 30.0F, 30.0F);

            
            this.slime.getNavigation().stop();
            this.slime.xxa = 0.0F;
            this.slime.zza = 0.0F;

            
            if (this.seeTime >= 20) {
                this.slime.setAggressive(true);
                this.startRangedAttack(target);
            }
        }

        private void startRangedAttack(LivingEntity target) {
            
            this.slime.rangedAttackCooldown = this.attackInterval;

            
            this.slime.primaryTarget = target;

            
            this.slime.additionalTargets = this.findAdditionalTargets(target);

            
            this.slime.shootAnimationTime = 20;
            this.slime.entityData.set(InfestedSlimeSize3.DATA_SHOOTING, true);
            this.slime.swinging = true;

            
            this.slime.projectileCount = 0;
        }

        private List<LivingEntity> findAdditionalTargets(LivingEntity primaryTarget) {
            
            List<LivingEntity> nearbyEnemies = this.slime.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.slime.getBoundingBox().inflate(16.0D), 
                    entity -> entity != null &&
                            entity.isAlive() &&
                            entity != primaryTarget &&
                            entity != this.slime &&
                            !IParasite.isParasiteByTagOrInterface(entity) &&
                            !(entity instanceof Creeper) &&
                            !(entity instanceof Player && (((Player) entity).isCreative() || ((Player) entity).isSpectator())) &&
                            this.slime.getSensing().hasLineOfSight(entity)
            );

            
            nearbyEnemies.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(this.slime)));

            
            int enemyCount = nearbyEnemies.size() + 1; 

            if (enemyCount == 1) {
                
                return List.of(primaryTarget, primaryTarget, primaryTarget);
            } else if (enemyCount == 2) {
                
                LivingEntity secondTarget = nearbyEnemies.isEmpty() ? primaryTarget : nearbyEnemies.get(0);
                return List.of(primaryTarget, primaryTarget, secondTarget);
            } else {
                
                LivingEntity secondTarget = nearbyEnemies.size() > 0 ? nearbyEnemies.get(0) : primaryTarget;
                LivingEntity thirdTarget = nearbyEnemies.size() > 1 ? nearbyEnemies.get(1) : primaryTarget;
                return List.of(primaryTarget, secondTarget, thirdTarget);
            }
        }
    }

    private List<LivingEntity> findAdditionalTargets(LivingEntity primaryTarget) {
        
        List<LivingEntity> nearbyEnemies = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(16.0D), 
                entity -> entity != null &&
                        entity.isAlive() &&
                        entity != primaryTarget &&
                        !IParasite.isParasiteByTagOrInterface(entity) &&
                        !(entity instanceof Creeper) &&
                        !(entity instanceof Player && (((Player) entity).isCreative() || ((Player) entity).isSpectator())) &&
                        this.getSensing().hasLineOfSight(entity)
        );

        
        nearbyEnemies.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(this)));

        
        int enemyCount = nearbyEnemies.size() + 1; 

        if (enemyCount == 1) {
            
            return List.of(primaryTarget, primaryTarget, primaryTarget);
        } else if (enemyCount == 2) {
            
            LivingEntity secondTarget = nearbyEnemies.isEmpty() ? primaryTarget : nearbyEnemies.get(0);
            return List.of(primaryTarget, primaryTarget, secondTarget);
        } else {
            
            LivingEntity secondTarget = nearbyEnemies.size() > 0 ? nearbyEnemies.get(0) : primaryTarget;
            LivingEntity thirdTarget = nearbyEnemies.size() > 1 ? nearbyEnemies.get(1) : primaryTarget;
            return List.of(primaryTarget, secondTarget, thirdTarget);
        }
    }

    
    private void shootProjectileAtTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) return;

        
        if (target instanceof Player) {
            Player player = (Player) target;
            if (player.isCreative() || player.isSpectator()) {
                return; 
            }
        }

        
        SlimeProjectile projectile = new SlimeProjectile(
                (EntityType<? extends ThrowableProjectile>) ModEntities.SLIME_PROJECTILE.get(),
                this,
                this.level()
        );

        
        Vec3 shootPos = this.position().add(0, this.getEyeHeight() * 0.5, 0);
        projectile.setPos(shootPos.x, shootPos.y, shootPos.z);

        
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);

        
        double dx = targetPos.x - shootPos.x;
        double dz = targetPos.z - shootPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        
        double dy = targetPos.y - shootPos.y;

        
        double gravity = 0.05; 
        double minSpeed = 0.6; 
        double maxSpeed = 1.2; 

        
        double baseSpeed = Math.min(maxSpeed, Math.max(minSpeed, horizontalDistance * 0.1));

        
        double angle = calculateLaunchAngle(horizontalDistance, dy, gravity, baseSpeed);

        
        if (Double.isNaN(angle)) {
            angle = Math.PI / 4; 
        }

        
        Vec3 horizontalDir = new Vec3(dx, 0, dz).normalize();

        
        double speedX = baseSpeed * Math.cos(angle) * horizontalDir.x;
        double speedY = baseSpeed * Math.sin(angle);
        double speedZ = baseSpeed * Math.cos(angle) * horizontalDir.z;

        Vec3 velocity = new Vec3(speedX, speedY, speedZ);

        
        double spread = 0.05D;
        velocity = velocity.add(
                (this.random.nextDouble() - 0.5) * spread,
                (this.random.nextDouble() - 0.5) * spread * 0.5,
                (this.random.nextDouble() - 0.5) * spread
        );

        
        projectile.shoot(velocity.x, velocity.y, velocity.z, (float) baseSpeed, 1.0F);

        
        projectile.setTarget(target); 
        this.level().addFreshEntity(projectile);

        
        this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
    }

    
    private double calculateLaunchAngle(double distance, double heightDiff, double gravity, double speed) {
        
        

        double speedSquared = speed * speed;
        double discriminant = speedSquared * speedSquared - gravity * (gravity * distance * distance + 2 * heightDiff * speedSquared);

        
        if (discriminant < 0) {
            return Double.NaN;
        }

        
        double sqrtDiscriminant = Math.sqrt(discriminant);
        double angle1 = Math.atan((speedSquared + sqrtDiscriminant) / (gravity * distance));
        double angle2 = Math.atan((speedSquared - sqrtDiscriminant) / (gravity * distance));

        
        return Math.max(angle1, angle2);
    }

    
    static class SlimeMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final InfestedSlimeSize3 slime;
        private boolean isAggressive;

        public SlimeMoveControl(InfestedSlimeSize3 slime) {
            super(slime);
            this.slime = slime;
            this.yRot = 180.0F * slime.getYRot() / (float)Math.PI;
        }

        public void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.isAggressive = aggressive;
        }

        public void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            
            if (this.slime.shootAnimationTime > 0) {
                this.mob.setZza(0.0F);
                return;
            }

            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
                return;
            }

            this.operation = MoveControl.Operation.WAIT;

            if (this.mob.onGround()) {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = this.slime.getJumpDelay();

                    if (this.isAggressive) {
                        this.jumpDelay /= 3;
                    }

                    ((InfestedSlimeSize3)this.mob).getJumpControl().jump();

                    
                    this.slime.playSound(this.slime.getJumpSound(), this.slime.getSoundVolume(),
                            ((this.slime.getRandom().nextFloat() - this.slime.getRandom().nextFloat()) * 0.2F + 1.0F) * 0.8F);
                } else {
                    this.slime.xxa = 0.0F;
                    this.slime.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            } else {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }

    protected int getJumpDelay() {
        SizeAttributes attributes = getCurrentSizeAttributes();
        return this.random.nextInt(attributes.maxJumpDelay - attributes.minJumpDelay + 1) + attributes.minJumpDelay;
    }

    @Override
    protected void jumpFromGround() {
        Vec3 vec3 = this.getDeltaMovement();
        SizeAttributes attributes = getCurrentSizeAttributes();

        
        this.setDeltaMovement(vec3.x * attributes.horizontalMultiplier, attributes.jumpPower, vec3.z * attributes.horizontalMultiplier);
        this.hasImpulse = true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        
        if (this.shootAnimationTime > 0) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            this.xxa = 0.0F;
            this.zza = 0.0F;
        }

        
        if (!this.onGround()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.x * 0.95D, vec3.y, vec3.z * 0.95D);
        }

        
        if (this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() < 0.01D) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }

        
        if (!this.level().isClientSide && this.attackCooldown <= 0) {
            this.checkContactDamage();
        }
    }

    
    private void checkContactDamage() {
        
        AABB collisionBox = this.getBoundingBox().inflate(0.0D); 
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, collisionBox)) {
            
            if (target == this || IParasite.isParasiteByTagOrInterface(target)) {
                continue;
            }

            
            if (this.getBoundingBox().intersects(target.getBoundingBox())) {
                
                DamageSource magicDamage = this.damageSources().magic();
                boolean hurt = target.hurt(magicDamage, 6.0F);

                if (hurt) {
                    
                    MobEffectInstance solidifyEffect = new MobEffectInstance(
                            ModEffects.SOLIDIFY.get(), 
                            40, 
                            0,  
                            false, 
                            true  
                    );
                    target.addEffect(solidifyEffect);

                    MobEffectInstance fearEffect = new MobEffectInstance(
                            ModEffects.FEAR.get(), 
                            400, 
                            0,  
                            false, 
                            true  
                    );
                    target.addEffect(fearEffect);

                    
                    this.attackCooldown = 10;

                    
                    this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, this.getVoicePitch());

                    break; 
                }
            }
        }
    }

    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    protected SoundEvent getJumpSound() {
        return SoundEvents.SLIME_JUMP;
    }

    protected SoundEvent getSquishSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F; 
    }

    @Override
    public float getVoicePitch() {
        return 0.6F; 
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        SizeAttributes attributes = getCurrentSizeAttributes();
        return super.getDimensions(pose).scale(attributes.scale);
    }

    public int getCurrentSize() {
        return currentSize;
    }

    
    public float getSquish() {
        return this.squish;
    }

    public float getoSquish() {
        return this.oSquish;
    }

    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::playState));
    }

    private PlayState playState(AnimationState<InfestedSlimeSize3> event) {
        String sizeSuffix = getSizeSuffix();

        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("dead" + sizeSuffix));
        } else if (!this.onGround() || this.getDeltaMovement().y > 0.0D) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("jump" + sizeSuffix));
        } else if (this.entityData.get(DATA_SHOOTING) || this.shootAnimationTime > 0) {
            
            event.getController().setAnimation(RawAnimation.begin().thenPlay("shoot" + sizeSuffix));
        } else if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle" + sizeSuffix));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle" + sizeSuffix));
        }

        return PlayState.CONTINUE;
    }

    private String getSizeSuffix() {
        return "_size3"; 
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        this.entityData.define(DATA_SHOOTING, false);
    }

    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Size", this.currentSize);
        compound.putInt("RangedAttackCooldown", this.rangedAttackCooldown);
        compound.putBoolean("Shooting", this.entityData.get(DATA_SHOOTING));
        compound.putInt("RangedAttackCooldown", this.rangedAttackCooldown);
        compound.putInt("ShootAnimationTime", this.shootAnimationTime);
        compound.putBoolean("FakingDeath", this.entityData.get(DATA_IS_FAKING_DEATH));
        compound.putInt("FakeDeathTimer", this.fakeDeathTimer);
        compound.putBoolean("Invulnerable", this.entityData.get(DATA_IS_INVULNERABLE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        
        this.currentSize = 3;
        this.rangedAttackCooldown = compound.getInt("RangedAttackCooldown");
        this.entityData.set(DATA_SHOOTING, compound.getBoolean("Shooting"));
        this.rangedAttackCooldown = compound.getInt("RangedAttackCooldown");
        this.shootAnimationTime = compound.getInt("ShootAnimationTime");
        this.entityData.set(DATA_IS_FAKING_DEATH, compound.getBoolean("FakingDeath"));
        this.fakeDeathTimer = compound.getInt("FakeDeathTimer");
        this.entityData.set(DATA_IS_INVULNERABLE, compound.getBoolean("Invulnerable"));
    }

    public void setSize(int size) {
        
    }

    
    public static InfestedSlimeSize3 create(Level level) {
        return new InfestedSlimeSize3((EntityType<? extends PathfinderMob>) ModEntities.INFESTED_SLIME_SIZE3.get(), level);
    }

    
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
    }

    
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
    }

    public static boolean checkInfestedSlimeSize3SpawnRules(
            EntityType<InfestedSlimeSize3> entityType,
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

        
        return level.getMaxLocalRawBrightness(pos) < 8;
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