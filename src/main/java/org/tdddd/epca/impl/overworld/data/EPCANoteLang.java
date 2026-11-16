package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.epca;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 笔记专用语言表（拆分语言文件）的加载器与名称解析链。
 *
 * <p>背景：Minecraft 只会自动加载 {@code assets/&lt;ns&gt;/lang/<code>.json}，**不会**加载子目录里的语言文件，
 * 所以笔记专用的语言键放在
 * {@code assets/epca/lang/para_curse_note/zh_cn.json} / {@code en_us.json} 里，必须由本类自己读进来。</p>
 *
 * <p>读取时机：
 * <ul>
 *   <li>客户端资源重载（启动、F3+T、换资源包）时由 {@code RegisterClientReloadListenersEvent}
 *       注册的 reload listener 调用 {@link #reload(ResourceManager)}；</li>
 *   <li>如果重载还没发生就被用到，{@link #find(String)} 会惰性补一次（{@link #reloadIfNeeded()}）。</li>
 * </ul>
 * 优先级（高 -> 低，最终合并成一张 Map）：当前语言 -> en_us -> zh_cn，缺失的键由后面的文件补齐。</p>
 *
 * <p>名称解析链（见 {@link #resolveKey(String, String)}）：
 * 拆分语言表 -> 主语言文件（{@code Language#has} 命中时走 {@code Component.translatable}）-> 调用方给的
 * 兜底值（寄生体用实体 id 原文）。任何一步都取不到也不会崩，最差返回 id 原文。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EPCANoteLang {

    /** 笔记拆分语言所在目录（相对 assets/epca/） */
    public static final String LANG_DIR = "lang/para_curse_note/";

    /** 数据/名称里表示“这是一个语言键”的前缀，沿用 main.json 的既有约定 */
    public static final String LANG_PREFIX = "lang:";

    private static final String FALLBACK_LOCALE = "en_us";
    private static final String SECONDARY_LOCALE = "zh_cn";

    /** 合并后的笔记语言表（不可变；整体替换，读的时候不需要额外同步） */
    private static volatile Map<String, String> table = Collections.emptyMap();
    private static volatile boolean loaded;

    private EPCANoteLang() {
    }

    // ======================= 加载 =======================

    /**
     * 从资源管理器重新读取两份（或三份，含当前语言）拆分语言文件并合并成一张表。
     * 由客户端资源重载调用；资源管理器为空时退化成空表（调用方会自然回退到主语言文件）。
     */
    public static void reload(ResourceManager resourceManager) {
        if (resourceManager == null) {
            table = Collections.emptyMap();
            loaded = false;
            return;
        }

        // 优先级从高到低：当前语言 -> en_us -> zh_cn
        List<String> byPriority = new ArrayList<>(3);
        String selected = selectedLocale();
        if (selected != null && !selected.isEmpty()) {
            byPriority.add(selected);
        }
        if (!byPriority.contains(FALLBACK_LOCALE)) {
            byPriority.add(FALLBACK_LOCALE);
        }
        if (!byPriority.contains(SECONDARY_LOCALE)) {
            byPriority.add(SECONDARY_LOCALE);
        }

        Map<String, String> merged = new LinkedHashMap<>();
        // 反过来写：低优先级的先放，高优先级的后 putAll 覆盖，于是当前语言总是赢
        for (int i = byPriority.size() - 1; i >= 0; i--) {
            merged.putAll(readOne(resourceManager, LANG_DIR + byPriority.get(i) + ".json"));
        }

        table = Collections.unmodifiableMap(merged);
        loaded = true;
    }

    /** 惰性兜底：重载还没跑过就用到了笔记语言时补读一次 */
    public static void reloadIfNeeded() {
        if (loaded) {
            return;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return;
            }
            reload(minecraft.getResourceManager());
        } catch (Throwable ignored) {
            loaded = true;
        }
    }

    private static String selectedLocale() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getLanguageManager() == null) {
                return null;
            }
            return minecraft.getLanguageManager().getSelected();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 读一份拆分语言文件；不存在/解析失败都返回空表，绝不影响游戏 */
    private static Map<String, String> readOne(ResourceManager resourceManager, String path) {
        Map<String, String> out = new LinkedHashMap<>();
        ResourceLocation location = new ResourceLocation(epca.MODID, path);
        try {
            Optional<Resource> resource = resourceManager.getResource(location);
            if (resource.isEmpty()) {
                return out;
            }
            try (InputStream in = resource.get().open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root == null || !root.isJsonObject()) {
                    return out;
                }
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value != null && value.isJsonPrimitive()) {
                        out.put(entry.getKey(), value.getAsString());
                    }
                }
            }
        } catch (Exception exception) {
            epca.LOGGER.warn("[epca] 读取笔记拆分语言失败: {}", path, exception);
        }
        return out;
    }

    // ======================= 查询 =======================

    /** 只查拆分语言表，缺失返回 null（调用方自己决定怎么回退） */
    public static String find(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        reloadIfNeeded();
        String value = table.get(key);
        return value == null || value.isEmpty() ? null : value;
    }

    /** 主语言文件里有没有这个键（有则会返回翻译后的文本，否则 null） */
    public static String mainLangOrNull(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            if (Language.getInstance().has(key)) {
                return Language.getInstance().getOrDefault(key);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * 名称解析链：拆分语言表 -> 主语言文件 -> 兜底值（一般是 id 原文）。
     *
     * @param key      语言键，例如 {@code epca.note.entry.curbug} / {@code entity.epca.curbug}
     * @param fallback 主语言文件也查不到时用的原文；为空则退回 key 本身
     */
    public static String resolveKey(String key, String fallback) {
        String defaultValue = fallback == null || fallback.isEmpty() ? key : fallback;
        if (key == null || key.isEmpty()) {
            return defaultValue == null ? "" : defaultValue;
        }

        String fromNote = find(key);
        if (fromNote != null) {
            return fromNote;
        }

        // 主语言文件：Language#has 命中就走官方解析（会处理语言回退与格式化），
        // 没命中时 Component.translatable(key).getString() 只会原样返回 key，所以我们自己判一次。
        try {
            if (Language.getInstance().has(key)) {
                String translated = Component.translatable(key).getString();
                if (!translated.isEmpty()) {
                    return translated;
                }
            }
            String translated = Component.translatable(key).getString();
            if (translated != null && !translated.isEmpty() && !key.equals(translated)) {
                return translated;
            }
        } catch (Throwable ignored) {
        }
        return defaultValue == null ? "" : defaultValue;
    }

    /**
     * 解析数据里的名字/正文：{@code lang:xxx} 走解析链，其它原样返回。
     * 笔记界面里所有“书签名 / 正文名”都从这里出，保证优先查拆分语言表。
     */
    public static String resolve(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.startsWith(LANG_PREFIX)) {
            String key = raw.substring(LANG_PREFIX.length());
            return resolveKey(key, key);
        }
        return raw;
    }
}
