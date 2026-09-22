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
 * ★ 统一物品渲染注册表（注册类）★
 *
 * <p>位于 {@code events.render} 包：所有「给某个物品挂某个渲染」的注册都从这里走。
 * 思路移植自 RottenRuinsSplendiding 的 {@code ItemRenderManager} /
 * {@code CosmicLayerRegistry}，并统一成一个入口。</p>
 *
 * <h3>一行注册</h3>
 * <pre>{@code
 * // 给末地碎片加崩坏渲染（贴图自动取物品自身贴图）
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
 *
 * // 带参数
 * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder()
 *                 .strength(0.7f)
 *                 .twitch(true)
 *                 .build());
 *
 * // 给一批物品一起加
 * ItemRenderRegistry.attachAll(ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT,
 *         Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
 *
 * // 按条件匹配（某个类、某个标签……）
 * ItemRenderRegistry.attach(stack -> stack.getItem() instanceof MyItemType,
 *         ItemShaderLayers.CORRUPTION, ItemLayerConfig.DEFAULT);
 * }</pre>
 *
 * <h3>解析顺序</h3>
 * <ol>
 *   <li>物品类自己实现 {@link IItemShaderRenderable} —— 优先级最高（自描述）</li>
 *   <li>本注册表，按注册先后顺序（先注册的先渲染，也就是在更下层）</li>
 * </ol>
 *
 * <h3>线程与生命周期</h3>
 * 注册请在客户端初始化阶段完成（{@code FMLClientSetupEvent}，见
 * {@code EpcaRenderClient#registerItemBindings()}）。渲染线程只读，
 * 用 {@link Collections#synchronizedList} 保护以防后续再注册。
 */
public final class ItemRenderRegistry {

    private record Entry(Predicate<ItemStack> predicate, ItemLayerBinding binding) {
    }

    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<>());

    private ItemRenderRegistry() {
    }

    // ── 注册 API ─────────────────────────────────────────────────────

    /** 把层挂到某个物品上，使用默认参数。 */
    public static void attach(Item item, IItemShaderLayer layer) {
        attach(item, layer, ItemLayerConfig.DEFAULT);
    }

    /** 把层挂到某个物品上。 */
    public static void attach(Item item, IItemShaderLayer layer, ItemLayerConfig config) {
        if (item == null || layer == null) {
            return;
        }
        attach(stack -> stack.getItem() == item, layer, config);
    }

    /**
     * 延迟注册版本：直接传 {@code ModItems.XXX}（{@code RegistryObject} 就是 {@code Supplier}）。
     *
     * <p>物品会在第一次匹配时解析并缓存，之后每次渲染只是两次引用比较，
     * 不会反复走 {@code RegistryObject.get()}。</p>
     */
    public static void attach(Supplier<? extends Item> item, IItemShaderLayer layer, ItemLayerConfig config) {
        if (item == null || layer == null) {
            return;
        }
        attach(new LazyItemPredicate(item), layer, config);
    }

    /** 延迟注册版本，使用默认参数。 */
    public static void attach(Supplier<? extends Item> item, IItemShaderLayer layer) {
        attach(item, layer, ItemLayerConfig.DEFAULT);
    }

    /** 按条件注册。 */
    public static void attach(Predicate<ItemStack> predicate, IItemShaderLayer layer, ItemLayerConfig config) {
        if (predicate == null || layer == null) {
            return;
        }
        ENTRIES.add(new Entry(predicate, ItemLayerBinding.of(layer, config)));
    }

    /** 按条件注册，使用默认参数。 */
    public static void attach(Predicate<ItemStack> predicate, IItemShaderLayer layer) {
        attach(predicate, layer, ItemLayerConfig.DEFAULT);
    }

    /** 一批物品共用同一套参数。 */
    public static void attachAll(IItemShaderLayer layer, ItemLayerConfig config, Item... items) {
        if (items == null || items.length == 0) {
            return;
        }
        Set<Item> set = new HashSet<>(Arrays.asList(items));
        attach(stack -> set.contains(stack.getItem()), layer, config);
    }

    /** 一批物品共用同一套参数（默认参数）。 */
    public static void attachAll(IItemShaderLayer layer, Item... items) {
        attachAll(layer, ItemLayerConfig.DEFAULT, items);
    }

    /** 清空全部注册（热重载 / 调试用）。 */
    public static void clear() {
        ENTRIES.clear();
    }

    // ── 查询 API ─────────────────────────────────────────────────────

    /**
     * 解析物品身上所有绑定的层（已按上下文过滤）。
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
     * 解析物品身上所有绑定的层（不过滤上下文）。
     */
    public static List<ItemLayerBinding> resolveAll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        // 优先级 1：物品类自描述
        Item item = stack.getItem();
        if (item instanceof IItemShaderRenderable renderable) {
            List<ItemLayerBinding> self = renderable.shaderBindings(stack);
            if (self != null && !self.isEmpty()) {
                return self;
            }
        }

        // 优先级 2：注册表（先注册的先渲染）
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

    /** 该物品是否有任何 shader 层。 */
    public static boolean hasAny(ItemStack stack) {
        return !resolveAll(stack).isEmpty();
    }

    /**
     * 该物品在当前上下文是否需要鬼畜抖动变换。
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

    /** 注册表条目数（调试用）。 */
    public static int size() {
        return ENTRIES.size();
    }

    /** 只读视图（调试用）。 */
    public static Collection<ItemLayerBinding> registeredBindings() {
        List<ItemLayerBinding> out = new ArrayList<>();
        synchronized (ENTRIES) {
            for (Entry entry : ENTRIES) {
                out.add(entry.binding());
            }
        }
        return Collections.unmodifiableList(out);
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    /**
     * 由物品注册名推导默认遮罩贴图：
     * {@code <namespace>:item/<path>} —— 也就是物品自己的贴图。
     */
    public static ResourceLocation defaultMaskFor(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return new ResourceLocation(epca.MODID, "item/unknown");
        }
        return new ResourceLocation(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    /**
     * 把一个 {@code Supplier<Item>}（例如 {@code RegistryObject}）变成物品判断，
     * 结果缓存一次，避免每次渲染都查注册表。
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
                    // 注册表还没就绪：下一帧再试
                    resolvedOnce = false;
                }
            }
            return resolved != null && stack.getItem() == resolved;
        }
    }
}
