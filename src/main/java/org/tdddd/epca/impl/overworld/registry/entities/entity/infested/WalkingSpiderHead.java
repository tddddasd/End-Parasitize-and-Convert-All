package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;


import net.minecraft.core.BlockPos;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
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

public class WalkingSpiderHead extends PathfinderMob implements GeoEntity, Enemy {

    public static final double WALK_SPEED = 0.15D;

    public static final double CAVE_WALK_SPEED = 0.12D;

    private static final String VARIANT_TAG = "Variant";

    private static final double POUNCE_RANGE = 5.0D;
    private static final int POUNCE_COOLDOWN_TICKS = 40;

    private static final double POUNCE_SPEED_MULTIPLIER = 1.44D;

    private static final double CLIMB_SPEED_FACTOR = 1.6D;

    private static final int CLIMB_COOLDOWN_TICKS = 200;

    private int climbCooldown = 0;
    private static final double SELF_DESTRUCT_RANGE = 1.6D;

    private static final float SELF_DESTRUCT_RADIUS = 3.0F;

    private static final float SELF_DESTRUCT_DAMAGE = 4.0F;

    private static final String CLIP_IDLE = "idle";
    private static final String CLIP_WALK = "walk";
    private static final String CLIP_CLIMB = "climb";
    private static final String CLIP_FLYING = "flying";

    private static final String CLIP_DEAD = "dead";

    private static final int DETONATE_TICKS = 12;

