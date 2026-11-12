package org.tdddd.epca.impl.client.blackhole;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BlackHoleEntity;

public class BlackHoleEntityRenderer extends EntityRenderer<BlackHoleEntity> {

    private static final ResourceLocation EMPTY = new ResourceLocation("minecraft", "textures/misc/white.png");

    public BlackHoleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return EMPTY;
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }
}