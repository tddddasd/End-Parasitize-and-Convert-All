package org.tdddd.epca.impl.compat.jade;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.ui.Element;

public class SoulIconElement extends Element {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/particle/sculk_soul_8.png");
    private static final int SOURCE_SIZE = 8;

    public SoulIconElement() {
        this.size(SOURCE_SIZE * 2, SOURCE_SIZE * 2);
    }

    @Override
    public Component getNarration() {
        return Component.literal("soul");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
                getX(), getY(), 0.0F, 0.0F, SOURCE_SIZE * 2, SOURCE_SIZE * 2, SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE);
    }
}