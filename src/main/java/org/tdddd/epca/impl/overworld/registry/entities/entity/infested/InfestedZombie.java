package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.client.entity.IHeadRotatable;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowPathGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.base.AbstractInfestedEntity;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;

public class InfestedZombie extends AbstractInfestedEntity implements IHeadRotatable, IGlowRenderable {

    @Override
    public ResourceLocation getGlowTexture() {
        return new ResourceLocation(epca.MODID, "textures/entity/infested_zombie_glow.png");
    }

    private boolean isHim = false;

    public InfestedZombie(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.baseSpeed = 0.27D;
        this.chaseSpeed = 0.38D;
        this.navigation = new GroundPathNavigation(this, level);
    }

    // ────────── Attributes ──────────

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

    // ────────── AI Goals ──────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
        this.targetSelector.addGoal(0, new FollowPathGoal(this));
    }

    // ────────── Custom AI Step ──────────

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.level().isClientSide) {
            // Cliff jumping
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

    // ────────── Tick ──────────

    @Override
    public void tick() {
        super.tick();

        if (isFakingDeath()) return;

        // Movement speed
        applyInfestedMovementSpeed();

        // Animation state update
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

            // Waypoint and lead
            recordWaypoint();
            if (--leadCooldown <= 0) {
                tryLeadCompanions();
                leadCooldown = 20;
            }
        }
    }

    // ────────── Fake death burst ──────────

    @Override
    protected void performFakeDeathBurst(BlockPos burstPos) {
        super.performFakeDeathBurst(burstPos);
        // Additionally spawn buglins
        if (this.level() instanceof ServerLevel serverLevel && burstPos != null) {
            long seed = this.random.nextLong();
            int delay = this.random.nextInt(30) + 40;
            serverLevel.getServer().tell(new TickTask(
                    serverLevel.getServer().getTickCount() + delay,
                    () -> spawnBuglins(serverLevel, burstPos, RandomSource.create(seed))
            ));
        }
    }

    // ────────── Normal death actions ──────────

    @Override
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

    // ────────── Buglin spawning ──────────

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

    // ────────── Ambience ──────────

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

    // ────────── Animation ──────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 4, this::animationPredicate));
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

    // ────────── Spawn Rules ──────────

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

    // ═══════════════════════════════════════════════════════════════
    // Waypoint / Path following system (InfestedZombie-specific)
    // ═══════════════════════════════════════════════════════════════

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
        // 1% 概率标记为稀有
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
}
