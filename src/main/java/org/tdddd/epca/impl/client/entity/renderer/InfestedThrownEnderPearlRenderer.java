package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.InfestedThrownEnderPearl;
import org.tdddd.epca.impl.epca;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InfestedThrownEnderPearlRenderer extends EntityRenderer<InfestedThrownEnderPearl> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(epca.MODID, "textures/item/infested_ender_pearl.png");
    private final ItemRenderer itemRenderer;

    public InfestedThrownEnderPearlRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.5F;
    }

    @Override
    public void render(InfestedThrownEnderPearl entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90F - entityYaw));   
        poseStack.mulPose(Axis.XP.rotationDegrees(45F));               

        
        ItemStack stack = new ItemStack(Items.ENDER_PEARL);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(InfestedThrownEnderPearl entity) {
        return TEXTURE; 
    }
}
