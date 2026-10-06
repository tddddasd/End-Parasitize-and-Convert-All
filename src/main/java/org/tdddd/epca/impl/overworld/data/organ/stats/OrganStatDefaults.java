package org.tdddd.epca.impl.overworld.data.organ.stats;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * SPEC  0  C9
 *
 * <p> {@code data/epca/organ_stats/_defaults.json} {@link OrganStatManager}
 *  {@link #FALLBACK} SPEC
 * </p>
 *
 * <h2> id  tag</h2>
 * <p>SPEC  kill <b>
 * id  tag </b><b></b>{@code #minecraft:is_fire}
 *  {@link DamageTypeDefaults} {@code typeTag} </p>
 *
 * @param minimumDamageIntervalTicks tick 40 tick = 2
 * @param adaptationChance           01 1.0 = 100%
 * @param damageTypes
 * @param damageTaken
 *                                   {@code tag}  {@code type}
 */
public record OrganStatDefaults(
        int minimumDamageIntervalTicks,
        double adaptationChance,
        List<DamageTypeDefaults> damageTypes,
        List<DamageTakenEntry> damageTaken) {

    /**
     *
     *
     * <p>{@code adaptationRequiredHits} {@code _defaults.json}
     * {@code adaptation_rounds}
     *  {@code adaptation_rounds}
     *  {@code OrganStatManager}  {@link OrganStatSummary#adaptationRequiredHits()}</p>
     */
    public record DamageTypeDefaults(
            @Nullable ResourceLocation typeId,
            @Nullable String typeTag,
            String label,
            boolean adaptable,
            Map<String, Integer> adaptationRequiredHits) {

        public DamageTypeDefaults {
            adaptationRequiredHits = adaptationRequiredHits == null
                    ? Map.of() : Map.copyOf(adaptationRequiredHits);
        }

        /**
         * 01 {@link OrganStatMath#roundsForShares}
         *
         * <p><b></b> {@code OrganStatManager}
         * {@code adaptation_rounds} SPEC C9 </p>
         */
        public int roundsFor(double strength) {
            return OrganStatMath.roundsFromTable(adaptationRequiredHits, strength);
        }

        /**  tag # id */
        public String displayKey() {
            if (typeTag != null && !typeTag.isBlank()) {
                return "#" + stripHash(typeTag);
            }
            return typeId == null ? "?" : typeId.toString();
        }

        /**  4  id  */
        public boolean isTag() {
            return typeTag != null && !typeTag.isBlank();
        }

        public @Nullable TagKey<DamageType> tagKey() {
            if (!isTag()) return null;
            ResourceLocation location = ResourceLocation.tryParse(stripHash(typeTag));
            return location == null ? null : TagKey.create(Registries.DAMAGE_TYPE, location);
        }
    }

    /** {@code tag}  {@code typeId}  */
    public record DamageTakenEntry(@Nullable String tag, @Nullable ResourceLocation typeId, double multiplier) {

        public boolean isFallback() {
            return (tag == null || tag.isBlank()) && typeId == null;
        }

        /** {@code #tag} / {@code id} / {@code } */
        public String display() {
            if (tag != null && !tag.isBlank()) return "#" + stripHash(tag);
            if (typeId != null) return typeId.toString();
            return "其余伤害";
        }
    }

    public OrganStatDefaults {
        damageTypes = damageTypes == null ? List.of() : List.copyOf(damageTypes);
        damageTaken = damageTaken == null ? List.of() : List.copyOf(damageTaken);
    }

    //  = SPEC

    /** 40 tick = 2  */
    public static final int DEFAULT_INTERVAL_TICKS = 40;
    /** 100% */
    public static final double DEFAULT_ADAPTATION_CHANCE = 1.0D;

    /** {@code minecraft:generic_kill}kill  */
    public static final ResourceLocation KILL_DAMAGE = new ResourceLocation("generic_kill");
    /** {@code minecraft:out_of_world} 1.20.1  id */
    public static final ResourceLocation VOID_DAMAGE = new ResourceLocation("out_of_world");
    /** {@code #minecraft:is_fire} JSON  # */
    public static final String FIRE_DAMAGE_TAG = "#" + DamageTypeTags.IS_FIRE.location();
    /**  */
    public static final double DEFAULT_FIRE_MULTIPLIER = 2.5D;
    /**  */
    public static final double DEFAULT_OTHER_MULTIPLIER = 1.0D;

    /**
     *  = SPEC  +  2.5 /  1
     *
     * <p> JSON</p>
     */
    public static final OrganStatDefaults FALLBACK = new OrganStatDefaults(
            DEFAULT_INTERVAL_TICKS,
            DEFAULT_ADAPTATION_CHANCE,
            List.of(
                    new DamageTypeDefaults(KILL_DAMAGE, null, "kill 伤害", false, Map.of()),
                    new DamageTypeDefaults(VOID_DAMAGE, null, "虚空伤害", false, Map.of()),
                    new DamageTypeDefaults(null, FIRE_DAMAGE_TAG, "火焰标签伤害", true,
                            Map.of("0.25", 4, "0.5", 8, "0.75", 12, "1.0", 16))),
            List.of(
                    new DamageTakenEntry(FIRE_DAMAGE_TAG, null, DEFAULT_FIRE_MULTIPLIER),
                    new DamageTakenEntry(null, null, DEFAULT_OTHER_MULTIPLIER)));

    private static String stripHash(String value) {
        return value.startsWith("#") ? value.substring(1) : value;
    }

    /** SPEC/ 2  */
    public static final int READOUT_DECIMAL_PLACES = 2;

    /**  0.05  */
    public static double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    /**
     *  double <b> 2 </b>
     *
     * <p> {@link Double#toString(double)}
     * {@code 0.35}  {@code 0.35000000000000003}
     *  {@link BigDecimal#stripTrailingZeros()}</p>
     * <ul>
     *   <li>{@code 1.2000 -> "1.2"}</li>
     *   <li>{@code 0.3500 -> "0.35"}</li>
     *   <li>{@code 2.0000 -> "2"}</li>
     *   <li>{@code -0.0 -> "0"}</li>
     *   <li>{@code NaN / Inf}  {@code "0"}</li>
     * </ul>
     * <p><b></b> STAGE 3 </p>
     */
    public static String format(double value) {
        double rounded = round2(value);
        if (Double.isNaN(rounded) || Double.isInfinite(rounded)) return "0";
        //  BigDecimal.valueOf(double)  new BigDecimal(double)
        //  Double.toString  0.35  0.34999999999999997779...
        BigDecimal decimal = BigDecimal.valueOf(rounded).stripTrailingZeros();
        // scale <= 0 stripTrailingZeros  2.00  scale  2E+0
        // toPlainString  "2"
        if (decimal.scale() <= 0) return decimal.toPlainString();
        if (decimal.scale() > READOUT_DECIMAL_PLACES) {
            decimal = decimal.setScale(READOUT_DECIMAL_PLACES, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return decimal.toPlainString();
    }

    /** {@code +0.1} / {@code -2} */
    public static String formatSigned(double value) {
        double rounded = round2(value);
        if (rounded == 0.0D) return "0";
        return (rounded > 0 ? "+" : "") + format(rounded);
    }

    /** {@code 0.35 -> "35%"} 2  */
    public static String formatPercent(double ratio) {
        return formatPercentNumber(ratio) + "%";
    }

    /**
     * <b></b>{@code 0.35 -> "35"} 2 <b></b> {@code "%"}
     *
     * <p> lang {@link #format(double)}
     * {@code "%"}  {@code OrganStatReadout.ReadoutLabels#percentUnit()}</p>
     */
    public static String formatPercentNumber(double ratio) {
        return format(ratio * 100.0D);
    }
}

