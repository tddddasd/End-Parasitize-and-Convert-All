package org.tdddd.epca.impl.overworld.data.organ.stats;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class OrganStatReadout {

    public static final int DEFAULT_STATS_MAX_LINES = 12;

    public static final int DEFAULT_DAMAGE_MAX_LINES = 10;

    public static final int DAMAGE_LINE_COUNT = 2;

    public static final String ELLIPSIS = "…";

    private OrganStatReadout() {
    }

    public interface ReadoutLabels {

        Component attributeName(String attributeKey);

        Component sourcePurple();

        Component sourceTorsoInner();

        Component sourceHeadInner();

        Component adaptationChance();

        Component adaptationReduction();

        Component percentUnit();
    }

    private static final ReadoutLabels FALLBACK_LABELS = new ReadoutLabels() {
        @Override
        public Component attributeName(String attributeKey) {
            return Component.literal(attributeKey);
        }

        @Override
        public Component sourcePurple() {
            return Component.literal("purple");
        }

        @Override
        public Component sourceTorsoInner() {
            return Component.literal("torso_inner");
        }

        @Override
        public Component sourceHeadInner() {
            return Component.literal("head_inner");
        }

        @Override
        public Component adaptationChance() {
            return Component.literal("adaptation_chance");
        }

        @Override
        public Component adaptationReduction() {
            return Component.literal("adaptation_reduction");
        }

        @Override
        public Component percentUnit() {
            return Component.literal("%");
        }
    };

    public static List<String> statsLines(@Nullable LivingEntity entity, OrganStatSummary summary) {
        return statsLines(entity, summary, DEFAULT_STATS_MAX_LINES);
    }

    public static List<String> statsLines(@Nullable LivingEntity entity, OrganStatSummary summary,
                                         int maxLines) {
        return statsLines(entity, summary, maxLines, FALLBACK_LABELS);
    }

    public static List<String> statsLines(@Nullable LivingEntity entity, OrganStatSummary summary,
                                         int maxLines, ReadoutLabels labels) {
        OrganStatSummary safe = summary == null ? OrganStatSummary.empty() : summary;
        ReadoutLabels text = labels == null ? FALLBACK_LABELS : labels;
        List<String> all = new ArrayList<>();

        for (String key : OrganStatSummary.STAT_ATTRIBUTE_KEYS) {
            double current = OrganStatSummary.currentAttributeValue(entity, key);
            double bonus = safe.bonusFor(key);

            all.add(text.attributeName(key).getString()
                    + " " + OrganStatDefaults.format(current)
                    + "（" + OrganStatDefaults.formatSigned(bonus) + "）");
        }

        appendSourceLine(all, text.sourcePurple(), safe.purpleBonus(), text);
        appendSourceLine(all, text.sourceTorsoInner(), safe.torsoInnerBonus(), text);
        appendSourceLine(all, text.sourceHeadInner(), safe.headInnerBonus(), text);
        return trim(all, maxLines);
    }

    private static void appendSourceLine(List<String> out, Component label,
                                         OrganStatProfile profile, ReadoutLabels text) {
        if (profile == null) return;
        StringBuilder builder = new StringBuilder();
        for (String key : OrganStatSummary.STAT_ATTRIBUTE_KEYS) {
            double value = attributeDelta(profile, key);
            if (OrganStatDefaults.round2(value) == 0.0D) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(text.attributeName(key).getString())
                    .append(' ')
                    .append(OrganStatDefaults.formatSigned(value));
        }
        if (builder.length() == 0) return;
        out.add(label.getString() + " " + builder);
    }

    private static double attributeDelta(OrganStatProfile profile, String attributeKey) {
        return switch (attributeKey) {
            case "health" -> profile.health();
            case "armor" -> profile.armor();
            case "armor_toughness" -> profile.armorToughness();
            case "attack_damage" -> profile.attackDamage();
            case "knockback_resistance" -> profile.knockbackResistance();
            case "movement_speed" -> profile.movementSpeed();
            case "swim_speed" -> profile.swimSpeed();
            case "attack_range" -> profile.attackRange();
            case "block_reach" -> profile.blockReach();

            default -> 0.0D;
        };
    }

    public static List<String> damageLines(OrganStatSummary summary,
                                           @Nullable Registry<DamageType> registry) {
        return damageLines(summary, registry, DEFAULT_DAMAGE_MAX_LINES);
    }

    public static List<String> damageLines(OrganStatSummary summary,
                                           @Nullable Registry<DamageType> registry,
                                           int maxLines) {
        return damageLines(summary, registry, maxLines, FALLBACK_LABELS);
    }

    public static List<String> damageLines(OrganStatSummary summary,
                                           @Nullable Registry<DamageType> registry,
                                           int maxLines, ReadoutLabels labels) {
        OrganStatSummary safe = summary == null ? OrganStatSummary.empty() : summary;
        ReadoutLabels text = labels == null ? FALLBACK_LABELS : labels;
        String percent = text.percentUnit().getString();
        List<String> all = new ArrayList<>(DAMAGE_LINE_COUNT);

        all.add(text.adaptationChance().getString() + " "
                + OrganStatDefaults.formatPercentNumber(safe.adaptationChance()) + percent);
        all.add(text.adaptationReduction().getString() + " "
                + OrganStatDefaults.formatPercentNumber(safe.adaptationReduction()) + percent);

        return trim(all, Math.min(maxLines, DAMAGE_LINE_COUNT));
    }

    private static List<String> trim(List<String> lines, int maxLines) {
        if (maxLines <= 0) return List.of();
        List<String> flattened = new ArrayList<>();
        for (String line : lines) {
            for (String part : line.split("\n", -1)) {
                flattened.add(part);
            }
        }
        if (flattened.size() <= maxLines) return flattened;
        List<String> trimmed = new ArrayList<>(flattened.subList(0, Math.max(0, maxLines - 1)));
        trimmed.add(ELLIPSIS);
        return trimmed;
    }

    public static List<String> statsLines(@Nullable LivingEntity entity, @Nullable HiveLeaderOrganData data) {
        return statsLines(entity, OrganStatSummary.compute(data));
    }

    public static List<String> damageLines(@Nullable HiveLeaderOrganData data,
                                           @Nullable Registry<DamageType> registry) {
        return damageLines(OrganStatSummary.compute(data), registry);
    }
}
