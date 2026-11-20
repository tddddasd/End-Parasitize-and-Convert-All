package org.tdddd.epca.impl.overworld.data.organ.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotCondition;
import org.tdddd.epca.impl.overworld.data.organ.OrganSlotGroup;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 器官属性 JSON（{@code data/epca/organ_stats/*.json}）的加载器。
 *
 * <p>2026-11 起器官<strong>不再提供适应属性</strong>：{@code adaptation_reduction}、
 * {@code adaptation_rounds}、{@code adaptation_rounds_tags}、{@code adaptation_chance}、
 * {@code damage_types}、{@code adaptable} 等键全部不再解析（旧数据文件里残留的同名字段会被
 * {@link JsonObject} 忽略，不会产生任何效果）。保留的字段：{@code attributes} /
 * {@code slot_bonuses} / {@code minimum_damage} / {@code minimum_damage_interval_ticks} /
 * {@code damage_taken_add}，以及 {@code _defaults.json} 的 {@code damage_taken}。</p>
 */
public final class OrganStatManager implements ResourceManagerReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    public static final String DIRECTORY = "organ_stats";

    public static final String DEFAULTS_FILE = "_defaults";

    private static volatile Map<ResourceLocation, OrganStatEntry> entries = Map.of();

    private static volatile Map<ResourceLocation, Integer> intervalOverrides = Map.of();

    private static volatile OrganStatDefaults defaults = OrganStatDefaults.FALLBACK;

    public OrganStatManager() {
    }

    public record OrganStatEntry(
            Map<OrganSlotCondition, OrganStatProfile> profiles,
            Map<String, Double> damageTakenAdd) {

        public static final OrganStatEntry EMPTY = new OrganStatEntry(
                Map.of(OrganSlotCondition.BASE, OrganStatProfile.ZERO), Map.of());

        public OrganStatEntry {
            Map<OrganSlotCondition, OrganStatProfile> safeProfiles = profiles == null
                    ? Map.of() : profiles;
            if (!safeProfiles.containsKey(OrganSlotCondition.BASE)) {
                Map<OrganSlotCondition, OrganStatProfile> withBase = new LinkedHashMap<>(safeProfiles);
                withBase.put(OrganSlotCondition.BASE, OrganStatProfile.ZERO);
                safeProfiles = withBase;
            }
            profiles = Map.copyOf(safeProfiles);
            damageTakenAdd = damageTakenAdd == null ? Map.of() : Map.copyOf(damageTakenAdd);
        }

        public OrganStatProfile baseProfile() {
            OrganStatProfile base = profiles.get(OrganSlotCondition.BASE);
            return base == null ? OrganStatProfile.ZERO : base;
        }

        public OrganStatProfile profileAt(@Nullable OrganSlotGroup group) {
            OrganStatProfile.Accumulator accumulator = new OrganStatProfile.Accumulator();
            for (Map.Entry<OrganSlotCondition, OrganStatProfile> entry : profiles.entrySet()) {
                if (entry.getKey().matches(group)) {
                    accumulator.add(entry.getValue());
                }
            }
            return accumulator.toImmutable();
        }
    }

    public static OrganStatDefaults defaults() {
        OrganStatDefaults current = defaults;
        return current == null ? OrganStatDefaults.FALLBACK : current;
    }

    public static OrganStatEntry entryOf(@Nullable ItemStack stack) {
        return entryOf(itemIdOf(stack));
    }

    public static OrganStatEntry entryOf(@Nullable ResourceLocation itemId) {
        if (itemId == null) return OrganStatEntry.EMPTY;
        OrganStatEntry entry = entries.get(itemId);
        return entry == null ? OrganStatEntry.EMPTY : entry;
    }

    public static OrganStatProfile profileOf(@Nullable ItemStack stack) {
        return profileOf(itemIdOf(stack));
    }

    public static OrganStatProfile profileOf(@Nullable ResourceLocation itemId) {
        return entryOf(itemId).baseProfile();
    }

    @Nullable
    public static Integer intervalOverrideTicks(@Nullable ItemStack stack) {
        ResourceLocation id = itemIdOf(stack);
        return id == null ? null : intervalOverrides.get(id);
    }

    public static String describe() {
        int conditional = 0;
        for (OrganStatEntry entry : entries.values()) {
            for (Map.Entry<OrganSlotCondition, OrganStatProfile> block : entry.profiles().entrySet()) {
                if (block.getKey() != OrganSlotCondition.BASE && !block.getValue().isZero()) {
                    conditional++;
                }
            }
        }
        return "OrganStatManager[items=" + entries.size()
                + ", conditionalBlocks=" + conditional
                + ", intervalOverrides=" + intervalOverrides.size()
                + ", damageTakenRows=" + defaults().damageTaken().size() + "]";
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<ResourceLocation, OrganStatEntry> newEntries = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> newIntervalOverrides = new LinkedHashMap<>();
        OrganStatDefaults newDefaults = readDefaults(resourceManager);

        resourceManager.listResources(DIRECTORY, file -> file.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    if (DEFAULTS_FILE.equals(fileNameOf(location.getPath()))) {
                        return;
                    }
                    try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                        JsonElement root = GSON.fromJson(reader, JsonElement.class);
                        if (root == null || !root.isJsonObject()) return;
                        readItemFile(root.getAsJsonObject(), fallbackItemId(location),
                                newEntries, newIntervalOverrides);
                    } catch (Exception e) {
                        System.err.println("[epca] 读取器官属性 JSON 失败: " + location + " -> " + e);
                    }
                });

        entries = Map.copyOf(newEntries);
        intervalOverrides = Map.copyOf(newIntervalOverrides);
        defaults = newDefaults;
    }

    private static String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;
    }

    @Nullable
    private static ResourceLocation fallbackItemId(ResourceLocation fileLocation) {
        String path = fileLocation.getPath();
        int slash = path.lastIndexOf('/');
        String relative = slash >= 0 ? path.substring(slash + 1) : path;
        if (relative.endsWith(".json")) {
            relative = relative.substring(0, relative.length() - ".json".length());
        }
        return ResourceLocation.tryParse(fileLocation.getNamespace() + ":" + relative);
    }

    private static void readItemFile(JsonObject root,
                                     @Nullable ResourceLocation fromFileName,
                                     Map<ResourceLocation, OrganStatEntry> entriesOut,
                                     Map<ResourceLocation, Integer> intervalOut) {
        if (looksLikeBareItem(root)) {
            if (fromFileName != null) {
                applyItem(fromFileName, root, entriesOut, intervalOut);
            }
            return;
        }
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            ResourceLocation itemId = ResourceLocation.tryParse(entry.getKey());
            if (itemId == null || !entry.getValue().isJsonObject()) {
                System.err.println("[epca] 器官属性 JSON 顶层键不是合法物品 id: " + entry.getKey());
                continue;
            }
            applyItem(itemId, entry.getValue().getAsJsonObject(), entriesOut, intervalOut);
        }
    }

    private static boolean looksLikeBareItem(JsonObject root) {
        return root.has("attributes") || root.has("minimum_damage")
                || root.has("slot_bonuses") || root.has("damage_taken_add");
    }

    private static void applyItem(ResourceLocation itemId,
                                  JsonObject object,
                                  Map<ResourceLocation, OrganStatEntry> entriesOut,
                                  Map<ResourceLocation, Integer> intervalOut) {
        Map<OrganSlotCondition, OrganStatProfile> profiles = new LinkedHashMap<>();

        readBonusBlock(object, OrganSlotCondition.BASE, itemId, profiles, intervalOut);

        JsonElement bonuses = object.get("slot_bonuses");
        if (bonuses != null && bonuses.isJsonArray()) {
            for (JsonElement element : bonuses.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject block = element.getAsJsonObject();
                OrganSlotCondition condition = OrganSlotCondition.byId(asString(block.get("condition")));
                if (condition == null) {
                    System.err.println("[epca] 器官属性 JSON 的 slot_bonuses 条件不认识: "
                            + itemId + " -> " + block.get("condition"));
                    continue;
                }

                readBonusBlock(block, condition, itemId, profiles, null);
            }
        }
        profiles.putIfAbsent(OrganSlotCondition.BASE, OrganStatProfile.ZERO);

        Map<String, Double> damageTakenAdd = readDamageTakenAdd(asObject(object.get("damage_taken_add")));

        entriesOut.put(itemId, new OrganStatEntry(profiles, damageTakenAdd));
    }

    private static void readBonusBlock(JsonObject block,
                                       OrganSlotCondition condition,
                                       ResourceLocation itemId,
                                       Map<OrganSlotCondition, OrganStatProfile> profilesOut,
                                       @Nullable Map<ResourceLocation, Integer> intervalOut) {
        Map<String, Double> attributes = new LinkedHashMap<>();
        JsonObject attributeObject = asObject(block.get("attributes"));
        if (attributeObject != null) {
            for (Map.Entry<String, JsonElement> attribute : attributeObject.entrySet()) {
                Double value = asDouble(attribute.getValue());
                if (value != null) attributes.put(attribute.getKey(), value);
            }
        }
        Double minimumDamage = asDouble(block.get("minimum_damage"));
        if (minimumDamage != null) attributes.put("minimum_damage", minimumDamage);

        OrganStatProfile profile = OrganStatProfile.fromJsonMap(attributes);
        if (!profile.isZero()) {
            OrganStatProfile existing = profilesOut.get(condition);
            profilesOut.put(condition, existing == null ? profile : existing.plus(profile));
        }

        if (intervalOut != null) {
            Integer interval = asInt(block.get("minimum_damage_interval_ticks"));
            if (interval != null && interval > 0) intervalOut.put(itemId, interval);
        }
    }

    private static Map<String, Double> readDamageTakenAdd(@Nullable JsonObject object) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (object == null) return result;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) continue;
            Double value = asDouble(entry.getValue());
            if (value == null) {
                System.err.println("[epca] 器官属性 damage_taken_add 的值不是数字: " + key);
                continue;
            }
            result.merge(key.trim(), value, Double::sum);
        }
        return result;
    }

    @Nullable
    private static JsonObject asObject(@Nullable JsonElement element) {
        return (element != null && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }

    private static OrganStatDefaults readDefaults(ResourceManager resourceManager) {
        ResourceLocation location = new ResourceLocation("epca", DIRECTORY + "/" + DEFAULTS_FILE + ".json");
        var resource = resourceManager.getResource(location);
        if (resource.isEmpty()) {
            System.err.println("[epca] 找不到 " + location + "，器官读数使用内置默认值");
            return OrganStatDefaults.FALLBACK;
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return OrganStatDefaults.FALLBACK;

            Integer interval = asInt(root.get("minimum_damage_interval_ticks"));

            List<OrganStatDefaults.DamageTakenEntry> damageTaken = new ArrayList<>();
            if (root.get("damage_taken") != null && root.get("damage_taken").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("damage_taken")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject entry = element.getAsJsonObject();
                    String tag = entry.has("tag") ? entry.get("tag").getAsString() : null;
                    ResourceLocation typeId = entry.has("type")
                            ? ResourceLocation.tryParse(entry.get("type").getAsString()) : null;
                    Double multiplier = asDouble(entry.get("multiplier"));
                    damageTaken.add(new OrganStatDefaults.DamageTakenEntry(
                            tag, typeId, multiplier == null ? 1.0D : multiplier));
                }
            }

            return new OrganStatDefaults(
                    interval == null ? OrganStatDefaults.DEFAULT_INTERVAL_TICKS : interval,
                    damageTaken.isEmpty() ? OrganStatDefaults.FALLBACK.damageTaken() : damageTaken);
        } catch (Exception e) {
            System.err.println("[epca] 读取 " + location + " 失败，器官读数使用内置默认值: " + e);
            return OrganStatDefaults.FALLBACK;
        }
    }

    @Nullable
    private static ResourceLocation itemIdOf(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return ForgeRegistries.ITEMS.getKey(stack.getItem());
    }

    @Nullable
    private static Double asDouble(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer asInt(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static String asString(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static boolean isFallbackKey(String key) {
        if (key == null || key.isBlank()) return true;
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("fallback") || normalized.equals("other")
                || normalized.equals("*") || normalized.equals("其余") || normalized.equals("其余伤害");
    }
}
