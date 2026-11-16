package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;


import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.client.entity.EpcaGeoAnimations;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedCaveSpiderWeb;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSpiderWeb;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSpiderWebBlood;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.utils.EntityHealthUtils;
import org.tdddd.epca.impl.overworld.registry.entities.ai.WebAwareRangedAttackGoal;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

public class InfestedSpider extends PathfinderMob implements GeoEntity, Enemy, RangedAttackMob {

    public static final double SPIDER_WALK_SPEED = 0.15D;

    public static final double SPIDER_RUN_SPEED = 0.30D;

    public static final double CAVE_WALK_SPEED = 0.12D;

    public static final double CAVE_RUN_SPEED = 0.36D;

    private static final double WEB_RANGE = 12.0D;

    public static final double MELEE_HORIZONTAL_RANGE = 4.0D;

    public static final double MELEE_HORIZONTAL_RANGE_SQ =
            MELEE_HORIZONTAL_RANGE * MELEE_HORIZONTAL_RANGE;

    private static final int WEB_COOLDOWN_TICKS = 80;

    private static final int SHOOT_ANIMATION_TICKS = 12;

    private static final float WEB_VELOCITY = 1.35F;

    private static final float WEB_INACCURACY = 0.35F;

    private static final double WEB_GRAVITY_PER_TICK = 0.06D;

    private static final double MIN_AIM_RISE = 0.35D;

    private static final int POUNCE_COOLDOWN_TICKS = 40;

    private static final double POUNCE_SPEED_MULTIPLIER = 1.44D;

    private static final double CLIMB_UP_SPEED = 0.2D;

    private static final int CLIMB_COOLDOWN_TICKS = 200;

    private int climbCooldown = 0;

    /** 连续远程弹射物未命中阈值：达到后切换为「近战攀爬追击」状态。 */
    public static final int RANGED_MISS_THRESHOLD = 3;

    /** 弹射物最长飞行时间（tick）：超过仍未命中目标就按一次「未命中」处理。 */
    private static final int WEB_MISS_TIMEOUT_TICKS = 80;

    /** 近战攀爬状态的最长持续时间（tick，20s）：期间一直没打出近战命中就退回远程。 */
    private static final int CLIMB_AGGRO_MAX_TICKS = 400;

    /** 退出近战攀爬状态后的冷却（tick，10s）：冷却内不会再因为未命中重新进入。 */
    private static final int CLIMB_AGGRO_COOLDOWN_TICKS = 200;

    /** 攀爬追击时向上起跳的冷却（tick）。 */
    private static final int CLIMB_JUMP_COOLDOWN_TICKS = 20;

    /** 「当前没有待判定的弹射物」的哨兵值。 */
    private static final int NO_PENDING_WEB_SHOT = Integer.MIN_VALUE;

    /**
     * 攀爬动画的「正在上升」阈值（格/tick）。
     * 0.02 高于重力/贴墙摩擦造成的微小 y 抖动（约 0.001~0.01），小于攀爬推进速度（0.2），
     * 因此只有真正被向上推进时才算上升。
     */
    private static final double CLIMB_ANIM_MIN_RISE = 0.02D;

    /**
     * 攀爬动画的「朝墙内推力」阈值（{@code Entity#zza}，范围 0..1，AI 前进时约为 1）。
     * 低于 0.1 视为「只是贴着墙站着」，不判为攀爬。
     */
    private static final float CLIMB_ANIM_MIN_FORWARD = 0.1F;

    /** 近战攀爬追击状态：由服务端设置并同步，仅影响 AI（不影响攀爬动画判定）。 */
    private int climbAggroTicks;

    private int climbAggroCooldown;

    private int climbJumpCooldown;

    /** 连续远程未命中次数。 */
    private int webMissStreak;

    /** 最近一次发射的弹射物（用于超时 / 消失判定）。 */
    private Entity pendingWebShot;

    /** 最近一次发射时的目标 UUID。 */
    private UUID pendingWebTargetId;

    private int pendingWebShotTick = NO_PENDING_WEB_SHOT;

    private Direction nearWallDir;

    private static final double POUNCE_RANGE = 5.0D;

