package org.tdddd.epca.impl.overworld.data;

import net.minecraft.world.entity.player.Player;

public class BiomassManager {
    private static final String BIOMASS_KEY = "BiomassPoints";

    public static int getBiomassPoints(Player player) {
        return player.getPersistentData().getInt(BIOMASS_KEY);
    }

    public static void setBiomassPoints(Player player, int points) {
        player.getPersistentData().putInt(BIOMASS_KEY, points);
    }

    public static void addBiomassPoints(Player player, int amount) {
        setBiomassPoints(player, getBiomassPoints(player) + amount);
    }
}