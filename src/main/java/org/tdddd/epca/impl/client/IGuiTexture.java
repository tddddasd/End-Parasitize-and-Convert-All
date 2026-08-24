package org.tdddd.epca.impl.client;


import net.minecraft.resources.ResourceLocation;

public interface IGuiTexture {

    public ResourceLocation getLocation();

    public int getStartX();

    public int getStartY();

    public int getWidth();

    public int getHeight();

    public int getTextureWidth();

    public int getTextureHeight();

    public void bind();
};