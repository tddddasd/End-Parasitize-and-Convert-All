package org.tdddd.epca.impl.overworld.registry.entities.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModBlocks;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.overworld.registry.entities.IInfested;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import org.tdddd.yawning_neko_api.data.DamageAdaptationConfig;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Abstract base class for all Infested-type EPCA entities.
 * Extends {@link AbstractEpcaEntity} and implements {@link IInfested}.
 *
 * <p>Provides common infested logic extracted from InfestedZombie, InfestedSkeleton,
 * InfestedSlimeSize*, Walking*Head, and other infested entities:</p>
 * <ul>
 *   <li>Fake death burst: explosion sound + COTH/SPLASHI particles + AreaEffectCloud + delayed remains blocks</li>
 *   <li>Movement speed management with wander speed reduction modifier</li>
 *   <li>Step sounds pattern</li>
 *   <li>Common hurt() / die() / onKillEntity() overrides</li>
 *   <li>Remains block spawning utilities</li>
 *   <li>isMoving() utility</li>
 * </ul>
 */
public abstract class AbstractInfestedEntity extends AbstractEpcaEntity implements IInfested, Enemy {

    // ────────── Wander speed modifier (shared by all infested) ──────────
    protected static final UUID WANDER_SPEED_ID = UUID.fromString("A3766B59-7066-4402-AD81-0E3B7B6C2B9B");
    protected static final AttributeModifier WANDER_SPEED_REDUCTION =
            new AttributeModifier(WANDER_SPEED_ID, "Wander speed reduction", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);

    // ────────── Movement speeds ──────────
    protected double baseSpeed = 0.27D;
    protected double chaseSpeed = 0.38D;

    // ────────── Fake death burst config ──────────
    protected float fakeDeathBurstChance = 0.4f;

    // ────────── Jump cooldown ──────────
    protected int jumpCooldown = 0;

    // ────────── Constructors ──────────

    protected AbstractInfestedEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.fakeDeathEnabled = true;
        this.fakeDeathChance = 40;       // 40% chance
        this.setMaxUpStep(0.5F);
    }

    protected AbstractInfestedEntity(EntityType<? extends PathfinderMob> entityType, Level level,
                                      Consumer<AbstractEpcaEntity> configurer) {
        super(entityType, level, configurer);
        this.fakeDeathEnabled = true;
        this.fakeDeathChance = 40;
        this.setMaxUpStep(0.5F);
    }

    // ────────── Fake death burst ──────────

    /**
     * Called when fake death timer expires. Performs the infested burst
     * (explosion sound, particles, AreaEffectCloud, delayed remains blocks).
     */
    @Override
    protected void onFakeDeathBurst() {
        performFakeDeathBurst(deathPosition);
    }

    /**
     * Perform the standard infested fake death burst at the given position.
     * Subclasses may override to customize burst behavior.
     */
    protected void performFakeDeathBurst(BlockPos burstPos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (burstPos == null) burstPos = this.blockPosition();

        // Explosion sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.SMALL_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        // COTH particles
        serverLevel.sendParticles(ModParticles.COTH.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                10, 0.3, 0.45, 0.3, 0.0);

        // SPLASHI particles
        serverLevel.sendParticles(ModParticles.SPLASHI.get(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                7, 0.4, 0.3, 0.4, 0.2);

        // Delayed remains blocks
        BlockPos deathPos = burstPos;
        long seed = this.random.nextLong();
        int delay = this.random.nextInt(30) + 40;
        serverLevel.getServer().tell(new TickTask(
                serverLevel.getServer().getTickCount() + delay,
                () -> spawnRemainsBlocksAt(serverLevel, deathPos, RandomSource.create(seed))
        ));

        // AreaEffectCloud with COTH + Poison
        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel,
                deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
        cloud.setRadius(1.5F);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(ModEffects.COTH.get(), 1200, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true));
        serverLevel.addFreshEntity(cloud);
    }

    // ────────── Remains block spawning ──────────

    /**
     * Spawn remains blocks (large, medium, small) around a death position.
     */
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

    /**
     * Place a batch of remains blocks of one type randomly around deathPos.
     */
    protected static void placeRemainsBlock(Level level, BlockPos deathPos, BlockPos.MutableBlockPos pos,
                                             RandomSource rand, BlockState state, int count) {
        for (int i = 0; i < count; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 5;
            double offsetZ = (rand.nextDouble() - 0.5) * 5;
            int offsetY = -rand.nextInt(3);

            pos.set(
                    deathPos.getX() + offsetX,
                    deathPos.getY() + 1 + offsetY,
                    deathPos.getZ() + offsetZ
            );

            if (level.isEmptyBlock(pos)) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }

    // ────────── Movement speed management ──────────

    /**
     * Apply standard infested movement speed: faster when chasing, slower when wandering.
     */
    protected void applyInfestedMovementSpeed() {
        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute == null) return;

        movementAttribute.removeModifier(WANDER_SPEED_ID);

        if (this.getTarget() != null) {
            movementAttribute.setBaseValue(chaseSpeed);
        } else {
            movementAttribute.setBaseValue(baseSpeed);
            movementAttribute.addTransientModifier(WANDER_SPEED_REDUCTION);
        }
    }

    // ────────── Step sounds (infested defaults: 20-30 tick delay, volume 1.0) ──────────

    protected void tickStepSounds(SoundEvent stepSound) {
        tickStepSounds(stepSound, 20, 30, 1.0F);
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
        // Already faking death — die normally
        if (isFakingDeath()) {
            super.die(source);
            return;
        }

        // Damage adaptation check
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

        // On fire — skip fake death
        if (this.isOnFire()) {
            super.die(source);
            this.onDeath(source);
            return;
        }

        // Try fake death, else normal death
        if (!this.level().isClientSide && this.getHealth() <= 0.0F && this.random.nextFloat() < fakeDeathBurstChance) {
            onTriggerFakeDeath(source);
            this.onDeath(source);
        } else {
            onNormalDeathActions(source);
            super.die(source);
            this.onDeath(source);
        }
    }

    /**
     * Called when the entity dies normally (not fake death).
     * Override to add subclass-specific spawns (e.g., WalkingHead entities).
     * Called server-side only, immediately before super.die(source).
     */
    protected void onNormalDeathActions(DamageSource source) {
        // default: no extra actions
    }

    /**
     * Trigger fake death from a damage source. Override to add variant-specific
     * post-death spawns (e.g., WalkingHead entities) before the fake death begins.
     */
    protected void onTriggerFakeDeath(DamageSource source) {
        startFakeDeath();
    }

    // ────────── onKillEntity() ──────────

    @Override
    public void onKillEntity(LivingEntity killedEntity) {
        if (!this.level().isClientSide) {
            if (this.level() instanceof ServerLevel serverLevel) {
                EvolutionManager.forDimension(serverLevel).addPoints(1);
            }
            this.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    60,
                    0,
                    false, false, true
            ));
        }
    }
}
