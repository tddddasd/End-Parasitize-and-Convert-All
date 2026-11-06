package org.tdddd.epca.impl.compat.jade;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import snownee.jade.api.ui.Element;

public class SoulBarElement extends Element {

    private static final int BORDER = 0xFF1B2A6B;
    private static final int BACKGROUND = 0xFF101018;
    private static final int START = 0xFF4A6CFF;
    private static final int END = 0xFFFFD24A;
    private static final int BAR_HEIGHT = 2;
    private static final int BAR_WIDTH = 60;
    private static final int VERTICAL_OFFSET = 0;

    private final float ratio;

    public SoulBarElement(float ratio) {
        this.ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        this.size(BAR_WIDTH + 2, BAR_HEIGHT + 2);
    }

    @Override
    public Component getNarration() {
        return Component.literal("soul bar");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY() + VERTICAL_OFFSET;
        extractor.fill(left, top, left + BAR_WIDTH + 2, top + BAR_HEIGHT + 2, BORDER);
        extractor.fill(left + 1, top + 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, BACKGROUND);
        int filled = Math.round(BAR_WIDTH * this.ratio);
        for (int i = 0; i < filled; i++) {
            float t = (float) i / BAR_WIDTH;
            extractor.fill(left + 1 + i, top + 1, left + 2 + i, top + 1 + BAR_HEIGHT, lerp(t));
        }
    }

    private int lerp(float t) {
        int a = (int) (((START >> 24) & 0xFF) + (((END >> 24) & 0xFF) - ((START >> 24) & 0xFF)) * t);
        int r = (int) (((START >> 16) & 0xFF) + (((END >> 16) & 0xFF) - ((START >> 16) & 0xFF)) * t);
        int g = (int) (((START >> 8) & 0xFF) + (((END >> 8) & 0xFF) - ((START >> 8) & 0xFF)) * t);
        int b = (int) ((START & 0xFF) + ((END & 0xFF) - (START & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}