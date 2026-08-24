package org.tdddd.epca.impl.overworld.registry.entities.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.entities.ILink;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Abstract base class for all Link-type EPCA entities (Beckons).
 * Extends {@link AbstractEpcaEntity} and implements {@link ILink}.
 *
 * <p>Provides common beckon logic extracted from StageIBeckon and StageIIBeckon:</p>
 * <ul>
 *   <li>Persistence (cannot despawn)</li>
 *   <li>Immovable / cannot be pushed</li>
 *   <li>Animation state machine constants and framework</li>
 *   <li>Rise effect system (rise from ground with particles)</li>
 *   <li>Suffocation handling (destroy blocks around head)</li>
 *   <li>Beckon core placement with nearby-entity checking</li>
 *   <li>Common beckon animation states: IDLE, OPEN, IDLE_OPEN, CLOSE, SPAWN</li>
 * </ul>
 */
public abstract class AbstractLinkEntity extends AbstractEpcaEntity implements ILink, Enemy {

    // ────────── Animation states (shared by all beckons) ──────────
    public static final int ANIM_STATE_IDLE = 0;
    public static final int ANIM_STATE_OPEN = 1;
    public static final int ANIM_STATE_IDLE_OPEN = 2;
    public static final int ANIM_STATE_CLOSE = 3;
    public static final int ANIM_STATE_SPAWN = 4;

    // ────────── Synched data ──────────
    protected static final EntityDataAccessor<Integer> TICK_COUNT =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> IS_RISING =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> RISE_TIMER =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> TARGET_Y =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> TARGET_X =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> TARGET_Z =
            SynchedEntityData.defineId(AbstractLinkEntity.class, EntityDataSerializers.FLOAT);

    // ────────── Persistent state ──────────
    protected BlockPos spawnTargetPos;
    protected int suffocationCooldown = 0;

    // ────────── Constructors ──────────

    protected AbstractLinkEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.noPhysics = false;
    }

    protected AbstractLinkEntity(EntityType<? extends PathfinderMob> entityType, Level level,
                                  Consumer<AbstractEpcaEntity> configurer) {
        super(entityType, level, configurer);
        this.setPersistenceRequired();
        this.noPhysics = false;
    }

    // ────────── Synched data ──────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK_COUNT, 0);
        this.entityData.define(ANIMATION_STATE, ANIM_STATE_IDLE);
        this.entityData.define(IS_RISING, false);
        this.entityData.define(RISE_TIMER, 0);
        this.entityData.define(TARGET_Y, 0.0f);
        this.entityData.define(TARGET_X, 0.0f);
        this.entityData.define(TARGET_Z, 0.0f);
    }

    // ────────── Persistence ──────────

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    // ────────── Pushable ──────────

    @Override
    public boolean isPushable() {
        return false;
    }

    // ────────── Rise effect ──────────

    /**
     * Set the target position for rise-from-ground and begin the rise effect.
     */
    public void setRiseTarget(net.minecraft.world.phys.Vec3 targetPosition) {
        this.entityData.set(IS_RISING, true);
        this.entityData.set(RISE_TIMER, 0);
        this.entityData.set(TARGET_Y, (float) targetPosition.y);
        this.entityData.set(TARGET_X, (float) targetPosition.x);
        this.entityData.set(TARGET_Z, (float) targetPosition.z);
        this.spawnTargetPos = BlockPos.containing(targetPosition);
        this.setPos(targetPosition.x, targetPosition.y - 3.0, targetPosition.z);
        this.entityData.set(ANIMATION_STATE, ANIM_STATE_SPAWN);
        startRiseEffect();
    }

    protected void startRiseEffect() {
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.entityData.set(RISE_TIMER, 0);
    }

    protected void finishRiseEffect() {
        this.setInvulnerable(false);
        this.setNoGravity(false);
        this.noPhysics = false;
        double targetX = this.entityData.get(TARGET_X);
        double targetY = this.entityData.get(TARGET_Y);
        double targetZ = this.entityData.get(TARGET_Z);
        this.setPos(targetX, targetY, targetZ);
        this.entityData.set(ANIMATION_STATE, ANIM_STATE_IDLE);
    }

    /**
     * Handle the rise animation and movement. Call from tick() when IS_RISING is true.
     * Subclasses can override to add custom rise particles.
     */
    protected void handleRiseAnimation() {
        int riseTimer = this.entityData.get(RISE_TIMER);
        riseTimer++;
        this.entityData.set(RISE_TIMER, riseTimer);

        if (riseTimer > 60) {
            this.entityData.set(IS_RISING, false);
            finishRiseEffect();
            return;
        }

        float progress = riseTimer / 60.0f;
        double targetY = this.entityData.get(TARGET_Y);
        double startY = targetY - 3.0;
        double currentY = startY + 3.0 * progress;
        this.setPos(this.getX(), currentY, this.getZ());
    }

    // ────────── Suffocation ──────────

    /**
     * Handle suffocation by destroying blocks around the entity's head.
     */
    protected void handleSuffocation() {
        if (suffocationCooldown > 0) {
            suffocationCooldown--;
            return;
        }
        BlockPos headPos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
        if (this.isInWall() || (!this.level().getFluidState(headPos).is(net.minecraft.world.level.material.Fluids.EMPTY)
                && !this.level().getBlockState(headPos).isAir())) {
            suffocationCooldown = 60;
            destroyBlocksAround(headPos);
        }
    }

    protected void destroyBlocksAround(BlockPos center) {
        int radius = 1;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState state = level().getBlockState(targetPos);
                    if (state.getDestroySpeed(level(), targetPos) <= 3.0f &&
                            state.getDestroySpeed(level(), targetPos) >= 0.0f &&
                            !state.isAir() && state.getFluidState().isEmpty()) {
                        level().destroyBlock(targetPos, true, this);
                    }
                }
            }
        }
    }

    // ────────── Core placement ──────────

    /**
     * Check if a beckon core exists nearby.
     */
    protected boolean isBeckonCoreNearby(Level level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getBlock() == ModBlocks.BECKON_CORE.get()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a beckon entity (StageI or StageII) exists nearby.
     */
    protected boolean isBeckonEntityNearby(Level level, BlockPos center, int radius) {
        AABB area = new AABB(center).inflate(radius);
        return !level.getEntitiesOfClass(AbstractLinkEntity.class, area, Entity::isAlive).isEmpty();
    }

    /**
     * Check if a block position is valid for core placement.
     */
    protected boolean isValidCorePlacementBlock(Level level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > 4) return false;
        if (!state.isCollisionShapeFullBlock(level, pos)) return false;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isCollisionShapeFullBlock(level, below)) return false;

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return !aboveState.isCollisionShapeFullBlock(level, above);
    }
}
