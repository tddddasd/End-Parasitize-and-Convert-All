package org.tdddd.epca.impl.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import org.tdddd.epca.impl.epca;

public enum GuiTexture implements IGuiTexture {
    
    CREATIVE_MODE_TAB_BLANK_ROW("creative_inventory", 0, 0, 162, 18),
    CREATIVE_MODE_TAB_BLANK_ROW_MATERIALS("creative_inventory_materials", 0, 0, 162, 18),   
    CREATIVE_MODE_TAB_BLANK_ROW_SPAWN("creative_inventory_spawn", 0, 0, 162, 18),          
    CREATIVE_MODE_TAB_BLANK_ROW_BLOCKS("creative_inventory_blocks", 0, 0, 162, 18);        
    public final Identifier location;
    public final int width, height, startX, startY, textureWidth, textureHeight;

    private GuiTexture(String location, int width, int height) {
        this(location, 0, 0, width, height);
    };

    private GuiTexture(String location, int startX, int startY, int width, int height) {
        this(location, startX, startY, width, height, 256, 256);
    };

    private GuiTexture(String location, int startX, int startY, int width, int height, int textureWidth, int textureHeight) {
        this.location = epca.asResource("textures/gui/" + location + ".png");
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    };

    // 26.1.2: RenderSystem#setShaderTexture was removed. GuiGraphicsExtractor binds the texture through the
    // RenderPipeline passed to blit(), so nothing is left for bind() to do; kept as a no-op to preserve the
    // IGuiTexture contract.
    @Override
    public void bind() {
    };

    /**
     * 26.1.2: GuiGraphics -> GuiGraphicsExtractor and {@code blit(...)} gained a leading RenderPipeline argument.
     */
    public void render(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, startX, startY, width, height, textureWidth, textureHeight);
    };

    @Override
    public Identifier getLocation() {
        return location;
    };

    @Override
    public int getStartX() {
        return startX;
    }

    @Override
    public int getStartY() {
        return startY;
    };

    @Override
    public int getWidth() {
        return width;
    };

    @Override
    public int getHeight() {
        return height;
    };

    @Override
    public int getTextureWidth() {
        return textureWidth;
    };

    @Override
    public int getTextureHeight() {
        return textureHeight;
    };
}
