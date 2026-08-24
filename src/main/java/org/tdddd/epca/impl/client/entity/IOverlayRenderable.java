package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Interface for entities that render an additional translucent overlay layer
 * on top of their base model. The overlay is rendered via {@link EpcaGeoRenderer}
 * after the main pass, without requiring a custom renderer subclass.
 */
public interface IOverlayRenderable {

    /** The overlay texture. Return null to skip overlay for this frame. */
    @Nullable
    ResourceLocation getOverlayTexture();

    /** RGB color tint for the overlay (each 0-1). Default white. */
    default float[] getOverlayColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }
}