    public enum Variant {
        DEFAULT,
        BLOOD,
        CAVE
    }

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(WalkingSpiderHead.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_POUNCING =
            SynchedEntityData.defineId(WalkingSpiderHead.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_IS_DETONATING =
            SynchedEntityData.defineId(WalkingSpiderHead.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    private int pounceCooldown;
    private int pounceTimer;

    private boolean detonated;

    private boolean burstApplied;

    private int burstCountdown;

    private boolean burstLocked;

    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(WalkingSpiderHead.class, EntityDataSerializers.BOOLEAN);

    private int fakeDeathTimer = 30;

    private boolean fakeDeathResolved;

    private DamageSource fakeDeathSource;

    public WalkingSpiderHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        this.navigation = new GroundPathNavigation(this, level);

        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
        this.entityData.define(DATA_IS_POUNCING, false);
        this.entityData.define(DATA_IS_DETONATING, false);
        this.entityData.define(DATA_IS_FAKING_DEATH, false);
    }

    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    public boolean isDetonating() {
        return this.entityData.get(DATA_IS_DETONATING);
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

    public double getWalkSpeed() {
        return isCaveVariant() ? CAVE_WALK_SPEED : WALK_SPEED;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(VARIANT_TAG, getVariant().name());
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
    }

    public ResourceLocation getTextureResource() {
        switch (getVariant()) {
            case CAVE:
                return new ResourceLocation("epca", "textures/entity/walking_cave_spider_head.png");
            case BLOOD:
                return new ResourceLocation("epca", "textures/entity/walking_spider_head_blood.png");
            default:
                return new ResourceLocation("epca", "textures/entity/walking_spider_head.png");
        }
    }

    public boolean isPouncing() {
        return this.entityData.get(DATA_IS_POUNCING);
    }

    private void setPouncing(boolean pouncing) {
        this.entityData.set(DATA_IS_POUNCING, pouncing);
    }

    private String animationSuffix() {
        return isCaveVariant() ? "_cave" : "";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",
                EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));

        AnimationController<WalkingSpiderHead> deadController = new AnimationController<>(this,
                "dead_controller", EpcaGeoAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        deadController.triggerableAnim("dead", RawAnimation.begin().thenPlay(CLIP_DEAD + animationSuffix()));
        controllers.add(deadController);
    }

    private PlayState animationPredicate(AnimationState<WalkingSpiderHead> event) {
        String suffix = animationSuffix();

        if (this.isDetonating()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay(CLIP_DEAD + suffix));
        } else if (isClimbingWall()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_CLIMB + suffix));
        } else if (isPouncing()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_FLYING + suffix));
        } else if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_WALK + suffix));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(CLIP_IDLE + suffix));
        }
        return PlayState.CONTINUE;
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

    @Override
    public boolean onClimbable() {
        return isClimbingWall();
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
        if (state.is(ModBlocks.INFESTED_SPIDER_WEB.get())
                || state.is(ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get())
                || state.is(ModBlocks.INFESTED_CAVE_SPIDER_WEB.get())
                || state.is(Blocks.COBWEB)) {
            return;
        }
        super.makeStuckInBlock(state, motionMultiplier);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.01F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (isClimbingWall() && (this.horizontalCollision || !this.onGround())) {
                Vec3 motion = this.getDeltaMovement();

                this.setDeltaMovement(motion.x, getWalkSpeed() * CLIMB_SPEED_FACTOR, motion.z);
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

        if (this.burstLocked) {
            if (this.burstCountdown > 0 && --this.burstCountdown <= 0) {

                resolveBurst(this.fakeDeathSource);
            }
            return;
        }

        if (this.pounceCooldown > 0) this.pounceCooldown--;
        if (this.pounceTimer > 0 && --this.pounceTimer == 0) {
            setPouncing(false);
        }

        if (this.tickCount % 10 == 0) {
            org.tdddd.epca.impl.epca.LOGGER.info(
                    "[climb-diag][head] nearWall={} target={} cd={} climbing={} onClimbable={} y={}",
                    this.isNearWall(), this.getTarget() != null, this.climbCooldown,
                    this.isClimbingWall(), this.onClimbable(), this.getY());
        }

        if (this.climbCooldown > 0) {
            this.climbCooldown--;
        }
        if (!this.level().isClientSide()
                && this.climbCooldown <= 0
                && this.isNearWall()
                && this.getTarget() != null
                && !this.onGround()
                && this.isBlockedAbove()) {
            this.climbCooldown = CLIMB_COOLDOWN_TICKS;
            Vec3 dm = this.getDeltaMovement();
            if (dm.y > 0) {
                this.setDeltaMovement(dm.x, 0, dm.z);
            }
        }

        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        double wanted = getWalkSpeed();
        if (speed != null && speed.getBaseValue() != wanted) {
            speed.setBaseValue(wanted);
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()
                && this.distanceToSqr(target) <= SELF_DESTRUCT_RANGE * SELF_DESTRUCT_RANGE) {
            selfDestruct();
            return;
        }

    }

    public void selfDestruct() {
        if (this.detonated || this.burstLocked || this.level().isClientSide) {
            return;
        }
        this.detonated = true;

        triggerDismemberment();
        selfDestructBurst();
    }

    private void burstLock(DamageSource source) {
        this.fakeDeathSource = source;
        this.burstLocked = true;

        this.setHealth(EntityHealthUtils.burstHealth(this, 0.02F));
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        this.entityData.set(DATA_IS_DETONATING, true);
        this.triggerAnim("dead_controller", "dead");
        this.burstCountdown = DETONATE_TICKS;
    }

    private void resolveBurst(DamageSource source) {
        this.fakeDeathResolved = true;
        setFakingDeath(false);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        if (source != null) {
            boolean burning = this.isOnFire();
            super.die(source);
            if (!burning) {
                selfDestructBurst();
            } else {
                this.discard();
            }
        } else {
            this.selfDestructBurst();
        }
    }

    private void selfDestructBurst() {
        if (this.burstApplied || this.level().isClientSide) {
            return;
        }
        this.burstApplied = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            applyBurstDamage(serverLevel);
            applyVariantEffectToNearby(serverLevel);
            placeVariantWeb(serverLevel);
        }

        this.discard();
    }

    private void finishDetonation() {
        selfDestructBurst();
    }

    private void applyBurstDamage(ServerLevel level) {
        DamageSource source = level.damageSources().explosion(this, this);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SELF_DESTRUCT_RADIUS),
                e -> e != null && e.isAlive() && e != this)) {
            living.hurt(source, SELF_DESTRUCT_DAMAGE);
        }
    }

    private void applyVariantEffectToNearby(ServerLevel level) {
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

    private void placeVariantWeb(ServerLevel level) {
        BlockState web;
        switch (getVariant()) {
            case BLOOD -> web = InfestedSpiderWebBlood.markSpiderWeb(
                    ModBlocks.INFESTED_SPIDER_WEB_BLOOD.get().defaultBlockState(), true);
            case CAVE -> web = InfestedCaveSpiderWeb.markSpiderWeb(
                    ModBlocks.INFESTED_CAVE_SPIDER_WEB.get().defaultBlockState(), true);
            default -> web = InfestedSpiderWeb.markSpiderWeb(
                    ModBlocks.INFESTED_SPIDER_WEB.get().defaultBlockState(), true);
        }

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
    public void die(DamageSource source) {

        super.die(source);
        if (!this.isOnFire()) {
            selfDestruct();
        }
    }

    private void triggerFakeDeath(DamageSource source) {
        setFakingDeath(true);
        fakeDeathTimer = 30;

        burstLock(source);
    }

    private void resolveFakeDeath(DamageSource source) {
        resolveBurst(source);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PounceGoal(this));

        this.goalSelector.addGoal(3, new KeepMovingToHighTargetGoal(this));

        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FollowTargetGoal(this, 1.0D, 16));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
    public boolean startRiding(Entity vehicle, boolean force) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) {
            return false;
        }
        return super.startRiding(vehicle, force);
    }

    @Override
    protected boolean canRide(Entity entity) {
        return !(entity instanceof Boat) && !(entity instanceof Minecart) && super.canRide(entity);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

        public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    public static boolean checkWalkingSpiderHeadSpawnRules(EntityType<WalkingSpiderHead> entityType,
                                                           ServerLevelAccessor levelAccessor,
                                                           MobSpawnType spawnType,
                                                           BlockPos pos,
                                                           RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            return stage >= 2 && stage <= 5;
        }
        return true;
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

    public static class PounceGoal extends Goal {
        private final WalkingSpiderHead head;

        public PounceGoal(WalkingSpiderHead head) {
            this.head = head;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = head.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!head.onGround() || head.pounceCooldown > 0 || head.isPouncing()) return false;
            double distSq = head.distanceToSqr(target);
            if (distSq < 1.5D * 1.5D || distSq > POUNCE_RANGE * POUNCE_RANGE) return false;
            return head.getSensing().hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = head.getTarget();
            if (target == null) return;

            double dx = target.getX() - head.getX();
            double dz = target.getZ() - head.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 0.1D) {

                double speed = Math.min(horizontal * 0.15D, 0.55D) * POUNCE_SPEED_MULTIPLIER;
                head.setDeltaMovement(dx / horizontal * speed, 0.34D * POUNCE_SPEED_MULTIPLIER,
                        dz / horizontal * speed);
            } else {
                head.setDeltaMovement(0.0D, 0.34D * POUNCE_SPEED_MULTIPLIER, 0.0D);
            }
            head.hasImpulse = true;
            head.setPouncing(true);
            head.pounceTimer = 14;
            head.pounceCooldown = POUNCE_COOLDOWN_TICKS;
        }

        @Override
        public boolean canContinueToUse() {
            return head.isPouncing() && head.pounceTimer > 0;
        }

        @Override
        public void stop() {
            head.setPouncing(false);
        }
    }

    public static class KeepMovingToHighTargetGoal extends Goal {
        private final WalkingSpiderHead head;
        private int repathTimer;

        public KeepMovingToHighTargetGoal(WalkingSpiderHead head) {
            this.head = head;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = head.getTarget();
            return target != null && target.isAlive() && target.getY() - head.getY() > 1.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = head.getTarget();
            return target != null && target.isAlive() && target.getY() - head.getY() > 0.5D;
        }

        @Override
        public void start() {
            this.repathTimer = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = head.getTarget();
            if (target == null) {
                return;
            }
            if (this.repathTimer > 0) {
                this.repathTimer--;
            }
            if (this.repathTimer <= 0 || head.getNavigation().isDone()) {
                this.repathTimer = 10;
                head.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0D);
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
