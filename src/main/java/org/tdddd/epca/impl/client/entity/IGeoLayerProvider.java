package org.tdddd.epca.impl.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public interface IGeoLayerProvider {

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
