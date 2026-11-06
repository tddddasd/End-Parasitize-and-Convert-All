package org.tdddd.epca.impl.client.entity;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;

public interface IGeoLayerProvider {

    default void addLayerData(GeoRenderState renderState, float partialTick) {
    }

    @SuppressWarnings("rawtypes")
    default void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
    }
}
