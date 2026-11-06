package org.tdddd.epca.impl.client.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;
import snownee.jade.api.ui.IBoxStyle;
import snownee.jade.api.ui.IElement;

public enum SoulClientProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final IBoxStyle NO_BOX = new IBoxStyle() {
        @Override
        public float borderWidth() {
            return 0.0F;
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, float x, float y, float width, float height) {
        }
    };

    private static final class SoulBarStyle implements IProgressStyle {
        private int start = SOUL_BLUE;
        private int end = SOUL_YELLOW;

        @Override
        public IProgressStyle color(int color) {
            this.start = color;
            this.end = color;
            return this;
        }

        @Override
        public IProgressStyle color(int start, int end) {
            this.start = start;
            this.end = end;
            return this;
        }

        @Override
        public IProgressStyle textColor(int color) {
            return this;
        }

        @Override
        public IProgressStyle vertical(boolean vertical) {
            return this;
        }

        @Override
        public IProgressStyle overlay(IElement element) {
            return this;
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, float x, float y, float width,
                           float height, float progress, net.minecraft.network.chat.Component text) {
            int total = (int) width;
            int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
            guiGraphics.fill((int) x, (int) y, (int) x + total, (int) (y + height), 0x60000000);
            for (int i = 0; i < filled; i++) {
                float t = total <= 0 ? 0.0F : (float) i / total;
                guiGraphics.fill((int) x + i, (int) y, (int) x + i + 1, (int) (y + height), lerp(t));
            }
        }

        private int lerp(float t) {
            int a = (int) (((this.start >> 24) & 0xFF) + (((this.end >> 24) & 0xFF) - ((this.start >> 24) & 0xFF)) * t);
            int r = (int) (((this.start >> 16) & 0xFF) + (((this.end >> 16) & 0xFF) - ((this.start >> 16) & 0xFF)) * t);
            int g = (int) (((this.start >> 8) & 0xFF) + (((this.end >> 8) & 0xFF) - ((this.start >> 8) & 0xFF)) * t);
            int b = (int) ((this.start & 0xFF) + ((this.end & 0xFF) - (this.start & 0xFF)) * t);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static final IProgressStyle SOUL_STYLE = new SoulBarStyle();

    private static final int SOUL_BLUE = 0xFF4A6CFF;
    private static final int SOUL_YELLOW = 0xFFFFD24A;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("EpcaSoulMaxValue")) return;
        double soul = data.getDouble("EpcaSoulValue");
        double max = data.getDouble("EpcaSoulMaxValue");
        if (max <= 0.0) return;

        float ratio = (float) Math.max(0.0, Math.min(1.0, soul / max));
        IElementHelper helper = IElementHelper.get();
        IProgressStyle style = helper.progressStyle()
                .color(SOUL_BLUE, SOUL_YELLOW)
                .textColor(0xFFFFFFFF)
                .overlay(new SoulIconElement(16.0F, 16.0F));
        Component label = Component.literal(String.format("%.1f/%.1f", soul, max))
                .withStyle(ChatFormatting.AQUA);
        tooltip.add(java.util.List.of(new SoulIconElement(16.0F, 16.0F), helper.text(label)));
        tooltip.add(new SoulBarElement(ratio));
    }

    @Override
    public ResourceLocation getUid() {
        return SoulServerProvider.UID;
    }
}
