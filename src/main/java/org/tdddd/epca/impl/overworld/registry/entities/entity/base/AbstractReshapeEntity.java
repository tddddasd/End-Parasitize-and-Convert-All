package org.tdddd.epca.impl.overworld.registry.entities.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.IReshape;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Abstract base class for all Reshape-type EPCA entities.
 * Extends {@link AbstractEpcaEntity} and implements {@link IReshape}.
 *
 * <p>Provides common reshape logic extracted from ReshapeLongarms and ReshapeYelloweye:</p>
 * <ul>
 *   <li>Fake death with large burst (big explosion + remains blocks)</li>
 *   <li>Attack state management (isAttacking, attackType)</li>
 *   <li>Block breaking system (contact-based block destruction)</li>
 *   <li>Remains block spawning</li>
 *   <li>Fall damage immunity and water breathing</li>
 * </ul>
 */
public abstract class AbstractReshapeEntity extends AbstractEpcaEntity implements IReshape, Enemy {

    // ────────── Synched data ──────────
    protected static final EntityDataAccessor<Boolean> DATA_IS_ATTACKING =
            SynchedEntityData.defineId(AbstractReshapeEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(AbstractReshapeEntity.class, EntityDataSerializers.INT);

    // ────────── Fake death config ──────────
    protected float fakeDeathBurstChance = 0.4f;

    // ────────── Block breaking ──────────
    protected int blockBreakTimer = 0;
    protected final List<BlockPos> blocksToDestroy = new ArrayList<>();
    protected int destructionIndex = 0;

    // ────────── Constructors ──────────

    protected AbstractReshapeEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.fakeDeathEnabled = true;
        this.setMaxUpStep(1.6F);
    }

    protected AbstractReshapeEntity(EntityType<? extends PathfinderMob> entityType, Level level,
                                     Consumer<AbstractEpcaEntity> configurer) {
        super(entityType, level, configurer);
        this.fakeDeathEnabled = true;
        this.setMaxUpStep(1.6F);
    }

