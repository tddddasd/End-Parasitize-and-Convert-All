package org.tdddd.epca.impl.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * Contract for rendering additional layers on top of a GeckoLib model.
 * Implementations are registered per entity type and invoked by the
 * {@link EpcaGeoRenderer}'s delegate render layer after the main model pass.
 *
 * <p>Pattern mirrors {@code IGeoLayerProvider} from OpenSRP's GeoBaseRender.</p>
 */
public interface IGeoLayerProvider {

    /**
     * Render an additional layer on top of the entity model.
     *
     * @param renderer      the renderer instance (cast to {@code EpcaGeoRenderer} as needed)
     * @param entity        the entity being rendered
     * @param bakedModel    the current baked model (with animation state for this frame)
     * @param renderType    current render type of the main pass
     * @param bufferSource  multibuffer source
     * @param buffer        current vertex consumer
     * @param poseStack     current pose stack (already at entity position)
     * @param partialTick   render partial tick
     * @param packedLight   packed light coordinates
     * @param packedOverlay packed overlay coordinates
     */
    @SuppressWarnings("rawtypes")
    void renderAdditionalLayer(
            EpcaGeoRenderer renderer,
            LivingEntity entity,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            PoseStack poseStack,
            float partialTick,
            int packedLight,
            int packedOverlay
    );
}
