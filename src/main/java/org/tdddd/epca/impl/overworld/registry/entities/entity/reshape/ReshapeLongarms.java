package org.tdddd.epca.impl.overworld.registry.entities.entity.reshape;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Creeper;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.tdddd.epca.impl.overworld.registry.*;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.block.SwallowCyst;
import org.tdddd.epca.impl.overworld.registry.blocks.block.entity.SwallowCystBlockEntity;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.IReshape;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class ReshapeLongarms extends PathfinderMob implements GeoEntity, IParasite, IReshape, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_ATTACKING = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.INT);

    
    private static final EntityDataAccessor<Boolean> DATA_IS_SHOCKWAVE_ATTACKING = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SHOCKWAVE_TIMER = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_IS_GASSING = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GASSING_TIMER = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.INT);
    private static final int GASSING_DURATION = 50;
    private boolean shouldCheckGassing = false;
    private int gassingParticleTimer = 0;
    private int gassingAreaParticleTimer = 0;
    private List<UUID> affectedEntities = new ArrayList<>();
    private int leftAttackCount = 0;
    private int rightAttackCount = 0;
    private int fakeDeathTimer = 30;
    private BlockPos deathPosition;
    private int attackTimer = 0;
    private int stopAttackDelay = 0;
    private int consecutiveAttackCount = 0;
    private int blockBreakTimer = 0;
    private double targetDistance = 0.0;
    private boolean hasAttackedSinceRunning = false;
    private List<BlockPos> blocksToDestroy = new ArrayList<>();
    private int destructionIndex = 0;
    private int jumpCooldown = 0;
    private int ceilingBreakCooldown = 0;
    private int shockwaveCooldownTimer = 0;
    private List<LivingEntity> previousTargets = new ArrayList<>();

    
    private static final EntityDataAccessor<Boolean> DATA_IS_STOMPING = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STOMP_TYPE = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STOMP_TIMER = SynchedEntityData.defineId(ReshapeLongarms.class, EntityDataSerializers.INT);
    private static final int STOMP_DURATION = 30;
    private static final int STOMP_DAMAGE_TICK = 18;
    private static final int STOMP_AREA_TICK = 16;
    private int stompCooldown = 0;
    private int stompEndCooldown = 0;  
    private LivingEntity stompTarget; 
    private int idleAfterStomp = 0; 
    
    private float stompStartYaw;      
    private float stompStartPitch;    
    private int stepSoundDelay = 0;

    public ReshapeLongarms(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 45;
        this.setMaxUpStep(1.6F);
        this.navigation = new GroundPathNavigation(this, level);

        if (!level.isClientSide()) {
            // 创建前部部件 (2x2x2)
            this.frontPart = new CustomPart(ModEntities.RESHAPE_PART.get(), level);
            this.frontPart.init(this, 2.0F, 2.0F, false);
            level.addFreshEntity(frontPart);

            // 创建后部部件 (1x1x1)
            this.backPart = new CustomPart(ModEntities.RESHAPE_PART.get(), level);
            this.backPart.init(this, 1.0F, 1.0F, true);
            level.addFreshEntity(backPart);
        }
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 3.6F;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(2.0F, 4.0F);
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        
        if (stompCooldown > 0) stompCooldown--;
        if (stompEndCooldown > 0) stompEndCooldown--;
        if (idleAfterStomp > 0) idleAfterStomp--;


        // 原有触发代码（大约在 customServerAiStep 开头附近）
        if (!this.level().isClientSide && shouldCheckGassing && !isGassing() && !isAttacking() && !isShockwaveAttacking() && !isStomping()) {
            if (this.random.nextFloat() < 0.004f) {
                // 如果后部部件已被移除，则禁止使用喷气
                if (!backPartRemoved) {
                    startGassingSkill();
                }
            }
            shouldCheckGassing = false;
        }
        if (isGassing()) {
            int gassingTimer = getGassingTimer();
            if (gassingTimer > 0) {
                setGassingTimer(gassingTimer - 1);
                int elapsedTicks = GASSING_DURATION - gassingTimer;
                if (elapsedTicks >= 12 && elapsedTicks < 35) {
                    gassingParticleTimer++;
                    if (gassingParticleTimer >= 3) {
                        gassingParticleTimer = 0;
                        spawnMovingInfestiveGasParticles();
                    }
                }
                gassingAreaParticleTimer++;
                if (gassingAreaParticleTimer >= 2) {
                    gassingAreaParticleTimer = 0;
                    spawnStaticInfestiveGasFadingParticles();
                }
                if (elapsedTicks >= 12 && elapsedTicks < 35) {
                    applyEffectsToNearbyEntities();
                }
                if (gassingTimer <= 1) {
                    endGassingSkill();
                }
            }
        }

        
        if (isStomping()) {
            
            this.setYRot(stompStartYaw);
            this.setXRot(stompStartPitch);

            
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(0, motion.y, 0);

            int stompTimer = getStompTimer();
            if (stompTimer > 0) {
                setStompTimer(stompTimer - 1);
                int elapsed = STOMP_DURATION - stompTimer;
                if (elapsed == STOMP_DAMAGE_TICK) {
                    performStompDamage();
                }
                if (elapsed == STOMP_AREA_TICK) {
                    performStompAreaEffect();
                }
                if (stompTimer <= 1) {
                    setStomping(false, 0);
                    setStompTimer(0);
                    this.setNoAi(false);
                    stompCooldown = 40; 
                    idleAfterStomp = 5;
                    stompEndCooldown = 10;  
                    stompTarget = null;
                }
            }
        }

        
        if (!this.level().isClientSide) {
            if (jumpCooldown > 0) jumpCooldown--;
            if (ceilingBreakCooldown > 0) ceilingBreakCooldown--;
            if (shockwaveCooldownTimer > 0) shockwaveCooldownTimer--;

            if (isShockwaveAttacking()) {
                shouldCheckGassing = true;
                int shockwaveTimer = getShockwaveTimer();
                if (shockwaveTimer > 0) {
                    setShockwaveTimer(shockwaveTimer - 1);
                    int elapsedTicks = SHOCKWAVE_DURATION - shockwaveTimer;
                    if (elapsedTicks == 1) performShockwaveMainDamage();
                    if (elapsedTicks == 20) performShockwaveAreaDamage();
                    if (shockwaveTimer <= 1) {
                        setShockwaveAttacking(false);
                        setShockwaveTimer(0);
                        shockwaveCooldownTimer = 5;
                    }
                }
            }

            if (attackTimer > 0) {
                attackTimer--;
                int elapsedTicks = ATTACK_DURATION - attackTimer;
                if (elapsedTicks == 4) performAttackDamage();
                if (attackTimer <= 0) {
                    setAttacking(false, 0);
                    stopAttackDelay = 1;
                    checkAndTriggerShockwaveByCombo();
                }
            }

            if (stopAttackDelay > 0) stopAttackDelay--;
            if (blockBreakTimer > 0) blockBreakTimer--;

            LivingEntity target = this.getTarget();
            if (target != null) {
                targetDistance = calculateSphericalDistance(target);
                boolean targetAbove = target.getY() > this.getY();
                boolean targetValid = target.isAlive() && this.hasLineOfSight(target);

                if (targetValid) {
                    
                    double distanceToTarget = calculateSphericalDistance(target);
                    if (!isAttacking() && !isShockwaveAttacking() && !isGassing() && !isStomping() && stompCooldown <= 0 && idleAfterStomp <= 0 && distanceToTarget <= 2.5) {
                        startStompAttack(target);
                    } else {
                        
                        if (!isAttacking() && !isShockwaveAttacking() && !isGassing() && !isStomping() && shockwaveCooldownTimer <= 0) {
                            shouldCheckGassing = true;
                            int enemyCountInCube = getEnemyCountInCube(4.5f);
                            if (enemyCountInCube >= 2) {
                                int extraEnemies = enemyCountInCube - 2;
                                float probability = 0.10f + (extraEnemies * 0.05f);
                                probability = Math.min(probability, 0.50f);
                                probability = Math.max(probability, 0.10f);
                                if (this.random.nextFloat() < probability) {
                                    startShockwaveAttack();
                                }
                            }
                        }

                        if (targetAbove && getHorizontalDistance(target) <= 5.0) {
                            handleCeilingBreaking(target);
                        }

                        if (canPerformAttack() && !isShockwaveAttacking() && !isGassing() && !isStomping()) {
                            shouldCheckGassing = true;
                            boolean shouldUseDoubleAttack = false;
                            if (!hasAttackedSinceRunning || consecutiveAttackCount == 0) {
                                shouldUseDoubleAttack = true;
                                consecutiveAttackCount = 1;
                                hasAttackedSinceRunning = true;
                            } else {
                                List<LivingEntity> targetsInSector = getTargetsInSector(3.5f, 45.0f);
                                if (targetsInSector.size() > 1) {
                                    shouldUseDoubleAttack = true;
                                }
                            }
                            if (shouldUseDoubleAttack) {
                                startAttack(3);
                            } else {
                                int attackType = this.random.nextBoolean() ? 1 : 2;
                                startAttack(attackType);
                                consecutiveAttackCount++;
                            }
                            if (!previousTargets.contains(target)) {
                                previousTargets.add(target);
                            }
                        }

                        if (targetDistance > 16.0) {
                            hasAttackedSinceRunning = false;
                            consecutiveAttackCount = 0;
                        }
                    }
                } else {
                    this.setTarget(null);
                    hasAttackedSinceRunning = false;
                    consecutiveAttackCount = 0;
                }
            } else {
                hasAttackedSinceRunning = false;
                consecutiveAttackCount = 0;
            }

            updateMovementState();

            if (blockBreakTimer <= 0 && !isShockwaveAttacking() && !isStomping()) {
                handleBlockBreaking();
            }
            if (!blocksToDestroy.isEmpty() && blockBreakTimer <= 0 && !isShockwaveAttacking() && !isStomping()) {
                destroyNextBlock();
            }
        }

        // 更新部件位置（服务端执行）
        if (!this.level().isClientSide) {
            // 计算水平朝向向量（忽略俯仰）
            float yaw = this.getYRot();
            double forwardX = -Math.sin(Math.toRadians(yaw));
            double forwardZ = Math.cos(Math.toRadians(yaw));
            Vec3 forward = new Vec3(forwardX, 0, forwardZ).normalize();

            // 生物中心（脚部 + 身高一半）
            Vec3 center = this.position().add(0, this.getBbHeight() / 2, 0);

            // 前部：中心 + 向前 0.1 格 + 向上 1 格
            if (frontPart != null && !frontPart.isRemoved()) {
                Vec3 frontPos = center.add(forward.scale(0.2)).add(0, 0.6, 0);
                frontPart.setPos(frontPos);
            }

            // 后部：中心 + 向后 0.1 格（贴于后方），不向上偏移
            if (backPart != null && !backPart.isRemoved()) {
                Vec3 backPos = center.add(forward.scale(-0.2));
                backPart.setPos(backPos);
            }
        }

        // 后部被移除后的气体喷发（每秒一次）
        if (!this.level().isClientSide && backPartRemoved) {
            gasEmitTimer++;
            if (gasEmitTimer >= 20) { // 20 ticks = 1 秒
                emitGasAfterBackRemoved();
                gasEmitTimer = 0;
            }
        }
    }

    private void emitGasAfterBackRemoved() {
        if (this.level().isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) this.level();
        BlockPos center = this.blockPosition();
        int radius = 3; // 7×7×7
        AABB area = new AABB(center).inflate(radius);

        // 获取范围内的所有活体实体（排除自身和创造/旁观玩家）
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() &&
                        !(e instanceof Player && (((Player) e).isCreative() || ((Player) e).isSpectator())));

        for (LivingEntity entity : entities) {
            if (IParasite.isParasiteByTagOrInterface(entity)) {
                // 寄生体：15秒力量I (300 ticks, 等级0)
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, true, true));
            } else {
                // 非寄生体：30秒COTH II、15秒虚弱I、15秒饥饿I
                entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 1, false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 0, false, true, true));
            }
        }

        // 粒子数量减半（原 4~8，现 2~4）
        int count = 2 + this.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double x = center.getX() + 0.5 + (this.random.nextDouble() - 0.5) * 6;
            double y = center.getY() + 1.0 + this.random.nextDouble() * 5;
            double z = center.getZ() + 0.5 + (this.random.nextDouble() - 0.5) * 6;
            serverLevel.sendParticles(ModParticles.INFESTIVE_GAS.get(), x, y, z, 1, 0, 0, 0, 0.0);
        }
    }
    
    private boolean isStomping() {
        return this.entityData.get(DATA_IS_STOMPING);
    }

    private int getStompType() {
        return this.entityData.get(DATA_STOMP_TYPE);
    }

    private int getStompTimer() {
        return this.entityData.get(DATA_STOMP_TIMER);
    }

    private void setStomping(boolean stomping, int type) {
        this.entityData.set(DATA_IS_STOMPING, stomping);
        this.entityData.set(DATA_STOMP_TYPE, type);
    }

    private void setStompTimer(int timer) {
        this.entityData.set(DATA_STOMP_TIMER, timer);
    }

    
    private int getTargetSide(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.position()).normalize();
        Vec3 forward = this.getLookAngle();
        double cross = forward.x * toTarget.z - forward.z * toTarget.x;
        if (cross > 0) return 1;
        if (cross < 0) return 2;
        return 0;
    }

    
    private void startStompAttack(LivingEntity target) {
        if (isStomping() || isAttacking() || isShockwaveAttacking() || isGassing()) return;
        this.stompTarget = target;

        
        this.stompStartYaw = this.getYRot();
        this.stompStartPitch = this.getXRot();

        int side = getTargetSide(target);
        int stompType;
        if (side == 1) stompType = 1;
        else if (side == 2) stompType = 2;
        else stompType = this.random.nextBoolean() ? 1 : 2;

        setStomping(true, stompType);
        setStompTimer(STOMP_DURATION);

        
        this.getNavigation().stop();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2F, 0.7F + this.random.nextFloat() * 0.3F);
        }
    }

    
    private void performStompDamage() {
        if (this.level().isClientSide) return;
        if (stompTarget != null && stompTarget.isAlive()) {
            float damage = this.onGround() ? 45.0F : 25.0F;
            boolean hurt = stompTarget.hurt(this.damageSources().mobAttack(this), damage);
            if (hurt && damage >= 30.0F) {
                Vec3 lookAngle = this.getLookAngle();
                stompTarget.setDeltaMovement(stompTarget.getDeltaMovement().add(lookAngle.x * 0.5, 0.2, lookAngle.z * 0.5));
            }
        }
    }

    
    private void performStompAreaEffect() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            BlockPos startPos = this.blockPosition();
            BlockPos surfacePos = findFirstSolidSurfaceBelow(serverLevel, startPos, 3);

            
            double spawnX, spawnY, spawnZ;
            if (surfacePos != null) {
                spawnX = surfacePos.getX() + 0.5;
                spawnY = surfacePos.getY() + 1.01;
                spawnZ = surfacePos.getZ() + 0.5;
            } else {
                
                spawnX = this.getX();
                spawnY = this.getY() + 0.1;
                spawnZ = this.getZ();
            }

            
            
            if (ModParticles.WAVE_SMALL.isPresent()) {
                serverLevel.sendParticles(ModParticles.WAVE_SMALL.get(), spawnX, spawnY, spawnZ, 1, 0, 0, 0, 0.0);
            }

            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSoundEvents.SLAM.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            }

            
            AABB area = new AABB(this.blockPosition()).inflate(1.0);
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != this && !IParasite.isParasiteByTagOrInterface(e) && e.isAlive() &&
                            !(e instanceof Player && (((Player) e).isCreative() || ((Player) e).isSpectator())));
            for (LivingEntity entity : entities) {
                entity.hurt(this.damageSources().mobAttack(this), 15.0F);
            }
        }
    }

    
    private BlockPos findFirstSolidSurfaceBelow(ServerLevel level, BlockPos startPos, int maxDistance) {
        BlockPos.MutableBlockPos pos = startPos.mutable();
        for (int i = 0; i <= maxDistance; i++) {
            pos.set(startPos.getX(), startPos.getY() - i, startPos.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender(level, pos)) {
                BlockPos above = pos.above();
                if (level.isEmptyBlock(above) || level.getBlockState(above).canBeReplaced()) {
                    return pos;
                }
            }
        }
        BlockPos feetPos = startPos.below();
        if (!level.getBlockState(feetPos).isAir()) return feetPos;
        return null;
    }

    private void startGassingSkill() {
        if (backPartRemoved) return;
        setGassing(true);
        setGassingTimer(GASSING_DURATION);
        this.getNavigation().stop();
        this.setNoAi(true);
        gassingParticleTimer = 0;
        gassingAreaParticleTimer = 0;
        affectedEntities.clear();
    }

    private void endGassingSkill() {
        setGassing(false);
        setGassingTimer(0);
        this.setNoAi(false);
        affectedEntities.clear();
    }

    private void spawnMovingInfestiveGasParticles() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            double spawnX = this.getX();
            double spawnY = this.getY() + 2.0;
            double spawnZ = this.getZ();
            int particleCount = 4 + this.random.nextInt(5);
            for (int i = 0; i < particleCount; i++) {
                double angle = this.random.nextDouble() * 2 * Math.PI;
                double pitch = this.random.nextDouble() * Math.PI - Math.PI/2;
                double dirX = Math.cos(angle) * Math.cos(pitch);
                double dirY = Math.sin(pitch);
                double dirZ = Math.sin(angle) * Math.cos(pitch);
                double speed = 0.3;
                serverLevel.sendParticles(ModParticles.INFESTIVE_GAS.get(), spawnX, spawnY, spawnZ, 1, dirX, dirY, dirZ, speed);
            }
        }
    }

    private void spawnStaticInfestiveGasFadingParticles() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            int particleCount = 4 + this.random.nextInt(5);
            double minX = this.getX() - 3.5, maxX = this.getX() + 3.5;
            double minY = this.getY() - 3.5, maxY = this.getY() + 3.5;
            double minZ = this.getZ() - 3.5, maxZ = this.getZ() + 3.5;
            for (int i = 0; i < particleCount; i++) {
                double posX = minX + this.random.nextDouble() * (maxX - minX);
                double posY = minY + this.random.nextDouble() * (maxY - minY);
                double posZ = minZ + this.random.nextDouble() * (maxZ - minZ);
                serverLevel.sendParticles(ModParticles.INFESTIVE_GAS.get(), posX, posY, posZ, 1, 0, 0, 0, 0.0);
            }
        }
    }

    private void applyEffectsToNearbyEntities() {
        if (this.level().isClientSide) return;
        AABB area = new AABB(this.getX() - 3.5, this.getY() - 3.5, this.getZ() - 3.5,
                this.getX() + 3.5, this.getY() + 3.5, this.getZ() + 3.5);

        List<LivingEntity> parasiteEntities = this.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> IParasite.isParasiteByTagOrInterface(entity) && entity.isAlive());
        for (LivingEntity entity : parasiteEntities) {
            UUID entityId = entity.getUUID();
            if (!affectedEntities.contains(entityId)) {
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, true, true));
                affectedEntities.add(entityId);
            }
        }

        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && !IParasite.isParasiteByTagOrInterface(entity) && entity.isAlive());
        for (LivingEntity entity : entities) {
            UUID entityId = entity.getUUID();
            if (!affectedEntities.contains(entityId)) {
                entity.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 200, 2, false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1, false, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1, false, true, true));
                affectedEntities.add(entityId);
            }
        }
    }

    private int getEnemyCountInCube(float halfSide) {
        if (this.level().isClientSide) return 0;
        Vec3 center = this.position();
        AABB cubeArea = new AABB(center.x - halfSide, center.y - halfSide, center.z - halfSide,
                center.x + halfSide, center.y + halfSide, center.z + halfSide);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, cubeArea,
                entity -> entity != this && entity.isAlive() && !IParasite.isParasiteByTagOrInterface(entity) && !(entity instanceof Creeper) &&
                        !(entity instanceof Player && (((Player) entity).isCreative() || ((Player) entity).isSpectator())));
        return entities.size();
    }

    private void incrementLeftAttackCount() { leftAttackCount++; }
    private void incrementRightAttackCount() { rightAttackCount++; }
    private void resetAttackCounts() { leftAttackCount = 0; rightAttackCount = 0; }

    private void checkAndTriggerShockwaveByCombo() {
        if (leftAttackCount >= 2 && rightAttackCount >= 2 && !isShockwaveAttacking() && shockwaveCooldownTimer <= 0) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                startShockwaveAttack();
                resetAttackCounts();
            }
        }
    }

    private static final int SHOCKWAVE_DURATION = 45;

    private void startShockwaveAttack() {
        setShockwaveAttacking(true);
        setShockwaveTimer(SHOCKWAVE_DURATION);
        this.getNavigation().stop();
        this.setNoAi(true);
        LivingEntity target = this.getTarget();
        if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.SLAM.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        }
    }

    private void performShockwaveMainDamage() {
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                
                double distance = this.distanceTo(target);
                if (distance > 4.0) {
                    return;
                }
                float damageAmount = 30.0f;
                boolean damageApplied = target.hurt(this.damageSources().mobAttack(this), damageAmount);
                if (damageApplied) {
                    Vec3 lookAngle = this.getLookAngle();
                    target.setDeltaMovement(target.getDeltaMovement().add(lookAngle.x * 0.5, 0.3, lookAngle.z * 0.5));
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2F, 0.9F);
            }
        }
    }

    private void performShockwaveAreaDamage() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Vec3 center = this.position();
            double radius = 4.5;
            AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != this && !IParasite.isParasiteByTagOrInterface(entity) && entity.isAlive() &&
                            (entity.onGround() || entity.isInWater() || entity.isInLava()) &&
                            !(entity instanceof Player && (((Player) entity).isCreative() || ((Player) entity).isSpectator())));
            for (LivingEntity entity : entities) {
                entity.hurt(this.damageSources().mobAttack(this), 20.0f);
                Vec3 toEntity = entity.position().subtract(center).normalize();
                entity.setDeltaMovement(entity.getDeltaMovement().add(toEntity.x, 0.6, toEntity.z));
            }
            int minX = (int) Math.floor(center.x - radius), maxX = (int) Math.ceil(center.x + radius);
            int minY = (int) Math.floor(center.y - radius), maxY = (int) Math.ceil(center.y + radius);
            int minZ = (int) Math.floor(center.z - radius), maxZ = (int) Math.ceil(center.z + radius);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockState = serverLevel.getBlockState(pos);
                        BlockPos abovePos = pos.above();
                        if (serverLevel.isEmptyBlock(abovePos) && !blockState.isAir()) {
                            for (int i = 0; i < Math.min(3, 1 + this.random.nextInt(3)); i++) {
                                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                                        pos.getX() + 0.5 + (this.random.nextDouble() - 0.5) * 0.8,
                                        pos.getY() + 1.0 + this.random.nextDouble() * 0.5,
                                        pos.getZ() + 0.5 + (this.random.nextDouble() - 0.5) * 0.8,
                                        1, (this.random.nextDouble() - 0.5) * 0.1, 0.1 + this.random.nextDouble() * 0.2,
                                        (this.random.nextDouble() - 0.5) * 0.1, 0.0);
                            }
                        }
                    }
                }
            }
            float bodyYaw = this.getYRot();
            float bodyYawRad = (float) Math.toRadians(bodyYaw);
            double forwardX = -Math.sin(bodyYawRad);
            double forwardZ = Math.cos(bodyYawRad);
            Vec3 bodyForward = new Vec3(forwardX, 0, forwardZ).normalize();
            double horizontalDistance = 2.5;
            double spawnX = this.getX() + bodyForward.x * horizontalDistance;
            double spawnZ = this.getZ() + bodyForward.z * horizontalDistance;
            BlockPos checkPos = BlockPos.containing(spawnX, this.getY() + 0.5, spawnZ);
            BlockPos surfacePos = findSurfacePosition(serverLevel, checkPos);
            if (surfacePos != null) {
                double particleX = surfacePos.getX() + 0.5;
                double particleY = surfacePos.getY() + 1.11;
                double particleZ = surfacePos.getZ() + 0.5;
                serverLevel.sendParticles(ModParticles.WAVE.get(), particleX, particleY, particleZ, 1,
                        bodyForward.x, 0, bodyForward.z, 1.0);
            } else {
                double particleY = this.getY() + 0.11;
                serverLevel.sendParticles(ModParticles.WAVE.get(), spawnX, particleY, spawnZ, 1,
                        bodyForward.x, 0, bodyForward.z, 1.0);
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.SLAM.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        }
    }

    private BlockPos findSurfacePosition(ServerLevel level, BlockPos startPos) {
        for (int i = 0; i < 1; i++) {
            BlockPos checkPos = startPos.above(i);
            BlockState blockState = level.getBlockState(checkPos);
            BlockPos belowPos = checkPos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (blockState.isAir() && !belowState.isAir() && belowState.isSolidRender(level, belowPos)) {
                return belowPos;
            }
        }
        for (int i = 0; i < 1; i++) {
            BlockPos checkPos = startPos.below(i);
            BlockState blockState = level.getBlockState(checkPos);
            BlockPos abovePos = checkPos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (!blockState.isAir() && blockState.isSolidRender(level, checkPos) && aboveState.isAir()) {
                return checkPos;
            }
        }
        return null;
    }

    public boolean isGassing() { return this.entityData.get(DATA_IS_GASSING); }
    public int getGassingTimer() { return this.entityData.get(DATA_GASSING_TIMER); }
    private void setGassing(boolean gassing) { this.entityData.set(DATA_IS_GASSING, gassing); }
    private void setGassingTimer(int timer) { this.entityData.set(DATA_GASSING_TIMER, timer); }

    public boolean isShockwaveAttacking() { return this.entityData.get(DATA_IS_SHOCKWAVE_ATTACKING); }
    public int getShockwaveTimer() { return this.entityData.get(DATA_SHOCKWAVE_TIMER); }
    private void setShockwaveAttacking(boolean attacking) { this.entityData.set(DATA_IS_SHOCKWAVE_ATTACKING, attacking); }
    private void setShockwaveTimer(int timer) { this.entityData.set(DATA_SHOCKWAVE_TIMER, timer); }

    private double calculateSphericalDistance(LivingEntity target) {
        double dx = target.getX() - this.getX(), dy = target.getY() - this.getY(), dz = target.getZ() - this.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double getHorizontalDistance(LivingEntity target) {
        double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void handleCeilingBreaking(LivingEntity target) {
        if (ceilingBreakCooldown > 0 || isAttacking() || isShockwaveAttacking() || isGassing() || isStomping()) return;
        List<BlockPos> ceilingBlocks = new ArrayList<>();
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                for (int dy = 4; dy <= 5; dy++) {
                    BlockPos posToCheck = this.blockPosition().offset(dx, dy, dz);
                    BlockState blockState = this.level().getBlockState(posToCheck);
                    if (blockState.isAir() || blockState.canBeReplaced()) continue;
                    float destroyTime = blockState.getDestroySpeed(this.level(), posToCheck);
                    if (destroyTime >= 0 && destroyTime <= 1.0f) ceilingBlocks.add(posToCheck);
                }
            }
        }
        if (!ceilingBlocks.isEmpty()) {
            BlockPos headPos = this.blockPosition().offset(0, 5, 0);
            if (ceilingBlocks.contains(headPos)) breakCeilingBlock(headPos);
            else breakCeilingBlock(ceilingBlocks.get(0));
            if (jumpCooldown <= 0) { this.jumpFromGround(); jumpCooldown = 80; }
            ceilingBreakCooldown = 5;
        } else {
            if (jumpCooldown <= 0) {
                this.jumpFromGround();
                jumpCooldown = 80;
                if (!this.onGround()) {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    Vec3 lookAngle = this.getLookAngle();
                    this.setDeltaMovement(this.getDeltaMovement().add(lookAngle.x * 0.2, 0.5, lookAngle.z * 0.2));
                }
            }
        }
    }

    private void breakCeilingBlock(BlockPos pos) {
        BlockState blockState = this.level().getBlockState(pos);
        if (!blockState.isAir() && !blockState.canBeReplaced()) {
            float destroyTime = blockState.getDestroySpeed(this.level(), pos);
            if (destroyTime >= 0 && destroyTime <= 1.0f) {
                if (this.level().destroyBlock(pos, true, this) && blockState.getSoundType() != null) {
                    this.level().playSound(null, pos, blockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }

    private static final int ATTACK_DURATION = 30;

    private void startAttack(int attackType) {
        setAttacking(true, attackType);
        attackTimer = ATTACK_DURATION;
        this.getNavigation().stop();
        LivingEntity target = this.getTarget();
        if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!this.level().isClientSide) {
            float pitch = 0.8F + this.random.nextFloat() * 0.4F;
            if (attackType == 3) this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2F, pitch);
            else this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.8F, pitch);
        }
    }

    public void performAttackDamage() {
        if (!this.level().isClientSide && isAttacking()) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                int attackType = getAttackType();
                if (attackType == 1) incrementLeftAttackCount();
                else if (attackType == 2) incrementRightAttackCount();
                if (attackType == 3) {
                    float damageAmount = 35.0f;
                    boolean damageApplied = target.hurt(this.damageSources().mobAttack(this), damageAmount);
                    if (damageApplied) {
                        Vec3 lookAngle = this.getLookAngle();
                        target.setDeltaMovement(target.getDeltaMovement().add(lookAngle.x * 0.3, 0.15, lookAngle.z * 0.3));
                    }
                } else if (attackType == 1 || attackType == 2) {
                    double distance = this.distanceTo(target);
                    double attackRange = 3.5;
                    if (distance <= attackRange && this.hasLineOfSight(target) && target.isAlive() && !target.isInvulnerable()) {
                        float damageAmount = 25.0f;
                        boolean damageApplied = target.hurt(this.damageSources().mobAttack(this), damageAmount);
                        if (damageApplied) {
                            Vec3 lookAngle = this.getLookAngle();
                            target.setDeltaMovement(target.getDeltaMovement().add(lookAngle.x * 0.3, 0.15, lookAngle.z * 0.3));
                        }
                    }
                }
            }
        }
    }

    private void updateMovementState() {
        if (isGassing() || isStomping()) {
            this.getNavigation().stop();
            this.setRunning(false);
            this.setWalking(false);
            return;
        }
        if (isShockwaveAttacking() || shockwaveCooldownTimer > 0) {
            this.getNavigation().stop();
            this.setRunning(false);
            this.setWalking(false);
            return;
        }
        boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        LivingEntity target = this.getTarget();
        boolean isAttacking = isAttacking();
        if (isAttacking || stopAttackDelay > 0) {
            this.getNavigation().stop();
            isMoving = false;
        }
        if (target != null) {
            boolean shouldRun = !hasAttackedSinceRunning;
            if (shouldRun) { this.setRunning(isMoving && !isAttacking); this.setWalking(false); }
            else { this.setWalking(isMoving && !isAttacking); this.setRunning(false); }
        } else {
            this.setWalking(isMoving && !isAttacking);
            this.setRunning(false);
        }
        if (!isMoving || isAttacking) { this.setRunning(false); this.setWalking(false); }
    }

    private void handleBlockBreaking() {
        if (isAttacking() || this.getTarget() == null || isShockwaveAttacking() || isGassing() || isStomping()) return;
        blocksToDestroy.clear();
        destructionIndex = 0;
        Set<BlockPos> toDestroy = new HashSet<>();
        LivingEntity target = getTarget();
        AABB aabb = getBoundingBox();
        int minX = (int) Math.floor(aabb.minX), maxX = (int) Math.ceil(aabb.maxX);
        int minZ = (int) Math.floor(aabb.minZ), maxZ = (int) Math.ceil(aabb.maxZ);
        int minY = (int) Math.floor(aabb.minY), maxY = (int) Math.ceil(aabb.maxY);

        
        int xEast = (int) Math.ceil(aabb.maxX);
        for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            BlockPos pos = new BlockPos(xEast, y, z);
            if (isBreakableBlock(pos)) toDestroy.add(pos);
        }
        int xWest = (int) Math.floor(aabb.minX) - 1;
        for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            BlockPos pos = new BlockPos(xWest, y, z);
            if (isBreakableBlock(pos)) toDestroy.add(pos);
        }
        int zSouth = (int) Math.ceil(aabb.maxZ);
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            BlockPos pos = new BlockPos(x, y, zSouth);
            if (isBreakableBlock(pos)) toDestroy.add(pos);
        }
        int zNorth = (int) Math.floor(aabb.minZ) - 1;
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            BlockPos pos = new BlockPos(x, y, zNorth);
            if (isBreakableBlock(pos)) toDestroy.add(pos);
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

        
        if (target != null && (target.getY() + target.getEyeHeight()) < this.getY()) {
            int startY = (int) Math.floor(this.getY()) - 1; 
            for (int y = startY; y >= startY - 2; y--) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (isBreakableBlock(pos)) toDestroy.add(pos);
                    }
                }
            }
        }

        blocksToDestroy.addAll(toDestroy);
        if (!blocksToDestroy.isEmpty()) destroyNextBlock();
    }

    private boolean isBreakableBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) return false;
        if (state.getBlock() instanceof SwallowCyst) return false;
        float hardness = state.getDestroySpeed(level(), pos);
        return hardness >= 0 && hardness <= 1.0f;
    }

    private void destroyNextBlock() {
        if (destructionIndex >= blocksToDestroy.size()) { blocksToDestroy.clear(); destructionIndex = 0; blockBreakTimer = 10; return; }
        BlockPos pos = blocksToDestroy.get(destructionIndex);
        BlockState state = level().getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced() && isBreakableBlock(pos)) {
            List<ItemStack> drops = getDrops(state, pos);
            boolean destroyed = level().destroyBlock(pos, false, this);
            if (destroyed) {
                if (state.getSoundType() != null) level().playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                for (ItemStack drop : drops) addItemToInventory(drop);
            }
        }
        destructionIndex++;
    }

    private List<ItemStack> getDrops(BlockState state, BlockPos pos) {
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

    private List<LivingEntity> getTargetsInSector(float radius, float angleDegrees) {
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(this.getX() - radius, this.getY() - 2, this.getZ() - radius,
                        this.getX() + radius, this.getY() + 2, this.getZ() + radius),
                entity -> entity != this && !IParasite.isParasiteByTagOrInterface(entity) && !(entity instanceof Creeper) && entity.isAlive());
        Vec3 forward = this.getLookAngle().normalize();
        double angleRad = Math.toRadians(angleDegrees / 2);
        return entities.stream().filter(entity -> {
            Vec3 toEntity = new Vec3(entity.getX() - this.getX(), 0, entity.getZ() - this.getZ()).normalize();
            double dot = forward.dot(toEntity);
            double angle = Math.acos(dot);
            return angle <= angleRad && this.distanceTo(entity) <= radius;
        }).toList();
    }

    public boolean isFakingDeath() { return this.entityData.get(DATA_IS_FAKING_DEATH); }
    private void setFakingDeath(boolean faking) { this.entityData.set(DATA_IS_FAKING_DEATH, faking); }
    public boolean isAttacking() { return this.entityData.get(DATA_IS_ATTACKING); }
    public int getAttackType() { return this.entityData.get(DATA_ATTACK_TYPE); }
    private void setAttacking(boolean attacking, int attackType) { this.entityData.set(DATA_IS_ATTACKING, attacking); this.entityData.set(DATA_ATTACK_TYPE, attackType); }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.JUMP_STRENGTH, 1.5D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(1, new Goal() {
            @Override public boolean canUse() { LivingEntity target = ReshapeLongarms.this.getTarget(); return target != null && target.isAlive() && !ReshapeLongarms.this.isAttacking() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public void tick() { LivingEntity target = ReshapeLongarms.this.getTarget(); if (target != null && target.isAlive()) { ReshapeLongarms.this.getNavigation().moveTo(target, 1.0); ReshapeLongarms.this.lookAt(target, 30.0F, 30.0F); } }
        });
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override public boolean canUse() { return super.canUse() && !ReshapeLongarms.this.isAttacking() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && ReshapeLongarms.this.getTarget() == null && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
            @Override public boolean canContinueToUse() { return super.canContinueToUse() && !ReshapeLongarms.this.isAttacking() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && ReshapeLongarms.this.getTarget() == null && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
        });
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override public boolean canUse() { return super.canUse() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
            @Override public boolean canContinueToUse() { return super.canContinueToUse() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
        });
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
            @Override public boolean canUse() { return super.canUse() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
            @Override public boolean canContinueToUse() { return super.canContinueToUse() && !ReshapeLongarms.this.isShockwaveAttacking() && ReshapeLongarms.this.shockwaveCooldownTimer <= 0 && !ReshapeLongarms.this.isGassing() && !ReshapeLongarms.this.isStomping(); }
        });
        this.targetSelector.addGoal(0, new PriorityTargetGoal(this, 32.0D));
        this.goalSelector.addGoal(5, new ReshapeLongarms.RandomSoundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (isShockwaveAttacking() && getShockwaveTimer() <= 0) { setShockwaveAttacking(false); this.setNoAi(false); }
            if (shockwaveCooldownTimer <= 0 && this.isNoAi() && !isStomping()) this.setNoAi(false);
            updateMovementState();
        }

        if (isFakingDeath()) {
            super.tick();
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                if (!this.level().isClientSide) {
                    
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundEvents.BIG_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ModParticles.COTH.get(), this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.45, 0.3, 0.0);
                        serverLevel.sendParticles(ModParticles.SPLASHI.get(), this.getX(), this.getY() + 0.5, this.getZ(), 7, 0.4, 0.3, 0.4, 0.2);
                        BlockPos deathPos = this.deathPosition;
                        long seed = this.random.nextLong();
                        int delay = this.random.nextInt(30) + 40;
                        serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + delay, () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))));
                        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
                        cloud.setRadius(1.5F); cloud.setDuration(60); cloud.setRadiusPerTick(0); cloud.setWaitTime(0);
                        cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 1, false, true));
                        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
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
            if (isShockwaveAttacking() || shockwaveCooldownTimer > 0 || isGassing() || isStomping()) {
                movementAttribute.setBaseValue(0.0D);
                return;
            }
            LivingEntity target = this.getTarget();
            boolean isAttacking = isAttacking();
            if (target != null && !isAttacking) {
                if (!hasAttackedSinceRunning) movementAttribute.setBaseValue(0.48D);
                else movementAttribute.setBaseValue(0.28D);
            } else if (!isAttacking) movementAttribute.setBaseValue(0.28D);
            else movementAttribute.setBaseValue(0.0D);
        }

        
        if (!this.level().isClientSide && this.onGround() && this.isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(ModSoundEvents.RESHAPE_STEP.get(), 1.0F, 1.0F);
                
                this.stepSoundDelay = 20 + this.random.nextInt(11);
            } else {
                this.stepSoundDelay--;
            }
        } else {
            
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }

        
        if (this.level().isClientSide && isAttacking() && (getAttackType() == 1 || getAttackType() == 2)) {
            int elapsed = ATTACK_DURATION - attackTimer; 
            if (elapsed >= 4 && elapsed <= 14) {
                int particleCount = 8 + this.random.nextInt(5); 
                String locatorName = (getAttackType() == 1) ? "left_hand_locators" : "right_hand_locators";
                Vec3 handWorldPos = getLocatorWorldPosition(locatorName);
                if (handWorldPos != null) {
                    
                    float yawRad = (float) Math.toRadians(this.getYRot());
                    Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
                    Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad)).normalize();
                    Vec3 up = new Vec3(0, 1, 0);

                    for (int i = 0; i < particleCount; i++) {
                        
                        double localX = (this.random.nextDouble() - 0.5) * 0.9;
                        double localY = (this.random.nextDouble() - 0.5) * 0.9;
                        double localZ = this.random.nextDouble() * 1.2;
                        Vec3 offset = right.scale(localX).add(up.scale(localY)).add(forward.scale(localZ));
                        Vec3 particlePos = handWorldPos.add(offset);
                        this.level().addParticle(ParticleTypes.FLAME,
                                particlePos.x, particlePos.y, particlePos.z,
                                0, 0, 0);
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private Vec3 getLocatorWorldPosition(String locatorName) {
        
        float yawRad = (float) Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        double handX, handZ;
        if (locatorName.contains("left")) {
            
            handX = this.getX() + forward.x * 0.2 - right.x * 0.2;
            handZ = this.getZ() + forward.z * 0.2 - right.z * 0.2;
        } else {
            
            handX = this.getX() + forward.x * 0.2 + right.x * 0.2;
            handZ = this.getZ() + forward.z * 0.2 + right.z * 0.2;
        }
        double handY = this.getY() + 2;
        return new Vec3(handX, handY, handZ);
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    private boolean canPerformAttack() {
        if (isShockwaveAttacking() || shockwaveCooldownTimer > 0 || isGassing() || isStomping() || stompEndCooldown > 0 || idleAfterStomp > 0) return false;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.hasLineOfSight(target)) return false;
        double dx = target.getX() - this.getX(), dy = target.getY() - this.getY(), dz = target.getZ() - this.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = Math.abs(dy);
        return horizontalDistance <= 3.5 && verticalDistance <= 0.5 && !isAttacking() && stopAttackDelay <= 0;
    }

    @Override public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) { return false; }
    @Override
    public void travel(Vec3 travelVector) {
        if (isStomping() && !this.level().isClientSide) {
            
            super.travel(new Vec3(0, travelVector.y, 0));
        } else {
            super.travel(travelVector);
        }
    }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected void jumpFromGround() {
        if (isShockwaveAttacking() || shockwaveCooldownTimer > 0 || isGassing() || isStomping()) return;
        super.jumpFromGround();
        this.setDeltaMovement(this.getDeltaMovement().x, 1.0D, this.getDeltaMovement().z);
        if (this.horizontalCollision) {
            BlockPos forwardPos = this.blockPosition().relative(this.getDirection());
            BlockState forwardState = this.level().getBlockState(forwardPos);
            if (!forwardState.isAir() && forwardState.getDestroySpeed(this.level(), forwardPos) >= 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(this.getLookAngle().x * 0.5, 0.8D, this.getLookAngle().z * 0.5));
            }
        }
    }
    @Override protected int calculateFallDamage(float distance, float damageMultiplier) { return 0; }
    @Override protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.RESHAPE_LONGARMS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.RESHAPE_LONGARMS_DEATH.get();
    }

    static class RandomSoundGoal extends Goal {
        private final ReshapeLongarms reshapeLongarms;
        private int nextSoundTick;

        RandomSoundGoal(ReshapeLongarms reshapeLongarms) {
            this.reshapeLongarms = reshapeLongarms;
        }

        

        @Override
        public boolean canUse() {
            return reshapeLongarms.isAlive() && !reshapeLongarms.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = reshapeLongarms.getRandom().nextInt(120) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                playRandomLivingSound();
                this.nextSoundTick = reshapeLongarms.getRandom().nextInt(120) + 80;
            }
        }

        private void playRandomLivingSound() {
            reshapeLongarms.playSound(ModSoundEvents.RESHAPE_LONGARMS_IDLE.get(), 1.0F, 1.0F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && shouldIgnoreDamageFrom(attacker)) return false;
        if (isFakingDeath() || isInvulnerable()) return false;
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) this.onAttacked(attacker);
        return super.hurt(source, amount);
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::playState));
    }

    private PlayState playState(AnimationState<ReshapeLongarms> event) {
        if (this.isRemoved() || !this.isAlive()) return PlayState.STOP;
        if (this.isFakingDeath()) event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        else if (this.isGassing()) event.getController().setAnimation(RawAnimation.begin().thenLoop("gassing"));
        else if (this.isShockwaveAttacking()) event.getController().setAnimation(RawAnimation.begin().thenPlay("shockwave_big"));
        else if (this.isStomping()) {
            int type = getStompType();
            if (type == 1) event.getController().setAnimation(RawAnimation.begin().thenPlay("shockwave_small_right"));
            else if (type == 2) event.getController().setAnimation(RawAnimation.begin().thenPlay("shockwave_small_left"));
            else event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        } else if (this.isAttacking()) {
            int attackType = getAttackType();
            switch (attackType) {
                case 1: event.getController().setAnimation(RawAnimation.begin().thenLoop("attack_leftarm")); break;
                case 2: event.getController().setAnimation(RawAnimation.begin().thenLoop("attack_rightarm")); break;
                case 3: event.getController().setAnimation(RawAnimation.begin().thenPlay("attack_dblarm")); break;
                default: event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
            }
        } else if (this.isRunning()) event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        else if (this.isWalking()) event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        else event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    public static boolean checkReshapeLongarmsSpawnRules(EntityType<ReshapeLongarms> entityType, ServerLevelAccessor levelAccessor, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            return stage >= 4 && stage <= 6;
        }
        return levelAccessor.getMaxLocalRawBrightness(pos) < 8;
    }

    @Override
    public void die(DamageSource source) {
        if (isFakingDeath()) { super.die(source); return; }
        if (!isInventoryEmpty()) tryPlaceCystAndTransfer();
        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = config.getMinimumKillCount();
            if (minKills > 0) {
                int currentDeaths = DamageAdaptation.getDeathCount(this);
                if (currentDeaths < minKills) {
                    DamageAdaptation.recordDeath(this);
                    this.setHealth(this.getMaxHealth());
                    if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                        RandomSource random = this.getRandom();
                        int particleCount = 7 + random.nextInt(6);
                        AABB bb = this.getBoundingBox();
                        for (int i = 0; i < particleCount; i++) {
                            double x = bb.minX + random.nextDouble() * (bb.maxX - bb.minX);
                            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
                            double z = bb.minZ + random.nextDouble() * (bb.maxZ - bb.minZ);
                            DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.2F), 1.0F);
                            serverLevel.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0.1);
                        }
                    }
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            
            super.die(source);
            this.onDeath(source);
            return;
        }

        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < 0.4f) { triggerFakeDeath(source); this.onDeath(source); }
        else { super.die(source); this.onDeath(source); }
    }

    private boolean isInventoryEmpty() { for (int i = 0; i < inventory.getSlots(); i++) if (!inventory.getStackInSlot(i).isEmpty()) return false; return true; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.put("Inventory", inventory.serializeNBT()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory")); }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandler.cast(); return super.getCapability(cap, side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemHandler.invalidate(); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IS_ATTACKING, false);
        this.entityData.define(DATA_ATTACK_TYPE, 0);
        this.entityData.define(DATA_IS_SHOCKWAVE_ATTACKING, false);
        this.entityData.define(DATA_SHOCKWAVE_TIMER, 0);
        this.entityData.define(DATA_IS_GASSING, false);
        this.entityData.define(DATA_GASSING_TIMER, 0);
        this.entityData.define(DATA_IS_STOMPING, false);
        this.entityData.define(DATA_STOMP_TYPE, 0);
        this.entityData.define(DATA_STOMP_TIMER, 0);
    }

    public boolean isRunning() { return this.entityData.get(DATA_IS_RUNNING); }
    public boolean isWalking() { return this.entityData.get(DATA_IS_WALKING); }
    private void setRunning(boolean running) { this.entityData.set(DATA_IS_RUNNING, running); }
    private void setWalking(boolean walking) { this.entityData.set(DATA_IS_WALKING, walking); }
    public boolean isInvulnerable() { return this.entityData.get(DATA_IS_INVULNERABLE); }
    public void setInvulnerable(boolean invulnerable) { this.entityData.set(DATA_IS_INVULNERABLE, invulnerable); }

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

    private static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos, RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5, offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);
            pos.set(deathPos.getX() + offsetX, deathPos.getY() + 1 + offsetY, deathPos.getZ() + offsetZ);
            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) level.setBlock(pos, state, 3);
            }
        }
    }

    @Override public boolean startRiding(Entity vehicle, boolean force) { if (vehicle instanceof Boat || vehicle instanceof Minecart) return false; return super.startRiding(vehicle, force); }
    @Override protected boolean canRide(Entity entity) { if (entity instanceof Boat || entity instanceof Minecart) return false; return super.canRide(entity); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return factory; }
    @Override public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) { return false; }

    private final ItemStackHandler inventory = new ItemStackHandler(27) { @Override protected void onContentsChanged(int slot) {} };
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    private ItemStack addItemToInventory(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = insertItemIntoInventory(stack);
        if (remaining.isEmpty()) return ItemStack.EMPTY;
        if (tryPlaceCystAndTransfer()) {
            remaining = insertItemIntoInventory(stack);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        if (!level().isClientSide) Containers.dropItemStack(level(), getX(), getY(), getZ(), remaining);
        return ItemStack.EMPTY;
    }

    private ItemStack insertItemIntoInventory(ItemStack stack) {
        ItemStack copy = stack.copy();
        for (int i = 0; i < inventory.getSlots(); i++) { copy = inventory.insertItem(i, copy, false); if (copy.isEmpty()) break; }
        return copy;
    }

    private boolean tryPlaceCystAndTransfer() {
        if (level().isClientSide) return false;
        boolean hasItems = false;
        for (int i = 0; i < inventory.getSlots(); i++) if (!inventory.getStackInSlot(i).isEmpty()) { hasItems = true; break; }
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
                    for (int j = 0; j < cystInventory.getSlots(); j++) { remaining = cystInventory.insertItem(j, remaining, false); if (remaining.isEmpty()) break; }
                    if (!remaining.isEmpty()) inventory.setStackInSlot(i, remaining);
                    else inventory.setStackInSlot(i, ItemStack.EMPTY);
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
        if (level().getBlockState(belowEntity).isCollisionShapeFullBlock(level(), belowEntity) && level().isEmptyBlock(belowEntity.above())) return belowEntity.above();
        return null;
    }

    // 额外受击体积
    private CustomPart frontPart;
    private CustomPart backPart;
    private boolean backPartRemoved = false;
    private int gasEmitTimer = 0; // 用于每秒喷发气体

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            if (frontPart != null) {
                frontPart.discard();
                frontPart = null;
            }
            if (backPart != null) {
                backPart.discard();
                backPart = null;
            }
        }
        super.remove(reason);
    }

    public static class CustomPart extends Entity {
        // 同步数据：父实体ID（0表示无效）
        private static final EntityDataAccessor<Integer> DATA_PARENT_ID =
                SynchedEntityData.defineId(CustomPart.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Boolean> DATA_IS_BACK =
                SynchedEntityData.defineId(CustomPart.class, EntityDataSerializers.BOOLEAN);
        private static final EntityDataAccessor<Float> DATA_WIDTH =
                SynchedEntityData.defineId(CustomPart.class, EntityDataSerializers.FLOAT);
        private static final EntityDataAccessor<Float> DATA_HEIGHT =
                SynchedEntityData.defineId(CustomPart.class, EntityDataSerializers.FLOAT);

        // 本地缓存字段（客户端从 Data 中读取）
        private float partWidth = 1.0F;
        private float partHeight = 1.0F;
        private boolean partIsBack = false;
        private ReshapeLongarms parent; // 服务端直接赋值，客户端通过ID查找

        public CustomPart(EntityType<?> type, Level level) {
            super(type, level);
            this.setNoGravity(true);
        }

        public void init(ReshapeLongarms parent, float width, float height, boolean isBack) {
            this.parent = parent;
            this.partWidth = width;
            this.partHeight = height;
            this.partIsBack = isBack;

            // 写入同步数据
            this.entityData.set(DATA_PARENT_ID, parent.getId());
            this.entityData.set(DATA_IS_BACK, isBack);
            this.entityData.set(DATA_WIDTH, width);
            this.entityData.set(DATA_HEIGHT, height);

            this.refreshDimensions();
        }

        @Override
        protected void defineSynchedData() {
            this.entityData.define(DATA_PARENT_ID, 0);
            this.entityData.define(DATA_IS_BACK, false);
            this.entityData.define(DATA_WIDTH, 1.0F);
            this.entityData.define(DATA_HEIGHT, 1.0F);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.fixed(this.partWidth, this.partHeight);
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
            // 空实现（部件不保存）
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
            // 空实现（部件不保存）
        }

        @Override
        public void tick() {
            // ========== 客户端逻辑 ==========
            if (this.level().isClientSide) {
                // 1. 同步尺寸和类型
                this.partIsBack = this.entityData.get(DATA_IS_BACK);
                float w = this.entityData.get(DATA_WIDTH);
                float h = this.entityData.get(DATA_HEIGHT);
                if (w != this.partWidth || h != this.partHeight) {
                    this.partWidth = w;
                    this.partHeight = h;
                    this.refreshDimensions();
                }

                // 2. 通过 ID 查找父实体（仅当 parent 为 null 时尝试）
                if (parent == null) {
                    int parentId = this.entityData.get(DATA_PARENT_ID);
                    if (parentId != 0 && this.level() instanceof ClientLevel clientLevel) {
                        Entity e = clientLevel.getEntity(parentId);
                        if (e instanceof ReshapeLongarms rl) {
                            parent = rl;
                        }
                    }
                }

                // 3. 若找到父实体，计算并设置位置
                if (parent != null) {
                    float yaw = parent.getYRot();
                    double forwardX = -Math.sin(Math.toRadians(yaw));
                    double forwardZ = Math.cos(Math.toRadians(yaw));
                    Vec3 forward = new Vec3(forwardX, 0, forwardZ).normalize();
                    Vec3 center = parent.position().add(0, parent.getBbHeight() / 2, 0);

                    Vec3 targetPos;
                    if (this.partIsBack) {
                        targetPos = center.add(forward.scale(-0.2));
                    } else {
                        targetPos = center.add(forward.scale(0.2)).add(0, 0.6, 0);
                    }
                    this.setPos(targetPos);
                }
                return;
            }

            // ========== 服务端逻辑 ==========
            if (parent == null) {
                this.discard();
                return;
            }
            if (!parent.isAlive() && parent.tickCount > 5) {
                this.discard();
            }
            // 服务端位置由父实体的 customServerAiStep 每帧通过 setPos 驱动，无需额外操作
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (parent == null || parent.isRemoved() || parent.isFakingDeath())
                return false;

            float multiplier = partIsBack ? 2.0F : 1.5F;
            float finalDamage = amount * multiplier;
            boolean hurtResult = parent.hurt(source, finalDamage);

            if (partIsBack && hurtResult && finalDamage > 7.0F && !parent.isBackPartRemoved()) {
                parent.removeBackPart();
            }
            return hurtResult;
        }

        @Override
        public boolean isPickable() { return true; }
        @Override
        public boolean isPushable() { return false; }
    }

    public void removeBackPart() {
        if (this.level().isClientSide) return;
        if (backPart != null && !backPart.isRemoved()) {
            backPart.discard();
            backPart = null;
            backPartRemoved = true;
            gasEmitTimer = 0; // 重置计时器
        }
    }

    public boolean isBackPartRemoved() {
        return backPartRemoved;
    }
}