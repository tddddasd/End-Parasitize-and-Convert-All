package org.tdddd.epca.impl.overworld.registry.entities.entity.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.overworld.registry.entities.IOnesent;

import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * Abstract base class for all Onesent-type EPCA entities.
 * Extends {@link AbstractEpcaEntity} and implements {@link IOnesent}.
 *
 * <p>Provides common onesent logic extracted from Ripper, Curbug, Fins, etc.:</p>
 * <ul>
 *   <li>Evolution stage helper for dimension-aware behavior</li>
 *   <li>Random idle sound goal (RandomSoundGoal)</li>
 *   <li>Step sound ticking</li>
 * </ul>
 */
public abstract class AbstractOnesentEntity extends AbstractEpcaEntity implements IOnesent, Enemy {

    // ────────── Constructors ──────────

    protected AbstractOnesentEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setMaxUpStep(0.5F);
    }

    protected AbstractOnesentEntity(EntityType<? extends PathfinderMob> entityType, Level level,
                                     Consumer<AbstractEpcaEntity> configurer) {
        super(entityType, level, configurer);
        this.setMaxUpStep(0.5F);
    }

    // ────────── Evolution stage ──────────

    /**
     * Get the current evolution stage for the dimension this entity is in.
     */
    protected int getEvolutionStage() {
        if (this.level() instanceof ServerLevel serverLevel) {
            return EvolutionManager.getStageForDimension(serverLevel);
        }
        return 0;
    }

    // ────────── Step sounds ──────────

    /**
     * Tick step sounds — call from tick(). Plays the step sound when on ground and moving.
     * Returns true if a step sound was played this tick.
     */
    protected boolean tickStepSounds(net.minecraft.sounds.SoundEvent stepSound) {
        if (!this.level().isClientSide && this.onGround() && isMoving()) {
            if (this.stepSoundDelay <= 0) {
                this.playSound(stepSound, 0.8F, 1.0F);
                this.stepSoundDelay = 10 + this.random.nextInt(6);
                return true;
            } else {
                this.stepSoundDelay--;
            }
        } else {
            if (this.stepSoundDelay > 0) this.stepSoundDelay--;
        }
        return false;
    }

    // ────────── Random Idle Sound Goal ──────────

    /**
     * A goal that periodically plays a random idle sound.
     * Used by Ripper, Curbug, and other onesent entities.
     */
    protected static class RandomSoundGoal extends Goal {
        private final AbstractOnesentEntity entity;
        private final net.minecraft.sounds.SoundEvent sound;
        private int nextSoundTick;

        public RandomSoundGoal(AbstractOnesentEntity entity, net.minecraft.sounds.SoundEvent sound) {
            this.entity = entity;
            this.sound = sound;
            this.setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return entity.isAlive() && !entity.isAggressive();
        }

        @Override
        public void start() {
            this.nextSoundTick = entity.getRandom().nextInt(120) + 80;
        }

        @Override
        public void tick() {
            if (this.nextSoundTick-- <= 0) {
                entity.playSound(sound, 1.0F, 1.0F);
                this.nextSoundTick = entity.getRandom().nextInt(120) + 80;
            }
        }
    }
}
