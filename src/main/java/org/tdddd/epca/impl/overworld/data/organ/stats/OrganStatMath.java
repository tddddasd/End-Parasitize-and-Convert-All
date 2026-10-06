package org.tdddd.epca.impl.overworld.data.organ.stats;

import java.util.Locale;
import java.util.Map;

/**
 *
 *
 * <p> {@link OrganStatSummary}
 * {@link OrganStatDefaults}JSON
 * STAGE 3 </p>
 *
 * <h2>rounds</h2>
 * <p>SPEC C9
 *  {@link #ADAPTATION_STEP} = 0.25 JSON  {@code "0.25"} / {@code "0.5"} /
 * {@code "0.75"} / {@code "1.0"}
 *  /
 * {@link #roundsForShares}4  1 </p>
 */
public final class OrganStatMath {

    /** 0.25 =  44/8/12/16  -&gt; 0.25/0.5/0.75/1.0 */
    public static final double ADAPTATION_STEP = 0.25D;

    private OrganStatMath() {
    }

    /**
     *
     *
     * @param organPartCount
     * @param totalSlots
     * @return {@code 0 / 0.25 / 0.5 / 0.75 / 1.0} &lt;= 0  &lt;= 0  0
     */
    public static double roundsForShares(int organPartCount, int totalSlots) {
        if (organPartCount <= 0 || totalSlots <= 0) return 0.0D;
        double ratio = (double) organPartCount / (double) totalSlots;
        //  ceilratio  0.25
        double steps = Math.ceil(ratio / ADAPTATION_STEP - 1.0E-9D);
        if (steps <= 0.0D) return 0.0D;
        return Math.min(1.0D, steps * ADAPTATION_STEP);
    }

    /**  -&gt; JSON {@code "0.25"} {@link Locale#ROOT} */
    public static String roundsKey(double strength) {
        if (strength <= 0.0D) return "0";
        return String.format(Locale.ROOT, "%.2f", snapRounds(strength));
    }

    /**  0.25  [0,1] */
    public static double snapRounds(double strength) {
        if (strength <= 0.0D) return 0.0D;
        double steps = Math.rint(strength / ADAPTATION_STEP);
        return Math.min(1.0D, Math.max(0.0D, steps * ADAPTATION_STEP));
    }

    /** 0  */
    public static int ceilToInt(double value) {
        if (value <= 0.0D) return 0;
        return (int) Math.ceil(value - 1.0E-9D);
    }

    /**
     *  -&gt;
     *
     * <p> &gt;=
     *  0</p>
     */
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

