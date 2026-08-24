package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.BoneArrow;
import org.tdddd.epca.impl.overworld.registry.entities.entity.base.AbstractInfestedEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class InfestedSkeleton extends AbstractInfestedEntity implements RangedAttackMob {

    public enum Variant { DEFAULT, FIRED }

    // ────────── Variant synched data ──────────
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_POSE_ANIM =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SHOOT_ANIM =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SHOOT_ANIM_TIMER =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TARGET_PITCH =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHOOT_THETA =
            SynchedEntityData.defineId(InfestedSkeleton.class, EntityDataSerializers.FLOAT);

    // ────────── Ranged attack config ──────────
    private static final int ATTACK_COOLDOWN = 40;
    private static final int MAX_RANGE = 21;
    private static final float ARROW_SPEED = 1.6F;
    private static final Vec3 LOCATOR_OFFSET = new Vec3(0.0, 1.2, 0.5);

    // ────────── Retreat config ──────────
    private static final double SAFE_DISTANCE = 0.0D;
    private static final double MIN_DISTANCE = 0.0D;
    private boolean isRetreating = false;
    private int retreatCooldown = 0;
    private static final int RETREAT_COOLDOWN = 5;

    private static final ThreadLocal<MobSpawnType> SPAWN_TYPE = new ThreadLocal<>();

    // ────────── Constructor ──────────

    public InfestedSkeleton(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.baseSpeed = 0.21D;
        this.chaseSpeed = 0.29D;

        if (!level.isClientSide) {
            MobSpawnType spawnType = SPAWN_TYPE.get();
            SPAWN_TYPE.remove();
            int stage = EvolutionManager.getStageForDimension((ServerLevel) level);
            boolean allowFireVariant = (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION)
                    ? (stage >= 4) : true;
            if (allowFireVariant && this.random.nextFloat() < 0.3f) {
                this.setVariant(Variant.FIRED);
            } else {
                this.setVariant(Variant.DEFAULT);
            }
        }
        this.navigation = new GroundPathNavigation(this, level);
    }

    // ────────── Variant ──────────

    public Variant getVariant() {
        Integer ordinal = this.entityData.get(DATA_VARIANT);
        if (ordinal == null) return Variant.DEFAULT;
        int index = Mth.clamp(ordinal, 0, Variant.values().length - 1);
        return Variant.values()[index];
    }

    public void setVariant(Variant variant) { this.entityData.set(DATA_VARIANT, variant.ordinal()); }

    public ResourceLocation getTextureResource() {
        return switch (getVariant()) {
            case FIRED -> new ResourceLocation("epca", "textures/entity/infested_skeleton_fired.png");
            default -> new ResourceLocation("epca", "textures/entity/infested_skeleton.png");
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", this.getVariant().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant", 8)) {
            try { this.setVariant(Variant.valueOf(tag.getString("Variant"))); }
            catch (IllegalArgumentException e) { this.setVariant(Variant.DEFAULT); }
        }
    }

    // ────────── Synched data ──────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_POSE_ANIM, "none");
        this.entityData.define(DATA_SHOOT_ANIM, false);
        this.entityData.define(DATA_SHOOT_ANIM_TIMER, 0);
        this.entityData.define(DATA_TARGET_PITCH, 0.0F);
        this.entityData.define(DATA_SHOOT_THETA, 0.0F);
        this.entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
    }

    public String getPoseAnim() { return this.entityData.get(DATA_POSE_ANIM); }
    public int getShootAnimationTimer() { return this.entityData.get(DATA_SHOOT_ANIM_TIMER); }
    private void setShootAnimationTimer(int timer) { this.entityData.set(DATA_SHOOT_ANIM_TIMER, timer); }
    public float getShootTheta() { return this.entityData.get(DATA_SHOOT_THETA); }

    // ────────── Attributes ──────────

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ARMOR, 2.0D)
                .build();
    }

    // ────────── AI Goals ──────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, ATTACK_COOLDOWN, MAX_RANGE) {
            @Override
            public boolean canUse() {
                LivingEntity target = InfestedSkeleton.this.getTarget();
                if (target == null || !target.isAlive()) return false;
                return super.canUse() && InfestedSkeleton.this.distanceToSqr(target) <= MAX_RANGE * MAX_RANGE;
            }
        });
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 48.0D));
    }

    // ────────── Ranged Attack ──────────

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (isFakingDeath()) return;
        Vec3 worldLocatorPos = calculateWorldLocatorPosition();
        BoneArrow arrow = new BoneArrow(this.level(), this);
        arrow.setPos(worldLocatorPos.x, worldLocatorPos.y, worldLocatorPos.z);

        double d0 = target.getX() - worldLocatorPos.x;
        double d1 = target.getY(0.3333333333333333D) - worldLocatorPos.y;
        double d2 = target.getZ() - worldLocatorPos.z;
        double horizontalDist = Math.sqrt(d0 * d0 + d2 * d2);
        arrow.shoot(d0, d1 + horizontalDist * 0.2D, d2, ARROW_SPEED, 0.5F);
        arrow.setYRot(this.getYRot());
        arrow.yRotO = this.getYRot();

        if (getVariant() == Variant.FIRED) {
            arrow.setRemainingFireTicks(600);
            arrow.getPersistentData().putBoolean("InfestedFireArrow", true);
        }

        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
        this.level().addFreshEntity(arrow);
        setShootAnimationTimer(5);
    }

    private Vec3 calculateWorldLocatorPosition() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double dx = LOCATOR_OFFSET.x * cos - LOCATOR_OFFSET.z * sin;
        double dz = LOCATOR_OFFSET.x * sin + LOCATOR_OFFSET.z * cos;
        return this.position().add(dx, LOCATOR_OFFSET.y, dz);
    }

    // ────────── Tick ──────────

    @Override
    public void tick() {
        super.tick();

        if (isFakingDeath()) return;

        if (!this.level().isClientSide) {
            if (getShootAnimationTimer() > 0) setShootAnimationTimer(getShootAnimationTimer() - 1);
            this.entityData.set(DATA_TARGET_PITCH, getTargetPitchDeg());

            // Animation state
            boolean isMoving = isMoving();
            boolean hasTarget = this.getTarget() != null;
            if (isRetreating) {
                setWalking(true); setRunning(false);
            } else {
                setRunning(isMoving && hasTarget);
                setWalking(isMoving && !hasTarget);
            }

            // Movement speed
            applyInfestedMovementSpeed();

            // Retreat logic
            LivingEntity target = this.getTarget();
            if (target != null && !isFakingDeath()) {
                if (retreatCooldown > 0) retreatCooldown--;
                if (this.distanceTo(target) < MIN_DISTANCE && retreatCooldown <= 0) {
                    startRetreating(target);
                    retreatCooldown = RETREAT_COOLDOWN;
                }
                if (isRetreating) {
                    updateRetreatMovement(target);
                    if (this.distanceTo(target) >= SAFE_DISTANCE) stopRetreating();
                }
            } else if (isRetreating) stopRetreating();

            // Step sounds
            tickStepSounds(ModSoundEvents.INFESTED_SKELETON_STEP.get());
        }
    }

    private float getTargetPitchDeg() {
        LivingEntity target = this.getTarget();
        if (target == null) return 0.0F;
        double dx = target.getX() - this.getX();
        double dy = target.getY() + target.getEyeHeight() - (this.getY() + this.getEyeHeight());
        double dz = target.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        return (float) Mth.clamp(Math.toDegrees(Math.atan2(dy, horizontalDist)), -30.0, 90.0);
    }

    // ────────── Retreat ──────────

    private void startRetreating(LivingEntity target) {
        if (target == null) return;
        isRetreating = true;
        this.getNavigation().stop();
    }

    private void updateRetreatMovement(LivingEntity target) {
        if (target == null) return;
        Vec3 awayDir = this.position().subtract(target.position()).normalize();
        this.getNavigation().moveTo(awayDir.scale(8.0).add(this.position()).x,
                this.getY(), awayDir.scale(8.0).add(this.position()).z, 1.0);
    }

    private void stopRetreating() {
        isRetreating = false;
        this.getNavigation().stop();
    }

    // ────────── Normal death ──────────

    @Override
    protected void onNormalDeathActions(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            WalkingSkeletonHead head = ModEntities.WALKING_SKELETON_HEAD.get().create(serverLevel);
            if (head != null) {
                head.setPos(this.getX(), this.getY(), this.getZ());
                head.setYRot(this.random.nextFloat() * 360.0F);
                head.setVariant(WalkingSkeletonHead.Variant.valueOf(getVariant().name()));
                serverLevel.addFreshEntity(head);
            }
        }
    }

    // ────────── Sounds ──────────

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.INFESTED_SKELETON_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.INFESTED_SKELETON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.INFESTED_SKELETON_DEATH.get();
    }

    // ────────── Animation ──────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<InfestedSkeleton> event) {
        if (this.isFakingDeath())
            event.getController().setAnimation(RawAnimation.begin().thenLoop("dead"));
        else if (this.isRunning())
            event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
        else if (this.isWalking())
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        else
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    // ────────── Spawn Rules ──────────

    public static boolean checkInfestedSkeletonSpawnRules(
            EntityType<InfestedSkeleton> entityType, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 5) return false;
        }
        SPAWN_TYPE.set(spawnType);
        return level.getMaxLocalRawBrightness(pos) < 8;
    }
}
