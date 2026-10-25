package org.tdddd.epca.impl.overworld.data;

public class BiomassClientData {
    private static boolean isHiveLeader = false;
    private static int points = 0;

    public static void update(boolean leader, int biomass) {
        isHiveLeader = leader;
        points = biomass;
    }

    public static boolean isHiveLeader() {
        return isHiveLeader;
    }

    public static int getPoints() {
        return points;
    }
}