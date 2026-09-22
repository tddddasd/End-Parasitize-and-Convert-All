package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;

/**
 * 内置 shader 层清单 + 外部层的注册点。
 *
 * <p>用法：{@code ItemRenderRegistry.attach(item, ItemShaderLayers.CORRUPTION)}。</p>
 *
 * <p>要新增一层（例如电击 electro）：写好 shader 后实现
 * {@link IItemShaderLayer}，然后 {@code ItemShaderLayers.register(new ElectroLayer())}，
 * 之后就能像内置层一样被 {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} 挂到任意物品上。</p>
 */
public final class ItemShaderLayers {

    /** 崩坏层：RGB 色散 + 扫描线 + 故障条带 + 坏点，移植自 RottenRuinsSplendiding。 */
    public static final IItemShaderLayer CORRUPTION = CorruptionLayer.INSTANCE;

    private static final Map<String, IItemShaderLayer> BY_NAME = new LinkedHashMap<>();

    static {
        register(CORRUPTION);
    }

    private ItemShaderLayers() {
    }

    /** 注册一个新层（同名后注册的覆盖先注册的）。 */
    public static void register(IItemShaderLayer layer) {
        if (layer != null && layer.name() != null) {
            BY_NAME.put(layer.name(), layer);
        }
    }

    /** 按名字取层；没注册过返回 {@code null}。 */
    public static IItemShaderLayer byName(String name) {
        return BY_NAME.get(name);
    }

    /** 按 id 取层（{@code epca:item_layer/<name>}）。 */
    public static IItemShaderLayer byId(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        String prefix = "item_layer/";
        return BY_NAME.get(path.startsWith(prefix) ? path.substring(prefix.length()) : path);
    }

    /** 所有已注册的层。 */
    public static Collection<IItemShaderLayer> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }
}
