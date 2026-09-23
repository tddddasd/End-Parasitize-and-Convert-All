package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.tdddd.epca.impl.client.render.layer.CorruptionLayer;

/**
 * The built-in shader layer list plus the registration point for external layers.
 *
 * <p>Usage: {@code ItemRenderRegistry.attach(item, ItemShaderLayers.CORRUPTION)}.</p>
 *
 * <p>To add a layer (an "electro" layer, say): write its shader and pipeline, implement
 * {@link IItemShaderLayer}, then {@code ItemShaderLayers.register(new ElectroLayer())}, after which
 * {@link org.tdddd.epca.impl.events.render.ItemRenderRegistry} can attach it to any item exactly like
 * the built-in one.</p>
 *
 * <p>Ported unchanged from 1.20.1 apart from {@code ResourceLocation} -&gt; {@link Identifier}.</p>
 */
public final class ItemShaderLayers {

    /** The corruption layer: RGB dispersion + scanlines + glitch bands + dead pixels. */
    public static final IItemShaderLayer CORRUPTION = CorruptionLayer.INSTANCE;

    private static final Map<String, IItemShaderLayer> BY_NAME = new LinkedHashMap<>();

    static {
        register(CORRUPTION);
    }

    private ItemShaderLayers() {
    }

    /** Registers a layer (a later registration with the same name replaces the earlier one). */
    public static void register(IItemShaderLayer layer) {
        if (layer != null && layer.name() != null) {
            BY_NAME.put(layer.name(), layer);
        }
    }

    /** Looks a layer up by name; {@code null} when unknown. */
    public static IItemShaderLayer byName(String name) {
        return BY_NAME.get(name);
    }

    /** Looks a layer up by id ({@code epca:item_layer/<name>}). */
    public static IItemShaderLayer byId(Identifier id) {
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        String prefix = "item_layer/";
        return BY_NAME.get(path.startsWith(prefix) ? path.substring(prefix.length()) : path);
    }

    /** Every registered layer. */
    public static Collection<IItemShaderLayer> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }
}