    private static final float IDLE2_CHANCE = 0.30F;

    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);

    private int fakeDeathTimer = 30;

    private boolean fakeDeathResolved;

    private DamageSource fakeDeathSource;

    private boolean burstApplied;

    private int detonateCountdown;

    private int fakeDeathBurstCountdown;

    public enum Variant {
        DEFAULT,
        BLOOD,
        CAVE
    }

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_POUNCING =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_SHOOTING =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IDLE2 =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);
    /** 近战攀爬追击状态（服务端驱动，同步给客户端便于观察/调试；动画判定不依赖它）。 */
    private static final EntityDataAccessor<Boolean> DATA_CLIMB_AGGRO =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);
    /** 攀爬动画状态（服务端按「水平碰撞 + 上升/朝墙推力」判定后同步，客户端动画控制器读取）。 */
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING_ANIM =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);

    private static final String VARIANT_TAG = "Variant";
    private static final String CLIMB_AGGRO_TAG = "ClimbAggro";
    private static final String WEB_MISS_STREAK_TAG = "WebMissStreak";

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    private int webCooldown;
    private int pounceCooldown;
    private int shootAnimationTicks;
    private int pounceTimer;
    private int idleAnimTimer;

    private boolean spawnBuffsRolled;

    public InfestedSpider(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.navigation = new GroundPathNavigation(this, level);

        this.setMaxUpStep(1.0F);
        this.idleAnimTimer = 100 + this.random.nextInt(200);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, SPIDER_WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
        this.entityData.define(DATA_IS_POUNCING, false);
        this.entityData.define(DATA_IS_SHOOTING, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_IDLE2, false);
        this.entityData.define(DATA_CLIMB_AGGRO, false);
        this.entityData.define(DATA_CLIMBING_ANIM, false);
    }

    public Variant getVariant() {
        Integer ordinal = this.entityData.get(DATA_VARIANT);
        if (ordinal == null) return Variant.DEFAULT;
        return Variant.values()[Mth.clamp(ordinal, 0, Variant.values().length - 1)];
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public boolean isCaveVariant() {
        return getVariant() == Variant.CAVE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(VARIANT_TAG, getVariant().name());
        tag.putBoolean(CLIMB_AGGRO_TAG, isClimbAggro());
        tag.putInt(WEB_MISS_STREAK_TAG, this.webMissStreak);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(VARIANT_TAG, 8)) {
            try {
                setVariant(Variant.valueOf(tag.getString(VARIANT_TAG)));
            } catch (IllegalArgumentException e) {
                setVariant(Variant.DEFAULT);
            }
        }
        setClimbAggro(tag.getBoolean(CLIMB_AGGRO_TAG));
        this.webMissStreak = tag.getInt(WEB_MISS_STREAK_TAG);
    }

    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case CAVE:
                return new ResourceLocation("epca", "textures/entity/infested_cave_spider.png");
            case BLOOD:
                return new ResourceLocation("epca", "textures/entity/infested_spider_blood.png");
            default:
                return new ResourceLocation("epca", "textures/entity/infested_spider.png");
        }
    }

    public boolean isPouncing() {
        return this.entityData.get(DATA_IS_POUNCING);
    }

    private void setPouncing(boolean pouncing) {
        this.entityData.set(DATA_IS_POUNCING, pouncing);
    }

    public boolean isShooting() {
        return this.entityData.get(DATA_IS_SHOOTING);
    }

    private void setShooting(boolean shooting) {
        this.entityData.set(DATA_IS_SHOOTING, shooting);
    }

    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    private void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    public boolean usingIdle2() {
        return this.entityData.get(DATA_IDLE2);
    }

    private void setIdle2(boolean idle2) {
        this.entityData.set(DATA_IDLE2, idle2);
    }

    private static final String CLIP_IDLE1 = "idle1";
    private static final String CLIP_IDLE2 = "idle2";
    private static final String CLIP_WALK = "walk";
    private static final String CLIP_RUN = "run";
    private static final String CLIP_CLIMB = "climb";
    private static final String CLIP_FLYING = "flying";

    private static final String CLIP_SHOOT = "shoot";

    private static final String CLIP_ATTACK = "attack";

    private static final String CLIP_DEAD = "dead";

    private static final int DETONATE_TICKS = 27;

    private static final float SELF_DESTRUCT_RADIUS = 3.0F;

    private String animationSuffix() {
        return isCaveVariant() ? "_cave" : "";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        // 移动控制器统一使用 6 tick 交叉淡入淡出（EpcaGeoAnimations.CLIMB_TRANSITION_TICKS），
        // 让「攀爬 ↔ 走路/待机」不是硬切，而是 6 tick 平滑过渡。
        controllers.add(new AnimationController<>(this, "controller",
                EpcaGeoAnimations.CLIMB_TRANSITION_TICKS, this::animationPredicate));

        AnimationController<InfestedSpider> shootController = new AnimationController<>(this,
                "shoot_controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        shootController.triggerableAnim("shoot", RawAnimation.begin().thenPlay(CLIP_SHOOT));
        controllers.add(shootController);

        AnimationController<InfestedSpider> attackController = new AnimationController<>(this,
                "attack_controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        attackController.triggerableAnim("attack", RawAnimation.begin().thenPlay(CLIP_ATTACK + animationSuffix()));
        controllers.add(attackController);

        AnimationController<InfestedSpider> deadController = new AnimationController<>(this,
                "dead_controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        deadController.triggerableAnim("dead", RawAnimation.begin().thenPlay(CLIP_DEAD + animationSuffix()));
        controllers.add(deadController);
    }

    private PlayState animationPredicate(AnimationState<InfestedSpider> event) {
        String suffix = animationSuffix();

        // 攀爬优先级最高：只要判定为攀爬状态就强制播放攀爬动画，
        // 不受 running/pouncing/移动与否影响（过渡由控制器的 6 tick 交叉淡化负责）。
        if (isClimbingAnimationState()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_CLIMB + suffix));
        } else if (isPouncing()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_FLYING + suffix));
        } else if (isRunning()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_RUN + suffix));
        } else if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_WALK + suffix));
        } else {

            String idle = usingIdle2() ? CLIP_IDLE2 : CLIP_IDLE1;
            event.getController().setAnimation(RawAnimation.begin().thenLoop(idle + suffix));
        }
        return PlayState.CONTINUE;
    }

    private void beginShootAnimation() {
        setShooting(true);
        this.shootAnimationTicks = SHOOT_ANIMATION_TICKS;

        this.triggerAnim("shoot_controller", "shoot");
    }

    public boolean isNearWall() {
        AABB bb = this.getBoundingBox();
        AABB expanded = bb.inflate(0.05);

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = this.blockPosition().relative(dir);
            BlockState state = this.level().getBlockState(neighbor);
            if (state.isSolid()) {
                VoxelShape shape = state.getCollisionShape(this.level(), neighbor);
                if (!shape.isEmpty()) {
                    AABB wallBox = shape.bounds().move(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                    if (expanded.intersects(wallBox)) {

                        this.nearWallDir = dir;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isClimbingWall() {
        if (this.climbCooldown > 0) {
            return false;
        }
        return this.isNearWall() && this.getTarget() != null;
    }

    // ------------------------------------------------------------------
    // 近战攀爬追击状态（远程多次未命中后切换）
    // ------------------------------------------------------------------

    public boolean isClimbAggro() {
        return this.entityData.get(DATA_CLIMB_AGGRO);
    }

    private void setClimbAggro(boolean value) {
        this.entityData.set(DATA_CLIMB_AGGRO, value);
    }

    /**
     * 进入「近战攀爬追击」：不再发射弹射物，改为贴墙攀爬靠近并近战。
     */
    private void enterClimbAggro() {
        if (isClimbAggro()) {
            return;
        }
        setClimbAggro(true);
        this.climbAggroTicks = 0;
        this.climbCooldown = 0;
        this.webMissStreak = 0;
        this.getNavigation().stop();
        this.playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.6F);
    }

    /**
     * 退出「近战攀爬追击」并进入冷却，随后恢复远程。
     *
     * @param cooldownTicks 冷却 tick 数（冷却内不会因为未命中重新进入）
     * @param resetStreak   是否清零未命中计数
     */
    private void exitClimbAggro(int cooldownTicks, boolean resetStreak) {
        if (!isClimbAggro()) {
            return;
        }
        setClimbAggro(false);
        this.climbAggroCooldown = Math.max(this.climbAggroCooldown, cooldownTicks);
        if (resetStreak) {
            this.webMissStreak = 0;
        }
    }

    /**
     * 记录一次「远程未命中」，达到 {@link #RANGED_MISS_THRESHOLD} 后切换到近战攀爬追击。
     */
    private void registerWebMiss() {
        if (isClimbAggro() || this.climbAggroCooldown > 0) {
            return;
        }
        this.webMissStreak++;
        if (this.webMissStreak >= RANGED_MISS_THRESHOLD) {
            enterClimbAggro();
        }
    }

    private void clearPendingWebShot() {
        this.pendingWebShot = null;
        this.pendingWebTargetId = null;
        this.pendingWebShotTick = NO_PENDING_WEB_SHOT;
    }

    /**
     * 弹射物结算回调（命中实体 / 命中方块 / 消失都会走到这里）。
     * <p>
     * 命中当前目标 = 命中，清零计数；命中其它东西 = 未命中，计一次。
     *
     * @param hit 被击中的实体，命中方块时为 {@code null}
     */
    public void onWebProjectileResolved(@Nullable Entity hit) {
        if (this.level().isClientSide || this.pendingWebShotTick == NO_PENDING_WEB_SHOT) {
            return;
        }
        boolean hitTarget = hit != null
                && this.pendingWebTargetId != null
                && this.pendingWebTargetId.equals(hit.getUUID());
        clearPendingWebShot();
        if (hitTarget) {
            this.webMissStreak = 0;
        } else {
            registerWebMiss();
        }
    }

    /** 每个 tick 检查待判定弹射物：消失 / 超时都算一次未命中。 */
    private void tickPendingWebShot() {
        if (this.pendingWebShotTick == NO_PENDING_WEB_SHOT) {
            return;
        }
        Entity shot = this.pendingWebShot;
        boolean gone = shot == null || shot.isRemoved() || shot.level() != this.level();
        boolean timeout = this.tickCount - this.pendingWebShotTick > WEB_MISS_TIMEOUT_TICKS;
        if (!gone && !timeout) {
            return;
        }
        if (!gone) {
            shot.discard();
        }
        clearPendingWebShot();
        registerWebMiss();
    }

    /** 近战攀爬状态自身的生命周期维护（丢失目标 / 超时）。 */
    private void tickClimbAggro() {
        if (!isClimbAggro()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            exitClimbAggro(CLIMB_AGGRO_COOLDOWN_TICKS / 4, true);
            return;
        }
        if (++this.climbAggroTicks > CLIMB_AGGRO_MAX_TICKS) {
            exitClimbAggro(CLIMB_AGGRO_COOLDOWN_TICKS, true);
        }
    }

    /**
     * 攀爬动画判定（第 6 项）：水平碰撞 + 有上升/朝墙推力。
     * <p>
     * 这个规则在服务端计算后同步（{@link #DATA_CLIMBING_ANIM}），因为非玩家生物的
     * {@code horizontalCollision}/{@code zza} 由 {@code Entity#move} / AI 在服务端更新，
     * 客户端本地值基本恒为 false。客户端另外用本地碰撞 + 本地 y 速度做兜底 OR。
     * <p>
     * 与 {@link #isClimbAggro()} 相互独立：近战状态不直接触发攀爬动画。
     */
    public boolean isClimbingAnimationState() {
        if (this.entityData.get(DATA_CLIMBING_ANIM)) {
            return true;
        }
        // 客户端兜底：若本地能拿到碰撞/速度信息也算攀爬
        return this.horizontalCollision
                && this.getDeltaMovement().y > CLIMB_ANIM_MIN_RISE;
    }

    /** 服务端每 tick 计算攀爬动画状态并同步。 */
    private void updateClimbingAnimationState() {
        boolean climbing = this.horizontalCollision
                && (this.getDeltaMovement().y > CLIMB_ANIM_MIN_RISE
                || (this.zza > CLIMB_ANIM_MIN_FORWARD && !this.onGround()));
        if (this.entityData.get(DATA_CLIMBING_ANIM) != climbing) {
            this.entityData.set(DATA_CLIMBING_ANIM, climbing);
        }
    }

    @Override
    public boolean onClimbable() {
        return isClimbingWall();
    }

    private void faceClimbedWall() {
        Direction dir = this.nearWallDir;
        if (dir == null) {
            return;
        }
        BlockPos wall = this.blockPosition().relative(dir);
        double dx = (wall.getX() + 0.5D) - this.getX();
        double dz = (wall.getZ() + 0.5D) - this.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    public float getMaxUpStep() {
        return 1.0F;
    }

    private boolean isBlockedAbove() {
        AABB bb = this.getBoundingBox();
        int blockY = Mth.ceil(bb.maxY);
        int minX = Mth.floor(bb.minX + 0.01);
        int maxX = Mth.floor(bb.maxX - 0.01);
        int minZ = Mth.floor(bb.minZ + 0.01);
        int maxZ = Mth.floor(bb.maxZ - 0.01);

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                mp.set(x, blockY, z);
                BlockState state = this.level().getBlockState(mp);
                if (!state.isSolid()) {
                    continue;
                }
                VoxelShape shape = state.getCollisionShape(this.level(), mp);
                if (!shape.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (isWebBlock(state)) {
            return;
        }
        super.makeStuckInBlock(state, motionMultiplier);
    }

    private static boolean isWebBlock(BlockState state) {
        return state.is(ModBlocks.INFESTED_SPIDER_WEB.get())
                || state.is(ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get())
                || state.is(ModBlocks.INFESTED_CAVE_SPIDER_WEB.get())
                || state.is(Blocks.COBWEB);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.01F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (isClimbingWall() && !this.onGround()) {
                Vec3 motion = this.getDeltaMovement();

                this.setDeltaMovement(motion.x, CLIMB_UP_SPEED, motion.z);
            }
        }
        super.travel(travelVector);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (this.isFakingDeath()) {

            if (this.fakeDeathBurstCountdown > 0 && --this.fakeDeathBurstCountdown <= 0) {
                DamageSource src = this.fakeDeathSource;
                resolveFakeDeath(src != null ? src : this.damageSources().generic());
            }
            return;
        }

        if (this.webCooldown > 0) this.webCooldown--;
        if (this.pounceCooldown > 0) this.pounceCooldown--;
        if (this.shootAnimationTicks > 0 && --this.shootAnimationTicks == 0) {
            setShooting(false);
        }

        // 远程未命中 → 近战攀爬：待判定弹射物结算 + 状态生命周期
        if (this.climbAggroCooldown > 0) this.climbAggroCooldown--;
        if (this.climbJumpCooldown > 0) this.climbJumpCooldown--;
        tickPendingWebShot();
        tickClimbAggro();


        boolean chasing = this.getTarget() != null;
        setRunning(chasing && this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D);

        double walk = isCaveVariant() ? CAVE_WALK_SPEED : SPIDER_WALK_SPEED;
        double run = isCaveVariant() ? CAVE_RUN_SPEED : SPIDER_RUN_SPEED;
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            double wanted = chasing ? run : walk;
            if (speed.getBaseValue() != wanted) {
                speed.setBaseValue(wanted);
            }
        }

        if (this.pounceTimer > 0 && --this.pounceTimer == 0) {
            setPouncing(false);
        }

        // 攀爬起步：目标在上方且贴着墙时起跳，之后交给 travel() 的攀爬推进接手。
        // （近战攀爬状态与普通追击都使用；20 tick 冷却避免在墙下连跳）
        if (this.climbJumpCooldown <= 0 && this.onGround() && this.isNearWall()
                && this.getTarget() != null
                && this.getTarget().getY() - this.getY() > 0.5D) {
            Vec3 dm = this.getDeltaMovement();
            this.setDeltaMovement(dm.x, 0.42D, dm.z);
            this.hasImpulse = true;
            this.climbJumpCooldown = CLIMB_JUMP_COOLDOWN_TICKS;
        }

        // 近战攀爬追击：目标在上方且地面寻路走不到（例如目标站在柱子上）时，
        // 直接朝目标水平位置推进，贴墙后由上面的起跳 + travel() 攀爬推进完成追击。
        if (isClimbAggro() && this.getTarget() != null
                && this.getTarget().getY() - this.getY() > 0.5D
                && this.getNavigation().isDone()
                && this.distanceToSqr(this.getTarget()) > 1.0D) {
            this.getMoveControl().setWantedPosition(
                    this.getTarget().getX(), this.getY(), this.getTarget().getZ(), 1.0D);
        }

        if (this.isClimbingWall()) {
            faceClimbedWall();
        }

        if (this.climbCooldown > 0) {
            this.climbCooldown--;
        }
        if (!this.level().isClientSide
                && this.climbCooldown <= 0
                && this.isNearWall()
                && this.getTarget() != null
                && !this.onGround()
                && this.isBlockedAbove()) {
            // 目标是上方且处于近战攀爬状态时，这是「有意义的攀爬」，不触发防卡住逻辑
            boolean purposefulClimb = isClimbAggro()
                    && this.getTarget().getY() - this.getY() > 0.5D;
            if (!purposefulClimb) {
                this.climbCooldown = CLIMB_COOLDOWN_TICKS;
                Vec3 dm = this.getDeltaMovement();
                if (dm.y > 0) {
                    this.setDeltaMovement(dm.x, 0, dm.z);
                }
            }
        }

        // 攀爬动画状态（服务端判定 + 同步；与近战攀爬状态相互独立）
        updateClimbingAnimationState();

        if (!chasing && this.getDeltaMovement().horizontalDistanceSqr() < 0.001D && !isPouncing()) {
            if (--this.idleAnimTimer <= 0) {
                setIdle2(this.random.nextFloat() < IDLE2_CHANCE);
                this.idleAnimTimer = 100 + this.random.nextInt(200);
            }
        } else {
            setIdle2(false);
            this.idleAnimTimer = 100 + this.random.nextInt(200);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData,
                                        CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);

        if (!this.spawnBuffsRolled) {
            this.spawnBuffsRolled = true;
            if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
                if (this.random.nextFloat() < 0.05F) {
                    applySpawnBuffs();
                }
            }
        }
        return result;
    }

    public void applySpawnBuffs() {
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, false, false));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0, false, false));
    }

    public void applyInheritedEffects(Collection<MobEffectInstance> inherited) {
        if (inherited == null) {
            return;
        }
        for (MobEffectInstance effect : inherited) {
            if (effect != null && effect.getEffect() != MobEffects.INVISIBILITY) {
                this.addEffect(new MobEffectInstance(effect));
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PounceGoal(this));

        this.goalSelector.addGoal(3, new WebAwareRangedAttackGoal(this, 1.0D, WEB_COOLDOWN_TICKS, (float) WEB_RANGE));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));

        this.goalSelector.addGoal(5, new KeepMovingToHighTargetGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FollowTargetGoal(this, 1.0D, 16));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 24.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static boolean isStandingInInfestedWeb(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        BlockPos feet = entity.blockPosition();
        return isInfestedWeb(entity.level().getBlockState(feet))
                || isInfestedWeb(entity.level().getBlockState(feet.below()));
    }

    private static boolean isInfestedWeb(BlockState state) {
        return state.is(ModBlocks.INFESTED_SPIDER_WEB.get())
                || state.is(ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get())
                || state.is(ModBlocks.INFESTED_CAVE_SPIDER_WEB.get());
    }

    public static boolean isWithinMeleeHorizontalRange(LivingEntity spider, LivingEntity target) {
        if (target == null) {
            return false;
        }
        double dx = target.getX() - spider.getX();
        double dz = target.getZ() - spider.getZ();
        return dx * dx + dz * dz < MELEE_HORIZONTAL_RANGE_SQ;
    }

    public boolean isWithinMeleeHorizontalRange(LivingEntity target) {
        return isWithinMeleeHorizontalRange(this, target);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level().isClientSide || this.webCooldown > 0) {
            return;
        }

        // 近战攀爬状态：不再发射弹射物，改由近战 AI 追击
        if (isClimbAggro()) {
            return;
        }

        if (isStandingInInfestedWeb(target)) {
            return;
        }

        if (isWithinMeleeHorizontalRange(target)) {
            return;
        }
        this.webCooldown = WEB_COOLDOWN_TICKS;
        beginShootAnimation();

        EntityType<? extends Projectile> type = switch (getVariant()) {
            case BLOOD -> ModEntities.INFESTED_SPIDER_WEB_BLOOD_PROJECTILE.get();
            case CAVE -> ModEntities.INFESTED_CAVE_SPIDER_WEB_PROJECTILE.get();
            default -> ModEntities.INFESTED_SPIDER_WEB_PROJECTILE.get();
        };

        Projectile web = type.create(this.level());
        if (web == null) {
            return;
        }

        double originY = this.getY();
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        double aimY = target.getY() + target.getBbHeight() * 0.5D;
        double dy = aimY - originY;

        double travelTicks = horizontal / WEB_VELOCITY;
        double drop = 0.5D * WEB_GRAVITY_PER_TICK * travelTicks * travelTicks;
        double aimRise = dy + drop;

        if (aimRise < MIN_AIM_RISE) {
            aimRise = MIN_AIM_RISE;
        }

        web.setOwner(this);
        web.setPos(this.getX(), originY, this.getZ());
        web.shoot(dx, aimRise, dz, WEB_VELOCITY, WEB_INACCURACY);
        this.playSound(SoundEvents.SPIDER_HURT, 0.4F, 0.6F);
        this.level().addFreshEntity(web);

        // 记录这一发，用于命中/未命中判定
        this.pendingWebShot = web;
        this.pendingWebTargetId = target.getUUID();
        this.pendingWebShotTick = this.tickCount;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            applyVariantOnHitEffect(living);

            this.triggerAnim("attack_controller", "attack");

            // 近战命中一次 → 退出近战攀爬状态，恢复远程（并进入 10s 冷却，避免来回抖动）
            if (isClimbAggro()) {
                exitClimbAggro(CLIMB_AGGRO_COOLDOWN_TICKS, true);
            }
        }
        return hit;
    }

    public void applyVariantOnHitEffect(LivingEntity target) {
        switch (getVariant()) {
            case BLOOD -> target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 100, 0, false, true));
            case CAVE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, true));
            default -> {
            }
        }
    }

    @Override
    public void die(DamageSource source) {

        if (!this.isFakingDeath() && !this.fakeDeathResolved && !this.level().isClientSide) {
            this.fakeDeathSource = source;
            setFakingDeath(true);
            setInvulnerable(true);
            this.fakeDeathTimer = 30;
            this.setHealth(EntityHealthUtils.burstHealth(this, 0.02F));
            this.setNoAi(true);
            this.setTarget(null);
            this.setPose(Pose.DYING);

            this.triggerAnim("dead_controller", "dead");
            this.fakeDeathBurstCountdown = DETONATE_TICKS;
            return;
        }
        super.die(source);
    }

    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    private void resolveFakeDeath(DamageSource source) {
        this.fakeDeathResolved = true;
        setFakingDeath(false);
        this.setInvulnerable(false);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        boolean burning = this.isOnFire();
        super.die(source);

        if (!burning) {
            finishBurst();
        } else {
            this.discard();
        }
    }

    private void finishBurst() {
        if (this.burstApplied || this.level().isClientSide) {
            return;
        }
        this.burstApplied = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            applyBurstVariantEffectToNearby(serverLevel);
            placeBurstWeb(serverLevel);
        }

        triggerDismemberment();
