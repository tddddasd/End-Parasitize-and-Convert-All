package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.epca.impl.overworld.registry.ModItems;
import org.tdddd.epca.impl.overworld.registry.ModTags;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 笔记“可列出的寄生体”目录：种阶（= 上方书签）到寄生体的映射。
 *
 * <p><b>可列出集合的来源</b>（对应需求 D）：直接遍历 {@link ModItems#ITEMS}，凡是 {@link SpawnEggItem}
 * 就取它的实体类型，得到「刷怪蛋 -> 实体类型」映射。<b>同一实体类型被多个刷怪蛋引用时只保留先注册的主条目</b>
 * （例如 {@code infested_spider_spawn_egg} 与 {@code infested_cave_spider_spawn_egg} 都是虫染蜘蛛，
 * 只列一次）；体型变体（{@code living_flesh_size0..4}）因为刷怪蛋本身只绑定
 * 主条目（size0），所以变体自然被排除。</p>
 *
 * <p><b>种阶的推导</b>（对应需求 D 的“分类字段/标签”）：优先用实体标签
 * {@code epca:onesent / epca:infested / epca:poverty / epca:reshape / epca:link}
 * （见 {@link ModTags} 与数据生成器 ItemAndEntityTagsData）。标签不可用时回退到显式映射表
 * {@code data/epca/epca_note/species_tiers.json}（同样列出每个种阶的实体，并决定种阶内顺序）。
 * 两边都没覆盖到的实体（目前只有 {@code biomass_small} / {@code biomass_medium}，它们没有种阶标签）
 * 落到映射表里的 {@code fallback_tier}，并在日志里提示一次。</p>
 *
 * <p>顺序：同一书签内，先按 species_tiers.json 里 {@code entities} 的书写顺序，未列出的再按实体 id 排序，
 * 保证每次打开笔记的列表顺序完全一致。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EPCANoteSpecies {

    /** 显式种阶映射表：data/epca/epca_note/species_tiers.json */
    public static final String FILE_PATH = "epca_note/species_tiers.json";
    private static final String JAR_PATH = "/data/epca/epca_note/species_tiers.json";

    private static final Gson GSON = new GsonBuilder().create();

    /** 五个种阶 id，顺序 = 默认书签顺序 = 标签探测顺序 */
    private static final String[] TIER_ORDER = {"onesent", "infested", "poverty", "reshape", "link"};

    /** 没有种阶标签的实体（如生物质）落到哪个种阶 */
    private static final String DEFAULT_FALLBACK_TIER = "poverty";

    /** 种阶 id -> 实体标签。标签是本模组已有的分类字段，用它推导种阶最可靠 */
    private static final Map<String, TagKey<EntityType<?>>> TIER_TAGS = new LinkedHashMap<>();

    static {
        TIER_TAGS.put("onesent", ModTags.ONESENT);
        TIER_TAGS.put("infested", ModTags.INFESTED);
        TIER_TAGS.put("poverty", ModTags.POVERTY);
        TIER_TAGS.put("reshape", ModTags.RESHAPE);
        TIER_TAGS.put("link", ModTags.LINK);
    }

    private static volatile List<Tier> tiers;
    private static volatile boolean warnedUnknown;

    private EPCANoteSpecies() {
    }

    // ======================= 数据模型 =======================

    /** 一个可列出的寄生体（= 一个独立刷怪蛋的主条目） */
    public static final class Entry {
        public final ResourceLocation entityId;

        private Entry(ResourceLocation entityId) {
            this.entityId = entityId;
        }

        public String path() {
            return entityId.getPath();
        }

        /** 笔记里的条目名（正文名/跳转条目名）解析链：拆分语言 -> 实体语言键 -> id 原文 */
        public String displayName() {
            String entityKey = "entity." + entityId.getNamespace() + "." + entityId.getPath();
            String entityName = EPCANoteLang.resolveKey(entityKey, entityId.getPath());
            return EPCANoteLang.resolveKey("epca.note.entry." + entityId.getPath(), entityName);
        }

        /** 可选的正文内容键（有就画在名字下面，没有就只画名字） */
        public String bodyKey() {
            return "epca.note.entry." + entityId.getPath() + ".body";
        }
    }

    /** 一个种阶（对应上方一个书签） */
    public static final class Tier {
        public final String id;
        /** 绑定的 child_tab id（默认与 id 相同，可在 species_tiers.json 里覆盖） */
        public final String childTabId;
        public final List<Entry> entries;

        private Tier(String id, String childTabId, List<Entry> entries) {
            this.id = id;
            this.childTabId = childTabId;
            this.entries = Collections.unmodifiableList(entries);
        }
    }

    // ======================= 对外查询 =======================

    public static List<Tier> tiers() {
        List<Tier> local = tiers;
        if (local == null) {
            synchronized (EPCANoteSpecies.class) {
                local = tiers;
                if (local == null) {
                    local = build();
                    tiers = local;
                }
            }
        }
        return local;
    }

    /** 按 child_tab id 找种阶；找不到返回 null（此时笔记回退到 main.json 里的纯文本 content） */
    public static Tier tierForChildTab(String childTabId) {
        if (childTabId == null || childTabId.isEmpty()) {
            return null;
        }
        for (Tier tier : tiers()) {
            if (childTabId.equals(tier.childTabId) || childTabId.equals(tier.id)) {
                return tier;
            }
        }
        return null;
    }

    // ======================= 构建 =======================

    private static List<Tier> build() {
        TierFile file = readFile();
        List<TierDef> defs = file != null && file.tiers != null && !file.tiers.isEmpty()
                ? file.tiers
                : defaultDefs();
        String fallbackTier = file != null && file.fallback_tier != null && !file.fallback_tier.isEmpty()
                ? file.fallback_tier
                : DEFAULT_FALLBACK_TIER;

        // 规范化：保序、去重、去空 id
        Map<String, TierDef> defById = new LinkedHashMap<>();
        for (TierDef def : defs) {
            if (def == null || def.id == null || def.id.isEmpty()) {
                continue;
            }
            defById.putIfAbsent(def.id, def);
        }
        if (defById.isEmpty()) {
            for (TierDef def : defaultDefs()) {
                defById.put(def.id, def);
            }
        }
        if (!defById.containsKey(fallbackTier)) {
            fallbackTier = defById.keySet().iterator().next();
        }

        // 显式映射表里的顺序（也用来判断“这个实体属于哪个种阶”的兜底）
        Map<String, Map<ResourceLocation, Integer>> orderOf = new HashMap<>();
        Map<ResourceLocation, String> explicitTier = new HashMap<>();
        for (TierDef def : defById.values()) {
            Map<ResourceLocation, Integer> order = new HashMap<>();
            if (def.entities != null) {
                int index = 0;
                for (String raw : def.entities) {
                    ResourceLocation entityId = parseEntityId(raw);
                    if (entityId == null) {
                        continue;
                    }
                    order.putIfAbsent(entityId, index++);
                    explicitTier.putIfAbsent(entityId, def.id);
                }
            }
            orderOf.put(def.id, order);
        }

        Map<String, List<Entry>> buckets = new LinkedHashMap<>();
        for (String id : defById.keySet()) {
            buckets.put(id, new ArrayList<>());
        }

        Set<String> knownTiers = new HashSet<>(defById.keySet());
        for (Entry entry : collectListable().values()) {
            String tierId = tierByTag(entry.entityId, knownTiers);
            if (tierId == null) {
                tierId = explicitTier.get(entry.entityId);
            }
            if (tierId == null) {
                tierId = fallbackTier;
                if (!warnedUnknown) {
                    warnedUnknown = true;
                    org.tdddd.epca.impl.epca.LOGGER.info(
                            "[epca] 寄生体 {} 既没有种阶标签也不在 species_tiers.json 里，归入 {}",
                            entry.entityId, fallbackTier);
                }
            }
            List<Entry> bucket = buckets.get(tierId);
            if (bucket == null) {
                bucket = buckets.get(fallbackTier);
            }
            bucket.add(entry);
        }

        List<Tier> result = new ArrayList<>(defById.size());
        for (Map.Entry<String, TierDef> defEntry : defById.entrySet()) {
            String id = defEntry.getKey();
            TierDef def = defEntry.getValue();
            String childTabId = def.child_tab == null || def.child_tab.isEmpty() ? id : def.child_tab;
            List<Entry> entries = buckets.get(id);
            Map<ResourceLocation, Integer> order = orderOf.getOrDefault(id, Collections.emptyMap());
            // 先按显式表顺序，再按实体 id：未列出的排到后面且顺序稳定
            entries.sort(Comparator
                    .comparingInt((Entry entry) -> {
                        Integer index = order.get(entry.entityId);
                        return index == null ? Integer.MAX_VALUE : index;
                    })
                    .thenComparing(entry -> entry.entityId.toString()));
            result.add(new Tier(id, childTabId, entries));
        }
        logTierSummary(result);
        return Collections.unmodifiableList(result);
    }

    /** 用实体标签推导种阶；标签未同步/查不到时返回 null（交给显式映射表） */
    private static String tierByTag(ResourceLocation entityId, Set<String> knownTiers) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null) {
            return null;
        }
        for (Map.Entry<String, TagKey<EntityType<?>>> tagEntry : TIER_TAGS.entrySet()) {
            if (!knownTiers.contains(tagEntry.getKey())) {
                continue;
            }
            try {
                if (type.is(tagEntry.getValue())) {
                    return tagEntry.getKey();
                }
            } catch (Throwable ignored) {
                // 标签还没绑定（例如极早期调用）时忽略，继续看下一个 / 回退到显式表
            }
        }
        return null;
    }

    /**
     * 遍历 {@link ModItems#ITEMS} 收集「刷怪蛋 -> 实体类型」映射。
     * 同一实体类型出现多次（变种蛋、同蛋不同体型）时只保留先注册的主条目。
     */
    @SuppressWarnings("deprecation")
    private static Map<ResourceLocation, Entry> collectListable() {
        Map<ResourceLocation, Entry> out = new LinkedHashMap<>();
        boolean debug = org.tdddd.epca.impl.epca.LOGGER.isDebugEnabled();
        try {
            for (RegistryObject<Item> holder : ModItems.ITEMS.getEntries()) {
                Item item;
                try {
                    item = holder.get();
                } catch (Throwable ignored) {
                    continue;
                }
                if (!(item instanceof SpawnEggItem egg)) {
                    continue;
                }
                EntityType<?> type;
                try {
                    type = egg.getType(null);
                } catch (Throwable ignored) {
                    continue;
                }
                if (type == null) {
                    if (debug) {
                        org.tdddd.epca.impl.epca.LOGGER.debug(
                                "[epca] 笔记可列出：刷怪蛋 {} 的 getType(null) 为 null，跳过", holder.getId());
                    }
                    continue;
                }
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (entityId == null) {
                    continue;
                }
                if (out.containsKey(entityId)) {
                    // 变体蛋（例如洞穴蜘蛛蛋 -> infested_spider）会指向已收录的主条目，按规则只保留先注册的那条
                    if (debug) {
                        org.tdddd.epca.impl.epca.LOGGER.debug(
                                "[epca] 笔记可列出：刷怪蛋 {} -> {}，该实体已被更早的刷怪蛋收录，按变体规则跳过",
                                holder.getId(), entityId);
                    }
                    continue;
                }
                out.put(entityId, new Entry(entityId));
                if (debug) {
                    org.tdddd.epca.impl.epca.LOGGER.debug(
                            "[epca] 笔记可列出：刷怪蛋 {} -> {}", holder.getId(), entityId);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /** DEBUG：输出每个种阶最终的实体 id 列表与条目数，方便核对（默认日志级别不会打印） */
    private static void logTierSummary(List<Tier> built) {
        if (!org.tdddd.epca.impl.epca.LOGGER.isDebugEnabled()) {
            return;
        }
        for (Tier tier : built) {
            List<ResourceLocation> ids = new ArrayList<>(tier.entries.size());
            for (Entry entry : tier.entries) {
                ids.add(entry.entityId);
            }
            org.tdddd.epca.impl.epca.LOGGER.debug("[epca] 笔记种阶 {}（child_tab={}）共 {} 条：{}",
                    tier.id, tier.childTabId, tier.entries.size(), ids);
        }
    }

    private static ResourceLocation parseEntityId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String value = raw.indexOf(':') >= 0 ? raw : "epca:" + raw;
        return ResourceLocation.tryParse(value);
    }

    // ======================= species_tiers.json =======================

    private static TierFile readFile() {
        // 1) 客户端资源管理器（允许资源包覆盖：把同名文件放到 assets/epca/epca_note/ 即可）
        //    注意 data/ 不在客户端资源包（CLIENT_RESOURCES）的搜索范围内，所以下面还要直接读模组文件
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getResourceManager() != null) {
                Optional<Resource> resource = minecraft.getResourceManager()
                        .getResource(new ResourceLocation(org.tdddd.epca.impl.epca.MODID, FILE_PATH));
                if (resource.isPresent()) {
                    try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                        TierFile parsed = GSON.fromJson(reader, TierFile.class);
                        if (parsed != null && parsed.tiers != null) {
                            return parsed;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 2) 模组 jar 内的 data/epca/epca_note/species_tiers.json（走 Forge 的模组文件系统，开发环境同样可用）
        try (InputStream in = openFromModFile()) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    TierFile parsed = GSON.fromJson(reader, TierFile.class);
                    if (parsed != null && parsed.tiers != null) {
                        return parsed;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 3) 兜底：直接按 classpath 资源读（某些环境下模组文件系统拿不到路径）
        try (InputStream in = EPCANoteSpecies.class.getResourceAsStream(JAR_PATH)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    TierFile parsed = GSON.fromJson(reader, TierFile.class);
                    if (parsed != null && parsed.tiers != null) {
                        return parsed;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static InputStream openFromModFile() {
        try {
            var modFileInfo = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList()
                    .getModFileById(org.tdddd.epca.impl.epca.MODID);
            if (modFileInfo == null) {
                return null;
            }
            java.nio.file.Path path = modFileInfo.getFile().findResource("data/epca/epca_note/species_tiers.json");
            if (path == null || !java.nio.file.Files.exists(path)) {
                return null;
            }
            return java.nio.file.Files.newInputStream(path);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 映射表读不到时的兜底：五个种阶空表（种阶内分组仍能靠标签完成） */
    private static List<TierDef> defaultDefs() {
        List<TierDef> defs = new ArrayList<>(TIER_ORDER.length);
        for (String id : TIER_ORDER) {
            TierDef def = new TierDef();
            def.id = id;
            def.child_tab = id;
            defs.add(def);
        }
        return defs;
    }

    private static final class TierFile {
        String fallback_tier;
        List<TierDef> tiers;
    }

    private static final class TierDef {
        String id;
        String child_tab;
        List<String> entities;
    }
}
