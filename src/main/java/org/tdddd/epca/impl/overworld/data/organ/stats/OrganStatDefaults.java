package org.tdddd.epca.impl.overworld.data.organ.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 器官读数的全局默认值（{@code organ_stats/_defaults.json}）。
 *
 * <p>2026-11 起器官<strong>不再提供任何伤害适应性</strong>：{@code adaptation_chance}、
 * {@code damage_types}、{@code adaptation_rounds} 等字段已从解析里移除（旧数据文件里的同名字段
 * 会被忽略）。这里只剩「攻击最小伤害间隔」与「受伤倍率表」。</p>
 */
public record OrganStatDefaults(
        int minimumDamageIntervalTicks,
        List<DamageTakenEntry> damageTaken) {

    /** 受伤倍率表的一行：命中某个伤害标签 / 精确 DamageType 时的倍率；两者都为空表示「其余伤害」兜底。 */
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
        damageTaken = damageTaken == null ? List.of() : List.copyOf(damageTaken);
    }

    public static final int DEFAULT_INTERVAL_TICKS = 40;

    public static final String FIRE_DAMAGE_TAG = "#" + DamageTypeTags.IS_FIRE.location();

    public static final double DEFAULT_FIRE_MULTIPLIER = 2.5D;

    public static final double DEFAULT_OTHER_MULTIPLIER = 1.0D;

    public static final OrganStatDefaults FALLBACK = new OrganStatDefaults(
            DEFAULT_INTERVAL_TICKS,
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
