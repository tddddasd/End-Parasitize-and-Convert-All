package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Render parameters of one item x one shader layer.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ItemRenderRegistry.attach(ModItems.ENDER_BLADE_SCRAP, ItemShaderLayers.CORRUPTION,
 *         ItemLayerConfig.builder()
 *                 .strength(0.8f)                     // generic strength multiplier
 *                 .mask("epca:item/ender_blade_mask") // custom mask texture
 *                 .showInGui(false)                   // do not draw in the inventory
 *                 .twitch(true)                       // add the geometric jitter
 *                 .build());
 * }</pre>
 *
 * <p>Defaults: {@link #DEFAULT} - shown in all three contexts, strength 1.0, no twitch, mask = the
 * item's own texture.</p>
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * Two changes, both deliberate:
 * <ul>
 *   <li>{@code uniformShaper} ({@code BiConsumer<ItemStack, ShaderInstance>}) is gone, because there
 *       is no {@code ShaderInstance} to shape and no per-draw uniform to shape it with. The tint and
 *       split strength it was normally used to change are now fields of the layer's
 *       {@link ItemLayerPayload}, and {@link IItemShaderLayer#prepare} is the place to vary them.
 *       This is the documented approximation for that hook.</li>
 *   <li>{@code ResourceLocation} became {@link Identifier}.</li>
 * </ul>
 * Everything else (the three context switches, {@code enabledWhen}, {@code mask}, {@code strength},
 * {@code twitch}) is unchanged.
 */
public final class ItemLayerConfig {

    public static final ItemLayerConfig DEFAULT = builder().build();

    private final Identifier maskOverride;
    private final float strength;
    private final boolean showInGui;
    private final boolean showWhenHeld;
    private final boolean showInWorld;
    private final boolean twitch;
    private final Predicate<ItemStack> enabledWhen;

    private ItemLayerConfig(Builder b) {
        this.maskOverride = b.maskOverride;
        this.strength = b.strength;
        this.showInGui = b.showInGui;
        this.showWhenHeld = b.showWhenHeld;
        this.showInWorld = b.showInWorld;
        this.twitch = b.twitch;
        this.enabledWhen = b.enabledWhen;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mask texture override; {@code null} means "use the item's own texture". */
    public Identifier maskOverride() {
        return maskOverride;
    }

    /** Generic strength multiplier; each layer interprets it (corruption multiplies its pulse by it). */
    public float strength() {
        return strength;
    }

    public boolean twitch() {
        return twitch;
    }

    /**
     * Whether this layer should render for the given item in the given context.
     *
     * <p>Faithful to 1.20.1: `GUI` uses {@code showInGui}, both first-person hands use
     * {@code showWhenHeld}, everything else uses {@code showInWorld}.</p>
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
        private Identifier maskOverride;
        private float strength = 1.0f;
        private boolean showInGui = true;
        private boolean showWhenHeld = true;
        private boolean showInWorld = true;
        private boolean twitch = false;
        private Predicate<ItemStack> enabledWhen;

        /** Sets the mask texture from an {@link Identifier}. */
        public Builder mask(Identifier mask) {
            this.maskOverride = mask;
            return this;
        }

        /** Sets the mask texture from a string; a missing namespace defaults to {@code epca}. */
        public Builder mask(String mask) {
            this.maskOverride = mask.indexOf(':') >= 0
                    ? Identifier.parse(mask)
                    : Identifier.fromNamespaceAndPath("epca", mask);
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

        /** Sets all three context switches at once. */
        public Builder showEverywhere(boolean v) {
            this.showInGui = v;
            this.showWhenHeld = v;
            this.showInWorld = v;
            return this;
        }

        /** Whether to add the geometric jitter (geometry-level decay). */
        public Builder twitch(boolean v) {
            this.twitch = v;
            return this;
        }

        /** Dynamic switch: returning false suppresses this layer. */
        public Builder enabledWhen(Predicate<ItemStack> predicate) {
            this.enabledWhen = predicate;
            return this;
        }

        public ItemLayerConfig build() {
            return new ItemLayerConfig(this);
        }
    }
}
