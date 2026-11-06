package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;


import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import org.tdddd.epca.impl.client.entity.IAutoRenderableEntity;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import org.tdddd.epca.impl.utils.EntityHealthUtils;
import org.tdddd.epca.impl.utils.entity.EntityBreakUtils;
import org.tdddd.epca.impl.utils.entity.EntityMovementUtils;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import org.tdddd.epca.impl.overworld.registry.entities.ai.pathing.GapJumper;
import org.tdddd.epca.impl.overworld.registry.entities.ai.pathing.ParasiteGroundNavigation;
import org.tdddd.epca.impl.overworld.registry.entities.ai.pathing.ParasiteMoveControl;
import org.tdddd.epca.impl.overworld.registry.entities.ai.pathing.ParasitePathing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.client.entity.EpcaGeoAnimations;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.*;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;

public class InfestedZombie extends PathfinderMob implements IAutoRenderableEntity, IHeadRotatable, Enemy, IGlowRenderable, GapJumper {

    @Override
    public ResourceLocation getGlowTexture() {
        return new ResourceLocation(epca.MODID, "textures/entity/infested_zombie_glow.png");
    }

    private boolean isHim = false;

    public InfestedZombie(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);

        EpcaEntityManager.track(this);
        this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
        this.setMaxUpStep(0.5F);
        this.fakeDeathEnabled = true;
        this.fakeDeathChance = 40;        this.xpReward = 8;
        this.baseSpeed = 0.27D;
        this.chaseSpeed = 0.38D;
        this.navigation = new ParasiteGroundNavigation(this, level);
        this.moveControl = new ParasiteMoveControl(this, ParasitePathing.MAX_GAP_JUMP_HUMANOID);
    }

    @Override
    public void epcaJumpFromGround() {
        super.jumpFromGround();
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ARMOR, 6.0D)
                .build();
    }

    @Override
    protected void registerGoals() {
        epcaRegisterGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0, 16));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
        this.targetSelector.addGoal(0, new FollowPathGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.level().isClientSide) {

            if (jumpCooldown > 0) jumpCooldown--;

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
                                BlockPos.containing(this.getX() + dir.x, this.getY(), this.getZ() + dir.z));
                        double forward2Y = getGroundHeightAt(
                                BlockPos.containing(this.getX() + dir.x * 2, this.getY(), this.getZ() + dir.z * 2));
                        boolean isCliffToJump = (currentGroundY - forward1Y > 0.5) && (currentGroundY - forward2Y <= 0.5);

                        if (isCliffToJump) {
                            double jumpPower = 0.45;
                            double horizontalSpeed = 0.28;
                            this.setDeltaMovement(dir.x * horizontalSpeed, jumpPower, dir.z * horizontalSpeed);
                            this.hasImpulse = true;
                            this.jumpCooldown = 10;
                        }
                    }
                }
            }
        }
    }

    private double getGroundHeightAt(BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int startY = Math.min((int) this.getY() + 5, level().getMaxBuildHeight());
        mutable.setY(startY);
        while (mutable.getY() > level().getMinBuildHeight()) {
            BlockState state = level().getBlockState(mutable);
            if (state.isSolid()) return mutable.getY() + 1;
            mutable.setY(mutable.getY() - 1);
        }
        return level().getMinBuildHeight();
    }

    @Override
    public void tick() {
        epcaTick();

        if (isFakingDeath()) return;

        applyInfestedMovementSpeed();

        if (!this.level().isClientSide) {
            boolean isMoving = isMoving();
            boolean hasTarget = this.getTarget() != null;
            if (isMoving && hasTarget) {
                setRunning(true);
                setWalking(false);
            } else if (isMoving && !hasTarget) {
                setRunning(false);
                setWalking(true);
            } else {
                setRunning(false);
                setWalking(false);
            }

            recordWaypoint();
            if (--leadCooldown <= 0) {
                tryLeadCompanions();
                leadCooldown = 20;
            }
        }
    }

    protected void performFakeDeathBurst(BlockPos burstPos) {
        infestedPerformFakeDeathBurst(burstPos);

        if (this.level() instanceof ServerLevel serverLevel && burstPos != null) {
            long seed = this.random.nextLong();
            int delay = this.random.nextInt(30) + 40;
            serverLevel.getServer().tell(new TickTask(
                    serverLevel.getServer().getTickCount() + delay,
                    () -> spawnBuglins(serverLevel, burstPos, RandomSource.create(seed))
            ));
        }
    }

    protected void onNormalDeathActions(DamageSource source) {
        if (!this.level().isClientSide && this.random.nextFloat() < 0.3f) {
            WalkingZombieHead head = ModEntities.WALKING_ZOMBIE_HEAD.get().create(this.level());
            if (head != null) {
                head.setPos(this.getX(), this.getY(), this.getZ());
                head.setYRot(this.random.nextFloat() * 360.0F);
                this.level().addFreshEntity(head);
            }
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
                buglin.setPos(pos.getX() + 0.5 + offsetX, pos.getY() + offsetY, pos.getZ() + 0.5 + offsetZ);
                if (buglin instanceof LivingEntity) {
                    ((LivingEntity) buglin).setDeltaMovement(
                            (random.nextDouble() - 0.5) * 0.1,
                            random.nextDouble() * 0.1,
                            (random.nextDouble() - 0.5) * 0.1);
                }
                level.addFreshEntity(buglin);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.INFESTED_ZOMBIE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_ZOMBIE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INFESTED_ZOMBIE_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedZombie> event) {
        if (this.isFakingDeath()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        } else if (this.isRunning()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        } else if (this.isWalking()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    public static boolean checkInfestedZombieSpawnRules(
            EntityType<InfestedZombie> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 4) return false;
        }
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    private final List<Waypoint> waypoints = new ArrayList<>();
    private long lastRecordTime = 0;
    private static final int RECORD_INTERVAL_TICKS = 14 * 20;
    private static final int WAYPOINT_LIFESPAN_TICKS = 15 * 20;
    private int leadCooldown = 0;
    private boolean isPathFollower = false;
    private List<BlockPos> followPath = null;
    private int currentPathIndex = 0;
    private LivingEntity sharedTarget = null;
    private int pathPriority = 0;
    private boolean hasExtendedLifetime = false;

    private void recordWaypoint() {
        long now = level().getGameTime();
        if (now - lastRecordTime >= RECORD_INTERVAL_TICKS) {
            waypoints.add(new Waypoint(blockPosition(), now));
            lastRecordTime = now;
        }
        waypoints.removeIf(wp -> now - wp.recordTime > WAYPOINT_LIFESPAN_TICKS);
    }

    public List<BlockPos> getActivePath() {
        List<BlockPos> active = new ArrayList<>();
        for (Waypoint wp : waypoints) active.add(wp.pos);
        return active;
    }

    private void tryLeadCompanions() {
        if (getTarget() == null) return;
        AABB searchBox = getBoundingBox().inflate(16);
        List<InfestedZombie> allZombies = level().getEntitiesOfClass(InfestedZombie.class, searchBox, z -> z != null);
        List<BlockPos> bestPath = null;
        int bestPriority = -1;
        InfestedZombie bestZombie = null;

        for (InfestedZombie z : allZombies) {
            List<BlockPos> path = z.getActivePath();
            if (!path.isEmpty()) {
                int priority = z.getPathPriority();
                if (priority > bestPriority) {
                    bestPriority = priority;
                    bestPath = path;
                    bestZombie = z;
                } else if (priority == bestPriority && bestPath != null && random.nextBoolean()) {
                    bestPath = path;
                    bestZombie = z;
                }
            }
        }

        if (bestPath == null || bestPath.isEmpty()) return;
        LivingEntity targetToShare = bestZombie != null ? bestZombie.getTarget() : this.getTarget();
        if (targetToShare == null) return;

        List<InfestedZombie> companions = level().getEntitiesOfClass(InfestedZombie.class, searchBox,
                z -> z != this && !z.isPathFollower);
        for (InfestedZombie companion : companions) {
            if (isOnPath(companion.blockPosition(), bestPath)) {
                companion.startFollowingPath(bestPath, targetToShare);
            }
        }
    }

    private boolean isOnPath(BlockPos pos, List<BlockPos> path) {
        for (BlockPos wp : path) if (pos.closerThan(wp, 2.0)) return true;
        return false;
    }

    public boolean isPathFollower() { return isPathFollower; }
    public void startFollowingPath(List<BlockPos> path, LivingEntity target) {
        this.followPath = new ArrayList<>(path);
        this.currentPathIndex = 0;
        this.sharedTarget = target;
        this.isPathFollower = true;
        this.setTarget(null);
    }

    public boolean followPathStep() {
        if (!isPathFollower || followPath == null || currentPathIndex >= followPath.size()) {
            finishFollowing();
            return false;
        }
        BlockPos targetPos = followPath.get(currentPathIndex);
        double distToTarget = distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        if (distToTarget < 1.21) {
            currentPathIndex++;
            if (currentPathIndex >= followPath.size()) {
                finishFollowing();
                return false;
            }
            targetPos = followPath.get(currentPathIndex);
        }
        getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.2);
        return true;
    }

    private void finishFollowing() {
        if (sharedTarget != null && sharedTarget.isAlive() && distanceToSqr(sharedTarget) <= 9) {
            this.setTarget(sharedTarget);
        }
        isPathFollower = false;
        followPath = null;
        sharedTarget = null;
        currentPathIndex = 0;
        getNavigation().stop();
    }

    public void boostPath() {
        if (!hasExtendedLifetime) {
            hasExtendedLifetime = true;
            this.pathPriority++;
            long extraTicks = 15 * 20;
            long now = level().getGameTime();
            for (Waypoint wp : waypoints) wp.recordTime += extraTicks;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && distanceToSqr(target) <= 9 && !level().isClientSide && target instanceof LivingEntity) {
            boostPath();
        }
        return hurt;
    }

    public int getPathPriority() { return pathPriority; }
    public void setPathPriority(int priority) { this.pathPriority = priority; }

    private static class Waypoint {
        BlockPos pos;
        long recordTime;
        Waypoint(BlockPos pos, long time) { this.pos = pos; this.recordTime = time; }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty,
                                        MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData,
                                        @Nullable CompoundTag pDataTag) {
        pSpawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData, pDataTag);

        if (this.random.nextFloat() < 0.01f) {
            this.isHim = true;
        }
        return pSpawnGroupData;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("Him", this.isHim);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Him")) {
            this.isHim = pCompound.getBoolean("Him");
        }
    }

    public boolean isHim() {
        return this.isHim;
    }

    public ResourceLocation model, texture, animation;
    protected static final software.bernie.geckolib.core.animation.RawAnimation ANIM_IDLE = software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("idle");
    protected static final software.bernie.geckolib.core.animation.RawAnimation ANIM_WALK = software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("walk");
    protected static final software.bernie.geckolib.core.animation.RawAnimation ANIM_RUN  = software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("run");
    protected static final software.bernie.geckolib.core.animation.RawAnimation ANIM_DIE  = software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("die");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final EntityDataAccessor<Boolean> DATA_IS_RUNNING =
            SynchedEntityData.defineId(InfestedZombie.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(InfestedZombie.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(InfestedZombie.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(InfestedZombie.class, EntityDataSerializers.BOOLEAN);

    protected double baseMoveSpeed = 0.25D;
    protected double chaseMoveSpeed = 0.38D;

    protected boolean fakeDeathEnabled = false;
    protected int fakeDeathChance = 40;
    protected int fakeDeathDuration = 30;
    protected float fakeDeathHealth = 0.02F;

    protected final EntityMovementUtils.FloatingState floatingState = new EntityMovementUtils.FloatingState();

    protected int breakCooldown = 0;
    protected int jumpCooldown = 0;

    protected int ambientSoundTime;
    protected int minAmbientSoundDelay = 12 * 20;
    protected int maxAmbientSoundDelay = 16 * 20;

    protected int fakeDeathTimer = 0;
    protected BlockPos deathPosition;

    protected int stepSoundDelay = 0;

    @Override public ResourceLocation model()     { return this.model; }
    @Override public ResourceLocation texture()   { return this.texture; }
    @Override public ResourceLocation animation() { return this.animation; }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_RUNNING, false);
        this.entityData.define(DATA_IS_WALKING, false);
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_INVULNERABLE, false);
    }

    public boolean isRunning() { return this.entityData.get(DATA_IS_RUNNING); }
    public void setRunning(boolean running) { this.entityData.set(DATA_IS_RUNNING, running); }

    public boolean isWalking() { return this.entityData.get(DATA_IS_WALKING); }
    public void setWalking(boolean walking) { this.entityData.set(DATA_IS_WALKING, walking); }

    public boolean isFakingDeath() { return this.entityData.get(DATA_IS_FAKING_DEATH); }
    protected void setFakingDeath(boolean faking) { this.entityData.set(DATA_IS_FAKING_DEATH, faking); }

    public boolean isInvulnerable() { return this.entityData.get(DATA_IS_INVULNERABLE); }
    public void setInvulnerable(boolean invulnerable) { this.entityData.set(DATA_IS_INVULNERABLE, invulnerable); }

    public static AttributeSupplier.Builder createBaseAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    protected void epcaRegisterGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    protected void epcaTick() {
        super.tick();

        if (!this.level().isClientSide) {
            boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean hasTarget = this.getTarget() != null;
            setRunning(moving && hasTarget);
            setWalking(moving && !hasTarget);
            if (!moving) { setRunning(false); setWalking(false); }

            if (this.getTarget() == null && --this.ambientSoundTime <= 0) {
                this.ambientSoundTime = this.random.nextInt(minAmbientSoundDelay, maxAmbientSoundDelay);
                SoundEvent ambient = getAmbientSound();
                if (ambient != null) this.playSound(ambient, 1.0F, 1.0F);
            }

            breakCooldown = EntityBreakUtils.tryBreakLightSources(this, breakCooldown);

            EntityMovementUtils.updateWaterFloating(this, floatingState);
        }

        if (isFakingDeath()) {
            if (!this.level().isClientSide) {
                fakeDeathTimer--;
                if (fakeDeathTimer <= 0) {
                    onFakeDeathEnd();
                }
            }
        }
    }

    protected boolean tryFakeDeath() {
        if (!fakeDeathEnabled || isFakingDeath()) return false;
        if (fakeDeathChance > 0 && this.getRandom().nextInt(100) >= fakeDeathChance) return false;
        startFakeDeath();
        return true;
    }

    protected void startFakeDeath() {
        if (isFakingDeath()) return;
        setFakingDeath(true);
        setInvulnerable(true);
        fakeDeathTimer = fakeDeathDuration;
        deathPosition = this.blockPosition();
        this.setBurstHealth(fakeDeathHealth);
        this.setNoAi(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
    }

    protected void setBurstHealth(float requested) {
        this.setHealth(EntityHealthUtils.burstHealth(this, requested));
    }

    protected void onFakeDeathEnd() {
        setFakingDeath(false);
        if (!this.level().isClientSide) {
            onFakeDeathBurst();
        }
        triggerDismemberment();
        this.discard();
    }

    protected void onFakeDeathBurst() {
        infestedPerformFakeDeathBurst(deathPosition);
    }

    public BlockPos getDeathPosition() { return deathPosition; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerable()) return false;
        return epcaHurt(source, amount);
    }

    protected boolean epcaHurt(DamageSource source, float amount) {
        if (isFakingDeath()) return false;

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (ParasiteEvents.shouldIgnoreDamageFrom(this, attacker)) return false;
        }

        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            ParasiteEvents.onAttacked(this, attacker);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (isFakingDeath()) {
            epcaDie(source);
            return;
        }

        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = 0;
            if (minKills > 0) {
                int currentDeaths = 0;
                if (false) {
                    ;
                    this.deathTime = 0;
                    this.setHealth(this.getMaxHealth());
                    this.clearFire();
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            epcaDie(source);
            return;
        }

        if (!this.level().isClientSide && this.fakeDeathEnabled && this.getHealth() <= 0.0F && this.random.nextFloat() < fakeDeathBurstChance) {
            onTriggerFakeDeath(source);
        } else {
            onNormalDeathActions(source);
            epcaDie(source);
        }
    }

    protected void epcaDie(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }
        if (!this.level().isClientSide && tryFakeDeath()) {
            return;
        }
        super.die(source);
    }

    protected void onTriggerFakeDeath(DamageSource source) {
        startFakeDeath();
    }

        public boolean canPassThroughInfestedLeaves() { return true; }

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
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            EntityMovementUtils.applyWaterTravel(this, travelVector);
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected boolean isAffectedByFluids() { return true; }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) { return false; }

    protected void applyMovementSpeed() {
        var attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double target = this.getTarget() != null ? chaseMoveSpeed : baseMoveSpeed;
            if (attr.getBaseValue() != target) attr.setBaseValue(target);
        }
    }

    protected void tickStepSounds(SoundEvent stepSound, int minDelay, int maxDelay, float volume) {
        if (!this.level().isClientSide && this.onGround() && isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(stepSound, volume, 1.0F);
                this.stepSoundDelay = minDelay + this.random.nextInt(maxDelay - minDelay + 1);
            } else {
                this.stepSoundDelay--;
            }
        } else {
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }
    }

    protected boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    private void triggerDismemberment() {
        try {
            Class.forName("me.ichun.mods.mobdismemberment.common.MobDismemberment")
                    .getMethod("trigger", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, this);
        } catch (Throwable ignored) {

        }
    }

    protected static final UUID WANDER_SPEED_ID = UUID.fromString("A3766B59-7066-4402-AD81-0E3B7B6C2B9B");
    protected static final AttributeModifier WANDER_SPEED_REDUCTION =
            new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);

    protected double baseSpeed = 0.27D;
    protected double chaseSpeed = 0.38D;

    protected float fakeDeathBurstChance = 0.4f;

    protected void infestedPerformFakeDeathBurst(BlockPos burstPos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (burstPos == null) burstPos = this.blockPosition();

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        serverLevel.sendParticles(ModParticles.PARA_CURSE.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                10, 0.3, 0.45, 0.3, 0.0);

        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                7, 0.4, 0.3, 0.4, 0.2);

        BlockPos deathPos = burstPos;
        long seed = this.random.nextLong();
        int delay = this.random.nextInt(30) + 40;
        serverLevel.getServer().tell(new TickTask(
                serverLevel.getServer().getTickCount() + delay,
                () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))
        ));

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
        cloud.setRadius(1.5F);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 1200, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
        serverLevel.addFreshEntity(cloud);
    }

    protected static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide || deathPos == null) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_LARGE.get().defaultBlockState(), 1);

        int mediumCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_MEDIUM.get().defaultBlockState(), mediumCount);

        int smallCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_SMALL.get().defaultBlockState(), smallCount);
    }

    protected static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos,
                                            RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5;
            double offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);

            pos.set(
                    deathPos.getX() + offsetX,
                    deathPos.getY() + 1 + offsetY,
                    deathPos.getZ() + offsetZ
            );

            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }

    protected void applyInfestedMovementSpeed() {
        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute == null) return;

        movementAttribute.removeModifier(WANDER_SPEED_ID);

        if (this.getTarget() != null) {
            movementAttribute.setBaseValue(chaseSpeed);
        } else {
            movementAttribute.setBaseValue(baseSpeed);
            movementAttribute.addTransientModifier(WANDER_SPEED_REDUCTION);
        }
    }

    protected void tickStepSounds(SoundEvent stepSound) {
        tickStepSounds(stepSound, 20, 30, 1.0F);
    }

        public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            if (this.level() instanceof ServerLevel serverLevel) {
                EvolutionManager.forDimension(serverLevel).addPoints(1);
            }
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
        }
    }
}
