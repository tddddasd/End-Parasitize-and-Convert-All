package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;

public class ReshapeLongarmsCustomPartRenderer extends EntityRenderer<ReshapeLongarms.CustomPart> {
    public ReshapeLongarmsCustomPartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ReshapeLongarms.CustomPart entity) {
        return null; // 不需要纹理
    }

    @Override
    public void render(ReshapeLongarms.CustomPart entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 不渲染任何东西
    }
}