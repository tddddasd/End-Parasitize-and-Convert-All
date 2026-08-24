package org.tdddd.epca.impl.overworld.registry.entities.entity.reshape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.blocks.block.SwallowCyst;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.IReshape;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.AcidBullet;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReshapeYelloweye extends PathfinderMob implements GeoEntity, IParasite, IReshape, Enemy {
    
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_EXPLODING = SynchedEntityData.defineId(ReshapeYelloweye.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_BOOMING  = SynchedEntityData.defineId(ReshapeYelloweye.class, EntityDataSerializers.BOOLEAN);
    
    public static final EntityDataAccessor<Integer> DATA_SHOOT_ANIM_TIMER = SynchedEntityData.defineId(ReshapeYelloweye.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_GASSING = SynchedEntityData.defineId(ReshapeYelloweye.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_GASSING_TIMER =
            SynchedEntityData.defineId(ReshapeYelloweye.class, EntityDataSerializers.INT);
    
    private int explosionTimer = 24;
    private BlockPos deathPosition;

    
    private int boomAnimationTimer = 0;
    private boolean isBoomTriggered = false;

    
    private int shootCooldown = 0;          
    private int shootDelayTimer = 0;        

    
    private boolean isRetreating = false;
    private int retreatCooldown = 0;
    private Vec3 retreatDirection = Vec3.ZERO;
    private static final double SAFE_DISTANCE = 18.0D;            
    private static final int RETREAT_COOLDOWN = 5;               
    private static final double RETREAT_SPEED = 0.4D;            

    
    private int floorCheckCooldown = 0;      
    private double floorY = Double.NEGATIVE_INFINITY;  

    
    private int touchCooldown = 0;
    private static final int TOUCH_COOLDOWN_MAX = 10; 

    private int gassingTimer = 0;          
    private int gassingCooldown = 0;       
    public static final int GASSING_DURATION = 30;      
    private static final int GASSING_COOLDOWN_MAX = 160; 
    private boolean hasGassingPush = false; 
    
    private List<BlockPos> blocksToDestroy = new ArrayList<>();
    private int destructionIndex = 0;
    private int blockBreakTimer = 0;
    public ReshapeYelloweye(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);   
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .build();
    }

    
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 48.0D));

        this.goalSelector.addGoal(7, new RandomSoundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_EXPLODING, false);
        this.entityData.define(DATA_IS_BOOMING, false);
        this.entityData.define(DATA_SHOOT_ANIM_TIMER, 0);
        this.entityData.define(DATA_IS_GASSING, false);
        this.entityData.define(DATA_GASSING_TIMER, 0);
    }

    
    public boolean isExploding() {
        return this.entityData.get(DATA_IS_EXPLODING);
    }
    private void setExploding(boolean exploding) {
        this.entityData.set(DATA_IS_EXPLODING, exploding);
    }
    private void triggerExplosionDeath(DamageSource source) {
        setExploding(true);
        explosionTimer = 24;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.entityData.set(DATA_IS_BOOMING, false);
    }
    private void executeExplosionEffects() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // 原有爆炸音效和粒子
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.BIG_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

            serverLevel.sendParticles(ModParticles.COTH.get(), this.getX(), this.getY() + 0.5, this.getZ(),
                    10, 0.3, 0.45, 0.3, 0.0);
            serverLevel.sendParticles(ModParticles.SPLASHI.get(), this.getX(), this.getY() + 0.5, this.getZ(),
                    7, 0.4, 0.3, 0.4, 0.2);

            BlockPos deathPos = this.deathPosition;
            long seed = this.random.nextLong();
            int delay = this.random.nextInt(30) + 40;
            serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + delay,
                    () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))));

            AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
            cloud.setRadius(1.5F);
            cloud.setDuration(60);
            cloud.setRadiusPerTick(0);
            cloud.setWaitTime(0);
            cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 1, false, true));
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
            serverLevel.addFreshEntity(cloud);

            // ========== 新增：向下斜 50° 内随机发射 4~5 枚酸液弹 ==========
            int count = 4 + this.random.nextInt(2); // 4 或 5
            Vec3 origin = this.position().add(0, 0.5, 0); // 发射位置（略高于身体中心）
            for (int i = 0; i < count; i++) {
                // 随机水平偏转角（0~360°）
                float yaw = this.random.nextFloat() * 2 * (float) Math.PI;
                // 随机俯仰角：范围 -90°（垂直向下） ~ -40°（与垂直向下夹角 50°）
                float pitchDeg = -90 + this.random.nextFloat() * 50; // [-90, -40]
                float pitch = (float) Math.toRadians(pitchDeg);
                // 计算方向向量
                Vec3 direction = new Vec3(
                        Math.cos(pitch) * Math.sin(yaw),
                        Math.sin(pitch),
                        Math.cos(pitch) * Math.cos(yaw)
                ).normalize();

                AcidBullet bullet = ModEntities.ACID_BULLET.get().create(serverLevel);
                if (bullet != null) {
                    bullet.setOwner(this);
                    bullet.setPos(origin.x, origin.y, origin.z);
                    bullet.shoot(direction.x, direction.y, direction.z, 1.5F, 0.5F);
                    serverLevel.addFreshEntity(bullet);
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
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)) return false;
        if (isExploding()) return false;
        if (source.getEntity() instanceof LivingEntity attacker && shouldIgnoreDamageFrom(attacker)) return false;
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        
        float finalAmount = amount;
        if (!this.level().isClientSide && source.getEntity() instanceof Player player) {
            Vec3 attackerPos = source.getSourcePosition(); 
            if (attackerPos != null) {
                
                double bottomThreshold = this.getY() + this.getBbHeight() * 0.25;
                if (attackerPos.y < bottomThreshold) {
                    finalAmount = amount * 2.0f;
                }
            }
        }

        float adjustedAmount = ((IParasite) this).onHurt(source, finalAmount);
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
            handleInventoryOnDeath();
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
    public void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) { }

    
    @Override
    public void tick() {
        super.tick();
        
        if (blockBreakTimer > 0) blockBreakTimer--;
        if (!isExploding() && blockBreakTimer <= 0) {
            handleBlockBreaking();
        }
        if (!blocksToDestroy.isEmpty() && blockBreakTimer <= 0 && !isExploding()) {
            destroyNextBlock();
        }

        
        if (this.level().isClientSide) {
            
            if (this.entityData.get(DATA_IS_BOOMING)) {
                if (clientBoomTimer <= 0) clientBoomTimer = 20;
                if (clientBoomTimer > 0) clientBoomTimer--;
            } else clientBoomTimer = 0;
        }

        
        if (isExploding()) {
            explosionTimer--;
            if (explosionTimer <= 0) {
                if (!this.level().isClientSide) executeExplosionEffects();
                this.discard();
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
                if (!this.level().isClientSide) triggerExplosionDeath(null);
            }
            return;
        }

        
        if (!this.level().isClientSide) {
            
            if (touchCooldown > 0) touchCooldown--;
            if (touchCooldown == 0) {
                AABB bottomBox = this.getBoundingBox().contract(0.2, 0, 0.2) 
                        .move(0, -0.2, 0); 
                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, bottomBox,
                        e -> e != this && !IParasite.isParasiteByTagOrInterface(e) && e.isAlive());
                if (!entities.isEmpty()) {
                    
                    for (LivingEntity e : entities) {
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                        e.addEffect(new MobEffectInstance(ModEffects.CORROSIVE.get(), 60, 0));
                    }
                    touchCooldown = TOUCH_COOLDOWN_MAX;
                }
            }

            
            if (shootCooldown > 0) shootCooldown--;
            if (shootDelayTimer > 0) {
                shootDelayTimer--;
                
                if (shootDelayTimer == 0) {
                    LivingEntity target = this.getTarget();
                    if (target != null && target.isAlive()) {
                        
                        if (target.getY() <= this.getY() + 0.5) {
                            shootAcidBullet(target);
                        }
                    }
                }
            }

            
            if (gassingCooldown > 0) gassingCooldown--;

            
            if (!isExploding() && boomAnimationTimer == 0 && !isGassing() && gassingCooldown == 0) {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    double dx = target.getX() - this.getX();
                    double dz = target.getZ() - this.getZ();
                    double distXZ = Math.sqrt(dx * dx + dz * dz);
                    boolean farHorizontal = distXZ >= 24.0;
                    boolean targetAbove = target.getY() > this.getY();
                    if (farHorizontal || targetAbove) {
                        startGassing();
                    }
                }
            }

            
            if (isGassing()) {
                if (gassingTimer > 0) {
                    gassingTimer--;
                    this.entityData.set(DATA_GASSING_TIMER, gassingTimer);
                    
                    if (!hasGassingPush && gassingTimer <= GASSING_DURATION - 10) {
                        applyGassingPush();
                        hasGassingPush = true;
                    }

                    
                    if (this.level() instanceof ServerLevel serverLevel) {
                        if (gassingTimer == GASSING_DURATION - 10) {
                            
                            spawnGassingParticleLine(serverLevel);
                        } else if (gassingTimer < GASSING_DURATION - 10) {
                            
                            if ((GASSING_DURATION - gassingTimer) % 2 == 0) {
                                spawnSingleGassingParticle(serverLevel);
                            }
                        }
                    }

                    
                    applyGassingAreaEffect();

                    
                    if (gassingTimer <= 0) {
                        stopGassing();
                    }
                } else {
                    
                    stopGassing();
                }
            }

            
            enforceMaxHeight();

            
            if (this.isInWater()) {
                Vec3 mot = this.getDeltaMovement();
                this.setDeltaMovement(mot.x * 0.75, mot.y, mot.z * 0.75);
            }

            LivingEntity target = this.getTarget();

            
            if (target != null && target.isAlive()) {
                double distance = this.distanceTo(target);
                
                boolean canShoot = shootCooldown == 0 && shootDelayTimer == 0 &&
                        target.getY() <= this.getY() + 0.5;   

                if (canShoot) {
                    
                    this.entityData.set(DATA_SHOOT_ANIM_TIMER, 10);   
                    shootDelayTimer = 5;      
                    shootCooldown = 120;      
                }

                if (retreatCooldown > 0) retreatCooldown--;

                double currentSafeDistance = SAFE_DISTANCE;


                boolean isBelow = this.getY() < target.getY();

                boolean yClose = (this.getY() + this.getBbHeight()) >= (target.getY() - 1.0);


                if (isBelow && !yClose) {
                    double targetSize = target.getBbWidth();   
                    double dynamicDist = Math.max(targetSize * 2.0, 3.0);
                    currentSafeDistance = dynamicDist;         
                }

                if (distance < currentSafeDistance) {
                    if (retreatCooldown <= 0) {
                        startRetreating(target);
                        retreatCooldown = RETREAT_COOLDOWN;
                    }
                    if (isRetreating) updateRetreatMovement();
                } else {
                    if (isRetreating && distance >= currentSafeDistance + 3.0) stopRetreating();
                    if (!isRetreating) {
                        double targetY = target.getY() + 7.0;
                        double maxAllowed = getFloorY() + 48.0;
                        if (targetY > maxAllowed) targetY = maxAllowed - 1.0;
                        if (distance > 10.0) {
                            this.getNavigation().moveTo(target.getX(), targetY, target.getZ(), 1.0);
                        } else {
                            this.getNavigation().moveTo(target, 1.0);
                        }
                    }
                }
            }
        }

        
        if (this.level().isClientSide) {
            int anim = this.entityData.get(DATA_SHOOT_ANIM_TIMER);
            if (anim > 0) this.entityData.set(DATA_SHOOT_ANIM_TIMER, anim - 1);
        }
    }

    
    public boolean isGassing() {
        return this.entityData.get(DATA_IS_GASSING);
    }

    private void setGassing(boolean gassing) {
        this.entityData.set(DATA_IS_GASSING, gassing);
    }

    private void startGassing() {
        if (this.level().isClientSide) return;
        setGassing(true);
        gassingTimer = GASSING_DURATION;
        hasGassingPush = false;
        gassingCooldown = GASSING_COOLDOWN_MAX;
        
        this.entityData.set(DATA_GASSING_TIMER, gassingTimer);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.RESHAPE_YELLOWEYE_GASSING.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void stopGassing() {
        setGassing(false);
        gassingTimer = 0;
        hasGassingPush = false;
        this.entityData.set(DATA_GASSING_TIMER, 0);
    }

    
    private void applyGassingPush() {
        Vec3 lookVec = this.getLookAngle();
        Vec3 push = new Vec3(lookVec.x, 0.6, lookVec.z).normalize().scale(0.3); 
        push = push.add(0, 0.15, 0); 
        this.setDeltaMovement(this.getDeltaMovement().add(push));
        this.hasImpulse = true;
    }

    
    private void spawnGassingParticleLine(ServerLevel level) {
        Vec3 center = this.position().add(0, this.getBbHeight() * 0.5, 0);
        Vec3 rightOffset = new Vec3(0.6, -0.3, 0.4);   
        Vec3 leftOffset = new Vec3(-0.6, -0.3, 0.4);   
        for (int i = 0; i < 3; i++) {
            double t = i * 0.4; 
            Vec3 rightPos = center.add(rightOffset.scale(t));
            Vec3 leftPos = center.add(leftOffset.scale(t));
            level.sendParticles(ModParticles.INFESTIVE_GAS.get(), rightPos.x, rightPos.y, rightPos.z,
                    1, 0, 0, 0, 0);
            level.sendParticles(ModParticles.INFESTIVE_GAS.get(), leftPos.x, leftPos.y, leftPos.z,
                    1, 0, 0, 0, 0);
        }
    }

    
    private void spawnSingleGassingParticle(ServerLevel level) {
        boolean side = (gassingTimer % 4) < 2; 
        Vec3 center = this.position().add(0, this.getBbHeight() * 0.5, 0);
        Vec3 offset = side ? new Vec3(0.6, -0.3, 0.4) : new Vec3(-0.6, -0.3, 0.4);
        Vec3 pos = center.add(offset);
        level.sendParticles(ModParticles.INFESTIVE_GAS.get(), pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0);
    }

    
    private void applyGassingAreaEffect() {
        AABB effectBox = this.getBoundingBox().inflate(0, -this.getBbHeight() + 2.0, 0)
                .move(0, -this.getBbHeight() * 0.5, 0);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, effectBox,
                e -> e.isAlive());
        for (LivingEntity e : entities) {
            if (IParasite.isParasiteByTagOrInterface(e)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0)); 
            } else if (e != this) {
                e.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 2));     
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));       
            }
        }
    }

    
    private static final Vec3 ACID_CORE_OFFSET = new Vec3(0.0D, 0.8D, 0.8D);

    
    private void shootAcidBullet(LivingEntity target) {
        if (this.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) this.level();
        Vec3 shootPos = getAcidCoreWorldPosition();
        Vec3 toTarget = target.getEyePosition().subtract(shootPos).normalize();

        
        AcidBullet bullet = ModEntities.ACID_BULLET.get().create(serverLevel);
        if (bullet != null) {
            bullet.setOwner(this);
            bullet.setPos(shootPos.x, shootPos.y, shootPos.z);
            bullet.shoot(toTarget.x, toTarget.y, toTarget.z, 1.5F, 0.5F);
            serverLevel.addFreshEntity(bullet);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.RESHAPE_YELLOWEYE_ATTACK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private Vec3 getAcidCoreWorldPosition() {
        Vec3 worldOffset = ACID_CORE_OFFSET.yRot((float) Math.toRadians(-this.getYRot()));
        return this.position().add(worldOffset);
    }

    
    private double getFloorY() {
        if (floorCheckCooldown-- <= 0) {
            floorCheckCooldown = 10;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = (int) this.getY(); y > this.getY() - 64; y--) {
                pos.set(this.getX(), y, this.getZ());
                if (!this.level().isEmptyBlock(pos) && !this.level().getBlockState(pos).isAir()) {
                    floorY = y + 1.0;  
                    break;
                }
            }
            if (floorY == Double.NEGATIVE_INFINITY) floorY = this.getY();
        }
        return floorY;
    }

    
    private void enforceMaxHeight() {
        double floor = getFloorY();
        double maxY = floor + 48.0;
        if (this.getY() > maxY) {
            
            this.setPos(this.getX(), maxY, this.getZ());
            Vec3 mot = this.getDeltaMovement();
            if (mot.y > 0) this.setDeltaMovement(mot.x, 0, mot.z);
        }
    }

    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.RESHAPE_YELLOWEYE_HURT.get();
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.RESHAPE_YELLOWEYE_HURT.get();
    }

    
    private int clientBoomTimer = 0;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<ReshapeYelloweye> event) {
        AnimationController<ReshapeYelloweye> controller = event.getController();
        ReshapeYelloweye entity = event.getAnimatable();

        if (entity.isExploding()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("dead"));
        } else if (entity.isMoving()) {
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
    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }
    
    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) return false;
        return super.startRiding(vehicle, force);
    }
    @Override
    protected boolean canRide(Entity entity) {
        if (entity instanceof Boat || entity instanceof Minecart) return false;
        return super.canRide(entity);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) IParasite.super.onKillEntity(killedEntity);
    }

    
    private static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide || deathPos == null) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_LARGE.get().defaultBlockState(), 1);
        int medium = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_MEDIUM.get().defaultBlockState(), medium);
        int small = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand, ModBlocks.INFESTED_REMAINS_SMALL.get().defaultBlockState(), small);
    }
    private static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos, RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offX = (rand.nextDouble() - 0.5) * 5;
            double offZ = (rand.nextDouble() - 0.5) * 5;
            int offY = -rand.nextInt(3);
            pos.set(deathPos.getX() + offX, deathPos.getY() + 1 + offY, deathPos.getZ() + offZ);
            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP))
                    level.setBlock(pos, state, 3);
            }
        }
    }

    
    static class RandomSoundGoal extends Goal {
        private final ReshapeYelloweye mob;
        private int nextSoundTick;
        RandomSoundGoal(ReshapeYelloweye mob) {
            this.mob = mob;
        }
        @Override
        public boolean canUse() {
            return mob.isAlive() && !mob.isAggressive();
        }
        @Override
        public void start() {
            this.nextSoundTick = mob.getRandom().nextInt(120) + 80;
        }
        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                mob.playSound(ModSoundEvents.RESHAPE_YELLOWEYE_IDLE.get());
                this.nextSoundTick = mob.getRandom().nextInt(120) + 80;
            }
        }
    }

    
    public static boolean checkReshapeYelloweyeSpawnRules(EntityType<ReshapeYelloweye> type,
                                                          ServerLevelAccessor levelAccessor, MobSpawnType spawnType,
                                                          BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            return stage >= 4 && stage <= 6;
        }
        return levelAccessor.getMaxLocalRawBrightness(pos) < 8;
    }

    
    private final ItemStackHandler inventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {}
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    private ItemStack addItemToInventory(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = insertItemIntoInventory(stack);
        if (remaining.isEmpty()) return ItemStack.EMPTY;
        if (tryPlaceCystAndTransfer()) {
            remaining = insertItemIntoInventory(stack);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        if (!level().isClientSide) {
            Containers.dropItemStack(level(), getX(), getY(), getZ(), remaining);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack insertItemIntoInventory(ItemStack stack) {
        ItemStack copy = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) {
            copy = inventory.insertItem(i, copy, false);
            if (copy.isEmpty()) break;
        }
        return copy;
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return false;
        }
        return true;
    }

    private boolean tryPlaceCystAndTransfer() {
        if (level().isClientSide) return false;
        
        boolean hasItems = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) return false;

        ServerLevel serverLevel = (ServerLevel) level();
        BlockPos cystPos = findPlacementPosition();
        if (cystPos == null) return false;

        BlockState cystState = ModBlocks.SWALLOW_CYST.get().defaultBlockState().setValue(SwallowCyst.LIVING, true);
        if (!serverLevel.setBlock(cystPos, cystState, 3)) return false;

        if (serverLevel.getBlockEntity(cystPos) instanceof SwallowCystBlockEntity cystBE) {
            ItemStackHandler cystInventory = cystBE.getInventory();
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack remaining = stack.copy();
                    for (int j = 0; j < cystInventory.getSlots(); j++) {
                        remaining = cystInventory.insertItem(j, remaining, false);
                        if (remaining.isEmpty()) break;
                    }
                    if (!remaining.isEmpty()) {
                        inventory.setStackInSlot(i, remaining);
                    } else {
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private BlockPos findPlacementPosition() {
        BlockPos entityPos = blockPosition();
        int radius = 6, verticalRange = 8;
        for (int dy = -verticalRange; dy <= verticalRange; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = entityPos.offset(dx, dy, dz);
                    BlockPos below = candidate.below();
                    BlockState belowState = level().getBlockState(below);
                    BlockState topState = level().getBlockState(candidate);
                    if (belowState.isCollisionShapeFullBlock(level(), below) && (topState.isAir() || topState.canBeReplaced())) {
                        if (!getBoundingBox().intersects(new AABB(candidate))) return candidate;
                    }
                }
            }
        }
        BlockPos belowEntity = blockPosition().below();
        if (level().getBlockState(belowEntity).isCollisionShapeFullBlock(level(), belowEntity) && level().isEmptyBlock(belowEntity.above()))
            return belowEntity.above();
        return null;
    }

    private void handleInventoryOnDeath() {
        if (!level().isClientSide) {
            if (!tryPlaceCystAndTransfer()) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level(), getX(), getY(), getZ(), stack);
                    }
                }
            }
        }
    }

    private void destroyNextBlock() {
        if (destructionIndex >= blocksToDestroy.size()) {
            blocksToDestroy.clear();
            destructionIndex = 0;
            blockBreakTimer = 10;
            return;
        }
        BlockPos pos = blocksToDestroy.get(destructionIndex);
        BlockState state = level().getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced() && isBreakableBlock(pos)) {
            List<ItemStack> drops = getDropsWithoutTool(state, pos);
            boolean destroyed = level().destroyBlock(pos, false, this);
            if (destroyed) {
                if (state.getSoundType() != null) {
                    level().playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                for (ItemStack drop : drops) {
                    addItemToInventory(drop);
                }
            }
        }
        destructionIndex++;
    }

    private boolean isBreakableBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) return false;
        
        float hardness = state.getDestroySpeed(level(), pos);
        return hardness >= 0 && hardness <= 1.0f;
    }

    private List<ItemStack> getDropsWithoutTool(BlockState state, BlockPos pos) {
        if (level() instanceof ServerLevel serverLevel) {
            BlockEntity be = level().getBlockEntity(pos);
            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY) 
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, be);
            return state.getDrops(builder);
        }
        return List.of();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    private void handleBlockBreaking() {
        if (this.getTarget() == null) return;
        blocksToDestroy.clear();
        destructionIndex = 0;
        Set<BlockPos> toDestroy = new HashSet<>();

        AABB aabb = this.getBoundingBox();
        int minX = (int) Math.floor(aabb.minX), maxX = (int) Math.ceil(aabb.maxX);
        int minZ = (int) Math.floor(aabb.minZ), maxZ = (int) Math.ceil(aabb.maxZ);
        int minY = (int) Math.floor(aabb.minY), maxY = (int) Math.ceil(aabb.maxY);

        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    
                    
                    if (y == minY) continue; 
                    if (isBreakableBlock(pos)) {
                        toDestroy.add(pos);
                    }
                }
            }
        }

        
        int xEast = (int) Math.ceil(aabb.maxX);
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(xEast, y, z);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        }
        int xWest = (int) Math.floor(aabb.minX) - 1;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(xWest, y, z);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        }
        int zSouth = (int) Math.ceil(aabb.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(x, y, zSouth);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        }
        int zNorth = (int) Math.floor(aabb.minZ) - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(x, y, zNorth);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        }

        
        int topY = (int) Math.ceil(aabb.maxY);
        for (int y = topY; y <= topY + 2; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isBreakableBlock(pos)) toDestroy.add(pos);
                }
            }
        }

        
        blocksToDestroy.addAll(toDestroy);
    }
}