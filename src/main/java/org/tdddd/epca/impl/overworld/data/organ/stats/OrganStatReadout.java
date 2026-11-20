package org.tdddd.epca.impl.overworld.data.organ.stats;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 器官界面的文本读数（属性增益 + 各槽位来源）。
 *
 * <p>2026-11 起<strong>不再输出任何「器官适应性」行</strong>（原「适应概率 / 适应满减免」两行已删除）。
 * 伤害相关的读数改由种阶特化提供：重塑种的伤害适应性显示在
 * {@code HiveLeaderOrganScreen} 的同一读数区（数据来自 {@code SyncHiveLeaderSpeciesPacket}）。</p>
 */
public final class OrganStatReadout {

    public static final int DEFAULT_STATS_MAX_LINES = 12;

    public static final String ELLIPSIS = "…";

    private OrganStatReadout() {
    }

    public interface ReadoutLabels {

        Component attributeName(String attributeKey);

        Component sourcePurple();

        Component sourceTorsoInner();

        Component sourceHeadInner();

        /** 四肢内部（左/右臂、左/右腿）增幅；默认与头部内部同标签，保证旧实现不需要改。 */
        default Component sourceLimbInner() {
            return sourceHeadInner();
        }
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
        appendSourceLine(all, text.sourceLimbInner(), safe.limbInnerBonus(), text);
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
}
