package org.tdddd.epca.impl.overworld.registry.blocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;
import org.tdddd.epca_physics.structure.SubLevel;
import org.tdddd.epca_physics.structure.SubLevelBehaviour;
import org.tdddd.epca_physics.structure.SubLevelRegistry;

import java.util.List;

/**
 * Phase 3 {@code InfestedPumpkinPhysics extends PhysicsEntity}
 *  {@link SubLevelBehaviour}  epca
 * {@link SubLevelRegistry#captureSingle} <b></b>
 *
 *
 * <p></p>
 * <ul>
 *   <li> 1 </li>
 *   <li>16 /
 *       </li>
 *   <li><b>FEATURE 1</b>/
 *        {@link #CLIMB_MAX_HEIGHT}1.4
 *        {@link #updateClimb}</li>
 *   <li>  1.6 1.0</li>
 *   <li> {@code epca:coth} I 30  + {@code epca:fear} I 15 {@code addEffect}
 *       </li>
 *   <li> 100 tick5    {@link InfestedCarvedPumpkin}{@code NATURAL_SPAWN=true}
 *        yaw</li>
 *   <li><b></b>
 *        {@link #onBlockMined}</li>
 * </ul>
 *
 * <p><b></b> {@code SubLevelRegistry}
 * GRAVITY 0.04 0.94/0.995 3.0 0.6 {@code PhysicsEntity}
 * 0.02 / 0.99-0.98 / 2.0 / 0.9
 * </p>
 */
public final class InfestedPumpkinBehaviour implements SubLevelBehaviour {

    /**
     *  id id {@code epca}
     * {@code SubLevelRegistry.registerBehaviour}
     */
    public static final ResourceLocation ID = new ResourceLocation(epca.MODID, "infested_pumpkin");

    /**  tick  5  */
    private static final int NO_TARGET_DELAY = 100;
    /**  */
    private static final double CHASE_RANGE = 16.0D;
    /**  */
    private static final double CHASE_SPEED = 0.25D;
    /**  tick  */
    private static final double CHASE_ACCEL = 0.15D;
    /**  */
    private static final double CONTACT_RANGE = 1.6D;
    /**  ATTACK_DAMAGE */
    private static final float ATTACK_DAMAGE = 1.0F;
    /**
     *  111  [0,1] 0.5
     *  =  /  {@link #updateRoll}
     */
    private static final double ROLL_RADIUS = 0.5D;
    /**  tick  0.001  */
    private static final double ROLL_MIN_SPEED = 0.001D;
    /**  */
    private static final float ROLL_DECAY = 0.92F;
    /**  */
    private static final float ROLL_MIN_ANGLE = 0.001F;

    // ------------------------------------------------------------------ FEATURE 1

    /**
     * <b>1.4 </b>/
     *  1.4 {@link #updateClimb}
     * {@code gained >= CLIMB_MAX_HEIGHT}
     *
     * <p> 1.41.0 1  0.4  +
     *  2   /</p>
     */
    public static final double CLIMB_MAX_HEIGHT = 1.4D;
    /**  tick  MAX_SPEED=3.0 4% */
    private static final double CLIMB_LIFT = 0.12D;
    /** / tick  */
    private static final double CLIMB_BLOCKED_EPSILON = 0.01D;
    /** / */
    private static final double CLIMB_PROBE = 0.15D;
    /**  */
    private static final double CLIMB_TARGET_MARGIN = 0.1D;
    /**  tick  physicsTick  moveAxis */
    private static final double GROUNDED_EPSILON = 1.0E-6D;
    /**  */
    private static final String TAG_NO_TARGET_TIMER = "NoTargetTimer";
    /**  {@link #yaw} */
    private static final String TAG_YAW = "Yaw";
    /**  */
    private static final String TAG_ROLL_X = "RollX";
    private static final String TAG_ROLL_Y = "RollY";
    private static final String TAG_ROLL_Z = "RollZ";
    private static final String TAG_ROLL_W = "RollW";

    /**  tick  */
    private int noTargetTimer;

