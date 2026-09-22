package org.tdddd.epca.impl.client.render.compat;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * 光影包（Iris / Oculus）检测 —— “光影兼容原理”的入口。
 *
 * <h3>为什么需要它</h3>
 * Oculus（Iris 的 Forge 移植）使用 <b>延迟渲染管线</b>：整场景先画进多个
 * GBuffer 中间目标（albedo / normal / depth / specular …），再由光影的
 * composite pass 重建最终画面。
 *
 * <p>如果我们在 {@code ItemRenderer.render()} 里直接用自己的 shader 画 quad，
 * 这些 quad 会被光影管线<b>捕获进 GBuffer</b>，随后被当作普通几何体重新着色 ——
 * 自定义效果就丢了。</p>
 *
 * <h3>对策</h3>
 * 光影激活时不在物品渲染时立刻绘制，而是把渲染状态快照进
 * {@link ItemLayerLateRenderQueue}，等 {@code GameRenderer.renderLevel()} 结束后
 * 回放，回放时绑定 {@code mainRenderTarget} 直写主 framebuffer，绕过 GBuffer。
 *
 * <p>检测全部走反射，<b>不产生对 Iris/Oculus 的编译期依赖</b>，
 * 未装光影时零开销。</p>
 */
public final class IrisShaderCompat {

    /** 是否装了 oculus（Forge）或 iris（Fabric 名，兼容部分整合包）。 */
    private static final boolean IRIS_LOADED = detectIrisLoaded();

    private static volatile boolean resolved;
    private static Method isShaderPackInUseMethod;
    private static Object apiInstance;

    private IrisShaderCompat() {
    }

    private static boolean detectIrisLoaded() {
        try {
            ModList modList = ModList.get();
            return modList != null && (modList.isLoaded("oculus") || modList.isLoaded("iris"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 是否安装了 Iris/Oculus。 */
    public static boolean isIrisLoaded() {
        return IRIS_LOADED;
    }

    /**
     * 当前是否有一个光影包正在生效。
     *
     * <p>反射调用 {@code IrisApi.getInstance().isShaderPackInUse()}；
     * 任何时候出错（API 变更、类缺失、链接错误）都安全地返回 {@code false}，
     * 也就是退化成“无光影即时渲染”。</p>
     */
    public static boolean isShaderPackActive() {
        if (!IRIS_LOADED) {
            return false;
        }
        if (!resolved) {
            resolveApi();
        }
        if (isShaderPackInUseMethod == null || apiInstance == null) {
            return false;
        }
        try {
            Object active = isShaderPackInUseMethod.invoke(apiInstance);
            return Boolean.TRUE.equals(active);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void resolveApi() {
        synchronized (IrisShaderCompat.class) {
            if (resolved) {
                return;
            }
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = apiClass.getMethod("getInstance").invoke(null);
                Method method = apiClass.getMethod("isShaderPackInUse");
                apiInstance = instance;
                isShaderPackInUseMethod = method;
            } catch (Throwable ignored) {
                // API 不可用：保持 null，永远返回 false
                apiInstance = null;
                isShaderPackInUseMethod = null;
            }
            resolved = true;
        }
    }

    /**
     * 该渲染上下文是否应当把自定义 shader 层延迟到世界渲染结束后回放。
     *
     * <p>GUI 不走光影管线（背包、创造栏、tooltip 都是直接画在 framebuffer 上的），
     * 因此永不延迟；其余上下文（世界、第一/第三人称、地面、物品展示框）
     * 在光影激活时必须延迟。</p>
     */
    public static boolean shouldDeferItemLayer(ItemDisplayContext context) {
        if (context == ItemDisplayContext.GUI) {
            return false;
        }
        return isShaderPackActive();
    }
}
