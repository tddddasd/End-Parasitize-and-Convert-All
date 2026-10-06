package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;

/**
 *  shader  +
 *
 * <p>{@code ItemRenderRegistry.attach(item, ItemShaderLayers.CORRUPTION)}</p>
 *
 * <p> electro shader
 * {@link IItemShaderLayer} {@code ItemShaderLayers.register(new ElectroLayer())}
 *  {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} </p>
 */
public final class ItemShaderLayers {

    /** RGB  +  +  +  RottenRuinsSplendiding */
    public static final IItemShaderLayer CORRUPTION = CorruptionLayer.INSTANCE;

    private static final Map<String, IItemShaderLayer> BY_NAME = new LinkedHashMap<>();

    static {
        register(CORRUPTION);
    }

    private ItemShaderLayers() {
    }

    /**  */
    public static void register(IItemShaderLayer layer) {
        if (layer != null && layer.name() != null) {
            BY_NAME.put(layer.name(), layer);
        }
    }

    /**  {@code null} */
    public static IItemShaderLayer byName(String name) {
        return BY_NAME.get(name);
    }

    /**  id {@code epca:item_layer/<name>} */
    public static IItemShaderLayer byId(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        String prefix = "item_layer/";
        return BY_NAME.get(path.startsWith(prefix) ? path.substring(prefix.length()) : path);
    }

    /**  */
    public static Collection<IItemShaderLayer> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }
}

