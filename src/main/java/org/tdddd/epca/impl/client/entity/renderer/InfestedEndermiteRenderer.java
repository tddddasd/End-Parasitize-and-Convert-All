package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.client.entity.model.InfestedEndermiteModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class InfestedEndermiteRenderer extends EpcaGeoRenderer<InfestedEndermite> {

    private static final EndermanAfterimageLayer AFTERIMAGE_LAYER = new EndermanAfterimageLayer();

    public InfestedEndermiteRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedEndermiteModel());
        addLayerProvider(AFTERIMAGE_LAYER);
    }

    @Override
    public void render(InfestedEndermite entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.animatable = entity;
        // Main render → afterimage layer renders automatically via OuterLayerDelegate
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (entity.isGlowEnabled()) {
            renderGlow(entity, entity, poseStack, bufferSource, partialTick);
        }
    }

    private void renderGlow(InfestedEndermite entity, IGlowRenderable glow, PoseStack poseStack,
                            MultiBufferSource bufferSource, float partialTick) {
        if (entity.getVariant() != InfestedEndermite.Variant.UNSTABLE) return;
        ResourceLocation glowTex = glow.getGlowTexture();
        if (glowTex == null) return;
        float[] color = glow.getGlowColor();
        BakedGeoModel bakedModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(entity));
        RenderType glowRenderType = RenderType.eyes(glowTex);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);
        poseStack.pushPose();
        preRender(poseStack, entity, bakedModel, bufferSource, glowBuffer,
                false, partialTick, 15728880, OverlayTexture.NO_OVERLAY,
                color[0], color[1], color[2], 1.0F);
        renderModelWithAlpha(poseStack, entity, bakedModel, glowRenderType,
                bufferSource, glowBuffer, partialTick,
                15728880, OverlayTexture.NO_OVERLAY,
                color[0], color[1], color[2], 1.0F);
        poseStack.popPose();
    }
}
