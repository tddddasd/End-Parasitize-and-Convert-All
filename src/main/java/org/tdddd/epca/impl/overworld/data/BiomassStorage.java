package org.tdddd.epca.impl.overworld.data;

/**
 * Anything that carries biomass: creatures (through {@code EntityBiomassCountManager}) and blocks such
 * as the beckon core and the hive node. Routing every read/write through this interface keeps the
 * increase and decrease rules identical for players, mobs and blocks.
 */
public interface BiomassStorage {

    int getBiomassCount();

    /** Stores the count, clamped into {@code [0, getMaxBiomassCount()]}. */
    void setBiomassCount(int count);

    int getMaxBiomassCount();
}
