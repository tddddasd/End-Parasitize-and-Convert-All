package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;

public final class ItemShaderLayers {

    public static final IItemShaderLayer CORRUPTION = CorruptionLayer.INSTANCE;

    private static final Map<String, IItemShaderLayer> BY_NAME = new LinkedHashMap<>();

    static {
        register(CORRUPTION);
    }

    private ItemShaderLayers() {
    }

    public static void register(IItemShaderLayer layer) {
        if (layer != null && layer.name() != null) {
            BY_NAME.put(layer.name(), layer);
        }
    }

    public static IItemShaderLayer byName(String name) {
        return BY_NAME.get(name);
    }

    public static IItemShaderLayer byId(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        String prefix = "item_layer/";
        return BY_NAME.get(path.startsWith(prefix) ? path.substring(prefix.length()) : path);
    }

    public static Collection<IItemShaderLayer> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }
}
