package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class BoneFragmentRenderer<T extends Entity> extends EntityRenderer<T> {
    public BoneFragmentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        
    }
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}