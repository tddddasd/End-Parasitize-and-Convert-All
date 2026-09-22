package org.tdddd.epca.impl.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 单个物品 × 单个 shader 层的渲染参数。
 *
 * <h3>用法</h3>
 * <pre>{@code
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder()
 *                 .strength(0.8f)                    // 通用强度倍率
 *                 .mask("epca:item/ender_blade_mask")// 自定义遮罩贴图
 *                 .showInGui(false)                  // 背包里不显示
 *                 .twitch(true)                      // 附带鬼畜抖动
 *                 .uniform("tint", uniform -> ...)   // 直接改 shader uniform
 *                 .build());
 * }</pre>
 *
 * <p>缺省值：{@link #DEFAULT} —— 三个上下文都显示、强度 1.0、不抖动、
 * 遮罩用物品自身贴图。</p>
 */
public final class ItemLayerConfig {

    public static final ItemLayerConfig DEFAULT = builder().build();

    private final ResourceLocation maskOverride;
    private final float strength;
    private final boolean showInGui;
    private final boolean showWhenHeld;
    private final boolean showInWorld;
    private final boolean twitch;
    private final Predicate<ItemStack> enabledWhen;
    private final BiConsumer<ItemStack, ShaderInstance> uniformShaper;

    private ItemLayerConfig(Builder b) {
        this.maskOverride = b.maskOverride;
        this.strength = b.strength;
        this.showInGui = b.showInGui;
        this.showWhenHeld = b.showWhenHeld;
        this.showInWorld = b.showInWorld;
        this.twitch = b.twitch;
        this.enabledWhen = b.enabledWhen;
        this.uniformShaper = b.uniformShaper;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 覆盖遮罩贴图；{@code null} 表示使用物品自身贴图。 */
    public ResourceLocation maskOverride() {
        return maskOverride;
    }

    /** 通用强度倍率，各层自行解释（崩坏层用它乘脉冲强度）。 */
    public float strength() {
        return strength;
    }

    public boolean twitch() {
        return twitch;
    }

    /** 额外 uniform 设置，在本层 {@code prepare()} 之后执行，因此可以覆盖层内默认值。 */
    public BiConsumer<ItemStack, ShaderInstance> uniformShaper() {
        return uniformShaper;
    }

    /**
     * 该物品在当前上下文是否应当渲染本层。
     */
    public boolean shouldRender(ItemStack stack, ItemDisplayContext ctx) {
        if (enabledWhen != null && !enabledWhen.test(stack)) {
            return false;
        }
        return switch (ctx) {
            case GUI -> showInGui;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> showWhenHeld;
            default -> showInWorld;
        };
    }

    public static final class Builder {
        private ResourceLocation maskOverride;
        private float strength = 1.0f;
        private boolean showInGui = true;
        private boolean showWhenHeld = true;
        private boolean showInWorld = true;
        private boolean twitch = false;
        private Predicate<ItemStack> enabledWhen;
        private BiConsumer<ItemStack, ShaderInstance> uniformShaper;

        /** 指定遮罩贴图；可传 {@code "epca:item/xxx"} 形式或直接给 ResourceLocation。 */
        public Builder mask(ResourceLocation mask) {
            this.maskOverride = mask;
            return this;
        }

        /** 指定遮罩贴图；字符串按 {@code namespace:path} 解析，缺省 namespace 为 epca。 */
        public Builder mask(String mask) {
            this.maskOverride = mask.indexOf(':') >= 0
                    ? new ResourceLocation(mask)
                    : new ResourceLocation("epca", mask);
            return this;
        }

        public Builder strength(float strength) {
            this.strength = strength;
            return this;
        }

        public Builder showInGui(boolean v) {
            this.showInGui = v;
            return this;
        }

        public Builder showWhenHeld(boolean v) {
            this.showWhenHeld = v;
            return this;
        }

        public Builder showInWorld(boolean v) {
            this.showInWorld = v;
            return this;
        }

        /** 一次性设置三个上下文开关。 */
        public Builder showEverywhere(boolean v) {
            this.showInGui = v;
            this.showWhenHeld = v;
            this.showInWorld = v;
            return this;
        }

        /** 是否附带鬼畜抖动（几何层面崩坏）。 */
        public Builder twitch(boolean v) {
            this.twitch = v;
            return this;
        }

        /** 动态开关：返回 false 时不渲染本层。 */
        public Builder enabledWhen(Predicate<ItemStack> predicate) {
            this.enabledWhen = predicate;
            return this;
        }

        /** 直接操作 shader uniform（在本层默认 uniform 之后执行）。 */
        public Builder uniforms(BiConsumer<ItemStack, ShaderInstance> shaper) {
            this.uniformShaper = shaper;
            return this;
        }

        public ItemLayerConfig build() {
            return new ItemLayerConfig(this);
        }
    }
}
