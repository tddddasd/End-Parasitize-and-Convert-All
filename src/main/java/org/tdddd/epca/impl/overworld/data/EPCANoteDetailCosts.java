package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.epca;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 虫咒笔记「详细数据」解锁消耗表：{@code data/epca/epca_note/detail_costs.json}。
 *
 * <p>文件结构（实体 id -&gt; 材料数组）：</p>
 * <pre>
 * {
 *   "_default": [ { "item": "epca:parasite_viscera", "count": 3 } ],
 *   "epca:curbug": [ { "item": "epca:parasite_viscera", "count": 3 } ],
 *   ...
 * }
 * </pre>
 *
 * <p>以 {@code _} 开头的键是注释 / 兜底，不作为实体 id。
 * 表中没有的实体回退到 {@code _default}（再没有就是 3 × 寄体内脏），
 * 这样以后加新条目不会因为忘了写消耗而变成“免费解锁”。</p>
 *
 * <p><b>两侧都读同一份文件，但读取路径不同</b>：</p>
 * <ul>
 *   <li>服务端优先走 {@link MinecraftServer#getResourceManager()}（尊重数据包覆盖，校验以它为准）；</li>
 *   <li>客户端走模组文件系统 / classpath（{@link EPCANoteBundledData}），只用于显示消耗行与 tooltip。</li>
 * </ul>
 * 服务端永远是最终裁判：材料不够时点击不会解锁，客户端只等 S2C 同步结果。
 */
public final class EPCANoteDetailCosts {

    /** 相对 data/epca/ 的路径 */
    public static final String FILE_PATH = "epca_note/detail_costs.json";

    /** 表里完全没有条目时的兜底消耗 */
    private static final ResourceLocation FALLBACK_ITEM = new ResourceLocation(epca.MODID, "parasite_viscera");
    private static final int FALLBACK_COUNT = 3;

    /** 实体 id -> 材料列表（不可变） */
    private static volatile Map<ResourceLocation, List<Cost>> table;
    private static volatile List<Cost> defaults;

    private EPCANoteDetailCosts() {
    }

    /** 一种材料需求 */
    public record Cost(ResourceLocation itemId, int count) {

        /** 物品的显示名（未注册时退回 id 原文），用于「消耗 N × 名称」与 tooltip */
        public String displayName() {
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                return itemId.toString();
            }
            try {
                return new ItemStack(item).getHoverName().getString();
            } catch (Throwable ignored) {
                return itemId.toString();
            }
        }

        public ItemStack stack(int stackSize) {
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, Math.max(1, Math.min(stackSize, item.getMaxStackSize())));
        }
    }

    // ======================= 查询 =======================

    /** 该实体的解锁消耗（永远非空；表里没有时返回默认消耗） */
    public static List<Cost> costsFor(ResourceLocation entityId) {
        Map<ResourceLocation, List<Cost>> local = ensureLoaded();
        if (entityId != null) {
            List<Cost> costs = local.get(entityId);
            if (costs != null && !costs.isEmpty()) {
                return costs;
            }
        }
        List<Cost> fallback = defaults;
        if (fallback != null && !fallback.isEmpty()) {
            return fallback;
        }
        return List.of(new Cost(FALLBACK_ITEM, FALLBACK_COUNT));
    }

    /** 表里是否**显式**写了这个实体的消耗（服务端校验用它判断“这个实体配不配解锁”） */
    public static boolean hasExplicitEntry(ResourceLocation entityId) {
        return entityId != null && ensureLoaded().containsKey(entityId);
    }

    /** 模组注册过的实体类型（服务端校验第一道闸：只认已注册的实体 id） */
    public static boolean isRegisteredEntity(ResourceLocation entityId) {
        if (entityId == null) {
            return false;
        }
        try {
            return ForgeRegistries.ENTITY_TYPES.containsKey(entityId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 重新读取（服务器启动 / 数据包重载后调用） */
    public static void invalidate() {
        table = null;
        defaults = null;
    }

    // ======================= 读取 =======================

    private static Map<ResourceLocation, List<Cost>> ensureLoaded() {
        Map<ResourceLocation, List<Cost>> local = table;
        if (local != null) {
            return local;
        }
        synchronized (EPCANoteDetailCosts.class) {
            local = table;
            if (local != null) {
                return local;
            }
            load();
            local = table;
            return local == null ? Map.of() : local;
        }
    }

    private static void load() {
        JsonObject root = readServerSide();
        if (root == null) {
            root = EPCANoteBundledData.readJson(FILE_PATH);
        }
        Map<ResourceLocation, List<Cost>> parsed = new LinkedHashMap<>();
        List<Cost> fallback = new ArrayList<>();
        if (root != null) {
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.startsWith("_")) {
                    if ("_default".equals(key)) {
                        fallback = parseCosts(entry.getValue());
                    }
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) {
                    continue;
                }
                List<Cost> costs = parseCosts(entry.getValue());
                if (!costs.isEmpty()) {
                    parsed.put(id, List.copyOf(costs));
                }
            }
        }
        if (parsed.isEmpty()) {
            epca.LOGGER.warn("[epca] 笔记消耗表 {} 读取失败或为空，全部条目回退到默认消耗", FILE_PATH);
        }
        defaults = fallback.isEmpty() ? List.of(new Cost(FALLBACK_ITEM, FALLBACK_COUNT)) : List.copyOf(fallback);
        table = Collections.unmodifiableMap(parsed);
    }

    /** 服务端优先读数据包（尊重覆盖）；不在服务端时返回 null，交给模组自带文件 */
    private static JsonObject readServerSide() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return null;
            }
            Optional<Resource> resource = server.getResourceManager()
                    .getResource(new ResourceLocation(epca.MODID, FILE_PATH));
            if (resource.isEmpty()) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonElement element = com.google.gson.JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Cost> parseCosts(JsonElement element) {
        List<Cost> out = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return out;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement item : array) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject obj = item.getAsJsonObject();
            String itemId = obj.has("item") ? obj.get("item").getAsString() : null;
            ResourceLocation id = itemId == null ? null : ResourceLocation.tryParse(itemId);
            if (id == null) {
                continue;
            }
            int count = 1;
            if (obj.has("count") && obj.get("count").isJsonPrimitive()) {
                try {
                    count = obj.get("count").getAsInt();
                } catch (Throwable ignored) {
                    count = 1;
                }
            }
            if (count <= 0) {
                continue;
            }
            out.add(new Cost(id, count));
        }
        return out;
    }
}
