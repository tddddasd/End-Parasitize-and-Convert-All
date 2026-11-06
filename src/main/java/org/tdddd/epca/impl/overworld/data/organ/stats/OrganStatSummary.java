package org.tdddd.epca.impl.overworld.data.organ.stats;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganData;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public record OrganStatSummary(
        OrganStatProfile purpleBonus,
        OrganStatProfile torsoInnerBonus,
        OrganStatProfile headInnerBonus,
        double minimumDamage,
        int minimumDamageIntervalTicks,
        double adaptationReduction,
        double adaptationChance,
        Map<String, Integer> adaptationRequiredHits,
        Map<String, Integer> adaptationRequiredHitsTags,
        List<String> nonAdaptableDamageTypes,
        List<DamageTakenDisplay> damageTakenMultipliers,
        List<ItemStack> purpleItems,
        Map<ResourceLocation, Integer> slotItemCounts,
        Map<OrganSlotGroup, Map<ResourceLocation, Integer>> groupItemCounts) {

    private static final Map<String, Attribute> ATTRIBUTE_KEYS = buildAttributeKeys();

    public record DamageTakenDisplay(String key, double multiplier, boolean fallback) {
    }

    public record DamageTypeDisplay(String key, int requiredHits) {
    }

    public OrganStatSummary {
        adaptationRequiredHits = adaptationRequiredHits == null
                ? Map.of() : Map.copyOf(adaptationRequiredHits);
        adaptationRequiredHitsTags = adaptationRequiredHitsTags == null
                ? Map.of() : Map.copyOf(adaptationRequiredHitsTags);
        nonAdaptableDamageTypes = nonAdaptableDamageTypes == null
                ? List.of() : List.copyOf(nonAdaptableDamageTypes);
        damageTakenMultipliers = damageTakenMultipliers == null
                ? List.of() : List.copyOf(damageTakenMultipliers);
        purpleItems = purpleItems == null ? List.of() : List.copyOf(purpleItems);
        slotItemCounts = slotItemCounts == null ? Map.of() : Map.copyOf(slotItemCounts);

        if (groupItemCounts == null || groupItemCounts.isEmpty()) {
            groupItemCounts = Map.of();
        } else {
            Map<OrganSlotGroup, Map<ResourceLocation, Integer>> frozen = new LinkedHashMap<>();
            for (Map.Entry<OrganSlotGroup, Map<ResourceLocation, Integer>> entry : groupItemCounts.entrySet()) {
                if (entry.getKey() == null) continue;
                frozen.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
            groupItemCounts = Map.copyOf(frozen);
        }
    }

    public static OrganStatSummary empty() {
        OrganStatDefaults defaults = OrganStatManager.defaults();
        List<DamageTakenDisplay> taken = effectiveDamageTaken(defaults, Map.of());
        List<String> nonAdaptable = new ArrayList<>();
        for (OrganStatDefaults.DamageTypeDefaults type : defaults.damageTypes()) {
            if (!type.adaptable()) nonAdaptable.add(type.displayKey());
        }
        return new OrganStatSummary(
                OrganStatProfile.ZERO,
                OrganStatProfile.ZERO,
                OrganStatProfile.ZERO,
                0.0D,
                defaults.minimumDamageIntervalTicks(),
                0.0D,
                defaults.adaptationChance(),
                Map.of(),
                Map.of(),
                nonAdaptable,
                taken,
                List.of(),
                Map.of(),
                Map.of());
    }

    private static final OrganSlotGroup OUTER_PROBE_GROUP = OrganSlotGroup.HEAD;

    public static OrganStatSummary compute(@Nullable HiveLeaderOrganData data) {
        if (data == null) return empty();

        OrganStatDefaults defaults = OrganStatManager.defaults();
        OrganStatProfile.Accumulator purple = new OrganStatProfile.Accumulator();
        OrganStatProfile.Accumulator torsoInner = new OrganStatProfile.Accumulator();
        OrganStatProfile.Accumulator headInner = new OrganStatProfile.Accumulator();

        List<ItemStack> purpleItems = new ArrayList<>();
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();

        Map<OrganSlotGroup, Map<ResourceLocation, Integer>> groupCounts = new LinkedHashMap<>();

        Map<ResourceLocation, Integer> purpleCounts = new LinkedHashMap<>();

        Map<String, Double> damageTakenAdd = new LinkedHashMap<>();
        int purpleTotal = 0;
        int minimumIntervalTicks = defaults.minimumDamageIntervalTicks();
        boolean intervalExplicit = false;

        for (OrganSlotGroup group : OrganSlotGroup.ALL) {
            for (int local = 0; local < group.size(); local++) {
                int index = group.globalIndex(local);
                ItemStack stack = data.getItem(index);
                if (stack == null || stack.isEmpty()) continue;

                ResourceLocation itemId = itemIdOf(stack);

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
                    } else {
                        headInner.add(entry.profileAt(group));
                    }
                    accumulateDamageTakenAdd(damageTakenAdd, entry);
                    continue;
                }

                if (!data.isUnlocked(index)) continue;

                purple.add(entry.profileAt(group));
                purpleItems.add(stack);
                purpleTotal++;
                accumulateDamageTakenAdd(damageTakenAdd, entry);
                if (itemId != null) {
                    purpleCounts.merge(itemId, 1, Integer::sum);
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

        double reduction = 0.0D;
        for (Map.Entry<ResourceLocation, Integer> entry : purpleCounts.entrySet()) {
            double perOrganReduction = OrganStatManager.entryOf(entry.getKey())
                    .profileAt(OUTER_PROBE_GROUP).adaptationReduction();
            reduction += perOrganReduction * outerShareWeight(entry.getValue(), purpleTotal);
        }

        Map<String, Integer> requiredHits = new LinkedHashMap<>();
        Map<String, Integer> requiredHitsTags = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : purpleCounts.entrySet()) {
            double strength = OrganStatMath.roundsForShares(entry.getValue(), purpleTotal);

            OrganStatManager.RoundsTable table =
                    OrganStatManager.adaptationRoundsForItem(entry.getKey(), OUTER_PROBE_GROUP);
            for (Map.Entry<String, Map<String, Integer>> typeEntry : table.byType().entrySet()) {
                int rounds = roundsForTiers(typeEntry.getValue(), strength);
                if (rounds > 0) requiredHits.merge(typeEntry.getKey(), rounds, Integer::sum);
            }

            for (Map.Entry<String, Map<String, Integer>> tagEntry : table.byTag().entrySet()) {
                int rounds = roundsForTiers(tagEntry.getValue(), strength);
                if (rounds > 0) requiredHitsTags.merge(tagEntry.getKey(), rounds, Integer::sum);
            }
        }

        List<String> nonAdaptable = new ArrayList<>();
        for (OrganStatDefaults.DamageTypeDefaults type : defaults.damageTypes()) {
            if (!type.adaptable()) nonAdaptable.add(type.displayKey());
        }

        List<DamageTakenDisplay> taken = effectiveDamageTaken(defaults, damageTakenAdd);

        return new OrganStatSummary(
                purpleBonus,
                torsoInner.toImmutable(),
                headInner.toImmutable(),
                purpleBonus.minimumDamage(),
                minimumIntervalTicks,
                reduction,
                defaults.adaptationChance(),
                requiredHits,
                requiredHitsTags,
                nonAdaptable,
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

    private static final boolean SHARE_DENOMINATOR_USES_OCCUPIED_SLOTS = true;

    static double outerShareWeight(int itemOccupiedOuterSlots, int totalOccupiedOuterSlots) {
        if (itemOccupiedOuterSlots <= 0) return 0.0D;
        int denominator = SHARE_DENOMINATOR_USES_OCCUPIED_SLOTS
                ? totalOccupiedOuterSlots
                : OrganSlotGroup.OUTER_SLOT_COUNT;
        if (denominator <= 0) return 0.0D;
        return (double) itemOccupiedOuterSlots / (double) denominator;
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
        Attribute attribute = ATTRIBUTE_KEYS.get(attributeKey);
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

    public List<DamageTypeDisplay> adaptationRoundsDisplay(@Nullable Registry<DamageType> registry) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : adaptationRequiredHits.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : adaptationRequiredHitsTags.entrySet()) {

            List<String> members = tagMemberIds(registry, entry.getKey());
            if (members.isEmpty()) {
                merged.merge("#" + stripHash(entry.getKey()), entry.getValue(), Integer::sum);
            } else {
                for (String member : members) {
                    merged.merge(member, entry.getValue(), Integer::sum);
                }
            }
        }

        if (merged.isEmpty()) {
            for (OrganStatDefaults.DamageTypeDefaults type : OrganStatManager.defaults().damageTypes()) {
                if (!type.adaptable()) continue;
                if (type.isTag()) {
                    TagKey<DamageType> tag = type.tagKey();
                    List<String> members = (tag == null || registry == null)
                            ? List.of() : tagMemberIds(registry, tag);
                    if (members.isEmpty()) {
                        merged.putIfAbsent(type.displayKey(), 0);
                    } else {
                        for (String member : members) {
                            merged.putIfAbsent(member, 0);
                        }
                    }
                } else if (type.typeId() != null) {
                    merged.putIfAbsent(type.typeId().toString(), 0);
                }
            }
        }

        List<DamageTypeDisplay> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            result.add(new DamageTypeDisplay(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparingInt(DamageTypeDisplay::requiredHits).reversed()
                .thenComparing(DamageTypeDisplay::key));
        return result;
    }

    private static String stripHash(String value) {
        return value.startsWith("#") ? value.substring(1) : value;
    }

    public List<String> nonAdaptableDisplay(@Nullable Registry<DamageType> registry) {
        Set<String> result = new TreeSet<>();
        for (OrganStatDefaults.DamageTypeDefaults type : OrganStatManager.defaults().damageTypes()) {
            if (type.adaptable()) continue;
            TagKey<DamageType> tag = type.tagKey();
            if (tag != null && registry != null) {
                List<String> members = tagMemberIds(registry, tag);
                if (!members.isEmpty()) {
                    result.addAll(members);
                    continue;
                }
            }
            ResourceLocation id = type.typeId();
            result.add(id != null ? id.toString() : type.displayKey());
        }
        result.addAll(nonAdaptableDamageTypes);
        return new ArrayList<>(result);
    }

    private static List<String> tagMemberIds(Registry<DamageType> registry, TagKey<DamageType> tag) {
        List<String> ids = new ArrayList<>();
        Optional<HolderSet.Named<DamageType>> named = registry.getTag(tag);
        if (named.isEmpty()) return ids;
        for (Holder<DamageType> holder : named.get()) {
            Optional<ResourceKey<DamageType>> key = holder.unwrapKey();
            if (key.isEmpty()) continue;
            ResourceLocation location = key.get().location();
            if (location != null) ids.add(location.toString());
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    private static List<String> tagMemberIds(@Nullable Registry<DamageType> registry, String tag) {
        if (registry == null || tag == null || tag.isBlank()) return List.of();
        ResourceLocation location = ResourceLocation.tryParse(stripHash(tag));
        if (location == null) return List.of();
        return tagMemberIds(registry, TagKey.create(Registries.DAMAGE_TYPE, location));
    }

    private static Map<String, Attribute> buildAttributeKeys() {
        Map<String, Attribute> keys = new LinkedHashMap<>();
        keys.put("health", Attributes.MAX_HEALTH);
        keys.put("armor", Attributes.ARMOR);

        keys.put("armor_toughness", Attributes.ARMOR_TOUGHNESS);
        keys.put("attack_damage", Attributes.ATTACK_DAMAGE);
        keys.put("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE);
        keys.put("movement_speed", Attributes.MOVEMENT_SPEED);

        keys.put("swim_speed", ForgeMod.SWIM_SPEED.get());

        keys.put("attack_range", ForgeMod.ENTITY_REACH.get());
        keys.put("block_reach", ForgeMod.BLOCK_REACH.get());

        keys.put("entity_gravity", ForgeMod.ENTITY_GRAVITY.get());
        return Map.copyOf(keys);
    }

    @Nullable
    private static ResourceLocation itemIdOf(ItemStack stack) {
        Item item = stack.getItem();
        return item == null ? null : ForgeRegistries.ITEMS.getKey(item);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private static int roundsAt(Map.Entry<String, Integer> round, double strength) {
        if (parseDouble(round.getKey()) > strength + 1.0E-9D) return 0;
        return Math.max(0, round.getValue());
    }

    private static int roundsForTiers(@Nullable Map<String, Integer> table, double strength) {
        if (table == null || table.isEmpty()) return 0;
        int total = 0;
        for (Map.Entry<String, Integer> round : table.entrySet()) {
            total += roundsAt(round, strength);
        }
        return total;
    }

    @Override
    public String toString() {
        return "OrganStatSummary[purple=" + purpleBonus.describeNonZero()
                + ", total=" + totalBonus().describeNonZero()
                + ", minDamage=" + minimumDamage
                + ", intervalTicks=" + minimumDamageIntervalTicks
                + ", reduction=" + adaptationReduction
                + ", purpleItems=" + purpleItems.size() + "]";
    }

    public int countOf(ResourceLocation itemId) {
        return itemId == null ? 0 : slotItemCounts.getOrDefault(itemId, 0);
    }

    public int countIn(@Nullable OrganSlotGroup group, @Nullable ResourceLocation itemId) {
        if (group == null || itemId == null) return 0;
        Map<ResourceLocation, Integer> perGroup = groupItemCounts.get(group);
        return perGroup == null ? 0 : perGroup.getOrDefault(itemId, 0);
    }
}
