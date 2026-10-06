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
 * SPEC  0  D6
 * <b> json </b>
 *
 * <h2></h2>
 * <p> {@code data/<namespace>/organ_stats/}<b> id  JSON </b></p>
 * <ul>
 *   <li>{@code data/epca/organ_stats/_defaults.json}
 *       </li>
 *   <li>{@code data/epca/organ_stats/infested_flesh.json}   {@code epca:infested_flesh}
 *        =  id  path {@code BiomassSpawnManager}
 *       </li>
 * </ul>
 *
 * <h2> schemaSTAGE A </h2>
 * <pre>
 * {
 *   "epca:reshape_flesh": {                  //  =  id
 *     "attributes": {                        //
 *       "health": 2.0,                       // minecraft:generic.max_health
 *       "armor": 0.0,                        // minecraft:generic.armor
 *       "armor_toughness": 0.0,              // minecraft:generic.armor_toughnessSTAGE A
 *       "attack_damage": 0.0,                // minecraft:generic.attack_damage
 *       "knockback_resistance": 0.0,         // minecraft:generic.knockback_resistance
 *       "movement_speed": 0.0,               // minecraft:generic.movement_speed
 *       "swim_speed": 0.0,                   // forge:swim_speedSTAGE A  1.0
 *       "attack_range": 0.0,                 // forge:entity_reach= forge:attack_range  1.20.1
 *       "block_reach": 0.0,                  // forge:block_reach
 *       "entity_gravity": 0.0                // forge:entity_gravity OrganStatProfile
 *     },
 *     "minimum_damage": 0.0,                 //
 *     "minimum_damage_interval_ticks": 40,   //
 *     "adaptation_reduction": 0.0,           //  01
 *     "adaptation_rounds": {                 //  ->  id
 *       "minecraft:in_fire": { "0.25": 4, "0.5": 8, "0.75": 12, "1.0": 16 }
 *     },
 *     "adaptation_rounds_tags": {            //  tag #
 *       "minecraft:is_fire": { "0.25": 4, "0.5": 8, "0.75": 12, "1.0": 16 }
 *     },
 *     "slot_bonuses": [                      // STAGE A  0..n
 *       {
 *         "condition": "outer",              // base / outer / head_inner / arm OrganSlotCondition
 *         "attributes": { "attack_damage": 0.5 },
 *         "minimum_damage": 0.0,
 *         "adaptation_reduction": 0.6,
 *         "adaptation_rounds_tags": { "minecraft:is_fire": { "0.25": 12 } }
 *       },
 *       { "condition": "arm", "minimum_damage": 1.0 }
 *     ],
 *     "damage_taken_add": {                  // STAGE A  1  1
 *       "#minecraft:is_fire": 0.05,          //  #  =  tagDamageSource#is(TagKey)
 *       "minecraft:in_fire": -0.1,           //  :  =  id id getMsgId()
 *       "fallback": -0.01                    // fallback / other / * =
 *     }
 *   }
 * }
 * </pre>
 *
 * <h3>STAGE A  schema  {@link OrganSlotCondition} </h3>
 * <ol>
 *   <li><b></b>{@code armor_toughness}{@code Attributes.ARMOR_TOUGHNESS}
 *       {@code swim_speed}{@code ForgeMod.SWIM_SPEED} {@code entity_gravity}
 *        -2% </li>
 *   <li><b>{@code slot_bonuses}</b> {@code condition}
 *       <b></b>{@code attributes} / {@code minimum_damage} / {@code adaptation_reduction} /
 *       {@code adaptation_rounds} / {@code adaptation_rounds_tags}
 *        = {@code condition: "base"} <b></b>
 *       base + outer + arm {@link OrganSlotCondition#matches(OrganSlotGroup)}</li>
 *   <li><b>{@code damage_taken_add}</b>
 *       {@code _defaults.json}  {@code damage_taken} tag  tag id  id/msgId
 *       {@code fallback} </li>
 *   <li><b>{@code minimum_damage_interval_ticks} </b>
 *       SPEC </li>
 * </ol>
 * <p> {@code attributes} / {@code slot_bonuses} /
 * {@code damage_taken_add} <b></b> id + </p>
 *
 * <h3>STAGE A </h3>
 * <ul>
 *   <li><b> = </b>{@code Operation.ADDITION}
 *       </li>
 *   <li><b>1.0 = 100%</b>+2.5%  {@code 0.025}+5%  {@code 0.05}
 *       +8%  {@code 0.08}-0.25 {@code -0.25} 1.0
 *        0.1 1.0 0.08
 *        SPEC
 *        2  {@code 0.0025}  +0.00
 *       </li>
 *   <li><b> /  /  / </b></li>
 *   <li><b>{@code entity_gravity}</b> 0.08
 *        -2%  = {@code -0.02 * 0.08 = -0.0016}
 *        {@code data/epca/organ_stats/gasbag_debris.json}  {@code _comment}
 *        -0.02  4  0</li>
 *   <li><b>{@code damage_taken_add} </b>{@code -0.01}
 *        -0.01 2.5  0.05  2.55 1.0  -0.01  0.99</li>
 *   <li> {@code _comment}<b></b>
 *       <b></b> id </li>
 * </ul>
 *
 * <h2>_defaults.json  schema</h2>
 * <pre>
 * {
 *   "minimum_damage_interval_ticks": 40,     //  2
 *   "adaptation_chance": 1.0,                //  100%
 *   "damage_types": [
 *     { "type": "minecraft:generic_kill", "label": "kill ", "adaptable": false },
 *     { "type": "minecraft:out_of_world", "label": "", "adaptable": false },
 *     { "tag": "#minecraft:is_fire", "label": "", "adaptable": true,
 *       "adaptation_rounds": { "0.25": 4, "0.5": 8, "0.75": 12, "1.0": 16 } }
 *   ],
 *   "damage_taken": [
 *     { "tag": "#minecraft:is_fire", "multiplier": 2.5 },
 *     { "multiplier": 1.0 }                  //  tag / type  =
 *   ]
 * }
 * </pre>
 *
 * <h2></h2>
 * <p> {@link ResourceManagerReloadListener} {@code epca#onAddReloadListeners}
 *  {@code EntityIntegrationManager} / {@code BiomassSpawnManager}
 *  GUI
 *  JSON 0{@link OrganStatProfile#ZERO}
 *  {@link OrganStatDefaults#FALLBACK}</p>
 */
public final class OrganStatManager implements ResourceManagerReloadListener {

    private static final Gson GSON = new GsonBuilder().create();

    /**  */
    public static final String DIRECTORY = "organ_stats";
    /**  .json */
    public static final String DEFAULTS_FILE = "_defaults";

    /**  id -&gt;  +  + volatile +  */
    private static volatile Map<ResourceLocation, OrganStatEntry> entries = Map.of();
    /**  id -&gt; tick */
    private static volatile Map<ResourceLocation, Integer> intervalOverrides = Map.of();
    /** reload  {@link OrganStatDefaults#FALLBACK} */
    private static volatile OrganStatDefaults defaults = OrganStatDefaults.FALLBACK;
    /**  -&gt;  id  */
    private static volatile Map<String, Map<String, Integer>> defaultRoundsByType = Map.of();
    /**  -&gt;  tag  # */
    private static volatile Map<String, Map<String, Integer>> defaultRoundsByTag = Map.of();

    /**
     *  {@link ResourceManagerReloadListener}
     * {@code epca#onAddReloadListeners}  {@code new OrganStatManager()}
     *  {@code EntityIntegrationManager} / {@code BiomassSpawnManager}
     *
     * <p>
     *  new / reload listener </p>
     */
    public OrganStatManager() {
    }

    /**  -&gt;  */
    public record RoundsTable(Map<String, Map<String, Integer>> byType,
                              Map<String, Map<String, Integer>> byTag) {

        public static final RoundsTable EMPTY = new RoundsTable(Map.of(), Map.of());

        public RoundsTable {
            byType = byType == null ? Map.of() : Map.copyOf(byType);
            byTag = byTag == null ? Map.of() : Map.copyOf(byTag);
        }

        public boolean isEmpty() {
            return byType.isEmpty() && byTag.isEmpty();
        }

        /**
         *
         *
         * <p>STAGE A  +  {@code adaptation_rounds}
         *  {@link #EMPTY}  map</p>
         */
        public RoundsTable mergedWith(@Nullable RoundsTable other) {
            if (other == null || other.isEmpty()) return this;
            if (this.isEmpty()) return other;
            return new RoundsTable(mergeTables(this.byType, other.byType),
                    mergeTables(this.byTag, other.byTag));
        }

        private static Map<String, Map<String, Integer>> mergeTables(
                Map<String, Map<String, Integer>> left, Map<String, Map<String, Integer>> right) {
            Map<String, Map<String, Integer>> merged = new LinkedHashMap<>();
            left.forEach((key, table) -> merged.put(key, new LinkedHashMap<>(table)));
            right.forEach((key, table) -> {
                Map<String, Integer> target = merged.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
                table.forEach((tier, count) -> target.merge(tier, count, Integer::sum));
            });
            Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
            merged.forEach((key, table) -> result.put(key, Map.copyOf(table)));
            return Map.copyOf(result);
        }
    }

    /**
     *  -&gt;  +
     *
     * <p>{@link OrganSlotCondition#BASE}  JSON
     *  {@code slot_bonuses}
     * {@link #profileAt(OrganSlotGroup)} / {@link #roundsAt(OrganSlotGroup)}
     * </p>
     *
     * @param profiles        -&gt; // BASE
     * @param rounds          -&gt;  -&gt;
     * @param damageTakenAdd  {@code damage_taken_add}  1  1
     */
    public record OrganStatEntry(
            Map<OrganSlotCondition, OrganStatProfile> profiles,
            Map<OrganSlotCondition, RoundsTable> rounds,
            Map<String, Double> damageTakenAdd) {

        /**  0 */
        public static final OrganStatEntry EMPTY = new OrganStatEntry(
                Map.of(OrganSlotCondition.BASE, OrganStatProfile.ZERO), Map.of(), Map.of());

        public OrganStatEntry {
            Map<OrganSlotCondition, OrganStatProfile> safeProfiles = profiles == null
                    ? Map.of() : profiles;
            if (!safeProfiles.containsKey(OrganSlotCondition.BASE)) {
                Map<OrganSlotCondition, OrganStatProfile> withBase = new LinkedHashMap<>(safeProfiles);
                withBase.put(OrganSlotCondition.BASE, OrganStatProfile.ZERO);
                safeProfiles = withBase;
            }
            profiles = Map.copyOf(safeProfiles);
            rounds = rounds == null ? Map.of() : Map.copyOf(rounds);
            damageTakenAdd = damageTakenAdd == null ? Map.of() : Map.copyOf(damageTakenAdd);
        }

        /**  */
        public OrganStatProfile baseProfile() {
            OrganStatProfile base = profiles.get(OrganSlotCondition.BASE);
            return base == null ? OrganStatProfile.ZERO : base;
        }

        /**  = {@code group == null}  */
        public OrganStatProfile profileAt(@Nullable OrganSlotGroup group) {
            OrganStatProfile.Accumulator accumulator = new OrganStatProfile.Accumulator();
            for (Map.Entry<OrganSlotCondition, OrganStatProfile> entry : profiles.entrySet()) {
                if (entry.getKey().matches(group)) {
                    accumulator.add(entry.getValue());
                }
            }
            return accumulator.toImmutable();
        }

        /**  -&gt;  =  */
        public RoundsTable roundsAt(@Nullable OrganSlotGroup group) {
            RoundsTable merged = RoundsTable.EMPTY;
            for (Map.Entry<OrganSlotCondition, RoundsTable> entry : rounds.entrySet()) {
                if (entry.getKey().matches(group)) {
                    merged = merged.mergedWith(entry.getValue());
                }
            }
            return merged;
        }
    }


    /**  null */
    public static OrganStatDefaults defaults() {
        OrganStatDefaults current = defaults;
        return current == null ? OrganStatDefaults.FALLBACK : current;
    }

    /** <b></b> {@link OrganStatEntry#EMPTY} null */
    public static OrganStatEntry entryOf(@Nullable ItemStack stack) {
        return entryOf(itemIdOf(stack));
    }

    /**  id  {@link OrganStatEntry#EMPTY} */
    public static OrganStatEntry entryOf(@Nullable ResourceLocation itemId) {
        if (itemId == null) return OrganStatEntry.EMPTY;
        OrganStatEntry entry = entries.get(itemId);
        return entry == null ? OrganStatEntry.EMPTY : entry;
    }

    /**
     * <b></b> {@link OrganStatProfile#ZERO} null
     *
     * <p>
     *  {@link #entryOf(ResourceLocation)} + {@link OrganStatEntry#profileAt(OrganSlotGroup)}</p>
     */
    public static OrganStatProfile profileOf(@Nullable ItemStack stack) {
        return profileOf(itemIdOf(stack));
    }

    /**  id  {@link OrganStatProfile#ZERO} */
    public static OrganStatProfile profileOf(@Nullable ResourceLocation itemId) {
        return entryOf(itemId).baseProfile();
    }

    /** tick {@code null} */
    @Nullable
    public static Integer intervalOverrideTicks(@Nullable ItemStack stack) {
        ResourceLocation id = itemIdOf(stack);
        return id == null ? null : intervalOverrides.get(id);
    }

    /**
     *  -&gt;  {@code adaptation_rounds}
     *  {@code _defaults.json}  {@code adaptation_rounds}
     *
     * <p><b></b></p>
     */
    public static RoundsTable adaptationRoundsForItem(@Nullable ResourceLocation itemId) {
        return adaptationRoundsForItem(itemId, null);
    }

    /**
     *  -&gt;
     *  +  {@code group}
     *  {@code _defaults.json}
     *  {@link #adaptationRoundsForItem(ResourceLocation)}
     *
     * @param group {@code null}
     */
    public static RoundsTable adaptationRoundsForItem(@Nullable ResourceLocation itemId,
                                                      @Nullable OrganSlotGroup group) {
        RoundsTable own = entryOf(itemId).roundsAt(group);
        if (!own.isEmpty()) {
            return own;
        }
        return new RoundsTable(defaultRoundsByType, defaultRoundsByTag);
    }

    /** / */
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
                + ", defaultRoundsByType=" + defaultRoundsByType.size()
                + ", defaultRoundsByTag=" + defaultRoundsByTag.size()
                + ", damageTypes=" + defaults().damageTypes().size() + "]";
    }


    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<ResourceLocation, OrganStatEntry> newEntries = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> newIntervalOverrides = new LinkedHashMap<>();
        OrganStatDefaults newDefaults = readDefaults(resourceManager);

        resourceManager.listResources(DIRECTORY, file -> file.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    if (DEFAULTS_FILE.equals(fileNameOf(location.getPath()))) {
                        return;  //  readDefaults
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

    /**  .json */
    private static String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;
    }

    /**
     *  id {@code <namespace>:<>}
     *  {@code epca}
     */
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

    /**  */
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
                || root.has("adaptation_reduction") || root.has("adaptation_rounds")
                || root.has("adaptation_rounds_tags")
                // STAGE A
                || root.has("slot_bonuses") || root.has("damage_taken_add");
    }

    /**  = {@code slot_bonuses} =  */
    private static void applyItem(ResourceLocation itemId,
                                  JsonObject object,
                                  Map<ResourceLocation, OrganStatEntry> entriesOut,
                                  Map<ResourceLocation, Integer> intervalOut) {
        Map<OrganSlotCondition, OrganStatProfile> profiles = new LinkedHashMap<>();
        Map<OrganSlotCondition, RoundsTable> rounds = new LinkedHashMap<>();

        //   =
        readBonusBlock(object, OrganSlotCondition.BASE, itemId, profiles, rounds, intervalOut);

        //  slot_bonuses
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
                //  minimum_damage_interval_ticks
                readBonusBlock(block, condition, itemId, profiles, rounds, null);
            }
        }
        profiles.putIfAbsent(OrganSlotCondition.BASE, OrganStatProfile.ZERO);

        //  damage_taken_add 1  1  OrganStatSummary
        Map<String, Double> damageTakenAdd = readDamageTakenAdd(asObject(object.get("damage_taken_add")));

        entriesOut.put(itemId, new OrganStatEntry(profiles, rounds, damageTakenAdd));
    }

    /**
     *  {@code slot_bonuses}
     *
     * @param intervalOut  {@code null}  {@code minimum_damage_interval_ticks}
     */
    private static void readBonusBlock(JsonObject block,
                                       OrganSlotCondition condition,
                                       ResourceLocation itemId,
                                       Map<OrganSlotCondition, OrganStatProfile> profilesOut,
                                       Map<OrganSlotCondition, RoundsTable> roundsOut,
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
        Double adaptationReduction = asDouble(block.get("adaptation_reduction"));
        if (adaptationReduction != null) attributes.put("adaptation_reduction", adaptationReduction);

        OrganStatProfile profile = OrganStatProfile.fromJsonMap(attributes);
        if (!profile.isZero()) {
            OrganStatProfile existing = profilesOut.get(condition);
            profilesOut.put(condition, existing == null ? profile : existing.plus(profile));
        }

        Map<String, Map<String, Integer>> byType = readRoundsMap(asObject(block.get("adaptation_rounds")));
        Map<String, Map<String, Integer>> byTag = readRoundsMap(asObject(block.get("adaptation_rounds_tags")));
        if (!byType.isEmpty() || !byTag.isEmpty()) {
            RoundsTable table = new RoundsTable(byType, byTag);
            RoundsTable existing = roundsOut.get(condition);
            roundsOut.put(condition, existing == null ? table : existing.mergedWith(table));
        }

        if (intervalOut != null) {
            Integer interval = asInt(block.get("minimum_damage_interval_ticks"));
            if (interval != null && interval > 0) intervalOut.put(itemId, interval);
        }
    }

    /** {@code {"#minecraft:is_fire": 0.05, "fallback": -0.01}} -&gt; map */
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

    /**  JSON  {@code null} */
    @Nullable
    private static JsonObject asObject(@Nullable JsonElement element) {
        return (element != null && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }

    /** {@code {"minecraft:in_fire": {"0.25": 4}}} -&gt;  map */
    private static Map<String, Map<String, Integer>> readRoundsMap(@Nullable JsonObject object) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        if (object == null) return result;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            Map<String, Integer> table = readFlatRounds(entry.getValue().getAsJsonObject());
            if (!table.isEmpty()) result.put(entry.getKey(), table);
        }
        return result;
    }

    /** {@code {"0.25": 4, "0.5": 8}} -&gt; map */
    private static Map<String, Integer> readFlatRounds(JsonObject object) {
        Map<String, Integer> table = new LinkedHashMap<>();
        if (object == null) return table;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            if (!isNumericKey(entry.getKey())) continue;
            Integer value = asInt(entry.getValue());
            if (value != null) table.put(entry.getKey(), value);
        }
        return table;
    }

    /** {@code "0.25"} {@code "label"}  */
    private static boolean isNumericKey(String key) {
        if (key == null || key.isBlank()) return false;
        try {
            Double.parseDouble(key);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**  {@code _defaults.json} /  {@link OrganStatDefaults#FALLBACK} */
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
            Double chance = asDouble(root.get("adaptation_chance"));

            List<OrganStatDefaults.DamageTypeDefaults> damageTypes = new ArrayList<>();
            Map<String, Map<String, Integer>> roundsByType = new LinkedHashMap<>();
            Map<String, Map<String, Integer>> roundsByTag = new LinkedHashMap<>();

            if (root.get("damage_types") != null && root.get("damage_types").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("damage_types")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject type = element.getAsJsonObject();
                    ResourceLocation typeId = type.has("type")
                            ? ResourceLocation.tryParse(type.get("type").getAsString()) : null;
                    String tag = type.has("tag") ? type.get("tag").getAsString() : null;
                    boolean adaptable = !type.has("adaptable") || type.get("adaptable").getAsBoolean();
                    Map<String, Integer> roundsTable = readFlatRounds(asObject(type.get("adaptation_rounds")));
                    if (roundsTable.isEmpty()) {
                        roundsTable = readFlatRounds(type);
                    }
                    String label = type.has("label") ? type.get("label").getAsString() : null;
                    if (label == null || label.isBlank()) {
                        label = typeId != null ? typeId.toString() : (tag != null ? tag : "?");
                    }
                    damageTypes.add(new OrganStatDefaults.DamageTypeDefaults(
                            typeId, tag, label, adaptable, roundsTable));
                    if (!roundsTable.isEmpty()) {
                        if (tag != null && !tag.isBlank()) {
                            roundsByTag.put(stripHash(tag), roundsTable);
                        } else if (typeId != null) {
                            roundsByType.put(typeId.toString(), roundsTable);
                        }
                    }
                }
            }

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

            defaultRoundsByType = Map.copyOf(roundsByType);
            defaultRoundsByTag = Map.copyOf(roundsByTag);

            return new OrganStatDefaults(
                    interval == null ? OrganStatDefaults.DEFAULT_INTERVAL_TICKS : interval,
                    chance == null ? OrganStatDefaults.DEFAULT_ADAPTATION_CHANCE : chance,
                    damageTypes.isEmpty() ? OrganStatDefaults.FALLBACK.damageTypes() : damageTypes,
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

    /** // {@code null} */
    @Nullable
    private static String asString(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String stripHash(String tag) {
        return tag.startsWith("#") ? tag.substring(1) : tag;
    }

    /**  {@code OrganStatSummary#effectiveDamageTaken} */
    static boolean isFallbackKey(String key) {
        if (key == null || key.isBlank()) return true;
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("fallback") || normalized.equals("other")
                || normalized.equals("*") || normalized.equals("其余") || normalized.equals("其余伤害");
    }
}

