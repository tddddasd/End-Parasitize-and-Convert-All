package org.tdddd.epca.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.epca;

public enum GuiTexture implements IGuiTexture {
    
    CREATIVE_MODE_TAB_BLANK_ROW("creative_inventory", 0, 0, 162, 18),
    CREATIVE_MODE_TAB_BLANK_ROW_MATERIALS("creative_inventory_materials", 0, 0, 162, 18),   
    CREATIVE_MODE_TAB_BLANK_ROW_SPAWN("creative_inventory_spawn", 0, 0, 162, 18),          
    CREATIVE_MODE_TAB_BLANK_ROW_BLOCKS("creative_inventory_blocks", 0, 0, 162, 18);        
    public final ResourceLocation location;
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

    @OnlyIn(Dist.CLIENT)
    @Override
    public void bind() {
        RenderSystem.setShaderTexture(0, location);
    };

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height, textureWidth, textureHeight);
    };

    @Override
    public ResourceLocation getLocation() {
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
