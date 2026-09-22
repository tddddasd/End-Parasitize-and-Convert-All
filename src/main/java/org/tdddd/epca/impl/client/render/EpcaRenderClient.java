package org.tdddd.epca.impl.client.render;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.tdddd.epca.impl.client.render.compat.IrisShaderCompat;
import org.tdddd.epca.impl.client.render.shader.EpcaShaders;
import org.tdddd.epca.impl.client.render.sky.SkyRuptureShaders;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.render.ItemRenderRegistry;
import org.tdddd.epca.impl.overworld.registry.ModItems;

/**
 * 客户端渲染子系统入口。
 *
 * <p>由 {@link org.tdddd.epca.impl.epca} 在客户端分支里调用
 * {@link #init(IEventBus)}；{@link #onClientSetup(FMLClientSetupEvent)}
 * 由 {@code ClientSetup} 转发。</p>
 */
public final class EpcaRenderClient {

    private static boolean bindingsRegistered;

    private EpcaRenderClient() {
    }

    /** 注册 shader 加载事件（mod 事件总线）。 */
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(EpcaShaders::onRegisterShaders);
        modEventBus.addListener(SkyRuptureShaders::onRegisterShaders);
        // 方块图集缝合后解析 12 张结界星点 sprite 的 UV（参考项目同款做法）
        modEventBus.addListener(SkyRuptureShaders::onTextureAtlasStitched);
    }

    /**
     * 客户端初始化：注册默认物品绑定。
     *
     * <p>用 {@code enqueueWork} 保证在注册表可用之后执行，因此可以直接用
     * {@code ModItems.XXX.get()}。</p>
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EpcaRenderClient::registerItemBindings);
    }

    /**
     * ★ 在这里给指定物品挂指定渲染 ★
     *
     * <p>下面这段就是“统一渲染注册类”的用法示例。<b>默认全部注释掉</b>，
     * 想给哪个物品加效果，取消对应行的注释即可（或者自己写几行）。</p>
     *
     * <pre>{@code
     * // ── 1. 最简：给物品加崩坏渲染（遮罩自动取物品自身贴图）──
     * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION);
     *
     * // ── 2. 带参数：强度 / 抖动 / 显示上下文 ──
     * ItemRenderRegistry.attach(ModItems.CLUSTER, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder()
     *                 .strength(0.85f)     // 崩坏强度倍率
     *                 .twitch(true)        // 附带鬼畜抖动
     *                 .showInGui(false)    // 背包里不显示
     *                 .build());
     *
     * // ── 3. 换一张遮罩贴图（比如只让物品的某部分崩坏）──
     * ItemRenderRegistry.attach(ModItems.DISEASED_HEART, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder()
     *                 .mask("epca:item/diseased_heart_corrupt")
     *                 .build());
     *
     * // ── 4. 一批物品一起加 ──
     * ItemRenderRegistry.attachAll(ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder().strength(0.6f).build(),
     *         ModItems.INFESTED_BONE.get(), ModItems.TWISTED_BONE.get());
     *
     * // ── 5. 按条件匹配 ──
     * ItemRenderRegistry.attach(
     *         stack -> stack.is(ModTags.Items.INFESTED_MATERIALS),
     *         ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.DEFAULT);
     *
     * // ── 6. 直接改 shader uniform（覆盖层内默认值）──
     * ItemRenderRegistry.attach(ModItems.KILL_STICK, ItemShaderLayers.CORRUPTION,
     *         ItemLayerConfig.builder()
     *                 .uniforms((stack, shader) -> {
     *                     var tint = shader.getUniform("tint");
     *                     if (tint != null) tint.set(0.10f, 0.55f, 0.20f); // 改成病态绿
     *                 })
     *                 .build());
     *
     * // ── 7. NBT 驱动的常驻崩坏（不靠周期爆发）──
     * //   /data modify entity @s SelectedItem.tag.epca_corruption set value 0.7f
     * }</pre>
     */
    public static void registerItemBindings() {
        if (bindingsRegistered) {
            return;
        }
        bindingsRegistered = true;

        // ★ 你的注册写在这里 ★
        //
        // ── 末地刃碎片：崩坏渲染 ──
        // 遮罩自动取物品自身贴图（assets/epca/textures/item/ender_blade_scrap.png，
        // 该贴图带 .mcmeta 动画，崩坏层会跟着一起动）。
        // twitch(true)：崩坏爆发窗口内叠加鬼畜抖动，和着色器同一个强度曲线。
        ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
                ItemLayerConfig.builder()
                        .strength(1.0f)
                        .twitch(true)
                        .build());

        // 注意：这里刻意不读 EpcaShaders 的字段 —— shader 要等首次资源重载
        // (RegisterShadersEvent) 才存在，此时读取会强制类初始化，得不偿失。
        epca.LOGGER.info("[epca-render] 物品 shader 层注册数 = {} | Iris/Oculus 已安装 = {} | 光影兼容 = {}",
                ItemRenderRegistry.size(),
                IrisShaderCompat.isIrisLoaded(),
                IrisShaderCompat.isIrisLoaded() ? "已启用（自动延迟回放）" : "无需启用");
    }

    /** 强制重置注册标志（热重载 / 调试用）。 */
    public static void resetBindingGuard() {
        bindingsRegistered = false;
    }
}
