package org.tdddd.epca.impl.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 *    shader
 *
 * <h3></h3>
 * <pre>{@code
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder()
 *                 .strength(0.8f)                    //
 *                 .mask("epca:item/ender_blade_mask")//
 *                 .showInGui(false)                  //
 *                 .twitch(true)                      //
 *                 .uniform("tint", uniform -> ...)   //  shader uniform
 *                 .build());
 * }</pre>
 *
 * <p>{@link #DEFAULT}   1.0
 * </p>
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

    /** {@code null}  */
    public ResourceLocation maskOverride() {
        return maskOverride;
    }

    /**  */
    public float strength() {
        return strength;
    }

    public boolean twitch() {
        return twitch;
    }

    /**  uniform  {@code prepare()}  */
    public BiConsumer<ItemStack, ShaderInstance> uniformShaper() {
        return uniformShaper;
    }

    /**
     *
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

        /**  {@code "epca:item/xxx"}  ResourceLocation */
        public Builder mask(ResourceLocation mask) {
            this.maskOverride = mask;
            return this;
        }

        /**  {@code namespace:path}  namespace  epca */
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

        /**  */
        public Builder showEverywhere(boolean v) {
            this.showInGui = v;
            this.showWhenHeld = v;
            this.showInWorld = v;
            return this;
        }

        /**  */
        public Builder twitch(boolean v) {
            this.twitch = v;
            return this;
        }

        /**  false  */
        public Builder enabledWhen(Predicate<ItemStack> predicate) {
            this.enabledWhen = predicate;
            return this;
        }

        /**  shader uniform uniform  */
        public Builder uniforms(BiConsumer<ItemStack, ShaderInstance> shaper) {
            this.uniformShaper = shaper;
            return this;
        }

        public ItemLayerConfig build() {
            return new ItemLayerConfig(this);
        }
    }
}

