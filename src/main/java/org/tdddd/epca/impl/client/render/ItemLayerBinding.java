package org.tdddd.epca.impl.client.render;

/**
 * 一次“给某物品挂某个 shader 层”的绑定：层本身 + 该层的参数。
 *
 * <p>{@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} 里存的、渲染时解析出来的都是这个对象。</p>
 */
public record ItemLayerBinding(IItemShaderLayer layer, ItemLayerConfig config) {

    public static ItemLayerBinding of(IItemShaderLayer layer) {
        return new ItemLayerBinding(layer, ItemLayerConfig.DEFAULT);
    }

    public static ItemLayerBinding of(IItemShaderLayer layer, ItemLayerConfig config) {
        return new ItemLayerBinding(layer, config == null ? ItemLayerConfig.DEFAULT : config);
    }
}
