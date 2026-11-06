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

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

    }

}