this.discard();
    }

    private void applyBurstVariantEffectToNearby(ServerLevel level) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SELF_DESTRUCT_RADIUS),
                e -> e != null && e.isAlive() && !ParasiteEvents.isParasiteByTagOrInterface(e))) {
            switch (getVariant()) {
                case BLOOD -> living.addEffect(
                        new MobEffectInstance(ModEffects.BLEEDING.get(), 100, 0, false, true));
                case CAVE -> living.addEffect(
                        new MobEffectInstance(MobEffects.POISON, 100, 1, false, true));
                default -> {
                }
            }
        }
    }

    private void placeBurstWeb(ServerLevel level) {
        BlockState web = switch (getVariant()) {
            case BLOOD -> InfestedSpiderWebBlood.markSpiderWeb(
                    ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get().defaultBlockState(), true);
            case CAVE -> InfestedCaveSpiderWeb.markSpiderWeb(
                    ModBlocks.INFESTED_CAVE_SPIDER_WEB.get().defaultBlockState(), true);
            default -> InfestedSpiderWeb.markSpiderWeb(
                    ModBlocks.INFESTED_SPIDER_WEB.get().defaultBlockState(), true);
        };

        BlockPos pos = this.blockPosition();
        if (!level.getBlockState(pos).canBeReplaced()) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).canBeReplaced()) {
                pos = below;
            }
        }
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, web, 3);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && ParasiteEvents.shouldIgnoreDamageFrom(this, attacker)) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            ParasiteEvents.onAttacked(this, attacker);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {

        this.playSound(SoundEvents.SPIDER_STEP, 0.05F, 1.0F);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

        public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    public static boolean checkInfestedSpiderSpawnRules(EntityType<InfestedSpider> entityType,
                                                        ServerLevelAccessor levelAccessor,
                                                        MobSpawnType spawnType,
                                                        BlockPos pos,
                                                        RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            return stage >= 1 && stage <= 5;
        }
        return true;
    }

    public static class PounceGoal extends Goal {
        private final InfestedSpider spider;

        public PounceGoal(InfestedSpider spider) {
            this.spider = spider;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = spider.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!spider.onGround() || spider.pounceCooldown > 0 || spider.isPouncing()) return false;
            double distSq = spider.distanceToSqr(target);
            if (distSq < 1.5D * 1.5D || distSq > POUNCE_RANGE * POUNCE_RANGE) return false;
            return spider.getSensing().hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = spider.getTarget();
            if (target == null) return;

            double dx = target.getX() - spider.getX();
            double dz = target.getZ() - spider.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 0.1D) {

                double speed = Math.min(horizontal * 0.12D, 0.7D) * POUNCE_SPEED_MULTIPLIER;
                spider.setDeltaMovement(dx / horizontal * speed, 0.42D * POUNCE_SPEED_MULTIPLIER,
                        dz / horizontal * speed);
            } else {
                spider.setDeltaMovement(0.0D, 0.42D * POUNCE_SPEED_MULTIPLIER, 0.0D);
            }
            spider.hasImpulse = true;
            spider.setPouncing(true);
            spider.pounceTimer = 16;
            spider.pounceCooldown = POUNCE_COOLDOWN_TICKS;
        }

        @Override
        public boolean canContinueToUse() {
            return spider.isPouncing() && spider.pounceTimer > 0;
        }

        @Override
        public void stop() {
            spider.setPouncing(false);
        }
    }

    public static class KeepMovingToHighTargetGoal extends Goal {
        private final InfestedSpider spider;
        private int repathTimer;

        public KeepMovingToHighTargetGoal(InfestedSpider spider) {
            this.spider = spider;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = spider.getTarget();

            return target != null && target.isAlive() && target.getY() - spider.getY() > 1.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = spider.getTarget();
            return target != null && target.isAlive() && target.getY() - spider.getY() > 0.5D;
        }

        @Override
        public void start() {
            this.repathTimer = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = spider.getTarget();
            if (target == null) {
                return;
            }

            if (this.repathTimer > 0) {
                this.repathTimer--;
            }
            if (this.repathTimer <= 0 || spider.getNavigation().isDone()) {
                this.repathTimer = 10;

                spider.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0D);
            }
        }
    }

    private void triggerDismemberment() {
        try {
            Class.forName("me.ichun.mods.mobdismemberment.common.MobDismemberment")
                    .getMethod("trigger", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, this);
        } catch (Throwable ignored) {
        }
    }

}
