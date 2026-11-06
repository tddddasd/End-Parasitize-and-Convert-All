package org.tdddd.epca.impl.overworld.data.organ.stats;

import java.util.Locale;
import java.util.Map;

public final class OrganStatMath {

    public static final double ADAPTATION_STEP = 0.25D;

    private OrganStatMath() {
    }

    public static double roundsForShares(int organPartCount, int totalSlots) {
        if (organPartCount <= 0 || totalSlots <= 0) return 0.0D;
        double ratio = (double) organPartCount / (double) totalSlots;

        double steps = Math.ceil(ratio / ADAPTATION_STEP - 1.0E-9D);
        if (steps <= 0.0D) return 0.0D;
        return Math.min(1.0D, steps * ADAPTATION_STEP);
    }

    public static String roundsKey(double strength) {
        if (strength <= 0.0D) return "0";
        return String.format(Locale.ROOT, "%.2f", snapRounds(strength));
    }

    public static double snapRounds(double strength) {
        if (strength <= 0.0D) return 0.0D;
        double steps = Math.rint(strength / ADAPTATION_STEP);
        return Math.min(1.0D, Math.max(0.0D, steps * ADAPTATION_STEP));
    }

    public static int ceilToInt(double value) {
        if (value <= 0.0D) return 0;
        return (int) Math.ceil(value - 1.0E-9D);
    }

    public static int roundsFromTable(Map<String, Integer> table, double strength) {
        if (table == null || table.isEmpty() || strength <= 0.0D) return 0;
        Integer exact = table.get(roundsKey(strength));
        if (exact != null) return exact;
        int best = 0;
        for (Map.Entry<String, Integer> entry : table.entrySet()) {
            double key = parseDouble(entry.getKey());
            if (key + 1.0E-9D >= strength) {
                if (best == 0 || entry.getValue() < best) {
                    best = entry.getValue();
                }
            }
        }
        return best;
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
