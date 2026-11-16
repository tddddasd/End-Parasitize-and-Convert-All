package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.tdddd.epca.impl.epca;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 笔记读取模组自带 {@code data/...} 资源的小工具。
 *
 * <p>背景：{@code data/} 目录不在客户端资源包（{@code assets/}）的搜索范围内，
 * {@code Minecraft#getResourceManager()} 拿不到它；而战利品表 / 消耗表这类数据在专用服务器上
 * 又只存在于服务端数据包。所以这里提供两条读取路径：</p>
 *
 * <ul>
 *   <li>{@link #openData} / {@link #readJson}：直接从**模组文件系统**（或 classpath）读 jar 内
 *       {@code data/<namespace>/<path>} —— 客户端用它渲染笔记（与 {@code EPCANoteSpecies}
 *       读 species_tiers.json 的做法一致）；</li>
 *   <li>服务端需要遵守数据包覆盖时走 {@code MinecraftServer#getResourceManager()}
 *       （见 {@link EPCANoteDetailCosts}），本类只管“模组自带的那一份”。</li>
 * </ul>
 *
 * <p>所有方法都不抛异常：读不到就返回 {@code null}，调用方自行回退。</p>
 */
public final class EPCANoteBundledData {

    private EPCANoteBundledData() {
    }

    /** 读本模组命名空间下的数据文件（默认 namespace = {@code epca}） */
    public static InputStream open(String pathUnderDataEpca) {
        return openData(epca.MODID, pathUnderDataEpca);
    }

    /**
     * 打开模组自带的任意数据文件。
     *
     * @param namespace 数据命名空间，例如 {@code epca} / {@code yawning_neko_api}
     * @param path      相对 {@code data/<namespace>/} 的路径，例如 {@code epca_note/detail_costs.json}
     * @return 输入流；读不到返回 {@code null}（调用方负责 close）
     */
    public static InputStream openData(String namespace, String path) {
        if (namespace == null || namespace.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }
        String full = "data/" + namespace + "/" + path;
        // 1) Forge 的模组文件系统：开发环境 / 正式环境都能拿到 jar 内路径
        try {
            var modFileInfo = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList()
                    .getModFileById(epca.MODID);
            if (modFileInfo != null) {
                Path found = modFileInfo.getFile().findResource(full);
                if (found != null && Files.exists(found)) {
                    return Files.newInputStream(found);
                }
            }
        } catch (Throwable ignored) {
        }
        // 2) classpath 兜底（某些环境下模组文件系统拿不到路径）
        try {
            return EPCANoteBundledData.class.getResourceAsStream("/" + full);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 读成 JSON 对象；读不到或解析失败返回 {@code null} */
    public static JsonObject readJson(String pathUnderDataEpca) {
        return readJsonData(epca.MODID, pathUnderDataEpca);
    }

    /** 读成 JSON 对象（指定命名空间）；读不到或解析失败返回 {@code null} */
    public static JsonObject readJsonData(String namespace, String path) {
        try (InputStream in = openData(namespace, path)) {
            if (in == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }
}
