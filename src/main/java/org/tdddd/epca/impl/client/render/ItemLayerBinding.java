package org.tdddd.epca.impl.client.render;

/**
 *  shader  +
 *
 * <p>{@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} </p>
 */
public record ItemLayerBinding(IItemShaderLayer layer, ItemLayerConfig config) {

    public static ItemLayerBinding of(IItemShaderLayer layer) {
        return new ItemLayerBinding(layer, ItemLayerConfig.DEFAULT);
    }

    public static ItemLayerBinding of(IItemShaderLayer layer, ItemLayerConfig config) {
        return new ItemLayerBinding(layer, config == null ? ItemLayerConfig.DEFAULT : config);
    }
}

