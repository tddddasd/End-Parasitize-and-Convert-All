package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedPlayer;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.yawning_neko_api.data.DamageAdaptation;
import software.bernie.geckolib.cache.object.BakedGeoModel;


@OnlyIn(Dist.CLIENT)
public class InfestedPlayerRenderer extends EpcaGeoRenderer<InfestedPlayer> {
    public InfestedPlayerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(InfestedPlayer entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        
        
        
        
    }

    @Override
    public void actuallyRender(PoseStack poseStack, InfestedPlayer animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        float r = red;
        float g = green;
        float b = blue;
        float a = alpha;
        int finalOverlay = packedOverlay;

        int adaptationLevel = DamageAdaptation.getAdaptationData(animatable);
int maxAdaptations = DamageAdaptation.getClientMaxAdaptations(animatable);  

if (maxAdaptations > 0 && adaptationLevel >= maxAdaptations) {
    
    finalOverlay = OverlayTexture.NO_OVERLAY;
    r = 0.9F; g = 0.2F; b = 0.9F; a = 1.0F;
} else {
    
    int eventType = DamageAdaptation.getLastAdaptationEventType(animatable);
    if (eventType == 1) {
        finalOverlay = OverlayTexture.NO_OVERLAY;
        r = 0.2F; g = 0.9F; b = 1.0F; a = 1.0F;
    }
    
}

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, finalOverlay, r, g, b, a);
    }

    private void renderHandItem(InfestedPlayer entity, ItemStack stack, ItemDisplayContext context,
                                PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        
        poseStack.translate(0.3, 0.2, 0.1);
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.scale(-1, 1, 1);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, context, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
        poseStack.popPose();
    }
}