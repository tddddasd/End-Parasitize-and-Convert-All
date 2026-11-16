package org.tdddd.epca.impl.overworld.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.ModList;
import org.tdddd.epca.impl.epca;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 笔记读取模组自带 {@code data/...} 资源的小工具。
 *
 * <p>背景：{@code data/} 目录不在客户端资源包（{@code assets/}）的搜索范围内，
 * {@code Minecraft#getResourceManager()} 拿不到它；而战利品表 / 消耗表这类数据在专用服务器上
 * 又只存在于服务端数据包。所以这里提供两条读取路径：</p>
 *
 * <ul>
 *   <li>{@link #open} / {@link #readJson}：直接从**模组文件系统**（或 classpath）读 jar 内
 *       {@code data/<namespace>/<path>} —— 客户端用它渲染笔记（与 {@code EPCANoteSpecies}
 *       读 species_tiers.json 的做法一致）；</li>
 *   <li>服务端需要遵守数据包覆盖时走 {@code MinecraftServer#getResourceManager()}
 *       （见 {@link EPCANoteDetailCosts}），本类只管“模组自带的那一份”。</li>
 * </ul>
 *
 * <p>所有方法都不抛异常：读不到就返回 {@code null}，调用方自行回退。</p>
 *
 * <p><b>26.1.2 移植说明</b>（相对 1.20.1）：</p>
 * <ul>
 *   <li>1.20.1 用 {@code FMLLoader.getLoadingModList().getModFileById(id).getFile().findResource(full)}
 *       拿到 jar 内路径；26 的 {@code IModFile} 已经没有 {@code findResource}，
 *       改成 {@code ModList.get().getModFileById(id).getFile().getContents()}
 *       返回的 {@code JarContents}：先 {@code containsFile(full)} 探测，再 {@code openFile(full)} 打开流。
 *       开发环境（build/resources/main 目录）同样可用，因为 FML 会把目录也包成 JarContents。</li>
 *   <li>{@code ResourceLocation} -&gt; {@code Identifier}（本类其实不用 id）。</li>
 * </ul>
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

        // 1) FML 的模组文件系统：开发环境 / 正式环境都能拿到 jar 内路径
        try {
            var modFileInfo = ModList.get().getModFileById(epca.MODID);
            if (modFileInfo != null) {
                var contents = modFileInfo.getFile().getContents();
                if (contents != null && contents.containsFile(full)) {
                    return contents.openFile(full);
                }
            }
        } catch (Throwable ignored) {
        }

        // 2) 模组主路径上的真实文件（开发环境 build/resources/main，或解包运行的目录）
        try {
            var modFileInfo = ModList.get().getModFileById(epca.MODID);
            if (modFileInfo != null) {
                java.nio.file.Path found = modFileInfo.getFile().getFilePath().resolve(full);
                if (java.nio.file.Files.isRegularFile(found)) {
                    return java.nio.file.Files.newInputStream(found);
                }
            }
        } catch (Throwable ignored) {
        }

        // 3) classpath 兜底（某些环境下模组文件系统拿不到路径）
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
