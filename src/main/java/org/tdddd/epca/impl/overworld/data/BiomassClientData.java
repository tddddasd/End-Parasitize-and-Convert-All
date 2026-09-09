package org.tdddd.epca.impl.overworld.data;

public class BiomassClientData {
    private static boolean isNestLeader = false;
    private static int points = 0;

    public static void update(boolean leader, int biomass) {
        isNestLeader = leader;
        points = biomass;
    }

    public static boolean isNestLeader() {
        return isNestLeader;
    }

    public static int getPoints() {
        return points;
    }
}