package org.tdddd.epca.impl.client.render;

/**
 * A layer plus the parameters it was attached with.
 *
 * <p>Immutable and shared: the registry stores one binding per registration and every rendered item
 * that matches reuses it, so the render thread never allocates here. Ported unchanged from 1.20.1.</p>
 */
public record ItemLayerBinding(IItemShaderLayer layer, ItemLayerConfig config) {

    public static ItemLayerBinding of(IItemShaderLayer layer, ItemLayerConfig config) {
        return new ItemLayerBinding(layer, config == null ? ItemLayerConfig.DEFAULT : config);
    }

    public static ItemLayerBinding of(IItemShaderLayer layer) {
        return new ItemLayerBinding(layer, ItemLayerConfig.DEFAULT);
    }
}