    /**
     *  {@code SubLevel#yawOnlyRotation} / {@code PhysicsCommands#yawPitchRoll}
     *
     *
     * <p><b></b>{@link #roll}
     * {@code rotation * (0,0,1)}
     *  {@code getYRot()}  {@code Direction.fromYRot(getYRot())}
     * yaw  tick
     *  {@link #facingOf()} </p>
     */
    private float yaw;
    /**
     *
     *
     * <p> tick   {@code  / ROLL_RADIUS}
     * {@link #updateRoll} = {@code roll * yawOnly}
     * <b></b>
     * </p>
     */
    private final Quaternionf roll = new Quaternionf();
    /**  {@link #ROLL_DECAY}  */
    private float rollAngle;
    /**  tick <b></b> {@link #updateRoll} tick  */
    private Vec3 lastPosition;
    /**  yaw */
    private boolean poseInitialised;
    /**  tick {@link #updateRoll}  */
    private double lastHorizontalStep;
    /**  tick = 0  vy = 0  */
    private double lastVerticalStep;
    /**  {@link Double#NaN} */
    private double climbStartY = Double.NaN;
    /** / */
    private boolean climbExhausted;

    public InfestedPumpkinBehaviour() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    // ------------------------------------------------------------------

    @Override
    public void serverTick(ServerLevel level, SubLevel subLevel) {
        //  1
        if (!isLoneInfestedPumpkin(subLevel)) {
            this.noTargetTimer = 0;
            // <b></b> updateRoll
            //    tick
            this.lastPosition = subLevel.worldCentreOfMass();
            endClimb();
            return;
        }

        //  tick
        updateRoll(subLevel);

        LivingEntity target = findTarget(level, subLevel);
        if (target != null) {
            this.noTargetTimer = 0;
            //  tick  +
            //  tick
            //  updateRoll
            chase(subLevel, target);
            updateClimb(level, subLevel, target);
            if (canAttackEntity(subLevel, target)) {
                attack(level, target);
            }
        } else {
            this.noTargetTimer++;
            endClimb();
            if (this.noTargetTimer >= NO_TARGET_DELAY) {
                transformBackToBlock(level, subLevel);
            }
        }
    }

    // ------------------------------------------------------------------ FEATURE 1

    /**
     *  1.4  tick
     *
     * <p><b></b> tick </p>
     * <ol>
     *   <li>=  tick  {@link #CLIMB_BLOCKED_EPSILON}{@link #updateRoll}
     *        {@link #lastHorizontalStep}<b></b>
     *       {@link #CLIMB_PROBE} <b></b>{@code level.getBlockCollisions}
     *        null
     *        0 tick
     *       </li>
     *   <li>=  {@link #CLIMB_TARGET_MARGIN} </li>
     *   <li>  / {@link #CLIMB_LIFT}
     *        tick / 0.08 /tick</li>
     *   <li><b>1.4 </b>{@link #climbStartY}
     *        {@code  - climbStartY >= }{@link #CLIMB_MAX_HEIGHT}
     *       {@link #climbExhausted}<b></b>
     *         </li>
     *   <li> tick   0
     *        /  /  /
     *        {@link #endClimb()} <b></b>
     *        1.4     1.4
     *         endClimb</li>
     * </ol>
     */
    private void updateClimb(ServerLevel level, SubLevel subLevel, LivingEntity target) {
        AABB self = subLevel.worldAabb();
        boolean targetAbove = target.getBoundingBox().minY >= self.maxY - CLIMB_TARGET_MARGIN;
        boolean wallAhead = wallAhead(level, subLevel, target);
        boolean blocked = this.lastHorizontalStep < CLIMB_BLOCKED_EPSILON && wallAhead;
        boolean grounded = subLevel.velocity().y == 0.0D
                && Math.abs(this.lastVerticalStep) < GROUNDED_EPSILON;
        if (grounded && Double.isNaN(this.climbStartY)) {
            //   1.4
            this.climbExhausted = false;
        }
        if (!blocked || !targetAbove) {
            //  /
            endClimb();
            return;
        }
        double currentY = subLevel.position().y;
        if (Double.isNaN(this.climbStartY)) {
            this.climbStartY = currentY;
            this.climbExhausted = false;
        }
        double gained = currentY - this.climbStartY;
        Vec3 velocity = subLevel.velocity();
        if (gained >= CLIMB_MAX_HEIGHT) {
            this.climbExhausted = true;
            if (velocity.y > 0.0D) {
                subLevel.setVelocity(new Vec3(velocity.x, 0.0D, velocity.z));
            }
            return;
        }
        if (this.climbExhausted) {
            if (velocity.y > 0.0D) {
                subLevel.setVelocity(new Vec3(velocity.x, 0.0D, velocity.z));
            }
            return;
        }
        subLevel.setVelocity(new Vec3(velocity.x, CLIMB_LIFT, velocity.z));
    }

