package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class InfestedBat extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(InfestedBat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(InfestedBat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CURRENT_STATE =
            SynchedEntityData.defineId(InfestedBat.class, EntityDataSerializers.INT);
    // ========== 新增：倒挂数据 ==========
    private static final EntityDataAccessor<Boolean> DATA_IS_RESTING =
            SynchedEntityData.defineId(InfestedBat.class, EntityDataSerializers.BOOLEAN);
    // ==================================

    public enum BatState {
        IDLE,
        HOVERING,
        DIVING,
        SUCKING,
        LEAVING
    }
    private BatState currentState = BatState.IDLE;
    private int hoverTimer;
    private int suckTimer;
    private int leaveTimer;
    private float circleRadius;
    private float angle;
    private boolean clockwise;
    private double targetYOffset;
    private Vec3 leaveDirection;
    private boolean wasHurtDuringSuck;
    private BlockPos deathPosition;
    private int fakeDeathTimer = 14;
    private Vec3 flyDirection;
    private int flyTimer;

    public InfestedBat(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 4;
        this.setNoGravity(true);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 0.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new InfestedBatAttackGoal());
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 24.0D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
        this.entityData.define(DATA_CURRENT_STATE, BatState.IDLE.ordinal());
        // ========== 新增 ==========
        this.entityData.define(DATA_IS_RESTING, false);
        // ===========================
    }

    public BatState getCurrentState() {
        return BatState.values()[this.entityData.get(DATA_CURRENT_STATE)];
    }

    public void setCurrentState(BatState state) {
        this.currentState = state;
        this.entityData.set(DATA_CURRENT_STATE, state.ordinal());
        if (state == BatState.HOVERING) {
            this.navigation.stop();
        }
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

    // ========== 新增：倒挂相关方法 ==========
    public boolean isResting() {
        return this.entityData.get(DATA_IS_RESTING);
    }

    public void setResting(boolean resting) {
        this.entityData.set(DATA_IS_RESTING, resting);
        if (resting) {
            this.setPose(Pose.STANDING);
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setPose(Pose.STANDING);
        }
    }

    private BlockPos findRestingPosition() {
        RandomSource random = this.random;
        Level level = this.level();
        if (level == null) return null;
        for (int attempt = 0; attempt < 20; attempt++) {
            int xOff = random.nextInt(9) - 4;
            int zOff = random.nextInt(9) - 4;
            int yOff = random.nextInt(7) - 3;
            BlockPos pos = this.blockPosition().offset(xOff, yOff, zOff);
            if (isValidRestingPosition(pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isValidRestingPosition(BlockPos pos) {
        Level level = this.level();
        if (level == null) return false;
        BlockState aboveState = level.getBlockState(pos.above());
        if (!aboveState.isSolid()) return false;
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.below())) {
            return false;
        }
        return true;
    }
    // ========================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            if (shouldIgnoreDamageFrom(attacker)) {
                return false;
            }
        }
        if (isFakingDeath() || isInvulnerable()) {
            return false;
        }

        if (this.getCurrentState() == BatState.SUCKING) {
            this.wasHurtDuringSuck = true;
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }

        float adjustedAmount = ((IParasite) this).onHurt(source, amount);
        boolean result = super.hurt(source, adjustedAmount);
        return result;
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

    private void triggerFakeDeath(DamageSource source) {
        setFakingDeath(true);
        setInvulnerable(true);
        fakeDeathTimer = 14;
        deathPosition = this.blockPosition();
        this.setHealth(0.02F);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
        this.setCurrentState(BatState.IDLE);
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

        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < 1.0f) {
            triggerFakeDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (isFakingDeath()) {
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ModParticles.COTH.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                10, 0.3, 0.45, 0.3, 0.0);
                        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                                this.getX(), this.getY() + 0.5, this.getZ(),
                                7, 0.4, 0.3, 0.4, 0.2);
                        BlockPos deathPos = this.deathPosition;
                        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
                        cloud.setRadius(1.0F);
                        cloud.setDuration(60);
                        cloud.setRadiusPerTick(0);
                        cloud.setWaitTime(0);
                        cloud.addEffect(new MobEffectInstance(
                                ModEffects.COTH.get(),
                                1200, 0, false, true
                        ));
                        serverLevel.addFreshEntity(cloud);
                    }
                }
                this.discard();
            }
            return;
        }

        if (this.level().isClientSide) {
            return;
        }

        if (this.getTarget() == null || !this.getTarget().isAlive()) {
            if (this.getCurrentState() != BatState.IDLE) {
                this.setCurrentState(BatState.IDLE);
            }

            if (!this.isFakingDeath()) {
                if (!isResting()) {
                    BlockPos pos = findRestingPosition();
                    if (pos != null) {
                        setResting(true);
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 0.2;
                        double z = pos.getZ() + 0.5;
                        this.setPos(x, y, z);
                        this.setXRot(0);
                        this.setYRot(0);
                        this.setDeltaMovement(Vec3.ZERO);
                    }
                } else {
                    BlockPos currentPos = this.blockPosition();
                    if (!isValidRestingPosition(currentPos)) {
                        setResting(false);
                    } else {
                        double x = currentPos.getX() + 0.5;
                        double y = currentPos.getY() + 0.2;
                        double z = currentPos.getZ() + 0.5;
                        this.setPos(x, y, z);
                        this.setDeltaMovement(Vec3.ZERO);
                        this.setXRot(0);
                        this.setYRot(0);
                    }
                }
                if (isResting()) {
                    return;
                }
            }

            if (this.getCurrentState() == BatState.IDLE && !this.isFakingDeath()) {
                if (flyDirection == null || flyTimer <= 0) {
                    float yaw = this.random.nextFloat() * 2 * (float) Math.PI;
                    float pitch = (this.random.nextFloat() - 0.5f) * 0.6f;
                    flyDirection = new Vec3(
                            Math.cos(yaw) * Math.cos(pitch),
                            Math.sin(pitch),
                            Math.sin(yaw) * Math.cos(pitch)
                    ).normalize();
                    flyTimer = 40 + this.random.nextInt(40); // 2~4 秒
                } else {
                    flyTimer--;
                }

                double speed = 0.36;
                Vec3 motion = flyDirection.scale(speed);
                motion = clampMovement(motion, 0.3);

                this.move(MoverType.SELF, motion);
                setDeltaMovement(Vec3.ZERO);

                if (motion.lengthSqr() > 1e-6) {
                    float yaw = (float) (Math.atan2(motion.z, motion.x) * 180 / Math.PI) - 90;
                    setYRot(yaw);
                }

                if (motion.lengthSqr() > 0 && this.getDeltaMovement().lengthSqr() < 0.001) {
                    flyTimer = 0;
                }
            }
        } else {
            if (isResting()) {
                setResting(false);
            }
        }
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) {
            return false;
        }
        return super.startRiding(vehicle, force);
    }

    @Override
    protected boolean canRide(Entity entity) {
        if (entity instanceof Boat || entity instanceof Minecart) {
            return false;
        }
        return super.canRide(entity);
    }

    public static boolean checkInfestedBatSpawnRules(
            EntityType<InfestedBat> entityType,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 2 || stage > 5) {
                return false;
            }

            if (levelAccessor.getRawBrightness(pos, 0) >= 8) {
                return false;
            }

            int skyLight = levelAccessor.getBrightness(LightLayer.SKY, pos);
            if (skyLight != 0) {
                long dayTime = levelAccessor.getLevel().getDayTime() % 24000;
                if (dayTime < 12000 || dayTime >= 24000) {
                    return false;
                }
            }

            return true;
        }
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedBat> event) {
        BatState state = this.getCurrentState();

        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        } else if (this.isResting()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        } else switch (state) {
            case SUCKING:
                event.getController().setAnimation(RawAnimation.begin().thenLoop("sucking"));
                break;
            case IDLE, HOVERING, LEAVING:
            default:
                event.getController().setAnimation(RawAnimation.begin().thenLoop("fly"));
                break;
        }
        return PlayState.CONTINUE;
    }

    private class InfestedBatAttackGoal extends Goal {
        private LivingEntity target;

        public InfestedBatAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isFakingDeath() || isInvulnerable()) return false;
            target = getTarget();
            if (target == null || !target.isAlive()) return false;
            BatState state = getCurrentState();
            return state == BatState.IDLE || state == BatState.HOVERING;
        }

        @Override
        public boolean canContinueToUse() {
            if (isFakingDeath()) return false;
            if (target == null || !target.isAlive()) return false;
            BatState state = getCurrentState();
            return state == BatState.HOVERING || state == BatState.DIVING || state == BatState.SUCKING || state == BatState.LEAVING;
        }

        @Override
        public void start() {
            startHovering();
        }

        @Override
        public void stop() {
            setCurrentState(BatState.IDLE);
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void tick() {
            if (target == null || !target.isAlive()) {
                stop();
                return;
            }

            BatState state = getCurrentState();
            switch (state) {
                case HOVERING:
                    tickHovering();
                    break;
                case DIVING:
                    tickDiving();
                    break;
                case SUCKING:
                    tickSucking();
                    break;
                case LEAVING:
                    tickLeaving();
                    break;
                default:
                    startHovering();
                    break;
            }
        }

        private void startHovering() {
            setCurrentState(BatState.HOVERING);
            setDeltaMovement(Vec3.ZERO);
            circleRadius = 3.2f + random.nextFloat() * 1.5f;
            clockwise = random.nextBoolean();
            angle = random.nextFloat() * (float) (2 * Math.PI);
            targetYOffset = 3.0 + random.nextFloat();
            hoverTimer = 120 + random.nextInt(40);
            wasHurtDuringSuck = false;
            if (isPassenger()) stopRiding();
        }

        private void tickHovering() {
            if (target == null) return;

            double targetX = target.getX();
            double targetY = target.getY() + target.getBbHeight() + targetYOffset;
            double targetZ = target.getZ();

            double xOff = circleRadius * Math.cos(angle);
            double zOff = circleRadius * Math.sin(angle);

            double finalX = targetX + xOff;
            double finalY = targetY;
            double finalZ = targetZ + zOff;

            BlockPos checkPos = new BlockPos(Mth.floor(finalX), Mth.floor(finalY), Mth.floor(finalZ));
            if (!level().isEmptyBlock(checkPos) || !level().getFluidState(checkPos).isEmpty()) {
                finalY += 0.5;
            }

            Vec3 currentPos = new Vec3(getX(), getY(), getZ());
            Vec3 targetPos = new Vec3(finalX, finalY, finalZ);
            Vec3 delta = targetPos.subtract(currentPos);

            double maxSpeed = 0.3;
            delta = clampMovement(delta, maxSpeed);

            InfestedBat.this.move(MoverType.SELF, delta);
            setDeltaMovement(Vec3.ZERO);

            if (delta.lengthSqr() > 1e-6) {
                float yaw = (float) (Math.atan2(delta.z, delta.x) * 180 / Math.PI) - 90;
                setYRot(yaw);
            }

            float angularSpeed = (float) (1.0 / circleRadius);
            angle += angularSpeed * (clockwise ? 1 : -1);

            hoverTimer--;
            if (hoverTimer <= 0) {
                setCurrentState(BatState.DIVING);
            }
        }

        private void tickDiving() {
            if (target == null) {
                setCurrentState(BatState.IDLE);
                return;
            }

            double targetX = target.getX();
            double targetY = target.getY() + target.getBbHeight() + 0.1;
            double targetZ = target.getZ();

            Vec3 currentPos = new Vec3(getX(), getY(), getZ());
            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
            Vec3 delta = targetPos.subtract(currentPos);
            double dist = delta.length();

            if (dist < 0.3) {
                setCurrentState(BatState.SUCKING);
                suckTimer = 0;
                InfestedBat.this.setPos(targetX, targetY, targetZ);
                setDeltaMovement(Vec3.ZERO);
                return;
            }

            double speed = 1.0;
            Vec3 motion = delta.normalize().scale(speed);
            motion = clampMovement(motion, 1.0);

            InfestedBat.this.move(MoverType.SELF, motion);
            setDeltaMovement(Vec3.ZERO);

            if (motion.lengthSqr() > 1e-6) {
                float yaw = (float) (Math.atan2(motion.z, motion.x) * 180 / Math.PI) - 90;
                setYRot(yaw);
                lookAt(target, 30.0F, 30.0F);
            }
        }

        private void tickSucking() {
            if (target == null || !target.isAlive()) {
                setCurrentState(BatState.IDLE);
                return;
            }

            setPos(target.getX(), target.getY() + target.getBbHeight() + 0.1, target.getZ());
            setDeltaMovement(Vec3.ZERO);
            lookAt(target, 30.0F, 30.0F);

            if (tickCount % 20 == 0) {
                if (target.getType() == EntityType.BAT) {
                    convertBatToInfested(target);
                    stopRiding();
                    setCurrentState(BatState.IDLE);
                    this.target = null;
                    return;
                }
                target.hurt(target.damageSources().cactus(), 2.0f);
                MobEffectInstance current = target.getEffect(ModEffects.COTH.get());
                int amp = (current == null) ? 0 : Math.min(current.getAmplifier() + 1, 2);
                target.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, amp, false, true));
                target.addEffect(new MobEffectInstance(ModEffects.FEAR.get(), 200, 0, false, true));
            }

            suckTimer++;

            boolean shouldLeave = false;
            if (suckTimer >= 40) {
                if (suckTimer % 20 == 0) {
                    if (random.nextFloat() < 0.5f) shouldLeave = true;
                    if (wasHurtDuringSuck && random.nextFloat() < 0.75f) shouldLeave = true;
                }
            }
            if (suckTimer >= 120) shouldLeave = true;

            if (shouldLeave) {
                if (random.nextFloat() < 0.3f) {
                    target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 200, 0, false, true));
                }
                setCurrentState(BatState.LEAVING);
                leaveTimer = 0;
                Vec3 dir = new Vec3(
                        getX() - target.getX(),
                        1.5 + random.nextFloat() * 1.0,
                        getZ() - target.getZ()
                ).normalize();
                leaveDirection = dir.scale(0.01);
                setDeltaMovement(leaveDirection);
                setPos(getX() + leaveDirection.x * 2, getY() + 1, getZ() + leaveDirection.z * 2);
            }
        }

        private void tickLeaving() {
            if (target == null || !target.isAlive()) {
                setCurrentState(BatState.IDLE);
                return;
            }

            leaveTimer++;
            if (leaveTimer <= 10) {
                Vec3 motion = leaveDirection;
                motion = clampMovement(motion, 0.3);
                InfestedBat.this.move(MoverType.SELF, motion);
                setDeltaMovement(Vec3.ZERO);
                if (motion.lengthSqr() > 1e-6) {
                    float yaw = (float) (Math.atan2(motion.z, motion.x) * 180 / Math.PI) - 90;
                    setYRot(yaw);
                }
            } else {
                startHovering();
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    private void convertBatToInfested(LivingEntity bat) {
        if (bat.level().isClientSide) return;
        if (!(bat.level() instanceof ServerLevel serverLevel)) return;

        InfestedBat newBat = ModEntities.INFESTED_BAT.get().create(serverLevel);
        if (newBat == null) return;

        newBat.setPos(bat.getX(), bat.getY(), bat.getZ());
        newBat.setYRot(bat.getYRot());
        newBat.setXRot(bat.getXRot());
        if (bat.hasCustomName()) {
            newBat.setCustomName(bat.getCustomName());
            newBat.setCustomNameVisible(bat.isCustomNameVisible());
        }

        serverLevel.playSound(null, bat.getX(), bat.getY(), bat.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                bat.getX(), bat.getY() + bat.getBbHeight() / 2, bat.getZ(),
                5, 0.5, 0.5, 0.5, 0.1);

        bat.remove(Entity.RemovalReason.KILLED);
        bat.teleportTo(1000000, -4000, 1000000);

        serverLevel.addFreshEntity(newBat);
    }

    private Vec3 clampMovement(Vec3 motion, double maxSpeed) {
        double len = motion.length();
        if (len > maxSpeed) {
            return motion.scale(maxSpeed / len);
        }
        return motion;
    }
}