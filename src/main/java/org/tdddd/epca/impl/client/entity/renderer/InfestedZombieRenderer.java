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
import org.tdddd.epca.impl.client.entity.model.InfestedZombieModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedZombie;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class InfestedZombieRenderer extends EpcaGeoRenderer<InfestedZombie> {

    public InfestedZombieRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedZombieModel());
    }

    @Override
    public void render(InfestedZombie entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.animatable = entity;
        // Main render → afterimage layer renders automatically via OuterLayerDelegate
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
/*/
        if (entity.isGlowEnabled()) {
            renderGlow(entity, entity, poseStack, bufferSource, partialTick);
        }

 */
    }
/*/
    private void renderGlow(InfestedZombie entity, IGlowRenderable glow, PoseStack poseStack,
                            MultiBufferSource bufferSource, float partialTick) {

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

 */
}
