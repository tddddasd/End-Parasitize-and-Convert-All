package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public interface IOverlayRenderable {

    @Nullable
    Identifier getOverlayTexture();

    default float[] getOverlayColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }
}
