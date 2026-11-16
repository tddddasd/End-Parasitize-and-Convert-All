package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 笔记正文页「详细数据」的数据来源（纯客户端，只读）。
 *
 * <p>每一行都取自工程里已经存在的数据，不另建副本：</p>
 * <ul>
 *   <li><b>基础生命值 / 基础护甲值 / 基础攻击伤害</b>：实体注册时
 *       {@code EntityAttributeCreationEvent} 把 {@code DefaultAttributes} 的供应商塞进 Forge 的属性视图，
 *       这里用 {@link DefaultAttributes#getSupplier} 取回 {@link AttributeSupplier}，读
 *       {@link AttributeSupplier#getBaseValue} 的 {@code MAX_HEALTH / ARMOR / ATTACK_DAMAGE}。
 *       没注册的属性按 0 处理（例如很多生物没有 ARMOR，就显示 0）。</li>
 *   <li><b>多体型</b>：在注册表里找 {@code <base>_size0/1/3...}，按 size 数字升序拼成 {@code a/b/c}；
 *       数值全都一样时只显示一个数（需求三的“各体型相同则只显示一个数”）。</li>
 *   <li><b>伤害适应性</b>：走 YNA 自己的映射表（{@code EntityAdaptationMapping} →
 *       {@code DamageAdaptationManager}，等价于 {@code AdaptationConfigResolver} 那条路径），
 *       并要求配置里真的开了适应（{@code damage_adaptability_probability} /
 *       {@code max_adaptable_damage_types} / {@code adaptation_percentage_per_injury} 至少一项非 0）；
 *       专用服务器上 YNA 的客户端静态表可能没加载，再回退到读取模组自带的
 *       {@code data/yawning_neko_api/entity_adaptation_mappings/entity_mapping.json}
 *       与对应的 {@code damage_adaptation_configs/*.json}。</li>
 *   <li><b>变种</b>：读实体类里已有的嵌套枚举 {@code Variant}（{@code DEFAULT} 不算变种，其余全部列出），
 *       枚举常量名过一遍拆分语言表 {@code epca.note.variant.<小写名>}。实体类是通过
 *       {@code ModEntities} 字段的泛型参数（{@code RegistryObject<EntityType<T>>}）反查出来的，
 *       所以以后新加带 Variant 的实体不需要改这里。</li>
 *   <li><b>掉落物</b>：读 {@code data/epca/loot_tables/entities/<id>.json}，
 *       取每个 {@code minecraft:item} 条目与它的 {@code minecraft:set_count}
 *       （固定值或 {@code min~max} 区间）。</li>
 * </ul>
 *
 * <p>数据来源的简化点（已在汇报里说明）：</p>
 * <ul>
 *   <li>掉落物只读模组自带的战利品表文件，不走服务端数据包 —— 数据包改写过战利品表时笔记里仍显示原表；</li>
 *   <li>战利品表里的 {@code minecraft:alternatives} / {@code minecraft:group} 等嵌套条目、
 *       以及 entry 上的 conditions 不展开，只统计最外层的 item 条目，同一物品只显示一次。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class EPCANoteEntryStats {

    private static final Pattern SIZE_PATTERN = Pattern.compile("^(.*_size)(\\d+)$");

    /** 掉落物最多显示这么多条，避免极端数据把一页撑爆 */
    private static final int MAX_DROPS = 24;

    /** 实体 id -> 实体类（用于反查嵌套 Variant 枚举） */
    private static volatile Map<ResourceLocation, Class<?>> entityClasses;

    /** 实体 id -> 体型链（size 升序） */
    private static final Map<ResourceLocation, List<ResourceLocation>> SIZE_CHAIN = new HashMap<>();

    /** 战利品表路径 -> 解析结果（Optional.empty() = 该实体没有战利品表） */
    private static final Map<String, Optional<JsonObject>> LOOT_CACHE = new HashMap<>();

    /** 有 YNA 伤害适应性映射的实体 id（模组自带映射文件的兜底） */
    private static volatile Map<ResourceLocation, ResourceLocation> bundledAdaptationMapping;

    /** 实体 id -> 是否真的会适应（缓存，避免每次重建页面都读配置） */
    private static final Map<ResourceLocation, Boolean> ADAPTATION_CACHE = new HashMap<>();

    private EPCANoteEntryStats() {
    }

    // ======================= 数据模型 =======================

    /** 一条掉落物：图标 + 数量文本（{@code 1} / {@code 0~2} 这种） */
    public static final class Drop {
        public final ItemStack stack;
        public final String countText;

        Drop(ItemStack stack, String countText) {
            this.stack = stack;
            this.countText = countText;
        }
    }

    /** 一个条目的全部详细数据 */
    public static final class Detail {
        public final String health;
        public final String armor;
        public final String attack;
        public final boolean hasAdaptation;
        public final List<String> variants;
        public final List<Drop> drops;

        Detail(String health, String armor, String attack, boolean hasAdaptation,
               List<String> variants, List<Drop> drops) {
            this.health = health;
            this.armor = armor;
            this.attack = attack;
            this.hasAdaptation = hasAdaptation;
            this.variants = variants;
            this.drops = drops;
        }
    }

    // ======================= 对外入口 =======================

    public static Detail detailFor(ResourceLocation entityId) {
        if (entityId == null) {
            return new Detail("0", "0", "0", false, List.of(), List.of());
        }
        List<Double> health = new ArrayList<>();
        List<Double> armor = new ArrayList<>();
        List<Double> attack = new ArrayList<>();
        for (ResourceLocation variantId : sizeChain(entityId)) {
            EntityType<?> type = safeType(variantId);
            AttributeSupplier supplier = supplier(type);
            health.add(baseValue(supplier, Attributes.MAX_HEALTH, 0.0D));
            armor.add(baseValue(supplier, Attributes.ARMOR, 0.0D));
            attack.add(baseValue(supplier, Attributes.ATTACK_DAMAGE, 0.0D));
        }
        return new Detail(
                join(health),
                join(armor),
                join(attack),
                hasAdaptation(entityId),
                variantNames(entityId),
                dropsFor(entityId));
    }

    // ======================= 属性 =======================

    private static EntityType<?> safeType(ResourceLocation id) {
        try {
            return ForgeRegistries.ENTITY_TYPES.getValue(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Forge 把 EntityAttributeCreationEvent 的结果放在 DefaultAttributes 的属性视图里，客户端也能拿到 */
    @SuppressWarnings("unchecked")
    private static AttributeSupplier supplier(EntityType<?> type) {
        if (!(type instanceof EntityType)) {
            return null;
        }
        try {
            return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double baseValue(AttributeSupplier supplier, Attribute attribute, double fallback) {
        if (supplier == null) {
            return fallback;
        }
        try {
            return supplier.hasAttribute(attribute) ? supplier.getBaseValue(attribute) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /** 数值列表 -> "12" 或 "12/20/30"（全部相等时只显示一个） */
    private static String join(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return "0";
        }
        boolean allSame = true;
        double first = values.get(0);
        for (double value : values) {
            if (Math.abs(value - first) > 1.0E-6D) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            return format(first);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(format(values.get(i)));
        }
        return sb.toString();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ======================= 多体型 =======================

    /**
     * 实体 id -&gt; 体型链。{@code epca:infested_slime_size0} 会展开成
     * {@code size0 / size1 / size3}（按数字升序，没有 size2 就跳过）；非体型实体只有自己一个。
     */
    public static List<ResourceLocation> sizeChain(ResourceLocation entityId) {
        if (entityId == null) {
            return List.of();
        }
        List<ResourceLocation> cached = SIZE_CHAIN.get(entityId);
        if (cached != null) {
            return cached;
        }
        List<ResourceLocation> chain = buildSizeChain(entityId);
        SIZE_CHAIN.put(entityId, chain);
        return chain;
    }

    private static List<ResourceLocation> buildSizeChain(ResourceLocation entityId) {
        Matcher matcher = SIZE_PATTERN.matcher(entityId.getPath());
        if (!matcher.matches()) {
            return List.of(entityId);
        }
        String base = matcher.group(1);
        TreeMap<Integer, ResourceLocation> ordered = new TreeMap<>();
        try {
            for (ResourceLocation candidate : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                if (!entityId.getNamespace().equals(candidate.getNamespace())) {
                    continue;
                }
                String path = candidate.getPath();
                if (!path.startsWith(base)) {
                    continue;
                }
                String suffix = path.substring(base.length());
                if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                try {
                    ordered.put(Integer.parseInt(suffix), candidate);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        if (ordered.isEmpty()) {
            return List.of(entityId);
        }
        return List.copyOf(ordered.values());
    }

    // ======================= 伤害适应性 =======================

    /**
     * 该实体是否**真的有**伤害适应性。
     *
     * <p>判定链（都只看工程里已有的 YNA 数据，不另建表）：</p>
     * <ol>
     *   <li>先问 YNA 运行时：{@code EntityAdaptationMapping.getConfigForEntity(entityId)} 拿到配置 id，
     *       再由 {@code DamageAdaptationManager.getConfig(id)} 拿到配置对象
     *       —— 这条正是 {@code AdaptationConfigResolver} / {@code ParasiteEvents#getDamageAdaptationConfig}
     *       走的同一条映射路径；</li>
     *   <li>配置对象在手时看它到底會不會触发适应：{@code adaptation_probability > 0}
     *       <b>或</b> {@code max_adaptable_damage_types > 0} <b>或</b> {@code adaptation_percentage_per_injury > 0}。
     *       只写了 {@code damage} 抗性、{@code damage_adaptability} 全 0 的配置（目前 onesent / infested /
     *       poverty 就是这样）在游戏里永远不会适应，所以算“无”，不显示该行；</li>
     *   <li>YNA 的静态表在专用服务器的客户端侧可能压根没加载，这时读模组自带的
     *       {@code data/yawning_neko_api/entity_adaptation_mappings/entity_mapping.json}
     *       + 对应 {@code damage_adaptation_configs/<id>.json} 自己判一遍；配置文件不在本模组里
     *       （别的模组注册的）时，只能退回“映射存在即算有”。</li>
     * </ol>
     */
    private static boolean hasAdaptation(ResourceLocation entityId) {
        Boolean cached = ADAPTATION_CACHE.get(entityId);
        if (cached != null) {
            return cached;
        }
        boolean result = computeAdaptation(entityId);
        ADAPTATION_CACHE.put(entityId, result);
        return result;
    }

    private static boolean computeAdaptation(ResourceLocation entityId) {
        // 1) YNA 运行时数据（单机 / 服务端同 JVM / 客户端已加载时都能拿到）
        try {
            ResourceLocation configId =
                    org.tdddd.yawning_neko_api.data.EntityAdaptationMapping.getConfigForEntity(entityId);
            if (configId != null) {
                var config = org.tdddd.yawning_neko_api.data.DamageAdaptationManager.getConfig(configId);
                if (config != null) {
                    return grantsAdaptation(config.getAdaptationProbability(),
                            config.getMaxAdaptableDamageTypes(),
                            config.getAdaptationPercentagePerInjury());
                }
            }
        } catch (Throwable ignored) {
            // 走文件兜底
        }

        // 2) 模组自带文件兜底
        ResourceLocation bundledConfig = bundledAdaptationMapping().get(entityId);
        if (bundledConfig == null) {
            return false;
        }
        JsonObject configJson = EPCANoteBundledData.readJsonData(bundledConfig.getNamespace(),
                "damage_adaptation_configs/" + bundledConfig.getPath() + ".json");
        if (configJson == null) {
            // 配置文件不在本模组（别的模组注册的）：只能按“映射存在”算有
            return true;
        }
        JsonElement block = configJson.get("damage_adaptability");
        if (block == null || !block.isJsonObject()) {
            return false;
        }
        JsonObject adaptability = block.getAsJsonObject();
        return grantsAdaptation(
                jsonDouble(adaptability, "damage_adaptability_probability", 0.0D),
                (int) jsonDouble(adaptability, "max_adaptable_damage_types", 0.0D),
                jsonDouble(adaptability, "adaptation_percentage_per_injury", 0.0D));
    }

    /** 配置里的“适应”参数有任意一项非 0 才算真的会适应 */
    private static boolean grantsAdaptation(double probability, int maxAdaptableTypes, double percentagePerInjury) {
        return probability > 0.0D || maxAdaptableTypes > 0 || percentagePerInjury > 0.0D;
    }

    private static double jsonDouble(JsonObject object, String key, double fallback) {
        try {
            JsonElement element = object.get(key);
            return element == null || !element.isJsonPrimitive() ? fallback : element.getAsDouble();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    /** 模组自带的 YNA 实体 -&gt; 适应性配置映射（只读一次） */
    private static Map<ResourceLocation, ResourceLocation> bundledAdaptationMapping() {
        Map<ResourceLocation, ResourceLocation> local = bundledAdaptationMapping;
        if (local != null) {
            return local;
        }
        Map<ResourceLocation, ResourceLocation> parsed = new LinkedHashMap<>();
        JsonObject root = EPCANoteBundledData.readJsonData("yawning_neko_api",
                "entity_adaptation_mappings/entity_mapping.json");
        if (root != null) {
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.startsWith("_")) {
                    continue;
                }
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonObject()) {
                    continue;
                }
                JsonElement configElement = value.getAsJsonObject().get("config");
                if (configElement == null || !configElement.isJsonPrimitive()) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(key);
                ResourceLocation configId = ResourceLocation.tryParse(configElement.getAsString());
                if (id != null && configId != null) {
                    parsed.putIfAbsent(id, configId);
                }
            }
        }
        if (parsed.isEmpty()) {
            epca.LOGGER.debug("[epca] 笔记：读不到 YNA 实体适应性映射文件，伤害适应性判定回退为“无”");
        }
        bundledAdaptationMapping = Collections.unmodifiableMap(parsed);
        return bundledAdaptationMapping;
    }

    // ======================= 变种 =======================

    /**
     * 该实体已有的变种（{@code Variant} 枚举里除 {@code DEFAULT} 以外的常量），
     * 名字走拆分语言表 {@code epca.note.variant.<小写>}，查不到就用常量名原文。
     */
    public static List<String> variantNames(ResourceLocation entityId) {
        Class<?> entityClass = entityClassOf(entityId);
        if (entityClass == null) {
            return List.of();
        }
        for (Class<?> nested : entityClass.getDeclaredClasses()) {
            if (!nested.isEnum() || !"Variant".equals(nested.getSimpleName())) {
                continue;
            }
            List<String> names = new ArrayList<>();
            try {
                for (Object constant : nested.getEnumConstants()) {
                    if (!(constant instanceof Enum<?> value)) {
                        continue;
                    }
                    String name = value.name();
                    if ("DEFAULT".equals(name) || name.isEmpty()) {
                        continue;
                    }
                    names.add(EPCANoteLang.resolveKey(
                            "epca.note.variant." + name.toLowerCase(Locale.ROOT), name));
                }
            } catch (Throwable ignored) {
                return List.of();
            }
            return List.copyOf(names);
        }
        return List.of();
    }

    /** 通过 ModEntities 字段的泛型参数反查实体类（RegistryObject&lt;EntityType&lt;T&gt;&gt; 里的 T） */
    private static Class<?> entityClassOf(ResourceLocation entityId) {
        Map<ResourceLocation, Class<?>> local = entityClasses;
        if (local == null) {
            synchronized (EPCANoteEntryStats.class) {
                local = entityClasses;
                if (local == null) {
                    local = buildEntityClasses();
                    entityClasses = local;
                }
            }
        }
        return local.get(entityId);
    }

    private static Map<ResourceLocation, Class<?>> buildEntityClasses() {
        Map<ResourceLocation, Class<?>> out = new LinkedHashMap<>();
        try {
            for (Field field : ModEntities.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !field.getType().getName().endsWith("RegistryObject")) {
                    continue;
                }
                String className = firstTypeArgument(field);
                if (className == null) {
                    continue;
                }
                try {
                    Object holder = field.get(null);
                    if (!(holder instanceof net.minecraftforge.registries.RegistryObject<?> object)) {
                        continue;
                    }
                    Object value = object.get();
                    if (!(value instanceof EntityType<?> type)) {
                        continue;
                    }
                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
                    if (id == null) {
                        continue;
                    }
                    out.putIfAbsent(id, Class.forName(className));
                } catch (Throwable ignored) {
                    // 某个实体没注册好 / 类找不到就跳过它，不影响其它条目
                }
            }
        } catch (Throwable ignored) {
        }
        return Collections.unmodifiableMap(out);
    }

    private static String firstTypeArgument(Field field) {
        try {
            Type generic = field.getGenericType();
            if (!(generic instanceof ParameterizedType registryType)) {
                return null;
            }
            Type[] args = registryType.getActualTypeArguments();
            if (args.length != 1 || !(args[0] instanceof ParameterizedType entityType)) {
                return null;
            }
            Type[] inner = entityType.getActualTypeArguments();
            if (inner.length != 1) {
                return null;
            }
            return inner[0].getTypeName();
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ======================= 掉落物 =======================

    private static List<Drop> dropsFor(ResourceLocation entityId) {
        JsonObject table = lootTable(entityId.getPath());
        if (table == null) {
            return List.of();
        }
        List<Drop> drops = new ArrayList<>();
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        try {
            JsonElement poolsElement = table.get("pools");
            if (poolsElement == null || !poolsElement.isJsonArray()) {
                return List.of();
            }
            JsonArray pools = poolsElement.getAsJsonArray();
            for (JsonElement poolElement : pools) {
                if (!poolElement.isJsonObject()) {
                    continue;
                }
                JsonElement entriesElement = poolElement.getAsJsonObject().get("entries");
                if (entriesElement == null || !entriesElement.isJsonArray()) {
                    continue;
                }
                for (JsonElement entryElement : entriesElement.getAsJsonArray()) {
                    if (drops.size() >= MAX_DROPS) {
                        return List.copyOf(drops);
                    }
                    if (!entryElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject entry = entryElement.getAsJsonObject();
                    String type = entry.has("type") ? entry.get("type").getAsString() : "minecraft:item";
                    if (!"minecraft:item".equals(type) || !entry.has("name")) {
                        continue;
                    }
                    ResourceLocation itemId = ResourceLocation.tryParse(entry.get("name").getAsString());
                    if (itemId == null || !seen.add(itemId)) {
                        continue;
                    }
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    if (item == null) {
                        continue;
                    }
                    drops.add(new Drop(new ItemStack(item), countText(entry)));
                }
            }
        } catch (Throwable ignored) {
            return List.copyOf(drops);
        }
        return List.copyOf(drops);
    }

    /** {@code minecraft:set_count}：固定值 -&gt; "N"，区间 -&gt; "min~max"；没有该函数就是 1 */
    private static String countText(JsonObject entry) {
        if (!entry.has("functions") || !entry.get("functions").isJsonArray()) {
            return "1";
        }
        for (JsonElement functionElement : entry.getAsJsonArray("functions")) {
            if (!functionElement.isJsonObject()) {
                continue;
            }
            JsonObject function = functionElement.getAsJsonObject();
            if (!function.has("function")
                    || !"minecraft:set_count".equals(function.get("function").getAsString())) {
                continue;
            }
            if (!function.has("count")) {
                return "1";
            }
            JsonElement count = function.get("count");
            if (count.isJsonPrimitive()) {
                try {
                    return Integer.toString(count.getAsInt());
                } catch (Throwable ignored) {
                    return "1";
                }
            }
            if (count.isJsonObject()) {
                JsonObject range = count.getAsJsonObject();
                int min = range.has("min") ? safeInt(range.get("min"), 0) : 0;
                int max = range.has("max") ? safeInt(range.get("max"), min) : min;
                if (min == max) {
                    return Integer.toString(min);
                }
                return min + "~" + max;
            }
        }
        return "1";
    }

    private static int safeInt(JsonElement element, int fallback) {
        try {
            return element == null || !element.isJsonPrimitive() ? fallback : element.getAsInt();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static JsonObject lootTable(String entityPath) {
        String path = "loot_tables/entities/" + entityPath + ".json";
        Optional<JsonObject> cached = LOOT_CACHE.get(path);
        if (cached != null) {
            return cached.orElse(null);
        }
        JsonObject parsed = EPCANoteBundledData.readJson(path);
        LOOT_CACHE.put(path, Optional.ofNullable(parsed));
        return parsed;
    }
}
