package org.tdddd.epca.impl.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

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

    public Identifier maskOverride() {
        return maskOverride;
    }

    public float strength() {
        return strength;
    }

    public boolean twitch() {
        return twitch;
    }

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

        public Builder mask(Identifier mask) {
            this.maskOverride = mask;
            return this;
        }

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

        public Builder showEverywhere(boolean v) {
            this.showInGui = v;
            this.showWhenHeld = v;
            this.showInWorld = v;
            return this;
        }

        public Builder twitch(boolean v) {
            this.twitch = v;
            return this;
        }

        public Builder enabledWhen(Predicate<ItemStack> predicate) {
            this.enabledWhen = predicate;
            return this;
        }

        public ItemLayerConfig build() {
            return new ItemLayerConfig(this);
        }
    }
}
