package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.client.entity.IAutoRenderableEntity;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.SlimeProjectile;
import org.tdddd.epca.impl.utils.EntityHealthUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * 虫染史莱姆（单实体 + {@code Size} 驱动体型）。
 *
 * <p>重构前是 {@code infested_slime_size0 / size1 / size3} 三个实体类型，本类把三者合并成一个实体，
 * 用同步数据 {@link #DATA_SIZE} 与 NBT {@code Size} 表示体型，取值沿用原三个实体的编号：
 * {@code 0}（小型，原 size0）、{@code 1}（中型，原 size1）、{@code 3}（大型，原 size3）。
 * 血量/护甲/攻击/移速/跳跃/碰撞箱、攻击方式（小型纯近战、中型 2 发弹射物、大型 3 发弹射物 + 附加目标）、
 * 死亡分裂（大型裂成中型、中型裂成小型、小型掉战利品后消失）都按体型一一还原重构前的行为。</p>
 *
 * <p><b>26.1.2 移植说明</b>（相对 1.20.1 权威实现）：</p>
 * <ul>
 *   <li>{@code SynchedEntityData.defineId} 保留，但 {@code defineSynchedData} 改成
 *       {@code defineSynchedData(SynchedEntityData.Builder)}；</li>
 *   <li>NBT 从 {@code CompoundTag} 改成 {@code ValueOutput / ValueInput}
 *       （{@code getIntOr / getBooleanOr}）；</li>
 *   <li>{@code hurt(DamageSource, float)} 在 26 已拆成 {@code hurtServer(ServerLevel, DamageSource, float)}；</li>
 *   <li>{@code dropFromLootTable(DamageSource, boolean)} 在 26 多一个 {@code ServerLevel} 参数；</li>
 *   <li>{@code MobSpawnType} -> {@code EntitySpawnReason}，且 {@code finalizeSpawn} 不再收 {@code CompoundTag}；</li>
 *   <li>{@code setSecondsOnFire(int)} -> {@code igniteForSeconds(float)}，{@code hasImpulse} -> {@code hurtMarked}；</li>
 *   <li>{@code ResourceLocation} -> {@code Identifier}（{@code Identifier.fromNamespaceAndPath}）；</li>
 *   <li>GeckoLib 5：{@code AnimationTest} + {@code event.setAnimation(...)} + {@code event.controller().reset()}，
 *       {@code AnimationController} 不再把实体本身当第一个参数；</li>
 *   <li>26 的模型/动画路径由 GeckoLib 5 自己拼前缀（{@code geckolib/models/...}、
 *       {@code geckolib/animations/...}），所以这里返回的 id 与
 *       {@code ModEntities.registerMobWithRender} 给三个旧实体注册时用的完全一致：</li>
 * </ul>
 */
public class InfestedSlime extends PathfinderMob implements GeoEntity, Enemy, IAutoRenderableEntity {

    /** 小型（原 infested_slime_size0） */
    public static final int SIZE_TINY = 0;
    /** 中型（原 infested_slime_size1） */
    public static final int SIZE_MEDIUM = 1;
    /** 大型（原 infested_slime_size3） */
    public static final int SIZE_LARGE = 3;
    /** 自然体型（升序），与笔记里的多体型展示顺序一致 */
    public static final int[] NATURAL_SIZES = {SIZE_TINY, SIZE_MEDIUM, SIZE_LARGE};

    private static final EntityDataAccessor<Integer> DATA_SIZE =
            SynchedEntityData.defineId(InfestedSlime.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(InfestedSlime.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_INVULNERABLE =
            SynchedEntityData.defineId(InfestedSlime.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHOOTING =
            SynchedEntityData.defineId(InfestedSlime.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    private float targetSquish;
    private float squish;
    private float oSquish;
    private boolean wasOnGround;
    private int attackCooldown = 0;

    private int fakeDeathTimer = 10;
    private BlockPos deathPosition;

    private int rangedAttackCooldown = 0;
    private int projectileCount = 0;
    private int shootAnimationTime = 0;
    private LivingEntity primaryTarget;
    private LivingEntity secondaryTarget;
    private List<LivingEntity> additionalTargets = new ArrayList<>();

    private int floatingTime;

    private static final SizeAttributes SIZE_0_ATTRIBUTES =
            new SizeAttributes(2.0D, 2.0D, 0.2D, 0.4F, 1.2F, 10, 30, 0.5F, 4, 16.0D, 0.6F, 1.4F);
    private static final SizeAttributes SIZE_1_ATTRIBUTES =
            new SizeAttributes(6.0D, 4.0D, 0.4D, 0.5F, 1.0F, 10, 40, 1.0F, 8, 24.0D, 0.8F, 1.0F);
    private static final SizeAttributes SIZE_3_ATTRIBUTES =
            new SizeAttributes(24.0D, 6.0D, 0.6D, 0.5F, 0.8F, 20, 60, 2.5F, 12, 32.0D, 1.0F, 0.6F);

    /**
     * 一个体型的全部数值。字段顺序与原 size0/1/3 实体里的 {@code SizeAttributes} 保持一致，
     * 新增了碰撞箱边长、经验、跟随/索敌范围、音量与音调。
     */
    private static class SizeAttributes {
        /** 血量上限 */
        public final double maxHealth;
        /** 沿用原表里的攻击力字段（原实现未使用，仅保留数据完整性） */
        public final double attackDamage;
        /** 移动速度 */
        public final double movementSpeed;
        /** 跳跃初速度 */
        public final float jumpPower;
        /** 水平速度倍率 */
        public final float horizontalMultiplier;
        /** 跳跃间隔下限（tick） */
        public final int minJumpDelay;
        /** 跳跃间隔上限（tick） */
        public final int maxJumpDelay;
        /** 碰撞箱边长（原实体类型注册时用的 sized(w,h)，三边一致） */
        public final float boxSize;
        /** 经验值 */
        public final int xpReward;
        /** 跟随/索敌范围 */
        public final double followRange;
        /** 音量 */
        public final float soundVolume;
        /** 音调 */
        public final float voicePitch;

        SizeAttributes(double maxHealth, double attackDamage, double movementSpeed, float jumpPower,
                       float horizontalMultiplier, int minJumpDelay, int maxJumpDelay,
                       float boxSize, int xpReward, double followRange, float soundVolume, float voicePitch) {
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.jumpPower = jumpPower;
            this.horizontalMultiplier = horizontalMultiplier;
            this.minJumpDelay = minJumpDelay;
            this.maxJumpDelay = maxJumpDelay;
            this.boxSize = boxSize;
            this.xpReward = xpReward;
            this.followRange = followRange;
            this.soundVolume = soundVolume;
            this.voicePitch = voicePitch;
        }
    }

    public InfestedSlime(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);

        this.navigation = new GroundPathNavigation(this, level);
        this.moveControl = new SlimeMoveControl(this);
        this.wasOnGround = this.onGround();
        this.additionalTargets = new ArrayList<>();

        this.setSize(SIZE_MEDIUM, false);
        this.setHealth(this.getMaxHealth());
    }

    // ======================= 体型 =======================

    /** 把任意数字折到三种自然体型之一：{@code <=0 -> 0}、{@code 1 -> 1}、其它（2/3/4/...）{@code -> 3} */
    public static int normalizeSize(int size) {
        if (size <= SIZE_TINY) {
            return SIZE_TINY;
        }
        return size == SIZE_MEDIUM ? SIZE_MEDIUM : SIZE_LARGE;
    }

    /** 原版史莱姆的 {@code getSize()}（1/2/4）换算到本实体的体型（0/1/3） */
    public static int fromVanillaSlimeSize(int vanillaSize) {
        return switch (vanillaSize) {
            case 1 -> SIZE_TINY;
            case 2 -> SIZE_MEDIUM;
            default -> SIZE_LARGE;
        };
    }

    public static SizeAttributes attributesFor(int size) {
        return switch (size) {
            case SIZE_TINY -> SIZE_0_ATTRIBUTES;
            case SIZE_LARGE -> SIZE_3_ATTRIBUTES;
            default -> SIZE_1_ATTRIBUTES;
        };
    }

    /** 笔记/展示用：[血量, 护甲, 攻击] 三个体型，顺序 0/1/3 */
    public static List<double[]> perSizeBaseStats() {
        List<double[]> stats = new ArrayList<>(NATURAL_SIZES.length);
        for (int size : NATURAL_SIZES) {
            SizeAttributes attributes = attributesFor(size);
            stats.add(new double[]{attributes.maxHealth, 0.0D, attributes.attackDamage});
        }
        return stats;
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /** 原实体里 {@code setSize(int)} 的空实现被替换成真正的体型切换（读档/刷怪蛋/转化都用它） */
    public void setSize(int size) {
        this.setSize(size, true);
    }

    public void setSize(int size, boolean heal) {
        int normalized = normalizeSize(size);
        this.entityData.set(DATA_SIZE, normalized);

        SizeAttributes attributes = attributesFor(normalized);
        this.xpReward = attributes.xpReward;
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(attributes.maxHealth);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(attributes.movementSpeed);
        }
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(attributes.followRange);
        }

        this.refreshDimensions();

        if (heal) {
            this.setHealth(this.getMaxHealth());
        }
    }

    public SizeAttributes getCurrentSizeAttributes() {
        return attributesFor(this.getSize());
    }

    /** 分裂出来的下一级体型：3 -> 1 -> 0（0 不再分裂） */
    public int nextSmallerSize() {
        return switch (this.getSize()) {
            case SIZE_LARGE -> SIZE_MEDIUM;
            case SIZE_MEDIUM -> SIZE_TINY;
            default -> -1;
        };
    }

    // 26.1.2 的 LivingEntity#getDimensions(Pose) 已是 final，可变的碰撞箱钩子改成了
    // getDefaultDimensions(Pose)（getDimensions = getDefaultDimensions(pose).scale(getScale())），
    // 与 26 原版 Slime 的做法一致。
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        float box = getCurrentSizeAttributes().boxSize;
        return EntityDimensions.scalable(box, box);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SIZE.equals(key)) {
            this.refreshDimensions();
        }
    }

    // ======================= 初始化 / 刷怪 =======================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SIZE_1_ATTRIBUTES.maxHealth)
                .add(Attributes.MOVEMENT_SPEED, SIZE_1_ATTRIBUTES.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, SIZE_1_ATTRIBUTES.followRange);
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

        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0, 16));
        // 索敌范围随体型变化（原 size0/1/3 分别是 16 / 24 / 32）
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, () -> getCurrentSizeAttributes().followRange));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION
                || spawnType == EntitySpawnReason.SPAWNER || spawnType == EntitySpawnReason.SPAWN_ITEM_USE) {
            // 与重构前三张刷怪表/刷怪蛋一致：三种自然体型里随机
            int size = NATURAL_SIZES[this.random.nextInt(NATURAL_SIZES.length)];
            this.setSize(size, true);
        }
        return data;
    }

    // ======================= tick =======================

    @Override
    public void tick() {
        if (this.additionalTargets == null) {
            this.additionalTargets = new ArrayList<>();
        }

        if (isFakingDeath()) {
            super.tick();
            fakeDeathTimer--;
            if (fakeDeathTimer <= 0) {
                this.finishFakeDeath();
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
            if (!this.level().isClientSide()) {
                // 中型：12/8 tick 各一发（主目标 + 副目标）；大型：12/8/4 tick 各一发（三个目标）
                if (this.shootAnimationTime == 12) {
                    this.shootProjectileAtTarget(this.getProjectileTarget(0));
                } else if (this.shootAnimationTime == 8) {
                    this.shootProjectileAtTarget(this.getProjectileTarget(1));
                } else if (this.shootAnimationTime == 4 && this.getSize() == SIZE_LARGE) {
                    this.shootProjectileAtTarget(this.getProjectileTarget(2));
                } else if (this.shootAnimationTime <= 0) {
                    this.entityData.set(DATA_SHOOTING, false);
                }
            } else if (this.shootAnimationTime <= 0) {
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
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                int particleCount = this.getSize() * 4;
                if (this.getSize() == SIZE_TINY) {
                    particleCount = 4;
                }
                for (int i = 0; i < particleCount; i++) {
                    double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                    double d1 = this.getY();
                    double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                    serverLevel.sendParticles(
                            new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get()),
                            d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.0D
                    );
                }
            }

            this.playSound(getSquishSound(), getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
        }

        this.wasOnGround = isOnGround;

        updateFloating();
    }

    /** 第 index 发弹射物的目标（0=主目标，1=副目标，2=第三目标） */
    private LivingEntity getProjectileTarget(int index) {
        if (this.getSize() == SIZE_MEDIUM) {
            if (index == 0) {
                return this.primaryTarget;
            }
            return this.secondaryTarget != null ? this.secondaryTarget : this.primaryTarget;
        }
        if (this.additionalTargets == null || this.additionalTargets.size() <= index) {
            return index == 0 ? this.primaryTarget : null;
        }
        return this.additionalTargets.get(index);
    }

    // ======================= 假死 / 死亡 / 分裂 =======================

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

        this.setHealth(EntityHealthUtils.burstHealth(this, 0.02F));
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
        this.entityData.set(DATA_IS_FAKING_DEATH, true);
    }

    /** 假死计时结束：按体型喷粒子 + 分裂/掉落，然后消失 */
    private void finishFakeDeath() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int particleCount = this.getSize() == SIZE_TINY ? 8 : (this.getSize() == SIZE_MEDIUM ? 16 : 24);
            for (int i = 0; i < particleCount; i++) {
                double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                double d1 = this.getY() + this.random.nextDouble() * this.getBbHeight();
                double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, ModItems.INFESTED_SLIME_BALL.get()),
                        d0, d1, d2, 1, 0.0D, 0.0D, 0.0D, 0.1D
                );
            }

            if (this.getSize() == SIZE_TINY) {
                // 原 size0：不分裂，掉战利品
                DamageSource damageSource = this.getLastDamageSource();
                if (damageSource == null) {
                    damageSource = this.damageSources().generic();
                }
                this.dropFromLootTable(serverLevel, damageSource, false);
            } else {
                this.splitIntoSmallerSlimes(false);
            }
        }

        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }

        if (this.isOnFire()) {
            if (!this.level().isClientSide() && this.getSize() != SIZE_TINY) {
                this.splitIntoSmallerSlimes(true);
            }
            super.die(source);
            return;
        }

        if (!this.level().isClientSide() && this.getHealth() <= 0.0F && this.random.nextFloat() < 1.0f) {
            triggerFakeDeath(source);
        } else {
            if (!this.level().isClientSide() && this.getSize() != SIZE_TINY) {
                this.splitIntoSmallerSlimes(false);
            }
            super.die(source);
        }
    }

    /**
     * 按体型分裂：大型裂成 2~4 只中型，中型裂成 4 只小型（与原 size3 / size1 的实现一致），
     * 小型不分裂。
     */
    private void splitIntoSmallerSlimes(boolean inheritFire) {
        int childSize = this.nextSmallerSize();
        if (childSize < 0) {
            return;
        }

        int spawnCount = this.getSize() == SIZE_LARGE ? 2 + this.random.nextInt(3) : 4;
        double spread = this.getSize() == SIZE_LARGE ? 3.0D : 2.0D;

        for (int i = 0; i < spawnCount; i++) {
            InfestedSlime child = create(this.level());
            if (child == null) {
                continue;
            }
            child.setSize(childSize, true);
            double offsetX = (this.random.nextDouble() - 0.5D) * spread;
            double offsetZ = (this.random.nextDouble() - 0.5D) * spread;
            child.setPos(this.getX() + offsetX, this.getY() + 0.5D, this.getZ() + offsetZ);
            if (inheritFire && this.isOnFire()) {
                child.igniteForSeconds(this.getRemainingFireTicks() / 20.0F);
            }
            this.level().addFreshEntity(child);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isFakingDeath() || isInvulnerable()) {
            return false;
        }

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (ParasiteEvents.shouldIgnoreDamageFrom(this, attacker)) {
                return false;
            }
        }

        if (!this.level().isClientSide() && source.getEntity() instanceof LivingEntity attacker) {
            ParasiteEvents.onAttacked(this, attacker);
        }

        return super.hurtServer(level, source, amount);
    }

    // ======================= 远程攻击 =======================

    static class SlimeRangedAttackGoal extends Goal {
        private final InfestedSlime slime;
        private int seeTime;
        private final int attackInterval = 40;

        public SlimeRangedAttackGoal(InfestedSlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.slime.getSize() == SIZE_TINY) {
                // 原 size0 没有远程攻击
                return false;
            }
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
            if (target == null) {
                return;
            }

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

            if (this.slime.getSize() == SIZE_LARGE) {
                this.slime.additionalTargets = this.findAdditionalTargets(target);
            } else {
                this.slime.secondaryTarget = this.findSecondaryTarget(target);
            }

            this.slime.shootAnimationTime = 20;
            this.slime.entityData.set(DATA_SHOOTING, true);
            this.slime.swinging = true;
            this.slime.projectileCount = 0;
        }

        /** 中型：主目标之外再找最近的一个敌人 */
        private LivingEntity findSecondaryTarget(LivingEntity primaryTarget) {
            List<LivingEntity> nearbyEnemies = this.slime.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.slime.getBoundingBox().inflate(12.0D),
                    entity -> entity != null &&
                            entity.isAlive() &&
                            entity != primaryTarget &&
                            entity != this.slime &&
                            !ParasiteEvents.isParasiteByTagOrInterface(entity) &&
                            !(entity instanceof Creeper) &&
                            this.slime.getSensing().hasLineOfSight(entity)
            );

            if (nearbyEnemies.isEmpty()) {
                return primaryTarget;
            }

            return nearbyEnemies.stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(primaryTarget)))
                    .orElse(primaryTarget);
        }

        /** 大型：三个目标（原 size3 的 findAdditionalTargets） */
        private List<LivingEntity> findAdditionalTargets(LivingEntity primaryTarget) {
            List<LivingEntity> nearbyEnemies = this.slime.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.slime.getBoundingBox().inflate(16.0D),
                    entity -> entity != null &&
                            entity.isAlive() &&
                            entity != primaryTarget &&
                            entity != this.slime &&
                            !ParasiteEvents.isParasiteByTagOrInterface(entity) &&
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
                LivingEntity secondTarget = !nearbyEnemies.isEmpty() ? nearbyEnemies.get(0) : primaryTarget;
                LivingEntity thirdTarget = nearbyEnemies.size() > 1 ? nearbyEnemies.get(1) : primaryTarget;
                return List.of(primaryTarget, secondTarget, thirdTarget);
            }
        }
    }

    private void shootProjectileAtTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (target instanceof Player player) {
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

    // ======================= 移动 =======================

    static class SlimeMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final InfestedSlime slime;
        private boolean isAggressive;

        public SlimeMoveControl(InfestedSlime slime) {
            super(slime);
            this.slime = slime;
            this.yRot = 180.0F * slime.getYRot() / (float) Math.PI;
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
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = this.slime.getJumpDelay();

                    if (this.isAggressive) {
                        this.jumpDelay /= 3;
                    }

                    this.slime.getJumpControl().jump();

                    this.slime.playSound(this.slime.getJumpSound(), this.slime.getSoundVolume(),
                            ((this.slime.getRandom().nextFloat() - this.slime.getRandom().nextFloat()) * 0.2F + 1.0F) * 0.8F);
                } else {
                    this.slime.xxa = 0.0F;
                    this.slime.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            } else {
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }

    protected int getJumpDelay() {
        SizeAttributes attributes = getCurrentSizeAttributes();
        return this.random.nextInt(attributes.maxJumpDelay - attributes.minJumpDelay + 1) + attributes.minJumpDelay;
    }

    @Override
    public void jumpFromGround() {
        Vec3 vec3 = this.getDeltaMovement();
        SizeAttributes attributes = getCurrentSizeAttributes();

        this.setDeltaMovement(vec3.x * attributes.horizontalMultiplier, attributes.jumpPower, vec3.z * attributes.horizontalMultiplier);
        this.hurtMarked = true;
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

        if (!this.level().isClientSide() && this.attackCooldown <= 0) {
            this.checkContactDamage();
        }
    }

    /** 接触伤害：小型 2 / 中型 4 / 大型 6（与原三实体一致） */
    private void checkContactDamage() {
        float damage = switch (this.getSize()) {
            case SIZE_TINY -> 2.0F;
            case SIZE_MEDIUM -> 4.0F;
            default -> 6.0F;
        };

        AABB collisionBox = this.getBoundingBox().inflate(0.0D);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, collisionBox)) {
            if (target == this || ParasiteEvents.isParasiteByTagOrInterface(target)) {
                continue;
            }

            if (this.getBoundingBox().intersects(target.getBoundingBox())) {
                DamageSource magicDamage = this.damageSources().magic();
                boolean hurt = target.hurtOrSimulate(magicDamage, damage);

                if (hurt) {
                    MobEffectInstance solidifyEffect = new MobEffectInstance(ModEffects.SOLIDIFY, 40, 0, false, true);
                    target.addEffect(solidifyEffect);

                    MobEffectInstance fearEffect = new MobEffectInstance(ModEffects.FEAR, 400, 0, false, true);
                    target.addEffect(fearEffect);

                    this.attackCooldown = 10;

                    this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, this.getVoicePitch());

                    break;
                }
            }
        }
    }

    // ======================= 音效 =======================

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
        return getCurrentSizeAttributes().soundVolume;
    }

    @Override
    public float getVoicePitch() {
        return getCurrentSizeAttributes().voicePitch;
    }

    // ======================= 渲染 =======================

    // 下面三个 id 与 ModEntities.registerMobWithRender 当初给
    // infested_slime_size0/1/3 注册的写法完全一致（GeckoLib 5 自己拼
    // geckolib/models/ 与 geckolib/animations/ 前缀），所以三套旧资源文件继续可用。
    @Override
    public Identifier model() {
        return Identifier.fromNamespaceAndPath("epca", "entity/" + assetName());
    }

    @Override
    public Identifier texture() {
        return Identifier.fromNamespaceAndPath("epca", "textures/entity/" + assetName() + ".png");
    }

    @Override
    public Identifier animation() {
        return Identifier.fromNamespaceAndPath("epca", assetName());
    }

    /** 沿用原三套资源文件名，按体型选择 */
    private String assetName() {
        return "infested_slime_size" + getSize();
    }

    public float getSquish() {
        return this.squish;
    }

    public float getoSquish() {
        return this.oSquish;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", EpcaAnimations.GEO_TRANSITION_TICKS, this::playState));
        controllers.add(new AnimationController<>("attack_controller", EpcaAnimations.GEO_TRANSITION_TICKS, this::attackPredicate));
    }

    private PlayState playState(AnimationTest<InfestedSlime> event) {
        String sizeSuffix = getSizeSuffix();

        if (this.isFakingDeath()) {
            event.setAnimation(RawAnimation.begin().thenPlay("dead" + sizeSuffix));
        } else if (!this.onGround() || this.getDeltaMovement().y > 0.0D) {
            event.setAnimation(RawAnimation.begin().thenPlay("jump" + sizeSuffix));
        } else if (this.getSize() != SIZE_TINY && (this.entityData.get(DATA_SHOOTING) || this.shootAnimationTime > 0)) {
            event.setAnimation(RawAnimation.begin().thenPlay("shoot" + sizeSuffix));
        } else if (event.isMoving()) {
            event.setAnimation(RawAnimation.begin().thenLoop("idle" + sizeSuffix));
        } else {
            event.setAnimation(RawAnimation.begin().thenLoop("idle" + sizeSuffix));
        }

        return PlayState.CONTINUE;
    }

    private PlayState attackPredicate(AnimationTest<InfestedSlime> event) {
        if (this.getSize() == SIZE_TINY && this.swinging) {
            return PlayState.CONTINUE;
        }

        event.controller().reset();
        return PlayState.STOP;
    }

    private String getSizeSuffix() {
        return "_size" + getSize();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    // ======================= 数据 =======================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_SIZE, SIZE_MEDIUM);
        entityData.define(DATA_IS_FAKING_DEATH, false);
        entityData.define(DATA_IS_INVULNERABLE, false);
        entityData.define(DATA_SHOOTING, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Size", this.getSize());
        output.putInt("RangedAttackCooldown", this.rangedAttackCooldown);
        output.putBoolean("Shooting", this.entityData.get(DATA_SHOOTING));
        output.putInt("ShootAnimationTime", this.shootAnimationTime);
        output.putBoolean("FakingDeath", this.entityData.get(DATA_IS_FAKING_DEATH));
        output.putInt("FakeDeathTimer", this.fakeDeathTimer);
        output.putBoolean("Invulnerable", this.entityData.get(DATA_IS_INVULNERABLE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        // 旧存档的 Size 就是 0/1/3，直接沿用；缺省按中型
        this.setSize(input.getIntOr("Size", SIZE_MEDIUM), false);
        this.rangedAttackCooldown = input.getIntOr("RangedAttackCooldown", 0);
        this.entityData.set(DATA_SHOOTING, input.getBooleanOr("Shooting", false));
        this.shootAnimationTime = input.getIntOr("ShootAnimationTime", 0);
        this.entityData.set(DATA_IS_FAKING_DEATH, input.getBooleanOr("FakingDeath", false));
        this.fakeDeathTimer = input.getIntOr("FakeDeathTimer", 10);
        this.entityData.set(DATA_IS_INVULNERABLE, input.getBooleanOr("Invulnerable", false));

        if (this.getHealth() <= 0.0F || this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    /** 原生创建入口（分裂、转化、刷怪蛋都用它） */
    public static InfestedSlime create(Level level) {
        return new InfestedSlime(ModEntities.INFESTED_SLIME.get(), level);
    }

    public static InfestedSlime create(Level level, int size) {
        InfestedSlime slime = create(level);
        if (slime != null) {
            slime.setSize(size, true);
        }
        return slime;
    }

    /**
     * 只有小型掉战利品（原 {@code infested_slime_size0} 有战利品表，size1/size3 没有）。
     * 单实体共用一张战利品表，所以在这里按体型把中/大型的掉落掐掉。
     */
    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        if (this.getSize() != SIZE_TINY) {
            return;
        }
        super.dropFromLootTable(level, source, playerKilled);
    }

    public static boolean checkInfestedSlimeSpawnRules(
            EntityType<InfestedSlime> entityType,
            ServerLevelAccessor level,
            EntitySpawnReason spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(level.getLevel());
            if (stage < 2 || stage > 4) {
                return false;
            }
        }

        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force, boolean sendEventAndTriggers) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) {
            return false;
        }
        return super.startRiding(vehicle, force, sendEventAndTriggers);
    }

    @Override
    protected boolean canRide(Entity entity) {
        if (entity instanceof Boat || entity instanceof Minecart) {
            return false;
        }
        return super.canRide(entity);
    }

    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide()) {
            ParasiteEvents.onKillEntity(this, killedEntity);
        }
    }

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
    public boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }
}
