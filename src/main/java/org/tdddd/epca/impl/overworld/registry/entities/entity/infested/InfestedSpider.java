package org.tdddd.epca.impl.overworld.registry.entities.entity.infested;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.WebAwareRangedAttackGoal;
import org.tdddd.epca.impl.utils.EntityHealthUtils;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Infested spider family (spider / cave spider). One entity type with a synced {@link Variant}
 * carries the README's three variants:
 *
 * <ul>
 *   <li>{@link Variant#DEFAULT} - the plain infested spider.</li>
 *   <li>{@link Variant#BLOOD} - the bleeding variant; melee hits always apply Bleeding I.</li>
 *   <li>{@link Variant#CAVE} - the infested cave spider; melee hits always apply Poison II and the
 *       cave animation/texture set is used.</li>
 * </ul>
 *
 * <p>This mirrors how the mod already models its other variant mobs: {@code Ripper} and
 * {@code InfestedFox} keep one entity type plus a synced variant ordinal and pick the texture per
 * instance, so no extra entity type is registered for the blood form.</p>
 *
 * <p>Movement follows the README: walk 1.5 blocks per second, run 4 blocks per second for the
 * spider and 1 / 5 for the cave spider, with wall climbing at the same speed. Vanilla movement
 * speed is blocks per tick, so 1.5 blocks/s = 0.075 and 4 blocks/s = 0.20.</p>
 */
public class InfestedSpider extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy, RangedAttackMob {

    /** Walk speed of the plain spider: 1.5 blocks per second. */
    public static final double SPIDER_WALK_SPEED = 0.15D;
    /** Run speed of the plain spider: 4 blocks per second. */
    public static final double SPIDER_RUN_SPEED = 0.30D;
    /** Walk speed of the cave spider: 1 block per second. */
    public static final double CAVE_WALK_SPEED = 0.12D;
    /** Run speed of the cave spider: 5 blocks per second. */
    public static final double CAVE_RUN_SPEED = 0.36D;

    /** Maximum web range in blocks (README: 12). */
    /** Maximum web range in blocks (README: 12). */
    private static final double WEB_RANGE = 12.0D;
    /** Ripper's climb cooldown: after a blocked climb the mob pauses this long before trying again. */
    private static final int CLIMB_COOLDOWN_TICKS = 200;
    /** Counts down the climb pause; also gates isClimbingWall(), exactly as Ripper does. */
    private int climbCooldown = 0;
    /** The face isNearWall() last found solid geometry against, or null. */
    private Direction nearWallDir;
    /** Inside this HORIZONTAL distance the spider switches to melee (user: under 4 blocks). */
    public static final double MELEE_HORIZONTAL_RANGE = 4.0D;
    /** The same threshold squared, so the check needs no square root. Strictly less than 4.0. */
    public static final double MELEE_HORIZONTAL_RANGE_SQ =
            MELEE_HORIZONTAL_RANGE * MELEE_HORIZONTAL_RANGE;
    /** Web shooting cooldown in ticks: 1.5 seconds. */
    /**
     * Web shooting cooldown in ticks: 4 seconds (user: one shot every 4 s).
     *
     * <p>Was 30 (1.5 s). This single constant is BOTH the entity-side gate (performRangedAttack sets
     * webCooldown = WEB_COOLDOWN_TICKS) and the WebAwareRangedAttackGoal constructor's attackInterval,
     * i.e. the vanilla RangedAttackGoal retry interval - the only two use sites, so raising it here
     * sets the real cadence to one shot per 4 s with nothing left at 30.</p>
     */
    private static final int WEB_COOLDOWN_TICKS = 80;
    /** How long the synced spit flag stays up, and therefore how long the shoot layer is active. */
    private static final int SHOOT_ANIMATION_TICKS = 12;
    /** Projectile launch speed. Was 1.2F; raised so the ballistic lead stays small over 12 blocks. */
    private static final float WEB_VELOCITY = 1.35F;
    /**
     * Projectile launch spread in degrees. Was 4.0F (the vanilla spider's value), which is a very
     * wide cone - at 12 blocks it could miss by several blocks. Tightened to a near-perfect shot.
     */
    private static final float WEB_INACCURACY = 0.35F;
    /** Per-tick vertical drag a thrown projectile applies: 0.03 * velocity on the Y axis. */
    private static final double WEB_GRAVITY_PER_TICK = 0.06D;
    /** Minimum upward aim component, so a level shot still clears the ground it was launched from. */
    private static final double MIN_AIM_RISE = 0.35D;
    /** Pounce cooldown in ticks: 2 seconds. */
    private static final int POUNCE_COOLDOWN_TICKS = 40;
    /** Pounce velocity multiplier (user: +20 %). Scales BOTH the horizontal dash and the upward
     * component, and nothing else - in particular not the walk/run speed constants. */
    private static final double POUNCE_SPEED_MULTIPLIER = 1.44D;
    /** Upward velocity gained while climbing a wall. */
    private static final double CLIMB_UP_SPEED = 0.2D;
    /** Distance at which the spider starts a pounce. */
    private static final double POUNCE_RANGE = 5.0D;
    /** Chance to use the second idle animation (README: 30 %). */
    private static final float IDLE2_CHANCE = 0.30F;

    /**
     * Cow-style fake-death bit, mirroring {@code InfestedCow#DATA_IS_FAKING_DEATH}. Synced so the
     * client shows {@link Pose#DYING}; the mob survives the pose and proceeds into the follow-up.
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(InfestedSpider.class, EntityDataSerializers.BOOLEAN);
    /** Copied from InfestedCow#fakeDeathTimer: 30 ticks. */
    private int fakeDeathTimer = 30;
    /** Server-side once-only latch: the follow-up runs a single time. */
    private boolean fakeDeathResolved;
    /** The source that killed the mob, replayed when the fake death resolves. */
    private DamageSource fakeDeathSource;

    /** The three asset variants of this family. Ordinal is what is synced. */
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

    /** Set once the natural-spawn buff roll (README: 5 % Speed I + Strength I) has been done. */
    private boolean spawnBuffsRolled;

    public InfestedSpider(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.navigation = new GroundPathNavigation(this, level);
        // 26.1.2: Entity#setMaxUpStep was replaced by the STEP_HEIGHT attribute.
        AttributeInstance stepHeight = this.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) stepHeight.setBaseValue(1.0F);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
        builder.define(DATA_IS_FAKING_DEATH, false);
        builder.define(DATA_IS_POUNCING, false);
        builder.define(DATA_IS_SHOOTING, false);
        builder.define(DATA_RUNNING, false);
        builder.define(DATA_IDLE2, false);
    }

    //  Variant plumbing

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
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(VARIANT_TAG, getVariant().name());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        // 26.1.2: CompoundTag's accessors are Optional-returning; use the *Or forms with a default.
        if (tag.getString(VARIANT_TAG).isPresent()) {
            try {
                setVariant(Variant.valueOf(tag.getStringOr(VARIANT_TAG, "")));
            } catch (IllegalArgumentException e) {
                setVariant(Variant.DEFAULT);
            }
        }
    }

    /**
     * Per-variant texture, read by {@code InfestedSpiderModel#getTextureResource}.
     *
     * <p>The cave variant ships a single texture and the blood variant only exists for the plain
     * spider, which is exactly the asset set supplied for this mob.</p>
     */
    public Identifier getTextureResource() {
        switch (getVariant()) {
            case CAVE:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_cave_spider.png");
            case BLOOD:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_spider_blood.png");
            default:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/infested_spider.png");
        }
    }

    //  Animation / state

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

    //  Animation clips
    //
    // GeckoLib keys its baked animation map by the LITERAL JSON key, and
    // infested_spider.animation.json ships exactly these keys:
    //
    //   idle1   idle2   walk   run   climb   attack   flying   dead   shoot        (plain)
    //   idle1_cave   idle2_cave   walk_cave   run_cave
    //   climb_cave   attack_cave   flying_cave   dead_cave                        (_cave)
    //
    // Note the "_cave" names in the supplied file: they are idle1_cave / idle2_cave, NOT
    // idle_cave / idle2_cave. There is no plain "idle" clip for this mob, and there is no
    // "shoot_cave", so the spit keeps the plain "shoot" clip for both variants. Requesting any name
    // outside the list above makes GeckoLib throw NoSuchElementException while extracting the render
    // state (it calls List#getLast() on an empty stage list), which crashes the game.
    //
    // The file was also shipped with Blockbench/GeckoLib-5 style keys ("animation.infested_spider.idle1");
    // those prefixes are stripped so the keys are the plain names every other animation file in this
    // mod uses. See the kit's fix10-animation-keys.ps1 and README10-ASSETS.txt.

    private static final String CLIP_IDLE1 = "idle1";
    private static final String CLIP_IDLE2 = "idle2";
    private static final String CLIP_WALK = "walk";
    private static final String CLIP_RUN = "run";
    private static final String CLIP_CLIMB = "climb";
    private static final String CLIP_FLYING = "flying";
    /** The only clip the file has without a cave twin. */
    private static final String CLIP_SHOOT = "shoot";
    /** Melee clip; has a cave twin via animationSuffix(). */
    private static final String CLIP_ATTACK = "attack";

    /** {@code "_cave"} selects the cave set; only the cave variant uses it. */
    private String animationSuffix() {
        return isCaveVariant() ? "_cave" : "";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Layer 1: locomotion. This controller ONLY ever plays idle / walk / run / climb / flying, so
        // a web spit can never interrupt it. It deliberately has no "shoot" branch.
        controllers.add(new AnimationController<>("controller",
                EpcaAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));

        // Layer 2: the web spit, on its OWN controller so GeckoLib blends the two layers. The
        // predicate returns STOP so the clip only plays when triggerAnim asks for it, the same
        // layout InfestedChicken uses for its egg throw.
        AnimationController<InfestedSpider> shootController = new AnimationController<>(
                "shoot_controller", EpcaAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        shootController.triggerableAnim("shoot", RawAnimation.begin().thenPlay(CLIP_SHOOT));
        controllers.add(shootController);

        // Layer 3: melee attack, same shape as the shoot layer. The `attack` clip animates only the
        // body and head (NOT the `legs` parent - verified in the live JSON), so it cannot cause the
        // leg twitch the old shoot clip did.
        AnimationController<InfestedSpider> attackController = new AnimationController<>(
                "attack_controller", EpcaAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        attackController.triggerableAnim("attack", RawAnimation.begin().thenPlay(CLIP_ATTACK + animationSuffix()));
        controllers.add(attackController);
    }

    /** Opens the spit window: sets the synced flag and triggers the dedicated shoot layer. */
    private void beginShootAnimation() {
        setShooting(true);
        this.shootAnimationTicks = SHOOT_ANIMATION_TICKS;
        // No "shoot_cave" clip exists, so the spit uses the plain "shoot" for both variants.
        this.triggerAnim("shoot_controller", "shoot");
    }

    /**
     * Every branch names a clip from the documented set, so the controller can never request a
     * missing animation. The final idle branch is also the spawn/default state, and
     * {@code idle1}/{@code idle1_cave} both exist.
     */
    private PlayState animationPredicate(AnimationTest<InfestedSpider> event) {
        String suffix = animationSuffix();

        // CLIMB first: a climbing mob is airborne, so isPouncing() could be true and would otherwise
        // win and render `flying`. Chosen over suppressing pouncing, because the pounce flag also
        // drives the lunge physics and must keep working when the mob leaves the wall.
        if (isClimbingWall()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_CLIMB + suffix));
        } else if (isPouncing()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_FLYING + suffix));
        } else if (isRunning()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_RUN + suffix));
        } else if (event.isMoving()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_WALK + suffix));
        } else {
            // README: while idling the spider plays idle1 and, 30 % of the time, idle2 instead.
            String idle = usingIdle2() ? CLIP_IDLE2 : CLIP_IDLE1;
            event.setAnimation(RawAnimation.begin().thenLoop(idle + suffix));
        }
        return PlayState.CONTINUE;
    }

    //  Wall climbing (Ripper-style: only while actually chasing something)

    /** True when a block with a collision shape sits immediately in front of the spider. */
    /**
     * True when a solid block's collision box touches this mob's own bounding box, in any of the four
     * HORIZONTAL directions.
     *
     * <p>Copied literally from Ripper#isNearWall: the bounding box is inflated by 0.05, every
     * horizontal neighbour of blockPosition() is tested, and the wall's collision bounds moved into
     * world space must INTERSECT that inflated box.</p>
     */
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
                        // Remember WHICH face we are against so the climb facing reuses this scan.
                        this.nearWallDir = dir;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * README: this mob takes no fall damage, and neither does Ripper.
     *
     * <p>Mirrors Ripper#causeFallDamage exactly: return false. NOTE the signature: 26.1.2 takes a
     * DOUBLE fallDistance (1.20.1 takes a float), which is what the live 26 Ripper uses - the float
     * form would not override. Only the fall-damage path is short-circuited, so every other damage
     * type still applies.</p>
     */

    /** Copied from Ripper: lets the mob step a full block, which is what gets it onto wall ledges. */
    // NOTE: NOT an override in this version - an @Override annotation is a compile error. Ripper has
    // the identical plain method, so this is only an accessor; the real assignment is
    // this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0F) (1.20.1: setMaxUpStep) in the
    // constructor.
    public float getMaxUpStep() {
        return 1.0F;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    /**
     * True when solid geometry sits directly above the mob, copied literally from
     * Ripper#isBlockedAbove. Used by the climb tick to stop an ascent that has run into a ceiling.
     */
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

    public boolean isClimbingWall() {
        if (this.climbCooldown > 0) {
            return false;
        }
        // NOTE: deliberately does NOT require !onGround(). It used to be
        //     return !this.onGround() && isNearWall();
        // and that made climbing unreachable: onClimbable() delegates here, so while the mob stood on
        // the ground next to a wall the whole mechanic reported false, travel() therefore never
        // applied its upward component, and the mob could never leave the ground to satisfy the
        // !onGround() half. The airborne requirement now lives in travel() only - exactly the split
        // Ripper (the class this was modelled on) uses.
        return isNearWall() && this.getTarget() != null;
    }

    @Override
    public boolean onClimbable() {
        return isClimbingWall();
    }

    /**
     * Turns the mob to LOOK INTO the wall face it is climbing.
     *
     * <p>Reuses the Direction isNearWall() already found. The yaw is vanilla's horizontal lookAt form
     * for a vector (dx, dz) running from the mob toward the CENTRE of the wall block:
     * atan2(dz, dx) * (180/PI) - 90. Correct for all four faces: east(+X) 0, south(+Z) 90,
     * west(-X) 180, north(-Z) -90.</p>
     */
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

    /**
     * README: the spider walks through its own webs freely.
     *
     * <p>The mod's web blocks call {@code LivingEntity#makeStuckInBlock} from
     * {@code entityInside}, and the vanilla cobweb does the same, so ignoring the slowdown here
     * covers both the infested web variants and {@code minecraft:cobweb}.</p>
     */
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
        // 26.1.2 renamed Entity#isControlledByLocalInstance to isLocalInstanceAuthoritative.
        if (this.isLocalInstanceAuthoritative()) {
            if (this.isInWater()) {
                this.moveRelative(0.01F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (isClimbingWall() && !this.onGround()) {
                Vec3 motion = this.getDeltaMovement();
                // Climb at the current movement speed: the README asks for the same speed on walls.
                //
                // The guard admits TWO cases, and the first is what makes the climb start:
                //   * horizontalCollision - pressed against the wall while GROUNDED. The old
                //     `&& !onGround()` guard rejected exactly this, which is why climbing never began:
                //     nothing else ever lifted the mob off the ground, so the flag it waited for could
                //     never become true.
                //   * !onGround() - already airborne on a wall, the original case.
                this.setDeltaMovement(motion.x, CLIMB_UP_SPEED, motion.z);
            }
        }
        super.travel(travelVector);
    }

    //  Server tick: speed switching, pounce bookkeeping, web cooldown

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        // Cow-style fake-death timer, copied from InfestedCow#tick (`:201-246`): run it down, then
        // resolve into the follow-up. Returns early so no other logic runs while playing dead.
        if (this.isFakingDeath()) {
            if (--this.fakeDeathTimer <= 0) {
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

        // Face the climbed wall. Only while actually on a wall.
        if (this.isClimbingWall()) {
            faceClimbedWall();
        }

        // Ripper's climb tick, copied: run the cooldown down, then stop the ascent when walled in
        // from above, so the mob does not creep up a wall forever.
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

        // Idle animation bookkeeping: re-roll idle1/idle2 on a timer while standing still.
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

    /**
     * README: a naturally spawned spider has a 5 % chance to receive Speed I and Strength I, and a
     * converted spider inherits the buffs the original carried. The conversion path is handled in
     * {@code CothEffect} plus {@link #applyInheritedEffects}; this method only rolls the
     * natural-spawn bonus.
     *
     * <p>26.1.2 dropped the trailing {@code CompoundTag} parameter of {@code finalizeSpawn}.</p>
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (!this.spawnBuffsRolled) {
            this.spawnBuffsRolled = true;
            if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
                if (this.random.nextFloat() < 0.05F) {
                    applySpawnBuffs();
                }
            }
        }
        return result;
    }

    /** Speed I and Strength I, the README's natural-spawn bonus. */
    public void applySpawnBuffs() {
        this.addEffect(new MobEffectInstance(MobEffects.SPEED, Integer.MAX_VALUE, 0, false, false));
        this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, Integer.MAX_VALUE, 0, false, false));
    }

    /**
     * Applies the effects the converted vanilla spider carried over. Invisibility is dropped
     * (README), and everything else is re-applied untouched.
     */
    public void applyInheritedEffects(java.util.Collection<MobEffectInstance> inherited) {
        if (inherited == null) {
            return;
        }
        for (MobEffectInstance effect : inherited) {
            if (effect != null && effect.getEffect() != MobEffects.INVISIBILITY) {
                this.addEffect(new MobEffectInstance(effect));
            }
        }
    }

    //  Goals

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new PlaceBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new GoToBeckonCoreGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbTowardsTargetGoal(this));
        this.goalSelector.addGoal(2, new PounceGoal(this));
        // Web-aware ranged goal: it stands down while the target is inside an infested web,
        // which lets the MeleeAttackGoal below become the driving goal (user requirement).
        this.goalSelector.addGoal(3, new WebAwareRangedAttackGoal(this, 1.0D, WEB_COOLDOWN_TICKS, (float) WEB_RANGE));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FollowTargetGoal(this, 1.0D, 16));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 24.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    //  Web occupancy (user requirement 1)

    /**
     * True when the given entity is standing in one of THIS MOD's infested web blocks.
     *
     * <p>Block list: epca:infested_spider_web, epca:infested_spider_web_blood and
     * epca:infested_cave_spider_web - all three, and the spider blockstate property is deliberately
     * NOT consulted, so a web placed by any source (spit, head burst, hand-placed) counts. Vanilla
     * minecraft:cobweb is NOT included: the requirement names the infested spider web specifically,
     * and vanilla cobweb has no spider flag to reason about.</p>
     *
     * <p>Both the block the entity occupies and the block below it are tested, so a target whose
     * feet are level with the web (the usual case, since a web is not solid and the entity stands
     * inside it) is detected as well as one spawned directly in it.</p>
     */
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

    /**
     * True when the target is inside the horizontal melee radius.
     *
     * <p>User requirement: within MELEE_HORIZONTAL_RANGE blocks of HORIZONTAL distance the spider
     * switches to melee instead of shooting. The Y axis is deliberately ignored, so a target on a
     * ledge above or below still counts - distanceToSqr would have missed those.</p>
     *
     * <p>Boundary: strictly less than 4 blocks (squared &lt; 16.0); at exactly 4 blocks the spider
     * still shoots.</p>
     */
    public static boolean isWithinMeleeHorizontalRange(LivingEntity spider, LivingEntity target) {
        if (target == null) {
            return false;
        }
        double dx = target.getX() - spider.getX();
        double dz = target.getZ() - spider.getZ();
        return dx * dx + dz * dz < MELEE_HORIZONTAL_RANGE_SQ;
    }

    /** Convenience for callers that already hold the spider. */
    public boolean isWithinMeleeHorizontalRange(LivingEntity target) {
        return isWithinMeleeHorizontalRange(this, target);
    }

    //  Ranged attack: the non-damaging web spit (README: up to 12 blocks, animation is a blend)

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level().isClientSide() || this.webCooldown > 0) {
            return;
        }
        // User requirement: a target standing inside one of this mod's web blocks is not shot at -
        // the spider closes in and melees instead. WebAwareRangedAttackGoal normally stops this goal
        // from running at all in that state; this is the second line of defence for the case where
        // the goal is mid-attack when the target steps into a web.
        if (isStandingInInfestedWeb(target)) {
            return;
        }
        // Same second line of defence for the melee radius: the goal stands down, and this guard also
        // stops an attack that was already committed before the target closed inside the radius.
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

        Projectile web = type.create(this.level(), EntitySpawnReason.MOB_SUMMONED);
        if (web == null) {
            return;
        }

        // The web ORIGINATES AT THE SPIDER'S FEET, not its eyes: the projectile leaves from the leg
        // position and the web it leaves behind therefore sits at foot level instead of floating at
        // mouth height. Aiming subtracts the same foot height so the arc is unchanged.
        double originY = this.getY();
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        // Aim point: the centre of the target's bounding box, which is what "hit the mob" means.
        double aimY = target.getY() + target.getBbHeight() * 0.5D;
        double dy = aimY - originY;

        // Gravity lead. A thrown projectile loses 0.03 * velocity on each axis per tick, so the drop
        // over the flight is 0.5 * g * t^2 with g = 0.06 and t = horizontal / velocity. Adding that
        // drop to the aim is what makes the shot arrive instead of landing short.
        double travelTicks = horizontal / WEB_VELOCITY;
        double drop = 0.5D * WEB_GRAVITY_PER_TICK * travelTicks * travelTicks;
        double aimRise = dy + drop;

        // Guarantee a minimum upward component so a shot at a target on the same level still lifts
        // clear of the ground the projectile was launched from.
        if (aimRise < MIN_AIM_RISE) {
            aimRise = MIN_AIM_RISE;
        }

        // Aim slightly above the target so the projectile arcs onto it. The projectile carries no
        // damage; only the web block it leaves behind matters.
        web.setOwner(this);
        web.setPos(this.getX(), originY, this.getZ());
        web.shoot(dx, aimRise, dz, WEB_VELOCITY, WEB_INACCURACY);
        this.playSound(SoundEvents.SPIDER_HURT, 0.4F, 0.6F);
        this.level().addFreshEntity(web);
    }

    //  Melee: per-variant on-hit effect

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            applyVariantOnHitEffect(living);
        }
        return hit;
    }

    /** README: BLOOD always applies Bleeding I, CAVE always applies Poison II. */
    public void applyVariantOnHitEffect(LivingEntity target) {
        switch (getVariant()) {
            case BLOOD -> target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 100, 0, false, true));
            case CAVE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, true));
            default -> {
            }
        }
    }

    //  Damage / death

    @Override
    public void die(DamageSource source) {
        // Cow-style fake death first (user: mirror the infested cow's fake death + sounds, then run
        // the follow-up). Unconditional rather than the cow's 40 % roll - see the report; the user
        // asked for "similar" logic, not the same chance, and a spider is a swarm mob.
        if (!this.isFakingDeath() && !this.fakeDeathResolved && !this.level().isClientSide()) {
            triggerFakeDeath(source);
            return;
        }
        super.die(source);
        this.onDeath(source);
    }

    /** Client-and-server readable fake-death state, mirroring the cow's {@code isFakingDeath()}. */
    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    /**
     * Enters the cow-style fake-death pose: every value is the cow's
     * ({@code InfestedCow#triggerFakeDeath}), namely the synced flag, invulnerability, the 30-tick
     * timer, health {@code burstHealth(this, 0.02F)} (2 %), no AI, no target and {@code Pose.DYING}.
     *
     * <p>The spider does NOT call {@code super.die()} here - it survives the pose and runs its
     * follow-up when the timer expires (see {@link #resolveFakeDeath(DamageSource)}).</p>
     */
    private void triggerFakeDeath(DamageSource source) {
        this.fakeDeathSource = source;
        setFakingDeath(true);
        setInvulnerable(true);
        this.fakeDeathTimer = 30;
        this.setHealth(EntityHealthUtils.burstHealth(this, 0.02F));
        this.setNoAi(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);
    }

    /**
     * End of the fake-death pose: play the cow's burst sound, then run the follow-up death logic.
     *
     * <p>The cow's own follow-up plays this exact sound, spawns the COTH/SPLASHI particles,
     * schedules remains + buglins with a delayed TickTask and places an AreaEffectCloud
     * (COTH 1200t II + Poison 200t I), then {@code discard()}s. The spider has no remains/cloud
     * resources of its own, so its equivalent follow-up is the plain death it always had
     * ({@code super.die()} + {@code onDeath()}, which drops loot). What it does NOT do is burst -
     * the cow has no death burst at all, so there is nothing of that kind to mirror.</p>
     */
    private void resolveFakeDeath(DamageSource source) {
        this.fakeDeathResolved = true;
        setFakingDeath(false);
        this.setInvulnerable(false);

        // The cow's burst sound, verbatim: same SoundEvent, source, volume and pitch.
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        super.die(source);
        this.onDeath(source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && shouldIgnoreDamageFrom(attacker)) {
            return false;
        }
        if (!this.level().isClientSide() && source.getEntity() instanceof LivingEntity attacker) {
            this.onAttacked(attacker);
        }
        return super.hurtServer(level, source, ((IParasite) this).onHurt(source, amount));
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
        // Spiders are silent steppers in vanilla; keep only a very quiet step.
        this.playSound(SoundEvents.SPIDER_STEP, 0.05F, 1.0F);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    /**
     * Natural spawn rule for both variants.
     *
     * <p>The mod gates its infested mobs on the evolution stage and, unlike the zombie family, the
     * spiders do not use a light level requirement (the README mentions none), so the rule only
     * checks the stage. Cave spiders share the same rule; the variant itself is assigned by
     * {@code CothEffect} during conversion.</p>
     */
    public static boolean checkInfestedSpiderSpawnRules(EntityType<InfestedSpider> entityType,
                                                        ServerLevelAccessor levelAccessor,
                                                        EntitySpawnReason spawnType,
                                                        BlockPos pos,
                                                        RandomSource random) {
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            return stage >= 1 && stage <= 5;
        }
        return true;
    }

    //  Goals

    /** Leaps at the target like {@code Ripper}; the animation switches to {@code flying}. */
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
                // Pounce speed +20 %: the horizontal dash is scaled, and so is the upward component.
                double speed = Math.min(horizontal * 0.12D, 0.7D) * POUNCE_SPEED_MULTIPLIER;
                spider.setDeltaMovement(dx / horizontal * speed, 0.42D * POUNCE_SPEED_MULTIPLIER,
                        dz / horizontal * speed);
            } else {
                spider.setDeltaMovement(0.0D, 0.42D * POUNCE_SPEED_MULTIPLIER, 0.0D);
            }
            spider.hurtMarked = true;
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

    /**
     * Copied from Ripper#ClimbTowardsTargetGoal: walk to a climbable wall base when the target is
     * well above, so the mob actually reaches a wall and can enter the climbing state. Without this no
     * goal ever sends the mob to a wall, which is why the climb never started.
     */
    static class ClimbTowardsTargetGoal extends Goal {
        private final InfestedSpider mob;
        private BlockPos wallBase;
        private int cooldown;

        public ClimbTowardsTargetGoal(InfestedSpider mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (target.getY() - mob.getY() < 1.5) return false;
            if (mob.isNearWall()) return false;
            if (--cooldown > 0) return false;
            cooldown = 10;
            return findClimbableWall();
        }

        private boolean findClimbableWall() {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    pos.set(mob.blockPosition().offset(dx, 0, dz));
                    BlockState state = mob.level().getBlockState(pos);
                    if (state.isSolid() && mob.level().getBlockState(pos.above()).isAir()) {
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
                Vec3 targetPos = new Vec3(wallBase.getX() + 0.5, wallBase.getY(), wallBase.getZ() + 0.5);
                mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.isNearWall() &&
                    mob.getTarget() != null &&
                    mob.getTarget().isAlive() &&
                    !mob.getNavigation().isDone();
        }

        @Override
        public void stop() {
            wallBase = null;
            mob.getNavigation().stop();
        }
    }
}
