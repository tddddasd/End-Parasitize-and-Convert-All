package org.tdddd.epca.impl.client.render;

public record ItemLayerBinding(IItemShaderLayer layer, ItemLayerConfig config) {

    public static ItemLayerBinding of(IItemShaderLayer layer) {
        return new ItemLayerBinding(layer, ItemLayerConfig.DEFAULT);
    }

    public static ItemLayerBinding of(IItemShaderLayer layer, ItemLayerConfig config) {
        return new ItemLayerBinding(layer, config == null ? ItemLayerConfig.DEFAULT : config);
    }
}
