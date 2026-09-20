package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.Identifier;


public interface IGlowRenderable {
    
    Identifier getGlowTexture();

    
    default float[] getGlowColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    
    default boolean isGlowEnabled() {
        return true;
    }
}