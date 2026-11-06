package org.tdddd.epca.impl.client.jade;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

public class SoulIconElement extends Element {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/particle/sculk_soul_8.png");
    private static final int SOURCE_SIZE = 8;

    private final float width;
    private final float height;

    public SoulIconElement(float width, float height) {
        this.width = width;
        this.height = height;
        this.size = new Vec2(width, height);
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(this.width, this.height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width, float height) {
        int w = SOURCE_SIZE * 2;
        int h = SOURCE_SIZE * 2;
        guiGraphics.blit(TEXTURE, (int) x, (int) y, w, h, 0.0F, 0.0F,
                SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE);
    }
}