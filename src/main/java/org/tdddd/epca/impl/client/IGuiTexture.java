package org.tdddd.epca.impl.client;


import net.minecraft.resources.Identifier;

public interface IGuiTexture {

    public Identifier getLocation();

    public int getStartX();

    public int getStartY();

    public int getWidth();

    public int getHeight();

    public int getTextureWidth();

    public int getTextureHeight();

    public void bind();
};