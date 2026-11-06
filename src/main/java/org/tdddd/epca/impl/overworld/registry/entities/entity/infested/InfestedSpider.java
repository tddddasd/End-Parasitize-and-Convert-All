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

    private static final String VARIANT_TAG = "Variant";

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

        controllers.add(new AnimationController<>(this, "controller",
                EpcaGeoAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));

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

        if (isClimbingWall()) {
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

        if (this.tickCount % 10 == 0) {
            org.tdddd.epca.impl.epca.LOGGER.info(
                    "[climb-diag][spider] nearWall={} target={} cd={} climbing={} onClimbable={} y={} inWeb={}",
                    this.isNearWall(), this.getTarget() != null, this.climbCooldown,
                    this.isClimbingWall(), this.onClimbable(), this.getY(),
                    isStandingInInfestedWeb(this));
        }

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
            this.climbCooldown = CLIMB_COOLDOWN_TICKS;
            Vec3 dm = this.getDeltaMovement();
            if (dm.y > 0) {
                this.setDeltaMovement(dm.x, 0, dm.z);
            }
        }

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
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            applyVariantOnHitEffect(living);

            this.triggerAnim("attack_controller", "attack");
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
