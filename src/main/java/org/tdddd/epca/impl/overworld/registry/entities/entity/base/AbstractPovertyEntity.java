package org.tdddd.epca.impl.overworld.registry.entities.entity.base;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.entities.IPoverty;

import java.util.function.Consumer;

/**
 * Abstract base class for all Poverty-type EPCA entities.
 * Extends {@link AbstractEpcaEntity} and implements {@link IPoverty}.
 *
 * <p>Provides common poverty entity logic:</p>
 * <ul>
 *   <li>Tick counter system for timed-life entities</li>
 *   <li>Lifecycle stage helpers</li>
 * </ul>
 */
public abstract class AbstractPovertyEntity extends AbstractEpcaEntity implements IPoverty, Enemy {

    // ────────── Tick counter ──────────
    protected static final EntityDataAccessor<Integer> TICK_COUNT =
            SynchedEntityData.defineId(AbstractPovertyEntity.class, EntityDataSerializers.INT);

    // ────────── Constructors ──────────

    protected AbstractPovertyEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    protected AbstractPovertyEntity(EntityType<? extends PathfinderMob> entityType, Level level,
                                     Consumer<AbstractEpcaEntity> configurer) {
        super(entityType, level, configurer);
        this.xpReward = 0;
    }

    // ────────── Synched data ──────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK_COUNT, 0);
    }

    // ────────── Tick counter ──────────

    /**
     * Get the current tick count (lifetime counter).
     */
    public int getTickCount() {
        return this.entityData.get(TICK_COUNT);
    }

    /**
     * Increment the tick count. Should be called from tick().
     */
    protected int incrementTickCount() {
        int current = this.entityData.get(TICK_COUNT);
        this.entityData.set(TICK_COUNT, current + 1);
        return current;
    }

    /**
     * Check if the tick count has reached a specific value (useful for lifecycle stages).
     */
    protected boolean hasTickCountReached(int tick) {
        return getTickCount() >= tick;
    }
}