    /** /FEATURE 1  */
    private void endClimb() {
        this.climbStartY = Double.NaN;
        this.climbExhausted = false;
    }

    /**
     * {@link #CLIMB_PROBE} <b></b>
     *
     * <p> {@code level.getBlockCollisions(null, probe)} {@code null}
     * {@code SubLevelCollisions#getBlockCollisions}  vanilla
     * /</p>
     */
    private static boolean wallAhead(ServerLevel level, SubLevel subLevel, LivingEntity target) {
        Vec3 center = subLevel.worldAabb().getCenter();
        double dx = target.getX() - center.x;
        double dz = target.getZ() - center.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4D) {
            return false;
        }
        AABB probe = subLevel.worldAabb().move(dx / length * CLIMB_PROBE, 0.0D,
                dz / length * CLIMB_PROBE);
        for (VoxelShape shape : level.getBlockCollisions(null, probe)) {
            if (!shape.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------

    /**
     *  tick ROOT
     * {@code onClientTick}
     *
     *
     * <p><b></b></p>
     * <ul>
     *   <li> {@code (dx, dz)} <b></b>
     *        {@code s = sqrt(dx + dz)}</li>
     *   <li> {@code  = s / R}{@code R = }{@link #ROLL_RADIUS}{@code = 0.5}111 </li>
     *   <li> =    = {@code normalize(dz, 0, -dx)}
     *        +X  =  -Z </li>
     *   <li>{@code roll = R(, )  roll} =  = {@code roll  yawOnly}
     *        yaw yaw
     *        FACING  {@link #facingOf()}<b></b>
     *       {@link SubLevel#rotateAboutCentreOfMass(Quaternionf)} {@link #applyPose}
     *       = </li>
     *   <li><b></b>{@link SubLevel#worldCentreOfMass()}
     *        tick  {@code }  {@link #updateRoll}</li>
     * </ul>
     *
     * <p> &lt; {@link #ROLL_MIN_SPEED} {@link #ROLL_DECAY}
     * {@link #ROLL_MIN_ANGLE}    {@code ROLL_DECAY/ROLL_MIN_ANGLE}
     * </p>
     */
    private void updateRoll(SubLevel subLevel) {
        // FEATURE 1<b></b>position
        //  = capture  applyPose  applyPose
        //  tick
        //  tick  ==
        //  ==
        Vec3 position = subLevel.worldCentreOfMass();
        if (this.lastPosition == null) {
            //  tick/
            if (!this.poseInitialised) {
                this.poseInitialised = true;
                this.yaw = yawOf(subLevel.rotation());
            }
            this.lastPosition = position;
            // / tick  tick  0
            this.lastHorizontalStep = 0.0D;
            this.lastVerticalStep = 0.0D;
            applyPose(subLevel);
            return;
        }
        Vec3 delta = position.subtract(this.lastPosition);
        this.lastPosition = position;
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        // FEATURE 1 tick
        this.lastHorizontalStep = horizontal;
        this.lastVerticalStep = delta.y;
        if (horizontal > ROLL_MIN_SPEED) {
            Vec3 axis = new Vec3(delta.z, 0.0D, -delta.x).normalize();
            float step = (float) (horizontal / ROLL_RADIUS);
            this.roll.set(new Quaternionf().rotateAxis(step, new Vector3f((float) axis.x, 0.0F,
                    (float) axis.z)).mul(this.roll));
            this.rollAngle = (float) ((this.rollAngle + step) % (Math.PI * 2.0D));
            applyPose(subLevel);
        } else if (this.rollAngle != 0.0F) {
            this.rollAngle *= ROLL_DECAY;
            if (Math.abs(this.rollAngle) < ROLL_MIN_ANGLE) {
                this.rollAngle = 0.0F;
                this.roll.set(new Quaternionf());
            } else {
                //  ROLL_DECAY
                this.roll.slerp(new Quaternionf(), 1.0F - ROLL_DECAY);
            }
            applyPose(subLevel);
        }
    }

    /**
     *  + yaw{@code Q = R_roll  R_yaw}
     * <b></b>FEATURE 1
     *
     * <p> {@code setRotation} {@code position} <b></b>
     * {@code capture}
     * <b></b>
     * {@link SubLevel#rotateAboutCentreOfMass(Quaternionf)}
     * {@code p_new = comWorld  R_newc}{@code c} = {@code comWorld} =
     *   </p>
     *
     * <p>{@code R_roll  R_yaw} {@code yaw}
     *  FACING  {@link #facingOf()}
     *  {@code SubLevelRegistry#carryAndPushEntities} </p>
     */
    private void applyPose(SubLevel subLevel) {
        subLevel.rotateAboutCentreOfMass(new Quaternionf(this.roll).mul(new Quaternionf().rotateY(this.yaw)));
    }

    /**  yaw (0,0,1)  */
    private static float yawOf(Quaternionf rotation) {
        Vector3f forward = rotation.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        return (float) Math.atan2(-forward.x, forward.z);
    }

    /**
     *  {@link #yaw}
     * {@code Direction.fromYRot(this.getYRot())}
     *
     * <p>{@code Direction.fromYRot} <b></b>{@code Direction.java:416-418}
     * {@code Mth.floor(p / 90 + 0.5) & 3}
     *  yaw0 </p>
     */
    private Direction facingOf() {
        return Direction.fromYRot(Math.toDegrees(this.yaw));
    }

    /**
     *  1
     *
     * <p> +
     *
     * </p>
     */
    private static boolean isLoneInfestedPumpkin(SubLevel subLevel) {
        return subLevel.countBlocks(InfestedPumpkinBehaviour::isCarvedPumpkin) == 1;
    }

    private static boolean isCarvedPumpkin(BlockState state) {
        return state.is(ModBlocks.INFESTED_CARVED_PUMPKIN.get());
    }

    /**
     * 16 <b></b><b></b>
     *
     * <p><b></b>
     * 26.1.2  {@code InfestedPumpkinPhysics#findTarget}
     * {@code epca-26-pumpkin/.../InfestedPumpkinPhysics.java:217-236} {@code level.players()}
     * <b></b>
     * //
     * <b></b>
     * </p>
     *
     * <p>{@code isAlive()}{@code isSpectator()}
     * 26.1.2 <b></b>
     * {@code Player#hurt}  {@code abilities.invulnerable}
     * / {@link IParasite#isParasiteByTagOrInterface}
     * {@code IParasite.java:61-67} {@code NestLeaderManager.isNestLeader}
     * =
     *  {@code player.distanceToSqr(this)} </p>
     */
    @Nullable
    private static LivingEntity findTarget(ServerLevel level, SubLevel subLevel) {
        Vec3 center = subLevel.worldAabb().getCenter();
        //  CHASE_RANGE  getEntitiesOfClass
        AABB search = new AABB(center.x - CHASE_RANGE, center.y - CHASE_RANGE, center.z - CHASE_RANGE,
                center.x + CHASE_RANGE, center.y + CHASE_RANGE, center.z + CHASE_RANGE);
        LivingEntity nearestPlayer = null;
        double nearestPlayerDistance = CHASE_RANGE * CHASE_RANGE;
        LivingEntity nearestOther = null;
        double nearestOtherDistance = CHASE_RANGE * CHASE_RANGE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, search)) {
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (IParasite.isParasiteByTagOrInterface(entity)) {
                continue;
            }
            double distance = entity.position().distanceToSqr(center);
            if (entity instanceof Player) {
                if (distance <= nearestPlayerDistance) {
                    nearestPlayerDistance = distance;
                    nearestPlayer = entity;
                }
            } else if (distance <= nearestOtherDistance) {
                nearestOtherDistance = distance;
                nearestOther = entity;
            }
        }
        return nearestPlayer != null ? nearestPlayer : nearestOther;
    }

