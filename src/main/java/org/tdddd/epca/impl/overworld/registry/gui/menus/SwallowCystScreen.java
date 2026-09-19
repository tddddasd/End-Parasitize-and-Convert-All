package org.tdddd.epca.impl.overworld.registry.gui.menus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 26.1.2: GUI drawing is extraction based. {@code AbstractContainerScreen#render}/{@code renderBg} became
 * {@code extractRenderState}/{@code extractBackground(GuiGraphicsExtractor, mouseX, mouseY, partialTick)},
 * the tooltip pass is already performed by {@code extractRenderState}, and {@code imageWidth/imageHeight}
 * are now final and must be passed to the super constructor.
 */
public class SwallowCystScreen extends AbstractContainerScreen<SwallowCystMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/shulker_box.png");

    public SwallowCystScreen(SwallowCystMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // 1.20.1 also issued RenderSystem.setShaderColor(1,1,1,1) here; with the extraction API the tint is
        // carried by the blit colour, and the default is already opaque white.
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }
}