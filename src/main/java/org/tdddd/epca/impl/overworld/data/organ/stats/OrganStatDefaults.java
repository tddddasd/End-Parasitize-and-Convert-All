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

public record OrganStatDefaults(
        int minimumDamageIntervalTicks,
        double adaptationChance,
        List<DamageTypeDefaults> damageTypes,
        List<DamageTakenEntry> damageTaken) {

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

        public int roundsFor(double strength) {
            return OrganStatMath.roundsFromTable(adaptationRequiredHits, strength);
        }

        public String displayKey() {
            if (typeTag != null && !typeTag.isBlank()) {
                return "#" + stripHash(typeTag);
            }
            return typeId == null ? "?" : typeId.toString();
        }

        public boolean isTag() {
            return typeTag != null && !typeTag.isBlank();
        }

        public @Nullable TagKey<DamageType> tagKey() {
            if (!isTag()) return null;
            ResourceLocation location = ResourceLocation.tryParse(stripHash(typeTag));
            return location == null ? null : TagKey.create(Registries.DAMAGE_TYPE, location);
        }
    }

    public record DamageTakenEntry(@Nullable String tag, @Nullable ResourceLocation typeId, double multiplier) {

        public boolean isFallback() {
            return (tag == null || tag.isBlank()) && typeId == null;
        }

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

    public static final int DEFAULT_INTERVAL_TICKS = 40;

    public static final double DEFAULT_ADAPTATION_CHANCE = 1.0D;

    public static final ResourceLocation KILL_DAMAGE = new ResourceLocation("generic_kill");

    public static final ResourceLocation VOID_DAMAGE = new ResourceLocation("out_of_world");

    public static final String FIRE_DAMAGE_TAG = "#" + DamageTypeTags.IS_FIRE.location();

    public static final double DEFAULT_FIRE_MULTIPLIER = 2.5D;

    public static final double DEFAULT_OTHER_MULTIPLIER = 1.0D;

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

    public static final int READOUT_DECIMAL_PLACES = 2;

    public static double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    public static String format(double value) {
        double rounded = round2(value);
        if (Double.isNaN(rounded) || Double.isInfinite(rounded)) return "0";

        BigDecimal decimal = BigDecimal.valueOf(rounded).stripTrailingZeros();

        if (decimal.scale() <= 0) return decimal.toPlainString();
        if (decimal.scale() > READOUT_DECIMAL_PLACES) {
            decimal = decimal.setScale(READOUT_DECIMAL_PLACES, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return decimal.toPlainString();
    }

    public static String formatSigned(double value) {
        double rounded = round2(value);
        if (rounded == 0.0D) return "0";
        return (rounded > 0 ? "+" : "") + format(rounded);
    }

    public static String formatPercent(double ratio) {
        return formatPercentNumber(ratio) + "%";
    }

    public static String formatPercentNumber(double ratio) {
        return format(ratio * 100.0D);
    }
}
