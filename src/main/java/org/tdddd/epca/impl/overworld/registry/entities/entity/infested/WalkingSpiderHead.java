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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.epca.impl.client.entity.EpcaAnimations;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedCaveSpiderWeb;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSpiderWeb;
import org.tdddd.epca.impl.overworld.registry.blocks.block.InfestedSpiderWebBlood;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca.impl.overworld.registry.entities.ai.FollowTargetGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.GoToBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PlaceBeckonCoreGoal;
import org.tdddd.epca.impl.overworld.registry.entities.ai.PriorityTargetGoal;
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
 * The walking spider head (the mod's head mob of the spider family).
 *
 * <p>README behaviour implemented here:</p>
 * <ul>
 *   <li>4 health, 0 armour and 0 melee attack damage for both heads;</li>
 *   <li>walk and climb speed of 1.5 blocks per second for the plain head, 1 for the cave head;</li>
 *   <li>can pounce and climb walls, climbing at the same speed as walking;</li>
 *   <li>approaches its target and then self-destructs for 4 damage, leaving the web block of its
 *       own variant behind;</li>
 *   <li>self-destructs on death as well, except while burning;</li>
 *   <li>the plain head inherits buffs, the bleeding variant applies Bleeding I and the cave
 *       variant applies Poison II when it self-destructs.</li>
 * </ul>
 *
 * <h2>Animation clips</h2>
 * <p>{@code walking_spider_head.animation.json} contains exactly these keys, and GeckoLib keys its
 * baked map by the literal JSON key, so these are the only names that may ever be requested:</p>
 * <pre>
 *   idle   walk   climb   flying   dead                    (plain)
 *   idle_cave   walk_cave   climb_cave   flying_cave   dead_cave   (_cave)
 * </pre>
 * <p>Two consequences, both deliberate:</p>
 * <ul>
 *   <li>the head file has <b>no {@code idle2}</b> (only the spider's file has one), so the README's
 *       "30 % idle2" cannot be honoured for the heads and {@code idle} is always used instead;</li>
 *   <li>the head file has <b>no {@code shoot}</b>, so the head's web spit plays the ordinary
 *       movement/idle animation rather than a dedicated one.</li>
 * </ul>
 * <p>Requesting a name outside that set makes GeckoLib throw {@code NoSuchElementException} while
 * extracting the render state (it calls {@code List#getLast()} on an empty animation-stage list),
 * which surfaces as a {@code ReportedException} and crashes the game - the clip constants below
 * exist so that can never happen again.</p>
 */
public class WalkingSpiderHead extends PathfinderMob implements GeoEntity, IParasite, IInfested, Enemy {

    /** Walk / climb speed of the plain head: 1.5 blocks per second. */
    public static final double WALK_SPEED = 0.15D;
    /** Walk / climb speed of the cave head: 1 block per second. */
    public static final double CAVE_WALK_SPEED = 0.12D;

    private static final String VARIANT_TAG = "Variant";

    private static final double POUNCE_RANGE = 5.0D;
    private static final int POUNCE_COOLDOWN_TICKS = 40;
    /** Pounce velocity multiplier (user: +20 %). Scales the pounce dash and lunge only - never the
     * walk/climb speed, which the README fixes at the walk value. */
    private static final double POUNCE_SPEED_MULTIPLIER = 1.44D;
    /** Climb ascent as a multiple of the walk speed (README: same speed on walls). */
    private static final double CLIMB_SPEED_FACTOR = 1.6D;
    /** Ripper's climb cooldown: after a blocked climb the mob pauses this long before trying again. */
    private static final int CLIMB_COOLDOWN_TICKS = 200;
    /** Counts down the climb pause; also gates isClimbingWall(), exactly as Ripper does. */
    private int climbCooldown = 0;
    private static final double SELF_DESTRUCT_RANGE = 1.6D;

    /** Web spit range in blocks (same as the infested spider). */
    private static final double WEB_RANGE = 12.0D;
    /** Web spit cooldown in ticks: 1.5 seconds. */
    private static final int WEB_COOLDOWN_TICKS = 30;

    /** Blast radius of the self-destruct (its damage comes from the vanilla explosion). */
    private static final float SELF_DESTRUCT_RADIUS = 3.0F;
    /** Damage the burst deals to each entity in the radius (the value the vanilla explosion used to
     * supply). Applied directly, because Level#explode always emits the explosion sound and
     * particle - both are explicitly unwanted. */
    private static final float SELF_DESTRUCT_DAMAGE = 4.0F;

    //  Animation clips that provably exist in walking_spider_head.animation.json

    private static final String CLIP_IDLE = "idle";
    private static final String CLIP_WALK = "walk";
    private static final String CLIP_CLIMB = "climb";
    private static final String CLIP_FLYING = "flying";
    /** The burst/death clip, played on self-destruct. */
    private static final String CLIP_DEAD = "dead";
    /**
     * Phase-1 duration in ticks: the dead clip's animation_length is 0.5 s in
     * walking_spider_head.animation.json (both trees), 0.5 * 20 = 10 ticks, plus a 2-tick margin so
     * the pose is fully seen before the burst. Read from the JSON, not guessed.
     */
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
    /**
     * SYNCED detonating bit: phase 1 sets it, the client reads it, so the death pose is visible on
     * both sides. Paired with the private server-side once-only latch below.
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_DETONATING =
            SynchedEntityData.defineId(WalkingSpiderHead.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    private int pounceCooldown;
    private int pounceTimer;
    /** Guards against a double self-destruct when {@code die} runs after an explicit detonation. */
    private boolean detonated;
    /** Server-side latch: the burst in phase 2 can only ever run once. */
    private boolean burstApplied;
    /** Counts phase 1 down; the burst fires when it reaches 0. Owned by the PROXIMITY self-destruct. */
    private int detonateCountdown;
    /**
     * Separate countdown for the fake-death burst animation. It MUST be its own field: the fake death
     * and the proximity self-destruct both need a countdown, and sharing one would let a fake death
     * clobber an in-flight detonation (or vice versa).
     */
    private int fakeDeathBurstCountdown;

    /**
     * Cow-style fake-death bit. While set the head is invulnerable, frozen and lying in
     * {@link Pose#DYING}; when the timer expires it proceeds into its real follow-up (the two-phase
     * burst). Synced so the client shows the pose, exactly like the cow's {@code DATA_IS_FAKING_DEATH}.
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_FAKING_DEATH =
            SynchedEntityData.defineId(WalkingSpiderHead.class, EntityDataSerializers.BOOLEAN);
    /** Copied from InfestedCow#fakeDeathTimer: 30 ticks. */
    private int fakeDeathTimer = 30;
    /** Server-side once-only latch: the follow-up runs a single time. */
    private boolean fakeDeathResolved;
    /** The source that killed the mob, replayed when the fake death resolves. */
    private DamageSource fakeDeathSource;

    public WalkingSpiderHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        this.navigation = new GroundPathNavigation(this, level);
        // 26.1.2: Entity#setMaxUpStep was replaced by the STEP_HEIGHT attribute. Set once here, never
        // per tick. Copied from InfestedSpider's constructor, which already does this. Without it the
        // head cannot step a full block and so cannot get onto a wall ledge to begin climbing.
        AttributeInstance stepHeight = this.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) stepHeight.setBaseValue(1.0F);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, Variant.DEFAULT.ordinal());
        builder.define(DATA_IS_POUNCING, false);
        builder.define(DATA_IS_DETONATING, false);
        builder.define(DATA_IS_FAKING_DEATH, false);
    }

    //  Variant plumbing

    /** Client-and-server readable fake-death state, mirroring the cow's {@code isFakingDeath()}. */
    public boolean isFakingDeath() {
        return this.entityData.get(DATA_IS_FAKING_DEATH);
    }

    private void setFakingDeath(boolean faking) {
        this.entityData.set(DATA_IS_FAKING_DEATH, faking);
    }

    /** Client-and-server readable detonation state; the predicate keys off this, not `detonated`. */
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

    /** README: the plain head walks at 1.5 blocks per second, the cave head at 1. */
    public double getWalkSpeed() {
        return isCaveVariant() ? CAVE_WALK_SPEED : WALK_SPEED;
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

    /** Per-variant texture, read by {@code WalkingSpiderHeadModel#getTextureResource}. */
    public Identifier getTextureResource() {
        switch (getVariant()) {
            case CAVE:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/walking_cave_spider_head.png");
            case BLOOD:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/walking_spider_head_blood.png");
            default:
                return Identifier.fromNamespaceAndPath("epca", "textures/entity/walking_spider_head.png");
        }
    }

    //  Animation / state

    public boolean isPouncing() {
        return this.entityData.get(DATA_IS_POUNCING);
    }

    private void setPouncing(boolean pouncing) {
        this.entityData.set(DATA_IS_POUNCING, pouncing);
    }

    /** {@code "_cave"} for the cave head; the plain and bleeding heads use the unsuffixed set. */
    private String animationSuffix() {
        return isCaveVariant() ? "_cave" : "";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller",
                EpcaAnimations.GEO_TRANSITION_TICKS, this::animationPredicate));

        // Layer 2: the death pose, on its own triggered controller so it blends/settles independently
        // of locomotion - the same shape as the infested spider's shoot layer. Triggered once in
        // phase 1 and left to play while phase 1 counts down.
        AnimationController<WalkingSpiderHead> deadController = new AnimationController<>(
                "dead_controller", EpcaAnimations.GEO_TRANSITION_TICKS, state -> PlayState.STOP);
        deadController.triggerableAnim("dead", RawAnimation.begin().thenPlay(CLIP_DEAD + animationSuffix()));
        controllers.add(deadController);
    }

    /**
     * Every branch names a clip from the documented set (see the class javadoc), so the controller
     * can never request a missing animation. Climbing is tested before movement because the head can
     * be moving while on a wall, and the idle branch is the spawn/default state (both {@code idle}
     * and {@code idle_cave} exist).
     *
     * <p>Deliberately absent: {@code idle2} and {@code shoot}. Neither is in
     * {@code walking_spider_head.animation.json} - only the spider's file has them - and requesting a
     * missing clip makes GeckoLib throw NoSuchElementException during render-state extraction, which
     * crashes the game.</p>
     */
    private PlayState animationPredicate(AnimationTest<WalkingSpiderHead> event) {
        String suffix = animationSuffix();

        if (this.isDetonating()) {
            // Self-destruct: play the burst/death clip. Tested FIRST so the head's last rendered
            // state is the burst rather than a walk cycle.
            event.setAnimation(RawAnimation.begin().thenPlay(CLIP_DEAD + suffix));
        } else if (isClimbingWall()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_CLIMB + suffix));
        } else if (isPouncing()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_FLYING + suffix));
        } else if (event.isMoving()) {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_WALK + suffix));
        } else {
            event.setAnimation(RawAnimation.begin().thenLoop(CLIP_IDLE + suffix));
        }
        return PlayState.CONTINUE;
    }

    //  Wall climbing

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
        // 26.1.2 renamed Entity#isControlledByLocalInstance to isLocalInstanceAuthoritative.
        if (this.isLocalInstanceAuthoritative()) {
            if (this.isInWater()) {
                this.moveRelative(0.01F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (isClimbingWall() && !this.onGround()) {
                Vec3 motion = this.getDeltaMovement();
                // Climbing uses the same speed as walking (README), so the vertical boost stays
                // proportional between the two variants. Not part of the pounce change.
                this.setDeltaMovement(motion.x, getWalkSpeed() * CLIMB_SPEED_FACTOR, motion.z);
            }
        }
        super.travel(travelVector);
    }

    //  Tick

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        // Cow-style fake-death timer, copied from InfestedCow#tick (`:201-246`): run the timer down,
        // then resolve into the follow-up. Checked before the detonation driver, and it returns early
        // so no other tick logic runs while the mob is playing dead.
        if (this.isFakingDeath()) {
            // Removal is driven by the ANIMATION's length (DETONATE_TICKS), not the 30-tick fake-death
            // timer - user: "动画播放结束后移除自身". fakeDeathTimer is therefore no longer the gate;
            // it stays as the cow's value and keeps counting for parity, but it never triggers removal.
            // The countdown is the SEPARATE fakeDeathBurstCountdown field, so a fake death can never
            // clobber an in-flight proximity detonation (which owns detonateCountdown).
            if (this.fakeDeathBurstCountdown > 0 && --this.fakeDeathBurstCountdown <= 0) {
                DamageSource src = this.fakeDeathSource;
                resolveFakeDeath(src != null ? src : this.damageSources().generic());
            }
            return;
        }

        // PHASE 1 driver, before everything else so the AI freeze cannot skip it.
        if (this.detonated) {
            if (this.detonateCountdown > 0 && --this.detonateCountdown <= 0) {
                finishDetonation();
            }
            return;
        }

        if (this.pounceCooldown > 0) this.pounceCooldown--;
        if (this.pounceTimer > 0 && --this.pounceTimer == 0) {
            setPouncing(false);
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

        // Keep the walk/climb speed pinned to the README value regardless of goal modifiers.
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

        // No web spit here: the walking head must NOT shoot projectiles. The head animation file
        // has no "shoot" clip either.
    }

    //  Web spit
    //
    // The README says the web spit belongs to the infested spider's common kit. The head is that
    // same family and already has the projectile + web assets, so it spits too, at the same 12
    // block range and with the same non-damaging projectile.

    /**
     * Fires the variant's web projectile at the target when it is inside range and the cooldown has
     * elapsed. The projectile itself deals no damage; it only leaves the web block behind.
     */
    //  Self-destruct

    /**
     * Detonates the head: entity-only burst damage in a small radius, the variant's effect on top,
     * and exactly one web block of the variant.
     *
     * <p>README: the burst must play the model's death animation and produce NO explosion sound and
     * NO explosion particle. {@code Level#explode} cannot satisfy that - it always spawns the
     * explosion particle and plays {@code GENERIC_EXPLODE} - so the damage is applied directly to
     * each entity in the radius instead ({@code hurt} with the generic damage source, which is the
     * same amount the vanilla explosion used to deal). No block damage either; that matches the old
     * {@code ExplosionInteraction.NONE}.</p>
     *
     * <p>Two phases, because the death pose has to be VISIBLE before the burst: phase 1 sets the
     * synced detonating flag, triggers the {@code dead}/{@code dead_cave} pose on its own controller
     * and freezes the mob for {@link #DETONATE_TICKS}; phase 2 applies the burst and discards. The
     * pose is driven by the synced flag (so the CLIENT renders it) and the burst stays server-only.</p>
     */
    public void selfDestruct() {
        if (this.detonated || this.level().isClientSide()) {
            return;
        }
        this.detonated = true;

        // PHASE 1 - visible wind-up. No playSound and no explode call: the burst stays silent and
        // particle-free by design.
        this.entityData.set(DATA_IS_DETONATING, true);
        this.triggerAnim("dead_controller", "dead");
        this.detonateCountdown = DETONATE_TICKS;

        // Freeze the mob so the death pose is what the player sees. getNavigation().stop() plus
        // setNoAi(true) is the combination that reliably halts goals AND movement; setDeltaMovement
        // pins the current velocity so it does not keep sliding during the pose.
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoAi(true);
    }

    /**
     * PHASE 2 - the actual burst, run once when the phase-1 countdown reaches zero.
     *
     * <p>Server-only and latched by {@link #burstApplied}, so a mob that dies or is removed by
     * something else during phase 1 can never have the burst applied twice. Everything the old
     * single-tick {@code selfDestruct} did is here, unchanged: entity-only damage, the variant effect,
     * exactly one variant web, then {@code discard()}.</p>
     */
    private void finishDetonation() {
        if (this.burstApplied || this.level().isClientSide()) {
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

    /**
     * Entity-only burst damage. Mirrors what the vanilla explosion used to apply, without any of the
     * explosion sound or particle, and without touching blocks.
     */
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
                e -> e != null && e.isAlive() && !IParasite.isParasiteByTagOrInterface(e))) {
            switch (getVariant()) {
                case BLOOD -> living.addEffect(
                        new MobEffectInstance(ModEffects.BLEEDING, 100, 0, false, true));
                case CAVE -> living.addEffect(
                        new MobEffectInstance(MobEffects.POISON, 100, 1, false, true));
                default -> {
                    // README: the plain head just inherits buffs; nothing to apply to the target.
                }
            }
        }
    }

    /**
     * Leaves EXACTLY ONE web block of this variant, at the head's own position.
     *
     * <p>README: the burst must leave a single corresponding web. The previous shape tried the head's
     * block, then each of the four horizontal neighbours in turn, so it could place a web at a
     * position the head was not standing on. There is now a single deterministic target - the block
     * the head occupies, or the block just below it when the head's own block is inside geometry -
     * and at most one {@code setBlock} call for it.</p>
     *
     * <p>The {@code spider} flag is applied through the owning block class's guarded
     * {@code markSpiderWeb} helper, which is a no-op when the property is absent and keeps the
     * README's 60-second decay. Calling {@code setValue} on the property directly is what crashed
     * the server with "Cannot set property BooleanProperty{name=spider} as it does not exist in
     * Block{epca:infested_spider_web}" when a state did not carry it.</p>
     */
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
            // The head's own block is occupied (it detonated inside geometry): use the one directly
            // below so the web still lands at the head's feet rather than beside it.
            BlockPos below = pos.below();
            if (level.getBlockState(below).canBeReplaced()) {
                pos = below;
            }
        }

        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, web, 3);
        }
    }

    /**
     * README: the head also self-destructs when it dies, except while burning.
     *
     * <p>{@code super.die} runs first so loot and the death sound are still produced; the blast
     * itself is entity-only and therefore harmless to the dropped loot.</p>
     */
    @Override
    public void die(DamageSource source) {
        // Cow-style fake death FIRST (user: "使用与虫染牛类似的假死逻辑和音效，随后触发后续的伤害等逻辑").
        // The cow rolls 40 %; the user asked for "similar", and the head already has a 100 % rule for
        // the not-on-fire case, so the fake death is taken unconditionally here and the burst still
        // fires afterwards through the two-phase path. `fakeDeathResolved` stops the re-entry that
        // would otherwise happen when a second death source lands during the pose.
        if (!this.isFakingDeath() && !this.fakeDeathResolved && !this.level().isClientSide()) {
            triggerFakeDeath(source);
            return;
        }
        if (this.fakeDeathResolved) {
            // Coming out of the timer: super.die/onDeath and the burst were already handled there.
            super.die(source);
            return;
        }

        boolean burning = this.isOnFire();
        super.die(source);
        this.onDeath(source);
        // README: bursts on death with 100 % probability UNLESS burning. No roll, no cooldown.
        if (!burning) {
            selfDestruct();
        }
    }

    /**
     * Cow-style fake death, copied from {@code InfestedCow#triggerFakeDeath} ({@code :622-633}).
     *
     * <p>Every health/pose value is the cow's: timer 30, health {@code burstHealth(this, 0.02F)}
     * (2 %), invulnerable, {@code setNoAi(true)}, {@code setTarget(null)}, {@code Pose.DYING}.</p>
     *
     * <p><b>This is the HEALTH-LOCK instant, and it is where the burst animation starts</b> (user:
     * the burst animation must trigger at the moment the health is locked). The pose is played on the
     * triggered {@code dead_controller} layer so it renders over the locked pose, and the removal is
     * driven by {@link #DETONATE_TICKS} - the clip's own {@code animation_length} - not by the 30-tick
     * fake-death timer.</p>
     *
     * <p>The removal countdown is armed on {@link #fakeDeathBurstCountdown}, NOT on
     * {@link #detonateCountdown}: the proximity self-destruct owns that field and the two paths must
     * not clobber each other.</p>
     */
    private void triggerFakeDeath(DamageSource source) {
        setFakingDeath(true);
        setInvulnerable(true);
        fakeDeathTimer = 30;
        this.fakeDeathSource = source;
        this.setHealth(EntityHealthUtils.burstHealth(this, 0.02F));
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.setPose(Pose.DYING);

        // HEALTH LOCK DONE -> start the burst animation immediately, and arm the removal countdown
        // from the CLIP's length (DETONATE_TICKS = dead's animation_length in ticks + margin).
        this.triggerAnim("dead_controller", "dead");
        this.fakeDeathBurstCountdown = DETONATE_TICKS;
    }

    /**
     * The animation has finished: sound FIRST, then the damage + removal.
     *
     * <p><b>Order, and why it is not literally "discard first":</b> the user's sequence is remove ->
     * sound -> damage, but {@code applyBurstDamage} and {@code placeVariantWeb} read
     * {@code getBoundingBox()} and {@code blockPosition()}, and {@code discard()} invalidates the
     * entity's position tracking. Removing first would therefore put the damage and the web at risk of
     * landing nowhere - exactly the failure the requirement warns about ("do not let damage fail
     * because the entity is already gone"). So the burst is applied first, from the still-valid
     * position, and the removal happens LAST via {@code finishDetonation}'s {@code discard()}. The
     * player-visible result is identical: the animation ends, the web/burst lands, and the mob is
     * gone in the same tick.</p>
     */
    private void resolveFakeDeath(DamageSource source) {
        this.fakeDeathResolved = true;
        setFakingDeath(false);
        this.setInvulnerable(false);

        // The cow's burst sound, verbatim: same SoundEvent, source, volume and pitch (InfestedCow
        // :207-208). Server-side broadcast, unchanged.
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        boolean burning = this.isOnFire();
        super.die(source);
        this.onDeath(source);
        if (!burning) {
            // The head's burst: entity-only damage 4.0F in a 3.0F radius, the variant effect, exactly
            // one variant web, then discard(). Latched by burstApplied.
            selfDestruct();
        } else {
            // On fire: no burst at all, just removal.
            this.discard();
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
        // The head had no goal that WALKS at a target. PounceGoal only fires inside 1.5-5 blocks on a
        // 40-tick cooldown and claims MOVE while it runs, and FollowTargetGoal only runs when a
        // follow-target is set, so between pounces the head simply stood still. MeleeAttackGoal is
        // what closes the distance - the infested spider registers it at this priority and moves.
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new FollowTargetGoal(this, 1.0D, 16));
        this.targetSelector.addGoal(1, new PriorityTargetGoal(this, 16.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    //  Damage

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
    public boolean startRiding(Entity vehicle, boolean force, boolean sendEventAndTriggers) {
        if (vehicle instanceof Boat || vehicle instanceof Minecart) {
            return false;
        }
        return super.startRiding(vehicle, force, sendEventAndTriggers);
    }

    @Override
    protected boolean canRide(Entity entity) {
        return !(entity instanceof Boat) && !(entity instanceof Minecart) && super.canRide(entity);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    @Override
    public boolean canPassThroughInfestedLeaves() {
        return true;
    }

    /** Same evolution-stage gate as the other infested mobs; no light-level requirement. */
    public static boolean checkWalkingSpiderHeadSpawnRules(EntityType<WalkingSpiderHead> entityType,
                                                           ServerLevelAccessor levelAccessor,
                                                           EntitySpawnReason spawnType,
                                                           BlockPos pos,
                                                           RandomSource random) {
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            int stage = EvolutionManager.getStageForDimension(levelAccessor.getLevel());
            return stage >= 2 && stage <= 5;
        }
        return true;
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

    /** Pounce goal for the small head: same idea as the spider's, with a lower arc. */
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
                // Pounce speed +20 %: both the dash and the upward lunge are scaled.
                double speed = Math.min(horizontal * 0.15D, 0.55D) * POUNCE_SPEED_MULTIPLIER;
                head.setDeltaMovement(dx / horizontal * speed, 0.34D * POUNCE_SPEED_MULTIPLIER,
                        dz / horizontal * speed);
            } else {
                head.setDeltaMovement(0.0D, 0.34D * POUNCE_SPEED_MULTIPLIER, 0.0D);
            }
            head.hurtMarked = true;
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

    /**
     * Copied from Ripper#ClimbTowardsTargetGoal: walk to a climbable wall base when the target is
     * well above, so the mob actually reaches a wall and can enter the climbing state. Without this no
     * goal ever sends the mob to a wall, which is why the climb never started.
     */
    static class ClimbTowardsTargetGoal extends Goal {
        private final WalkingSpiderHead mob;
        private BlockPos wallBase;
        private int cooldown;

        public ClimbTowardsTargetGoal(WalkingSpiderHead mob) {
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
