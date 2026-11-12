package org.tdddd.epca.impl.client.illusion;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.illusion.IllusoryButterflyEntity;

/**
 * The butterfly itself is invisible on purpose: it is a 8x8x8 hitbox driven entirely by the server side
 * time line. Whatever the player is supposed to see is drawn through the black hole renderer (present
 * phase) and the {@link IllusoryShadowRenderer} cages (past / future phase).
 */
public class IllusoryButterflyRenderer extends EntityRenderer<IllusoryButterflyEntity> {

    private static final ResourceLocation EMPTY = new ResourceLocation("minecraft", "textures/misc/white.png");

    public IllusoryButterflyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IllusoryButterflyEntity entity) {
        return EMPTY;
    }

    @Override
    public void render(IllusoryButterflyEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }
}