    // ────────── Synched data ──────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_ATTACKING, false);
        this.entityData.define(DATA_ATTACK_TYPE, 0);
    }

    // ────────── Attack state ──────────

    public boolean isAttacking() { return this.entityData.get(DATA_IS_ATTACKING); }
    public int getAttackType() { return this.entityData.get(DATA_ATTACK_TYPE); }
    protected void setAttacking(boolean attacking, int type) {
        this.entityData.set(DATA_IS_ATTACKING, attacking);
        this.entityData.set(DATA_ATTACK_TYPE, type);
    }

    // ────────── Fake death ──────────

    @Override
    protected void onFakeDeathBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // Big explosion sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.BIG_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        // Particles
        serverLevel.sendParticles(ModParticles.COTH.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                10, 0.3, 0.45, 0.3, 0.0);
        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                7, 0.4, 0.3, 0.4, 0.2);

        // Delayed remains blocks
        BlockPos deathPos = this.deathPosition;
        if (deathPos != null) {
            long seed = this.random.nextLong();
            int delay = this.random.nextInt(30) + 40;
            serverLevel.getServer().tell(new TickTask(
                    serverLevel.getServer().getTickCount() + delay,
                    () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))
            ));
        }

        // AreaEffectCloud
        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5);
        cloud.setRadius(1.5F);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 1, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
        serverLevel.addFreshEntity(cloud);
    }

    // ────────── Remains blocks ──────────

    protected static void spawnRemainsBlocksAt(ServerLevel level, BlockPos deathPos, RandomSource rand) {
        if (level.isClientSide || deathPos == null) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_LARGE.get().defaultBlockState(), 1);

        int mediumCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_MEDIUM.get().defaultBlockState(), mediumCount);

        int smallCount = rand.nextInt(3) + 2;
        placeRemainsBlock(level, deathPos, pos, rand,
                ModBlocks.INFESTED_REMAINS_SMALL.get().defaultBlockState(), smallCount);
    }

    protected static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos,
                                             RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5;
            double offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);
            pos.set(deathPos.getX() + offsetX, deathPos.getY() + 1 + offsetY, deathPos.getZ() + offsetZ);
            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }

    // ────────── Block breaking ──────────

    /**
     * Build a list of breakable blocks in contact with the entity's bounding box.
     * Call from tick()/customServerAiStep() to initiate block breaking.
     */
    protected void scanContactBlocks() {
        if (isAttacking() || this.getTarget() == null) return;
        blocksToDestroy.clear();
        destructionIndex = 0;
        Set<BlockPos> toDestroy = new HashSet<>();

        AABB aabb = getBoundingBox();
        int minX = (int) Math.floor(aabb.minX), maxX = (int) Math.ceil(aabb.maxX);
        int minY = (int) Math.floor(aabb.minY), maxY = (int) Math.ceil(aabb.maxY);
        int minZ = (int) Math.floor(aabb.minZ), maxZ = (int) Math.ceil(aabb.maxZ);

        // East face
        int xEast = (int) Math.ceil(aabb.maxX);
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(xEast, y, z);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        // West face
        int xWest = (int) Math.floor(aabb.minX) - 1;
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(xWest, y, z);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        // South face
        int zSouth = (int) Math.ceil(aabb.maxZ);
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                BlockPos pos = new BlockPos(x, y, zSouth);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        // North face
        int zNorth = (int) Math.floor(aabb.minZ) - 1;
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {
                BlockPos pos = new BlockPos(x, y, zNorth);
                if (isBreakableBlock(pos)) toDestroy.add(pos);
            }
        // Top face
        int topY = (int) Math.ceil(aabb.maxY);
        for (int y = topY; y <= topY + 2; y++)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isBreakableBlock(pos)) toDestroy.add(pos);
                }

        blocksToDestroy.addAll(toDestroy);
        if (!blocksToDestroy.isEmpty()) destroyNextBlock();
    }

    /**
     * Check if a block can be broken on contact by a reshape entity.
     */
    protected boolean isBreakableBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) return false;
        float hardness = state.getDestroySpeed(level(), pos);
        return hardness >= 0 && hardness <= 1.0f;
    }

    /**
     * Destroy the next block in the contact-breaking queue.
     */
    protected void destroyNextBlock() {
        if (destructionIndex >= blocksToDestroy.size()) {
            blocksToDestroy.clear();
            destructionIndex = 0;
            blockBreakTimer = 10;
            return;
        }
        BlockPos pos = blocksToDestroy.get(destructionIndex);
        BlockState state = level().getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced() && isBreakableBlock(pos)) {
            boolean destroyed = level().destroyBlock(pos, false, this);
            if (destroyed && state.getSoundType() != null) {
                level().playSound(null, pos, state.getSoundType().getBreakSound(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        destructionIndex++;
    }

    // ────────── Step sounds (reshape defaults: 20-30 tick delay, volume 1.0) ──────────

    protected void tickStepSounds(net.minecraft.sounds.SoundEvent stepSound) {
        tickStepSounds(stepSound, 20, 30, 1.0F);
    }

    // ────────── Overrides ──────────

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    // ────────── hurt() ──────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerable()) return false;
        return super.hurt(source, amount); // AbstractEpcaEntity handles the rest
    }

    // ────────── die() ──────────

    @Override
    public void die(DamageSource source) {
        if (isFakingDeath()) {
            super.die(source);
            return;
        }

        DamageAdaptationConfig config = DamageAdaptation.getEntityConfig(this);
        if (config != null) {
            int minKills = config.getMinimumKillCount();
            if (minKills > 0) {
                int currentDeaths = DamageAdaptation.getDeathCount(this);
                if (currentDeaths < minKills) {
                    DamageAdaptation.recordDeath(this);
                    this.setHealth(this.getMaxHealth());
                    return;
                }
            }
        }

        if (this.isOnFire()) {
            super.die(source);
            this.onDeath(source);
            return;
        }

        if (!this.level().isClientSide && this.getHealth() <= 0.0F
                && this.random.nextFloat() < fakeDeathBurstChance) {
            onTriggerFakeDeath(source);
            this.onDeath(source);
        } else {
            super.die(source);
            this.onDeath(source);
        }
    }

    protected void onTriggerFakeDeath(DamageSource source) {
        startFakeDeath();
    }
}
