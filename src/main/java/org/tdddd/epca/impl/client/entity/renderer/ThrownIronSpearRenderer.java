package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ThrownIronSpear;
import org.tdddd.epca.impl.epca;

public class ThrownIronSpearRenderer extends EntityRenderer<ThrownIronSpear> {
    private final ItemRenderer itemRenderer;

    public ThrownIronSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownIronSpear entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        
        double motionX = entity.getDeltaMovement().x;
        double motionY = entity.getDeltaMovement().y;
        double motionZ = entity.getDeltaMovement().z;

        
        float yaw = (float) (Math.atan2(motionX, motionZ) * (180.0 / Math.PI)) - 90;
        
        float pitch = (float) (Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)) * (180.0 / Math.PI)) + 225.0F;

        
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.translate(0, -0.4, 0);

        
        poseStack.scale(4f, 4f, 4f);
        poseStack.translate(0.3D, -0.2D, 0.0D);

        ItemStack stack = entity.getPickupItem();
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownIronSpear entity) {
        return new ResourceLocation(epca.MODID, "textures/entity/iron_spear.png");
    }
}