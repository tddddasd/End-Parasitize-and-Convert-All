package org.tdddd.epca.impl.events.render;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.client.render.IItemShaderLayer;
import org.tdddd.epca.impl.client.render.IItemShaderRenderable;
import org.tdddd.epca.impl.client.render.ItemLayerBinding;
import org.tdddd.epca.impl.client.render.ItemLayerConfig;
import org.tdddd.epca.impl.epca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The unified item render registry.
 *
 * <p>Every "attach a renderer to an item" registration goes through here. The idea is ported from the
 * reference project's {@code ItemRenderManager} / {@code CosmicLayerRegistry}, unified into one entry
 * point.</p>
 *
 * <h2>One line to register</h2>
 * <pre>{@code
 * // corruption on the ender blade scrap (mask = the item's own texture)
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
 *
 * // with parameters
 * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder().strength(0.7f).twitch(true).build());
 *
 * // a batch
 * ItemRenderRegistry.attachAll(ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT,
 *         Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
 *
 * // by predicate (a class, a tag, ...)
 * ItemRenderRegistry.attach(stack -> stack.getItem() instanceof MyItemType,
 *         ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT);
 * }</pre>
 *
 * <h2>Resolution order</h2>
 * <ol>
 *   <li>the item class itself implements {@link IItemShaderRenderable} - highest priority;</li>
 *   <li>this registry, in registration order (first registered renders first, i.e. lower).</li>
 * </ol>
 *
 * <h2>Threading and lifetime</h2>
 * Register during client initialisation ({@code FMLClientSetupEvent}, see
 * {@code EpcaRenderClient#registerItemBindings()}). The render thread only reads; the list is
 * synchronized so a late registration cannot corrupt it.
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * <ul>
 *   <li>{@code ResourceLocation} became {@link Identifier};</li>
 *   <li>{@code ForgeRegistries.ITEMS.getKey} became {@link BuiltInRegistries#ITEM}
 *       {@code .getKey}, which is the NeoForge equivalent;</li>
 *   <li>{@code RegistryObject} as a lazy item supplier became {@code DeferredItem} / any
 *       {@link Supplier}, so the lazy predicate below is unchanged in spirit.</li>
 * </ul>
 */
public final class ItemRenderRegistry {

    private record Entry(Predicate<ItemStack> predicate, ItemLayerBinding binding) {
    }

    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<>());

    private ItemRenderRegistry() {
    }

    // -- registration API -----------------------------------------------------

    /** Attaches a layer to an item with default parameters. */
    public static void attach(Item item, IItemShaderLayer layer) {
        attach(item, layer, ItemLayerConfig.DEFAULT);
    }

    /** Attaches a layer to an item. */
    public static void attach(Item item, IItemShaderLayer layer, ItemLayerConfig config) {
        if (item == null || layer == null) {
            return;
        }
        attach(stack -> stack.getItem() == item, layer, config);
    }

    /**
     * Lazy registration: pass {@code ModItems.XXX} directly (a {@code DeferredItem} is a
     * {@link Supplier}).
     *
     * <p>The item is resolved on first match and cached, so the render path only does two reference
     * comparisons afterwards instead of calling {@code Supplier#get} every frame.</p>
     */
    public static void attach(Supplier<? extends Item> item, IItemShaderLayer layer,
                              ItemLayerConfig config) {
        if (item == null || layer == null) {
            return;
        }
        attach(new LazyItemPredicate(item), layer, config);
    }

    /** Lazy registration with default parameters. */
    public static void attach(Supplier<? extends Item> item, IItemShaderLayer layer) {
        attach(item, layer, ItemLayerConfig.DEFAULT);
    }

    /** Attaches by predicate. */
    public static void attach(Predicate<ItemStack> predicate, IItemShaderLayer layer,
                              ItemLayerConfig config) {
        if (predicate == null || layer == null) {
            return;
        }
        ENTRIES.add(new Entry(predicate, ItemLayerBinding.of(layer, config)));
    }

    /** Attaches by predicate with default parameters. */
    public static void attach(Predicate<ItemStack> predicate, IItemShaderLayer layer) {
        attach(predicate, layer, ItemLayerConfig.DEFAULT);
    }

    /** A batch of items sharing one configuration. */
    public static void attachAll(IItemShaderLayer layer, ItemLayerConfig config, Item... items) {
        if (items == null || items.length == 0) {
            return;
        }
        Set<Item> set = new HashSet<>(Arrays.asList(items));
        attach(stack -> set.contains(stack.getItem()), layer, config);
    }

    /** A batch of items sharing the default configuration. */
    public static void attachAll(IItemShaderLayer layer, Item... items) {
        attachAll(layer, ItemLayerConfig.DEFAULT, items);
    }

    /** Clears every registration (hot reload / debugging). */
    public static void clear() {
        ENTRIES.clear();
    }

    // -- query API ------------------------------------------------------------

    /** Resolves every layer bound to the item, filtered by context. */
    public static List<ItemLayerBinding> resolve(ItemStack stack, ItemDisplayContext ctx) {
        List<ItemLayerBinding> all = resolveAll(stack);
        if (all.isEmpty()) {
            return all;
        }
        List<ItemLayerBinding> filtered = new ArrayList<>(all.size());
        for (ItemLayerBinding binding : all) {
            if (binding.config().shouldRender(stack, ctx)) {
                filtered.add(binding);
            }
        }
        return filtered;
    }

    /** Resolves every layer bound to the item, without context filtering. */
    public static List<ItemLayerBinding> resolveAll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        // Priority 1: the item describes itself.
        Item item = stack.getItem();
        if (item instanceof IItemShaderRenderable renderable) {
            List<ItemLayerBinding> self = renderable.shaderBindings(stack);
            if (self != null && !self.isEmpty()) {
                return self;
            }
        }

        // Priority 2: the registry, in registration order.
        List<ItemLayerBinding> result = new ArrayList<>();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                if (entry.predicate().test(stack)) {
                    result.add(entry.binding());
                }
            }
        }
        return result;
    }

    /** Whether the item has any shader layer at all. */
    public static boolean hasAny(ItemStack stack) {
        return !resolveAll(stack).isEmpty();
    }

    /** Whether the pose twitch is wanted for this item in this context. */
    public static boolean isTwitchEnabled(ItemStack stack, ItemDisplayContext ctx) {
        for (ItemLayerBinding binding : resolveAll(stack)) {
            if (!binding.config().shouldRender(stack, ctx)) {
                continue;
            }
            if (binding.layer().usesTwitchTransform(stack, binding.config())) {
                return true;
            }
        }
        return false;
    }

    /** Number of registry entries (diagnostics). */
    public static int size() {
        return ENTRIES.size();
    }

    /** Read-only view (diagnostics). */
    public static Collection<ItemLayerBinding> registeredBindings() {
        List<ItemLayerBinding> out = new ArrayList<>();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                out.add(entry.binding());
            }
        }
        return Collections.unmodifiableList(out);
    }

    // -- helpers --------------------------------------------------------------

    /**
     * Derives the default mask texture from the item's registry name:
     * {@code <namespace>:item/<path>} - the item's own texture.
     */
    public static Identifier defaultMaskFor(ItemStack stack) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return Identifier.fromNamespaceAndPath(epca.MODID, "item/unknown");
        }
        return Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    /**
     * Turns a {@link Supplier} of an {@link Item} (for example {@code ModItems.XXX}) into an item
     * predicate, caching the resolved value so the registry is never queried per frame.
     */
    private static final class LazyItemPredicate implements Predicate<ItemStack> {
        private final Supplier<? extends Item> supplier;
        private Item resolved;
        private boolean resolvedOnce;

        private LazyItemPredicate(Supplier<? extends Item> supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean test(ItemStack stack) {
            if (!resolvedOnce) {
                resolvedOnce = true;
                try {
                    resolved = supplier.get();
                } catch (Throwable ignored) {
                    // The registry is not ready yet: try again next frame.
                    resolvedOnce = false;
                }
            }
            return resolved != null && stack.getItem() == resolved;
        }
    }
}
