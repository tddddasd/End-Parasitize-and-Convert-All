package org.tdddd.epca.impl.events.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
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
 *
 *
 * <p> {@code events.render}
 *  RottenRuinsSplendiding  {@code ItemRenderManager} /
 * {@code CosmicLayerRegistry}</p>
 *
 * <h3></h3>
 * <pre>{@code
 * //
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
 *
 * //
 * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder()
 *                 .strength(0.7f)
 *                 .twitch(true)
 *                 .build());
 *
 * //
 * ItemRenderRegistry.attachAll(ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT,
 *         Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
 *
 * //
 * ItemRenderRegistry.attach(stack -> stack.getItem() instanceof MyItemType,
 *         ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT);
 * }</pre>
 *
 * <h3></h3>
 * <ol>
 *   <li> {@link IItemShaderRenderable}  </li>
 *   <li></li>
 * </ol>
 *
 * <h3></h3>
 * {@code FMLClientSetupEvent}
 * {@code EpcaRenderClient#registerItemBindings()}
 *  {@link Collections#synchronizedList}
 */
public final class ItemRenderRegistry {

    private record Entry(Predicate<ItemStack> predicate, ItemLayerBinding binding) {
    }

    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<>());

    private ItemRenderRegistry() {
    }

    //   API

    /**  */
    public static void attach(Item item, IItemShaderLayer layer) {
        attach(item, layer, ItemLayerConfig.DEFAULT);
    }

    /**  */
    public static void attach(Item item, IItemShaderLayer layer, ItemLayerConfig config) {
        if (item == null || layer == null) {
            return;
        }
        attach(stack -> stack.getItem() == item, layer, config);
    }

    /**
     *  {@code ModItems.XXX}{@code RegistryObject}  {@code Supplier}
     *
     * <p>
     *  {@code RegistryObject.get()}</p>
     */
    public static void attach(Supplier<? extends Item> item, IItemShaderLayer layer, ItemLayerConfig config) {
        if (item == null || layer == null) {
            return;
        }
        attach(new LazyItemPredicate(item), layer, config);
    }

    /**  */
    public static void attach(Supplier<? extends Item> item, IItemShaderLayer layer) {
        attach(item, layer, ItemLayerConfig.DEFAULT);
    }

    /**  */
    public static void attach(Predicate<ItemStack> predicate, IItemShaderLayer layer, ItemLayerConfig config) {
        if (predicate == null || layer == null) {
            return;
        }
        ENTRIES.add(new Entry(predicate, ItemLayerBinding.of(layer, config)));
    }

    /**  */
    public static void attach(Predicate<ItemStack> predicate, IItemShaderLayer layer) {
        attach(predicate, layer, ItemLayerConfig.DEFAULT);
    }

    /**  */
    public static void attachAll(IItemShaderLayer layer, ItemLayerConfig config, Item... items) {
        if (items == null || items.length == 0) {
            return;
        }
        Set<Item> set = new HashSet<>(Arrays.asList(items));
        attach(stack -> set.contains(stack.getItem()), layer, config);
    }

    /**  */
    public static void attachAll(IItemShaderLayer layer, Item... items) {
        attachAll(layer, ItemLayerConfig.DEFAULT, items);
    }

    /**  /  */
    public static void clear() {
        ENTRIES.clear();
    }

    //   API

    /**
     *
     */
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

    /**
     *
     */
    public static List<ItemLayerBinding> resolveAll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        //  1
        Item item = stack.getItem();
        if (item instanceof IItemShaderRenderable renderable) {
            List<ItemLayerBinding> self = renderable.shaderBindings(stack);
            if (self != null && !self.isEmpty()) {
                return self;
            }
        }

        //  2
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

    /**  shader  */
    public static boolean hasAny(ItemStack stack) {
        return !resolveAll(stack).isEmpty();
    }

    /**
     *
     */
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

    /**  */
    public static int size() {
        return ENTRIES.size();
    }

    /**  */
    public static Collection<ItemLayerBinding> registeredBindings() {
        List<ItemLayerBinding> out = new ArrayList<>();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                out.add(entry.binding());
            }
        }
        return Collections.unmodifiableList(out);
    }


    /**
     *
     * {@code <namespace>:item/<path>}
     */
    public static ResourceLocation defaultMaskFor(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return new ResourceLocation(epca.MODID, "item/unknown");
        }
        return new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    /**
     *  {@code Supplier<Item>} {@code RegistryObject}
     *
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
                    resolvedOnce = false;
                }
            }
            return resolved != null && stack.getItem() == resolved;
        }
    }
}

