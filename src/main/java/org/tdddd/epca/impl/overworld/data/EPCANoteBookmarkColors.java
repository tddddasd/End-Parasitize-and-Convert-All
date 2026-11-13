package org.tdddd.epca.impl.overworld.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 「虫咒笔记」书签颜色配置（纯客户端）。
 *
 * 文件：config/epca_note_colors.json
 * 结构：
 * {
 *   "bookmarks": { "top": "yellow", "left": "light_blue" },
 *   "colors": {
 *     "white": "#E9ECEC",
 *     "orange": "#F07613",
 *     ...
 *   }
 * }
 *
 * bookmarks.top / bookmarks.left 可以填颜色名（见 colors 表，或下面 16 种默认染料色），
 * 也可以直接填 "#RRGGBB"。colors 表可以覆盖默认颜色或新增自定义名字。
 * 文件不存在时自动写出默认文件；解析失败或颜色缺失时回退到 黄色 / 浅蓝色，不会抛异常。
 * 修改后重新打开笔记界面即可生效（每次打开界面会检查文件修改时间）。
 *
 * 兼容性：bookmarks.top 的旧默认值是 white，新默认值是 yellow。
 * 因此读到「top 恰好等于 white」的旧配置时会自动改写成 yellow 并写回文件（同时记一条 info 日志），
 * 用户自己填的其它颜色（包括自定义 #RRGGBB）一律不动。
 *
 * 26.1.2 移植说明：与 1.20.1 版逻辑完全一致，只有两处平台差异——
 *   * FMLPaths 的包名为 net.neoforged.fml.loading（1.20.1 为 net.minecraftforge.fml.loading）；
 *   * Dist / OnlyIn 来自 net.neoforged.api.distmarker。
 */
@OnlyIn(Dist.CLIENT)
public final class EPCANoteBookmarkColors {

    public static final String FILE_NAME = "epca_note_colors.json";

    /** 解析失败 / 颜色名不存在时的回退色（与默认值保持一致） */
    public static final int FALLBACK_TOP = 0xF8C627;  // yellow
    public static final int FALLBACK_LEFT = 0x3AAFD9; // light_blue

    public static final String DEFAULT_TOP_NAME = "yellow";
    public static final String DEFAULT_LEFT_NAME = "light_blue";

    /** 旧版本的 bookmarks.top 默认值：读到它就自动迁移成 DEFAULT_TOP_NAME */
    public static final String LEGACY_DEFAULT_TOP_NAME = "white";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** 16 种默认颜色，对应 Minecraft 的 16 种染料色 */
    private static final Map<String, Integer> DEFAULT_COLORS = new LinkedHashMap<>();

    static {
        DEFAULT_COLORS.put("white", 0xE9ECEC);
        DEFAULT_COLORS.put("orange", 0xF07613);
        DEFAULT_COLORS.put("magenta", 0xBD44B3);
        DEFAULT_COLORS.put("light_blue", 0x3AAFD9);
        DEFAULT_COLORS.put("yellow", 0xF8C627);
        DEFAULT_COLORS.put("lime", 0x70B919);
        DEFAULT_COLORS.put("pink", 0xED8DAC);
        DEFAULT_COLORS.put("gray", 0x3E4447);
        DEFAULT_COLORS.put("light_gray", 0x8E8E86);
        DEFAULT_COLORS.put("cyan", 0x158991);
        DEFAULT_COLORS.put("purple", 0x792AAC);
        DEFAULT_COLORS.put("blue", 0x35399D);
        DEFAULT_COLORS.put("brown", 0x724728);
        DEFAULT_COLORS.put("green", 0x546D1B);
        DEFAULT_COLORS.put("red", 0xA12722);
        DEFAULT_COLORS.put("black", 0x141519);
    }

    private static int topColor = FALLBACK_TOP;
    private static int leftColor = FALLBACK_LEFT;
    private static long lastModified = Long.MIN_VALUE;
    private static boolean loaded = false;

    private EPCANoteBookmarkColors() {
    }

    /** 上方书签颜色（0xRRGGBB） */
    public static int getTopColor() {
        return topColor;
    }

    /** 左侧书签颜色（0xRRGGBB） */
    public static int getLeftColor() {
        return leftColor;
    }

