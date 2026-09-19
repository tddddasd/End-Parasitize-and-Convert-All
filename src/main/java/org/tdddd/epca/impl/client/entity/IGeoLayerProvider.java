package org.tdddd.epca.impl.client.entity;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Contract for rendering additional layers on top of a GeckoLib model.
 * Implementations are registered per entity type and invoked by the
 * {@link EpcaGeoRenderer}'s delegate render layer.
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>The GeckoLib 4 contract was one imperative method
 * {@code renderAdditionalLayer(renderer, entity, bakedModel, renderType, bufferSource, buffer, poseStack, partialTick, packedLight, packedOverlay)}
 * — it drew immediately and had direct access to the entity, the {@code MultiBufferSource}
 * and the {@code VertexConsumer}.</p>
 * <p>GeckoLib 5 removed all three: rendering is split into a render-state <b>extraction</b>
 * phase (while the animatable is still reachable) and a <b>submission</b> phase (after the
 * entity is forgotten, where draw calls go into a {@code SubmitNodeCollector}). The contract
 * therefore has two methods:</p>
 * <ul>
 *   <li>{@link #addLayerData} — extraction: read whatever per-entity data the layer needs
 *       (reachable via {@link EpcaGeoModel#entityOf}) into the render state.</li>
 *   <li>{@link #submitLayer} — submission: emit this layer's draw calls. The renderer is
 *       reachable again through {@link RenderPassInfo#renderer()}.</li>
 * </ul>
 */
public interface IGeoLayerProvider {

    /**
     * Render-state extraction phase. The entity being rendered is reachable through
     * {@link EpcaGeoModel#entityOf(GeoRenderState)}.
     *
     * @param renderState the render state being filled
     * @param partialTick render partial tick
     */
    default void addLayerData(GeoRenderState renderState, float partialTick) {
    }

    /**
     * Rendering phase. Use {@link EpcaGeoRenderer#submitModelWithAlpha} /
     * {@link EpcaGeoRenderer#submitModelWithArgb} to draw the model again with an
     * additional texture, render type, colour and alpha.
     *
     * <p>{@code RenderPassInfo} is taken raw for the reason documented on
     * {@link EpcaGeoRenderer}: the {@code GeoRenderState} bound on its type parameter is only
     * satisfied by vanilla render states at runtime.</p>
     *
     * @param passInfo  the active render pass
     * @param collector the collector draw calls are submitted to
     */
    @SuppressWarnings("rawtypes")
    default void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
    }
}
