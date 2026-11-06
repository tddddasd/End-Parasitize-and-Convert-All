package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public interface IOverlayRenderable {

    @Nullable
    ResourceLocation getOverlayTexture();

    default float[] getOverlayColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }
}
