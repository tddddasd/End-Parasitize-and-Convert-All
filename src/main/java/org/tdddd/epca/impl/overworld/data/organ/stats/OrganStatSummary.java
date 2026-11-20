package org.tdddd.epca.impl.overworld.data.organ.stats;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 领巢者当前器官构成算出来的读数快照。
 *
 * <p>2026-11 起<strong>完全不含适应数据</strong>：器官不再提供任何伤害适应性。唯一保留的伤害相关
 * 读数是 {@link #damageTakenMultipliers()}「受伤倍率」，由 {@code HiveLeaderOrganDamageTaken} 结算。</p>
 */
public record OrganStatSummary(
        OrganStatProfile purpleBonus,
        OrganStatProfile torsoInnerBonus,
        OrganStatProfile headInnerBonus,
        OrganStatProfile limbInnerBonus,
        double minimumDamage,
        int minimumDamageIntervalTicks,
        List<DamageTakenDisplay> damageTakenMultipliers,
        List<ItemStack> purpleItems,
        Map<Identifier, Integer> slotItemCounts,
        Map<OrganSlotGroup, Map<Identifier, Integer>> groupItemCounts) {

    private static final Map<String, Holder<Attribute>> ATTRIBUTE_KEYS = buildAttributeKeys();

    public record DamageTakenDisplay(String key, double multiplier, boolean fallback) {
    }

    public OrganStatSummary {
        damageTakenMultipliers = damageTakenMultipliers == null
                ? List.of() : List.copyOf(damageTakenMultipliers);
        purpleItems = purpleItems == null ? List.of() : List.copyOf(purpleItems);
        slotItemCounts = slotItemCounts == null ? Map.of() : Map.copyOf(slotItemCounts);

        if (groupItemCounts == null || groupItemCounts.isEmpty()) {
            groupItemCounts = Map.of();
        } else {
            Map<OrganSlotGroup, Map<Identifier, Integer>> frozen = new LinkedHashMap<>();
            for (Map.Entry<OrganSlotGroup, Map<Identifier, Integer>> entry : groupItemCounts.entrySet()) {
                if (entry.getKey() == null) continue;
                frozen.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
            groupItemCounts = Map.copyOf(frozen);
        }
    }

    public static OrganStatSummary empty() {
        OrganStatDefaults defaults = OrganStatManager.defaults();
        List<DamageTakenDisplay> taken = effectiveDamageTaken(defaults, Map.of());
        return new OrganStatSummary(
                OrganStatProfile.ZERO,
                OrganStatProfile.ZERO,
                OrganStatProfile.ZERO,
                OrganStatProfile.ZERO,
                0.0D,
                defaults.minimumDamageIntervalTicks(),
                taken,
                List.of(),
                Map.of(),
                Map.of());
    }

    public static OrganStatSummary compute(@Nullable HiveLeaderOrganData data) {
        if (data == null) return empty();

        OrganStatDefaults defaults = OrganStatManager.defaults();
        OrganStatProfile.Accumulator purple = new OrganStatProfile.Accumulator();
        OrganStatProfile.Accumulator torsoInner = new OrganStatProfile.Accumulator();
        OrganStatProfile.Accumulator headInner = new OrganStatProfile.Accumulator();
        OrganStatProfile.Accumulator limbInner = new OrganStatProfile.Accumulator();

        List<ItemStack> purpleItems = new ArrayList<>();
        Map<Identifier, Integer> counts = new LinkedHashMap<>();

        Map<OrganSlotGroup, Map<Identifier, Integer>> groupCounts = new LinkedHashMap<>();

        Map<String, Double> damageTakenAdd = new LinkedHashMap<>();
        int minimumIntervalTicks = defaults.minimumDamageIntervalTicks();
        boolean intervalExplicit = false;

        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            for (int local = 0; local < group.size(); local++) {
                int index = group.globalIndex(local);
                ItemStack stack = data.getItem(index);
                if (stack == null || stack.isEmpty()) continue;

                Identifier itemId = itemIdOf(stack);

                boolean participates = group.kind().isInnerGrid() || data.isUnlocked(index);
                if (itemId != null && participates) {
                    counts.merge(itemId, 1, Integer::sum);
                    groupCounts.computeIfAbsent(group, ignored -> new LinkedHashMap<>())
                            .merge(itemId, 1, Integer::sum);
                }

                OrganStatManager.OrganStatEntry entry = OrganStatManager.entryOf(itemId);
                if (group.kind().isInnerGrid()) {

                    if (group == OrganSlotGroup.TORSO_INNER) {
                        torsoInner.add(entry.profileAt(group));
                    } else if (group.isLimbInner()) {
                        // 四肢内部槽（左/右臂、左/右腿）单独统计，便于读数区分。
                        limbInner.add(entry.profileAt(group));
                    } else {
                        headInner.add(entry.profileAt(group));
                    }
                    accumulateDamageTakenAdd(damageTakenAdd, entry);
                    continue;
                }

                if (!data.isUnlocked(index)) continue;

                purple.add(entry.profileAt(group));
                purpleItems.add(stack);
                accumulateDamageTakenAdd(damageTakenAdd, entry);
                if (itemId != null) {
                    Integer override = OrganStatManager.intervalOverrideTicks(stack);
                    if (override != null && override > 0) {
                        intervalExplicit = true;
                        if (override < minimumIntervalTicks) {
                            minimumIntervalTicks = override;
                        }
                    }
                }
            }
        }

        if (!intervalExplicit) {
            minimumIntervalTicks = defaults.minimumDamageIntervalTicks();
        }

        OrganStatProfile purpleBonus = purple.toImmutable();

        List<DamageTakenDisplay> taken = effectiveDamageTaken(defaults, damageTakenAdd);

        return new OrganStatSummary(
                purpleBonus,
                torsoInner.toImmutable(),
                headInner.toImmutable(),
                limbInner.toImmutable(),
                purpleBonus.minimumDamage(),
                minimumIntervalTicks,
                taken,
                purpleItems,
                counts,
                groupCounts);
    }

    private static void accumulateDamageTakenAdd(Map<String, Double> accumulator,
                                                 OrganStatManager.OrganStatEntry entry) {
        if (entry == null || entry.damageTakenAdd().isEmpty()) return;
        entry.damageTakenAdd().forEach((key, value) -> {
            if (key == null || value == null) return;
            accumulator.merge(key, value, Double::sum);
        });
    }

    private static final class MutableMultiplier {
        private final String key;
        private final boolean fallback;
        private double multiplier;

        private MutableMultiplier(String key, boolean fallback, double multiplier) {
            this.key = key;
            this.fallback = fallback;
            this.multiplier = multiplier;
        }
    }

    static List<DamageTakenDisplay> effectiveDamageTaken(OrganStatDefaults defaults,
                                                         Map<String, Double> additions) {
        List<MutableMultiplier> rows = new ArrayList<>();
        for (OrganStatDefaults.DamageTakenEntry entry : defaults.damageTaken()) {
            rows.add(new MutableMultiplier(entry.display(), entry.isFallback(), entry.multiplier()));
        }

        if (additions != null && !additions.isEmpty()) {
            double fallbackDelta = 0.0D;
            List<Map.Entry<String, Double>> newKeys = new ArrayList<>();
            for (Map.Entry<String, Double> addition : additions.entrySet()) {
                if (OrganStatManager.isFallbackKey(addition.getKey())) {
                    fallbackDelta += addition.getValue();
                } else {
                    newKeys.add(addition);
                }
            }

            int insertAt = indexOfFallback(rows);
            if (insertAt < 0) insertAt = rows.size();

            double fallbackBase = fallbackMultiplier(rows);
            for (Map.Entry<String, Double> addition : newKeys) {
                String key = normalizeDamageKey(addition.getKey());
                if (key == null) continue;
                MutableMultiplier existing = rowWithKey(rows, key);
                if (existing != null) {
                    existing.multiplier += addition.getValue();
                    continue;
                }
                rows.add(insertAt, new MutableMultiplier(key, false, fallbackBase + addition.getValue()));
                insertAt++;
            }

            if (fallbackDelta != 0.0D) {
                MutableMultiplier fallback = fallbackRow(rows);
                if (fallback == null) {
                    rows.add(new MutableMultiplier("其余伤害", true,
                            OrganStatDefaults.DEFAULT_OTHER_MULTIPLIER + fallbackDelta));
                } else {
                    fallback.multiplier += fallbackDelta;
                }
            }
        }

        List<DamageTakenDisplay> result = new ArrayList<>(rows.size());
        for (MutableMultiplier row : rows) {
            result.add(new DamageTakenDisplay(row.key, row.multiplier, row.fallback));
        }
        return result;
    }

    @Nullable
    private static String normalizeDamageKey(@Nullable String raw) {
        if (raw == null) return null;
        String key = raw.trim();
        if (key.isEmpty()) return null;
        return key.startsWith("#") ? "#" + stripHash(key) : key;
    }

    private static int indexOfFallback(List<MutableMultiplier> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).fallback) return i;
        }
        return -1;
    }

    @Nullable
    private static MutableMultiplier fallbackRow(List<MutableMultiplier> rows) {
        int index = indexOfFallback(rows);
        return index < 0 ? null : rows.get(index);
    }

    private static double fallbackMultiplier(List<MutableMultiplier> rows) {
        MutableMultiplier fallback = fallbackRow(rows);
        return fallback == null ? OrganStatDefaults.DEFAULT_OTHER_MULTIPLIER : fallback.multiplier;
    }

    @Nullable
    private static MutableMultiplier rowWithKey(List<MutableMultiplier> rows, String key) {
        for (MutableMultiplier row : rows) {
            if (!row.fallback && row.key.equals(key)) return row;
        }
        return null;
    }

    public double damageTakenFallbackMultiplier() {
        for (DamageTakenDisplay entry : damageTakenMultipliers) {
            if (entry.fallback()) return entry.multiplier();
        }
        return OrganStatDefaults.DEFAULT_OTHER_MULTIPLIER;
    }

    public static double currentAttributeValue(LivingEntity entity, String attributeKey) {
        Holder<Attribute> attribute = ATTRIBUTE_KEYS.get(attributeKey);
        if (entity == null || attribute == null) return 0.0D;

        return entity.getAttributeValue(attribute);
    }

    public double bonusFor(String attributeKey) {
        OrganStatProfile total = totalBonus();
        return switch (attributeKey) {
            case "health" -> total.health();
            case "armor" -> total.armor();
            case "armor_toughness" -> total.armorToughness();
            case "attack_damage" -> total.attackDamage();
            case "knockback_resistance" -> total.knockbackResistance();
            case "movement_speed" -> total.movementSpeed();
            case "swim_speed" -> total.swimSpeed();
            case "attack_range" -> total.attackRange();
            case "block_reach" -> total.blockReach();
            case "entity_gravity" -> total.entityGravity();
            default -> 0.0D;
        };
    }

    public OrganStatProfile totalBonus() {
        OrganStatProfile.Accumulator accumulator = new OrganStatProfile.Accumulator();
        accumulator.add(purpleBonus);
        accumulator.add(torsoInnerBonus);
        accumulator.add(headInnerBonus);
        accumulator.add(limbInnerBonus);
        return accumulator.toImmutable();
    }

    public static final List<String> STAT_ATTRIBUTE_KEYS = List.of(
            "health", "armor", "armor_toughness", "attack_damage", "knockback_resistance",
            "movement_speed", "swim_speed", "attack_range", "block_reach");

    public static String attributeDisplayName(String attributeKey) {
        return switch (attributeKey) {
            case "health" -> "最大生命值";
            case "armor" -> "护甲值";
            case "armor_toughness" -> "盔甲韧性";
            case "attack_damage" -> "基础攻击伤害";
            case "knockback_resistance" -> "基础击退抗性";
            case "movement_speed" -> "移动速度";
            case "swim_speed" -> "游泳速度";
            case "attack_range" -> "基础攻击距离";
            case "block_reach" -> "基础方块触及距离";
            default -> attributeKey;
        };
    }

    private static String stripHash(String value) {
        return value.startsWith("#") ? value.substring(1) : value;
    }

    private static Map<String, Holder<Attribute>> buildAttributeKeys() {
        Map<String, Holder<Attribute>> keys = new LinkedHashMap<>();
        keys.put("health", Attributes.MAX_HEALTH);
        keys.put("armor", Attributes.ARMOR);

        keys.put("armor_toughness", Attributes.ARMOR_TOUGHNESS);
        keys.put("attack_damage", Attributes.ATTACK_DAMAGE);
        keys.put("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE);
        keys.put("movement_speed", Attributes.MOVEMENT_SPEED);

        keys.put("swim_speed", NeoForgeMod.SWIM_SPEED);

        keys.put("attack_range", Attributes.ENTITY_INTERACTION_RANGE);
        keys.put("block_reach", Attributes.BLOCK_INTERACTION_RANGE);

        keys.put("entity_gravity", Attributes.GRAVITY);
        return Map.copyOf(keys);
    }

    @Nullable
    private static Identifier itemIdOf(ItemStack stack) {
        Item item = stack.getItem();

        return item == null ? null : BuiltInRegistries.ITEM.getKey(item);
    }

    @Override
    public String toString() {
        return "OrganStatSummary[purple=" + purpleBonus.describeNonZero()
                + ", total=" + totalBonus().describeNonZero()
                + ", minDamage=" + minimumDamage
                + ", intervalTicks=" + minimumDamageIntervalTicks
                + ", purpleItems=" + purpleItems.size() + "]";
    }

    public int countOf(Identifier itemId) {
        return itemId == null ? 0 : slotItemCounts.getOrDefault(itemId, 0);
    }

    public int countIn(@Nullable OrganSlotGroup group, @Nullable Identifier itemId) {
        if (group == null || itemId == null) return 0;
        Map<Identifier, Integer> perGroup = groupItemCounts.get(group);
        return perGroup == null ? 0 : perGroup.getOrDefault(itemId, 0);
    }
}
