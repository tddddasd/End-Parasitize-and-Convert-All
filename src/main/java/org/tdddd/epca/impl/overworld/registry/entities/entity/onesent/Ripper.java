package org.tdddd.epca.impl.overworld.registry.entities.entity.onesent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class Ripper extends PathfinderMob implements GeoEntity, IParasite, IOnesent, Enemy {

    @Override
    public boolean onClimbable() {
        // 仅当有攻击目标且水平碰撞时，才视为可攀爬
        return this.isNearWall() && this.getTarget() != null;
    }

    public boolean isClimbing() {
        return this.onClimbable();
    }

    @Override
    public void travel(Vec3 travelVector) {
        // 完全采用原版蜘蛛的 travel 逻辑，提供攀爬时的垂直速度
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.01F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (this.onClimbable() && !this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.2, this.getDeltaMovement().z);
            }
        }
        super.travel(travelVector);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        // 原版蜘蛛使用地面导航，攀爬由碰撞和 travel 控制
        return new GroundPathNavigation(this, level);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    public enum Variant {
        DEFAULT,
        BLEED,
        VIRAL
    }

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            Ripper.class, EntityDataSerializers.INT
    );
    // 仅保留用于攻击动画的跳跃状态
    private static final EntityDataAccessor<Boolean> DATA_IS_LEAPING = SynchedEntityData.defineId(Ripper.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private int attackTime = 0;

    private double normalSpeed = 0.30D;
    private int cothCloudCooldown = 0;
    private static final int COTH_CLOUD_COOLDOWN = 20;

    private int leapDirectionTicks = 0;
    private float leapTargetYaw = 0;
    private int stepSoundDelay = 0;
    private int jumpCooldown = 0;

    // ==================== 构造器 ====================
    public Ripper(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;

        if (!level.isClientSide) {
            int roll = this.random.nextInt(100);
            if (roll < 66) {
                this.setVariant(Variant.DEFAULT);
            } else if (roll < 83) {
                this.setVariant(Variant.BLEED);
            } else {
                this.setVariant(Variant.VIRAL);
            }
        }
    }

    // ==================== 数据同步 ====================
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
        this.entityData.define(DATA_IS_LEAPING, false);
    }

    // ==================== 变种相关 ====================
    public Variant getVariant() {
        Integer variantOrdinal = this.entityData.get(DATA_VARIANT);
        if (variantOrdinal == null) {
            return Variant.DEFAULT;
        }
        int index = Mth.clamp(variantOrdinal, 0, Variant.values().length - 1);
        return Variant.values()[index];
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
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
            String variantName = tag.getString("Variant");
            try {
                this.setVariant(Variant.valueOf(variantName));
            } catch (IllegalArgumentException e) {
                this.setVariant(Variant.DEFAULT);
            }
        }
    }

    // ==================== 跳跃与攻击状态 ====================
    public void setLeaping(boolean leaping) {
        this.entityData.set(DATA_IS_LEAPING, leaping);
    }

    public boolean isLeaping() {
        return this.entityData.get(DATA_IS_LEAPING);
    }

    public void setLeapDirectionTicks(int ticks) {
        this.leapDirectionTicks = ticks;
    }

    // ==================== 属性 ====================
    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build();
    }

    // ==================== 生成规则 ====================
    public static boolean checkRupterSpawnRules(
            EntityType<Ripper> entityType,
            ServerLevelAccessor levelAccessor,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            if (stage < 1 || stage > 5) {
                return false;
            }
        }
        return levelAccessor.getMaxLocalRawBrightness(pos) < 8;
    }

    // ==================== 目标与 AI（移除 ClimbToTargetGoal） ====================
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(1, new ClimbTowardsTargetGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ConditionalPanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.1D, 0.01F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new RandomSoundGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));

        this.goalSelector.addGoal(2, new MoveToPassiveGoal(this, 1.5D, 16.0F) {
            @Override
            public void tick() {
                if (target != null && rupter.distanceToSqr(target) < 9.0) {
                    double dy = target.getY() - rupter.getY();
                    if (dy > rupter.getMaxUpStep()) {
                        rupter.getJumpControl().jump();
                    }
                }
                super.tick();
            }
        });

        this.goalSelector.addGoal(4, new ConditionalAvoidGoal<>(this, Mob.class, 16.0F, 1.5D, 1.5D, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
        this.targetSelector.addGoal(3, new ConditionalAttackGoal(this));

        this.goalSelector.addGoal(1, new LeapAttackGoal(this, 1.0D, 15, 3, 35));

        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D) {
            @Override
            public boolean canUse() {
                int stage = Ripper.this.getEvolutionStage();
                if (stage < 2 || stage > 13) {
                    return false;
                }
                return super.canUse();
            }
        });

        this.goalSelector.addGoal(4, new RandomTargetSelectionGoal(this));
    }

    static class ClimbTowardsTargetGoal extends Goal {
        private final Ripper ripper;
        private BlockPos wallBase;
        private int cooldown;

        public ClimbTowardsTargetGoal(Ripper ripper) {
            this.ripper = ripper;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ripper.getTarget();
            if (target == null || !target.isAlive()) return false;
            // 目标至少比实体高 1.5 格才触发攀爬引导
            if (target.getY() - ripper.getY() < 1.5) return false;
            // 如果已经贴墙，则由 onClimbable 直接处理，不再引导
            if (ripper.isNearWall()) return false;
            // 冷却，避免每 tick 搜索
            if (--cooldown > 0) return false;
            cooldown = 10;
            return findClimbableWall();
        }

        private boolean findClimbableWall() {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            // 在水平 5 格范围内寻找合适的墙壁底部
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    pos.set(ripper.blockPosition().offset(dx, 0, dz));
                    BlockState state = ripper.level().getBlockState(pos);
                    if (state.isSolid() && ripper.level().getBlockState(pos.above()).isAir()) {
                        // 检查墙壁是否在实体可到达的高度（通常直接前往底部）
                        wallBase = pos.immutable();
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void start() {
            if (wallBase != null) {
                // 移动到墙壁底部中心位置
                Vec3 targetPos = new Vec3(wallBase.getX() + 0.5, wallBase.getY(), wallBase.getZ() + 0.5);
                ripper.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            // 继续执行直到贴墙或目标消失
            return !ripper.isNearWall() &&
                    ripper.getTarget() != null &&
                    ripper.getTarget().isAlive() &&
                    !ripper.getNavigation().isDone();
        }

        @Override
        public void stop() {
            wallBase = null;
            ripper.getNavigation().stop();
        }
    }

    // ==================== 核心 tick ====================
    @Override
    public void tick() {
        super.tick();

        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        if (leapDirectionTicks > 0) {
            leapDirectionTicks--;
            if (this.getTarget() != null) {
                double dx = this.getTarget().getX() - this.getX();
                double dz = this.getTarget().getZ() - this.getZ();
                leapTargetYaw = (float) (Math.atan2(dz, dx) * (180F / Math.PI)) - 90F;
            }
        }

        if (!this.level().isClientSide) {
            if (this.isLeaping() && this.onGround()) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(normalSpeed);
                this.setLeaping(false);
            }

            if (this.getVariant() == Variant.VIRAL && this.tickCount % 40 == 0) {
                for (LivingEntity entity : this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(0.5),
                        e -> e != null &&
                                e.isAlive() &&
                                e != this &&
                                !IParasite.isParasiteByTagOrInterface(e)
                )) {
                    applyViralEffect(entity);
                }
            }

            int stage = this.getEvolutionStage();
            if (stage >= -2 && stage <= 1) {
                if (cothCloudCooldown > 0) {
                    cothCloudCooldown--;
                }
                checkPassiveEntityContact();
            }
        }
        if (this.attackTime > 0) {
            --this.attackTime;
        }

        if (!this.level().isClientSide && this.onGround() && this.isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(ModSoundEvents.RIPPER_STEP.get(), 0.8F, 1.0F);
                this.stepSoundDelay = 10 + this.random.nextInt(6);
            } else {
                this.stepSoundDelay--;
            }
        } else {
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }

        if (this.isClimbing() && this.getTarget() != null) {
            double dx = this.getTarget().getX() - this.getX();
            double dz = this.getTarget().getZ() - this.getZ();
            float targetYaw = (float) (Math.atan2(dz, dx) * (180F / Math.PI)) - 90F;
            this.setYRot(targetYaw);
            this.yBodyRot = targetYaw;
            this.yHeadRot = targetYaw;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        super.move(type, movement);
    }

    @Override
    public BlockPos getOnPos() {
        return super.getOnPos();
    }

    @Override
    protected void jumpFromGround() {
        super.jumpFromGround();
    }

    public float getMaxUpStep() {
        return 1.0F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    // ==================== 战斗相关 ====================
    protected double getAttackReachSqr(LivingEntity target) {
        return 1.4;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.attackTime > 0) {
            return false;
        }
        double distanceSqr = this.distanceToSqr(target);
        if (distanceSqr > 9.0) {
            return false;
        }
        if (IParasite.isParasiteByTagOrInterface(target)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean attackSuccess = super.doHurtTarget(target);
        if (attackSuccess && target instanceof LivingEntity livingTarget) {
            this.attackTime = 15;
            switch (this.getVariant()) {
                case BLEED -> applyBleedingEffect(livingTarget);
                case VIRAL -> applyViralEffect(livingTarget);
            }
            if (!this.level().isClientSide) {
                if (!(livingTarget instanceof Ripper) && !IParasite.isParasiteByTagOrInterface(livingTarget)) {
                    alertNearbyRupters(livingTarget);
                }
            }
        }
        return attackSuccess;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
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
        if (!this.level().isClientSide && result && source.getEntity() instanceof LivingEntity attacker) {
            if (!IParasite.isParasiteByTagOrInterface(attacker)) {
                alertNearbyRupters(attacker);
            }
        }
        return result;
    }

    // ==================== 效果应用 ====================
    private void applyBleedingEffect(LivingEntity target) {
        MobEffectInstance existingEffect = target.getEffect(ModEffects.BLEEDING.get());
        int newAmplifier = 0;
        if (existingEffect != null) {
            newAmplifier = Math.min(existingEffect.getAmplifier() + 1, 4);
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.BLEEDING.get(),
                100,
                newAmplifier,
                false, false, true
        ));
    }

    private void applyViralEffect(LivingEntity target) {
        MobEffectInstance existingEffect = target.getEffect(ModEffects.VIRAL.get());
        int newAmplifier = 0;
        if (existingEffect != null) {
            newAmplifier = Math.min(existingEffect.getAmplifier() + 1, 255);
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.VIRAL.get(),
                100,
                newAmplifier,
                false, false, true
        ));
    }

    // ==================== COTH 云 ====================
    private void checkPassiveEntityContact() {
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox());
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living &&
                    entity.isAlive() &&
                    !IParasite.isParasiteByTagOrInterface(living) &&
                    isPassiveOrNeutral(living) &&
                    !isAggressiveUtilityMob(living) &&
                    cothCloudCooldown <= 0) {
                MobEffectInstance cothEffect = living.getEffect(ModEffects.COTH.get());
                if (cothEffect == null || cothEffect.getAmplifier() < 2) {
                    spawnCothCloud();
                    cothCloudCooldown = COTH_CLOUD_COOLDOWN;
                }
            }
        }
    }

    private void spawnCothCloud() {
        if (this.level() instanceof ServerLevel serverLevel) {
            AreaEffectCloud cloud = new AreaEffectCloud(
                    serverLevel,
                    this.getX(),
                    this.getY() + 0.5,
                    this.getZ()
            );
            cloud.setRadius(1.5F);
            cloud.setDuration(160);
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(0);
            cloud.setParticle(new DustParticleOptions(
                    new Vector3f(0.6f, 0.0f, 0.0f),
                    1.0f
            ));
            cloud.addEffect(new MobEffectInstance(
                    ModEffects.COTH.get(),
                    40,
                    1,
                    false, false, true
            ));
            serverLevel.addFreshEntity(cloud);
        }
    }

    // ==================== 辅助判断 ====================
    private boolean isPassiveOrNeutral(LivingEntity entity) {
        return entity instanceof Animal ||
                entity instanceof Villager ||
                entity instanceof Wolf ||
                entity instanceof Panda ||
                entity instanceof Dolphin ||
                entity instanceof Fox ||
                entity instanceof Ocelot ||
                entity instanceof Rabbit ||
                entity instanceof Parrot ||
                entity instanceof PolarBear ||
                entity instanceof Turtle ||
                entity instanceof Bee ||
                entity instanceof Creeper;
    }

    private boolean isAggressiveUtilityMob(LivingEntity entity) {
        return entity instanceof IronGolem ||
                entity instanceof SnowGolem ||
                IParasite.isParasiteByTagOrInterface(entity);
    }

    private void alertNearbyRupters(LivingEntity target) {
        if (this.level().isClientSide) return;
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }
        for (Ripper rupter : this.level().getEntitiesOfClass(
                Ripper.class,
                this.getBoundingBox().inflate(32.0),
                e -> e != null && e.isAlive() && e != this
        )) {
            if (rupter.getTarget() == null || !rupter.getTarget().isAlive()) {
                rupter.setTarget(target);
            } else if (rupter.getTarget() instanceof Ripper || IParasite.isParasiteByTagOrInterface(rupter.getTarget())) {
                rupter.setTarget(target);
            }
        }
    }

    private void alertNearbyRuptersAboutTarget(LivingEntity target) {
        if (this.level().isClientSide) return;
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }
        for (Ripper rupter : this.level().getEntitiesOfClass(
                Ripper.class,
                this.getBoundingBox().inflate(25.0),
                e -> e != null && e.isAlive() && e != this
        )) {
            rupter.setTarget(target);
        }
    }

    private int getNearbyRupterCount() {
        return this.level().getEntitiesOfClass(
                Ripper.class,
                this.getBoundingBox().inflate(32.0),
                e -> e != null && e.isAlive()
        ).size();
    }

    private int getEvolutionStage() {
        if (this.level() instanceof ServerLevel serverLevel) {
            EvolutionManager evolutionManager;
            if (serverLevel.dimension().equals(Level.OVERWORLD)) {
                evolutionManager = EvolutionManager.forOverworld(serverLevel);
            } else if (serverLevel.dimension().equals(Level.NETHER)) {
                evolutionManager = EvolutionManager.forNether(serverLevel);
            } else if (serverLevel.dimension().equals(Level.END)) {
                evolutionManager = EvolutionManager.forEnd(serverLevel);
            } else {
                evolutionManager = EvolutionManager.forOverworld(serverLevel);
            }
            return evolutionManager.getStage();
        }
        return 0;
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
    }

    // ==================== 内部 AI 类 ====================
    static class MoveToPassiveGoal extends Goal {
        public final Ripper rupter;
        private final double speedModifier;
        private final float followDistance;
        public LivingEntity target;
        private int calmDown;

        public MoveToPassiveGoal(Ripper rupter, double speedModifier, float followDistance) {
            this.rupter = rupter;
            this.speedModifier = speedModifier;
            this.followDistance = followDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            int stage = rupter.getEvolutionStage();
            if (stage < -2 || stage > 1) {
                return false;
            }
            List<LivingEntity> list = rupter.level().getEntitiesOfClass(
                    LivingEntity.class,
                    rupter.getBoundingBox().inflate(followDistance),
                    this::isValidTarget
            );
            if (list.isEmpty()) {
                return false;
            }
            this.target = list.get(rupter.getRandom().nextInt(list.size()));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null &&
                    target.isAlive() &&
                    rupter.distanceToSqr(target) < (double) (followDistance * followDistance) &&
                    isValidTarget(target);
        }

        @Override
        public void start() {
            this.calmDown = reducedTickDelay(10);
        }

        @Override
        public void stop() {
            this.target = null;
            rupter.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (--calmDown <= 0) {
                calmDown = 10;
                rupter.getLookControl().setLookAt(target, 10.0F, (float) rupter.getMaxHeadXRot());
                rupter.getNavigation().moveTo(target, speedModifier);
            }
        }

        private boolean isValidTarget(LivingEntity entity) {
            if (entity == null ||
                    !entity.isAlive() ||
                    entity == rupter ||
                    IParasite.isParasiteByTagOrInterface(entity) ||
                    (entity instanceof Player player && !(player.isCreative() || player.isSpectator())) ||
                    rupter.isAggressiveUtilityMob(entity)) {
                return false;
            }
            MobEffectInstance cothEffect = entity.getEffect(ModEffects.COTH.get());
            if (cothEffect != null && cothEffect.getAmplifier() >= 2) {
                return false;
            }
            return rupter.isPassiveOrNeutral(entity);
        }
    }

    static class RandomTargetSelectionGoal extends Goal {
        private final Ripper rupter;
        private int cooldown;
        private final int minCooldown = 60;
        private final int maxCooldown = 100;

        public RandomTargetSelectionGoal(Ripper rupter) {
            this.rupter = rupter;
            this.cooldown = rupter.getRandom().nextInt(maxCooldown - minCooldown) + minCooldown;
        }

        @Override
        public boolean canUse() {
            int stage = rupter.getEvolutionStage();
            if (stage < 2 || stage > 10) {
                return false;
            }
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            cooldown = rupter.getRandom().nextInt(maxCooldown - minCooldown) + minCooldown;
            LivingEntity target = findRandomTarget();
            if (target != null) {
                rupter.alertNearbyRuptersAboutTarget(target);
            }
        }

        private LivingEntity findRandomTarget() {
            var entities = rupter.level().getEntitiesOfClass(
                    LivingEntity.class,
                    rupter.getBoundingBox().inflate(20.0),
                    e -> isValidTarget(e)
            );
            if (entities.isEmpty()) {
                return null;
            }
            return entities.get(rupter.getRandom().nextInt(entities.size()));
        }

        private boolean isValidTarget(LivingEntity entity) {
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                return false;
            }
            return entity != rupter &&
                    !IParasite.isParasiteByTagOrInterface(entity) &&
                    !(entity instanceof Creeper) &&
                    EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity);
        }
    }

    static class ConditionalPanicGoal extends PanicGoal {
        private final Ripper rupter;

        public ConditionalPanicGoal(Ripper rupter, double speedModifier) {
            super(rupter, speedModifier);
            this.rupter = rupter;
        }

        @Override
        public boolean canUse() {
            int stage = rupter.getEvolutionStage();
            if (stage >= -2 && stage <= 1) {
                return super.canUse();
            }
            return false;
        }
    }

    static class ConditionalAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private final Ripper rupter;

        public ConditionalAvoidGoal(Ripper rupter, Class<T> entityClass, float maxDistance, double walkSpeed, double sprintSpeed, Predicate<LivingEntity> predicate) {
            super(rupter, entityClass, maxDistance, walkSpeed, sprintSpeed, predicate);
            this.rupter = rupter;
        }

        @Override
        public boolean canUse() {
            int stage = rupter.getEvolutionStage();
            if (stage >= -2 && stage <= 1) {
                return super.canUse();
            }
            return false;
        }
    }

    static class ConditionalAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private final Ripper rupter;

        private static final Predicate<LivingEntity> TARGET_PREDICATE = entity -> {
            if (entity == null || !entity.isAlive() || entity.isRemoved())
                return false;
            if (IParasite.isParasiteByTagOrInterface(entity) || entity instanceof Creeper) {
                return false;
            }
            if (entity instanceof Animal ||
                    entity instanceof AmbientCreature ||
                    entity instanceof WaterAnimal ||
                    entity instanceof Villager) {
                return false;
            }
            return true;
        };

        public ConditionalAttackGoal(Ripper rupter) {
            super(rupter, LivingEntity.class, 10, true, false, TARGET_PREDICATE);
            this.rupter = rupter;
        }

        @Override
        public boolean canUse() {
            int stage = rupter.getEvolutionStage();
            if (stage < -2 || stage > 1) {
                return false;
            }
            int count = rupter.getNearbyRupterCount();
            return count >= 3 && super.canUse();
        }

        @Override
        protected void findTarget() {
            super.findTarget();
            if (target != null && (target instanceof Creeper ||
                    IParasite.isParasiteByTagOrInterface(target))) {
                target = null;
            }
        }
    }

    static class LeapAttackGoal extends Goal {
        private final Ripper rupter;
        private LivingEntity target;
        private final double speedModifier;
        private final int minLeapDistance;
        private final int maxLeapDistance;
        private int leapCooldown;
        private final int leapCooldownTime;
        private int leapingTicks;

        public LeapAttackGoal(Ripper rupter, double speedModifier, int maxLeapDistance, int minLeapDistance, int leapCooldownTime) {
            this.rupter = rupter;
            this.speedModifier = speedModifier;
            this.minLeapDistance = minLeapDistance;
            this.maxLeapDistance = maxLeapDistance;
            this.leapCooldownTime = leapCooldownTime;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 如果正在攀爬，禁止飞扑
            if (rupter.isClimbing()) {
                return false;
            }

            int stage = rupter.getEvolutionStage();
            if (stage < 2 || stage > 10) {
                return false;
            }
            this.target = rupter.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            if (target instanceof Creeper ||
                    IParasite.isParasiteByTagOrInterface(target)) {
                return false;
            }
            if (leapCooldown > 0) {
                leapCooldown--;
                return false;
            }
            double distance = rupter.distanceToSqr(target);
            if (distance < minLeapDistance * minLeapDistance) {
                return false;
            }
            if (distance > maxLeapDistance * maxLeapDistance) {
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            if (target != null) {
                double dx = target.getX() - rupter.getX();
                double dz = target.getZ() - rupter.getZ();
                float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                rupter.setYRot(yaw);
                rupter.yBodyRot = yaw;
                rupter.yHeadRot = yaw;
            }
            Vec3 vec = new Vec3(
                    target.getX() - rupter.getX(),
                    0,
                    target.getZ() - rupter.getZ()
            ).normalize();
            rupter.setDeltaMovement(
                    vec.x * speedModifier,
                    0.5,
                    vec.z * speedModifier
            );
            rupter.normalSpeed = rupter.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            rupter.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(rupter.normalSpeed * 1.0);
            rupter.setLeaping(true);
            leapingTicks = 0;
            rupter.setLeapDirectionTicks(5);
            rupter.getLookControl().setLookAt(target, 30.0F, 30.0F);
            leapCooldown = leapCooldownTime;
        }

        @Override
        public void tick() {
            if (rupter.onGround() && rupter.isLeaping()) {
                rupter.setLeaping(false);
            }
            leapingTicks++;
        }

        @Override
        public void stop() {
            rupter.setLeaping(false);
            rupter.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(rupter.normalSpeed);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    static class RandomSoundGoal extends Goal {
        private final Ripper rupter;
        private int nextSoundTick;

        RandomSoundGoal(Ripper rupter) {
            this.rupter = rupter;
        }

        @Override
        public boolean canUse() {
            return rupter.isAlive() && !rupter.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = rupter.getRandom().nextInt(120) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                playRandomLivingSound();
                this.nextSoundTick = rupter.getRandom().nextInt(120) + 80;
            }
        }

        private void playRandomLivingSound() {
            rupter.playSound(ModSoundEvents.RIPPER_IDLE.get(), 1.0F, 1.0F);
        }
    }

    /**
     * 基于碰撞箱与固体方块的相交检测，判断实体是否紧贴墙壁。
     * 收缩碰撞箱避免地面干扰。
     */
    private boolean isNearWall() {
        AABB bb = this.getBoundingBox();
        AABB expanded = bb.inflate(0.05);
        // 只检查水平相邻方块（上下不检查）
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = this.blockPosition().relative(dir);
            BlockState state = this.level().getBlockState(neighbor);
            if (state.isSolid()) {
                VoxelShape shape = state.getCollisionShape(this.level(), neighbor);
                if (!shape.isEmpty()) {
                    AABB wallBox = shape.bounds().move(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                    if (expanded.intersects(wallBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ==================== 动画 ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Ripper> controller = new AnimationController<>(this, "controller", 4, event -> {
            if (isAprilFoolsDay()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("dance"));
                return PlayState.CONTINUE;
            }
            boolean isMoving = event.isMoving();
            if (isLeaping()) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("pounce_flying"));
            } else {
                if (isMoving) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
                } else if (this.isNearWall() && !this.onGround()) {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("climb"));
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                }
            }
            return PlayState.CONTINUE;
        });
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    // ==================== 声音 ====================
    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.RIPPER_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSoundEvents.RIPPER_HUNT.get();
    }

    // ==================== 纹理 ====================
    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case BLEED:
                return new ResourceLocation("epca", "textures/entity/ripper_bleed.png");
            case VIRAL:
                return new ResourceLocation("epca", "textures/entity/ripper_viral.png");
            default:
                return new ResourceLocation("epca", "textures/entity/ripper.png");
        }
    }

    // ==================== 杂项 ====================
    private static boolean isAprilFoolsDay() {
        return LocalDate.now().getMonthValue() == 4 && LocalDate.now().getDayOfMonth() == 1;
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
    public void die(DamageSource source) {
        super.die(source);
        this.onDeath(source);
    }

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            IParasite.super.onKillEntity(killedEntity);
        }
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return false;
    }
}