    /** 颜色名 -> 颜色 的默认表（只读副本） */
    public static Map<String, Integer> defaultColors() {
        return new LinkedHashMap<>(DEFAULT_COLORS);
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    /**
     * 检查文件是否变化并重新读取；每次打开笔记界面调用一次即可。
     * 任何 IO / 解析异常都只记录日志并回退到默认色。
     */
    public static synchronized void loadIfChanged() {
        Path path = configPath();
        try {
            if (!Files.exists(path)) {
                writeDefaultFile(path);
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (loaded && modified == lastModified) {
                return;
            }
            // 先做旧默认值迁移（可能会重写文件），再解析，保证本次打开界面就用上新默认色
            migrateLegacyTopDefault(path);
            parseFile(path);
            lastModified = Files.getLastModifiedTime(path).toMillis();
            loaded = true;
        } catch (Exception e) {
            LOGGER.warn("[epca] 读取书签颜色配置失败，使用默认颜色: {}", e.toString());
            resetToFallback();
        }
    }

    /**
     * 旧版本 bookmarks.top 的默认值是 white，新默认值是 yellow。
     * 只有当文件里的 top 确实等于旧默认值 white 时才改写成 yellow 并写回文件，
     * 用户在 colors 表里的自定义颜色、bookmarks.left 以及其它键全部原样保留。
     */
    private static void migrateLegacyTopDefault(Path path) {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            root = parsed.getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("[epca] 书签颜色配置读取失败，跳过旧默认值迁移: {}", e.toString());
            return;
        }

        JsonObject bookmarks = asObject(root.get("bookmarks"));
        if (bookmarks == null || !bookmarks.has("top") || !bookmarks.get("top").isJsonPrimitive()) {
            return;
        }
        String current = bookmarks.get("top").getAsString();
        if (current == null || !current.trim().equalsIgnoreCase(LEGACY_DEFAULT_TOP_NAME)) {
            return;
        }

        bookmarks.addProperty("top", DEFAULT_TOP_NAME);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
            LOGGER.info("[epca] 书签颜色配置: bookmarks.top 的旧默认值 \"{}\" 已自动迁移为 \"{}\"（{}）；如需其它颜色请直接改 {}",
                    current, DEFAULT_TOP_NAME, path, FILE_NAME);
        } catch (Exception e) {
            LOGGER.warn("[epca] 书签颜色配置旧默认值迁移写回失败（本次仍按 {} 显示）: {}", DEFAULT_TOP_NAME, e.toString());
        }
    }

    private static void parseFile(Path path) {
        Map<String, Integer> palette = new LinkedHashMap<>(DEFAULT_COLORS);
        String topName = DEFAULT_TOP_NAME;
        String leftName = DEFAULT_LEFT_NAME;

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null && root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();

                JsonObject colors = asObject(obj.get("colors"));
                if (colors != null) {
                    for (Map.Entry<String, JsonElement> entry : colors.entrySet()) {
                        int parsed = parseColorElement(entry.getValue());
                        if (parsed >= 0) {
                            palette.put(entry.getKey().toLowerCase(Locale.ROOT), parsed);
                        } else {
                            LOGGER.warn("[epca] 书签颜色配置: 无法解析颜色 {} = {}, 已忽略",
                                    entry.getKey(), entry.getValue());
                        }
                    }
                }

                JsonObject bookmarks = asObject(obj.get("bookmarks"));
                if (bookmarks != null) {
                    if (bookmarks.has("top") && bookmarks.get("top").isJsonPrimitive()) {
                        topName = bookmarks.get("top").getAsString();
                    }
                    if (bookmarks.has("left") && bookmarks.get("left").isJsonPrimitive()) {
                        leftName = bookmarks.get("left").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[epca] 书签颜色配置解析失败，使用默认颜色: {}", e.toString());
            resetToFallback();
            return;
        }

        topColor = resolve(topName, palette, FALLBACK_TOP, "top");
        leftColor = resolve(leftName, palette, FALLBACK_LEFT, "left");
        LOGGER.info("[epca] 书签颜色: top=#{}, left=#{}",
                String.format("%06X", topColor), String.format("%06X", leftColor));
    }

    private static void resetToFallback() {
        topColor = FALLBACK_TOP;
        leftColor = FALLBACK_LEFT;
    }

    private static JsonObject asObject(JsonElement element) {
        return (element != null && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }

    private static int resolve(String value, Map<String, Integer> palette, int fallback, String which) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        if (trimmed.startsWith("#")) {
            int parsed = parseHex(trimmed);
            if (parsed >= 0) {
                return parsed;
            }
            LOGGER.warn("[epca] 书签颜色配置: bookmarks.{} 的 \"{}\" 不是合法的 #RRGGBB，回退到默认色", which, trimmed);
            return fallback;
        }
        Integer color = palette.get(trimmed.toLowerCase(Locale.ROOT));
        if (color != null) {
            return color;
        }
        LOGGER.warn("[epca] 书签颜色配置: bookmarks.{} 引用了未知颜色名 \"{}\"，回退到默认色", which, trimmed);
        return fallback;
    }

    private static int parseColorElement(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return -1;
        }
        return parseHex(element.getAsString());
    }

    /** 支持 "#RRGGBB" 和 "#AARRGGBB"（alpha 会被丢弃，书签始终不透明） */
    private static int parseHex(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            if (s.length() == 6) {
                return Integer.parseInt(s, 16) & 0xFFFFFF;
            }
            if (s.length() == 8) {
                return Integer.parseInt(s, 16) & 0xFFFFFF;
            }
        } catch (NumberFormatException ignored) {
            return -1;
        }
        return -1;
    }

    private static void writeDefaultFile(Path path) throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "书签颜色: bookmarks.top / bookmarks.left 填颜色名或 #RRGGBB，可用的颜色名见下方 colors 表");

        JsonObject bookmarks = new JsonObject();
        bookmarks.addProperty("top", DEFAULT_TOP_NAME);
        bookmarks.addProperty("left", DEFAULT_LEFT_NAME);
        root.add("bookmarks", bookmarks);

        JsonObject colors = new JsonObject();
        for (Map.Entry<String, Integer> entry : DEFAULT_COLORS.entrySet()) {
            colors.addProperty(entry.getKey(), toHex(entry.getValue()));
        }
        root.add("colors", colors);

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        LOGGER.info("[epca] 已生成书签颜色配置文件: {}", path);
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