    /**  */
    private static void chase(SubLevel subLevel, LivingEntity target) {
        Vec3 center = subLevel.worldAabb().getCenter();
        double dx = target.getX() - center.x;
        double dz = target.getZ() - center.z;
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }
        Vec3 desired = new Vec3(dx, 0.0D, dz).normalize().scale(CHASE_SPEED);
        Vec3 current = subLevel.velocity();
        subLevel.setVelocity(new Vec3(
                current.x + (desired.x - current.x) * CHASE_ACCEL,
                current.y,
                current.z + (desired.z - current.z) * CHASE_ACCEL));
    }

    /**  {@code canAttackEntity}  */
    private static boolean canAttackEntity(SubLevel subLevel, LivingEntity target) {
        AABB self = subLevel.worldAabb();
        Vec3 center = self.getCenter();
        double dx = target.getX() - center.x;
        double dz = target.getZ() - center.z;
        if (dx * dx + dz * dz > CONTACT_RANGE * CONTACT_RANGE) {
            return false;
        }
        AABB other = target.getBoundingBox();
        return self.minY < other.maxY && self.maxY > other.minY;
    }

    private static void attack(ServerLevel level, LivingEntity target) {
        target.hurt(mobAttackSource(level), ATTACK_DAMAGE);
        // COTH I 30  +  I 15 addEffect
        target.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 600, 0, false, true));
        target.addEffect(new MobEffectInstance(ModEffects.FEAR.get(), 300, 0, false, true));
    }

    /**
     * {@code DamageTypes.MOB_ATTACK}
     *
     * <p> {@code this}  {@code DamageSource(Holder, Entity)}
     *  {@code DamageSource}
     * /</p>
     */
    private static DamageSource mobAttackSource(ServerLevel level) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MOB_ATTACK);
        return new DamageSource(type);
    }

    /**  5  */
    private void transformBackToBlock(ServerLevel level, SubLevel subLevel) {
        //  blockPosition()=
        //  BlockPos.containing()
        BlockPos center = BlockPos.containing(subLevel.worldAabb().getCenter());
        BlockPos pos = findTransformPos(level, center);
        if (pos == null) {
            //  tick
            return;
        }
        BlockState state = ModBlocks.INFESTED_CARVED_PUMPKIN.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facingOf())
                .setValue(InfestedCarvedPumpkin.NATURAL_SPAWN, true);
        level.setBlock(pos, state, 3);
        //  this.discard() +  +
        SubLevelRegistry.release(level, subLevel);
    }

    /**  333  */
    @Nullable
    private static BlockPos findTransformPos(ServerLevel level, BlockPos center) {
        if (isReplaceable(level, center)) {
            return center;
        }
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (isReplaceable(level, pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    // ------------------------------------------------------------------

    /**
     * <b></b> {@code true}
     * =
     *
     * <p> {@code InfestedPumpkinPhysics#shouldDropOnDestroy} +
     * {@code epca:infested_carved_pumpkin}
     * {@code match_tool=silk_touch}</p>
     *
     * <p>
     * </p>
     */
    @Override
    public boolean onBlockMined(ServerLevel level, SubLevel subLevel, SubLevel.BlockEntry entry,
                                @Nullable ServerPlayer player) {
        if (!isCarvedPumpkin(entry.state())) {
            return false;
        }
        if (player != null && PumpkinPhysicsHelper.hasSilkTouch(player)) {
            BlockPos worldPos = subLevel.blockWorldPos(entry);
            List<ItemStack> drops = Block.getDrops(entry.state(), level, worldPos, null, player,
                    player.getMainHandItem());
            for (ItemStack stack : drops) {
                Block.popResource(level, worldPos, stack);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------

    @Override
    public void save(CompoundTag tag) {
        tag.putInt(TAG_NO_TARGET_TIMER, this.noTargetTimer);
        //  NBT  yaw
        //  yaw  tick
        tag.putFloat(TAG_YAW, this.yaw);
        tag.putFloat(TAG_ROLL_X, this.roll.x);
        tag.putFloat(TAG_ROLL_Y, this.roll.y);
        tag.putFloat(TAG_ROLL_Z, this.roll.z);
        tag.putFloat(TAG_ROLL_W, this.roll.w);
    }

    @Override
    public void load(CompoundTag tag) {
        this.noTargetTimer = tag.getInt(TAG_NO_TARGET_TIMER);
        if (tag.contains(TAG_YAW)) {
            this.yaw = tag.getFloat(TAG_YAW);
            this.poseInitialised = true;
        }
        if (tag.contains(TAG_ROLL_W)) {
            this.roll.set(tag.getFloat(TAG_ROLL_X), tag.getFloat(TAG_ROLL_Y),
                    tag.getFloat(TAG_ROLL_Z), tag.getFloat(TAG_ROLL_W));
            this.rollAngle = 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(this.roll.w)));
        } else {
            this.roll.set(new Quaternionf());
        }
        //  tick  updateRoll
        this.lastPosition = null;
        // FEATURE 1 tick
        //  1.4
        endClimb();
        this.lastHorizontalStep = 0.0D;
        this.lastVerticalStep = 0.0D;
    }
}

