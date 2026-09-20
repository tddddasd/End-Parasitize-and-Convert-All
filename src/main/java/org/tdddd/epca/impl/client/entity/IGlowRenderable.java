package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.ResourceLocation;


public interface IGlowRenderable {
    
    ResourceLocation getGlowTexture();

    
    default float[] getGlowColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    
    default boolean isGlowEnabled() {
        return true;
    }
}