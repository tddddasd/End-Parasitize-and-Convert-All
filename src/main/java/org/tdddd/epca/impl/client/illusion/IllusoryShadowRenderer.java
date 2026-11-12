package org.tdddd.epca.impl.client.illusion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.illusion.IllusoryShadowEntity;

/**
 * Placeholder "model" for the butterfly's past / future shadow: a coloured wireframe cage that is a bit
 * larger than the butterfly's own 8x8x8 hitbox (scale 1.1). The tint comes from the entity itself, so the
 * purple-grey past shadow and the white future shadow need no separate renderer.
 */
public class IllusoryShadowRenderer extends EntityRenderer<IllusoryShadowEntity> {

    private static final ResourceLocation EMPTY = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final float BASE_SIZE = 8.0F;

    public IllusoryShadowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IllusoryShadowEntity entity) {
        return EMPTY;
    }

    @Override
    public void render(IllusoryShadowEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float scale = entity.scale() <= 0.0F ? 1.0F : entity.scale();
        int color = entity.color();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float size = BASE_SIZE * scale;
        float half = size * 0.5F;
        // outer cage, then a slightly inset one so the shape reads as a volume instead of a flat frame
        drawCage(consumer, matrix, half, 0.0F, size, red, green, blue, 0.55F);
        drawCage(consumer, matrix, half * 0.92F, size * 0.04F, size * 0.96F, red, green, blue, 0.25F);
    }

    private static void drawCage(VertexConsumer consumer, Matrix4f matrix, float half,
                                 float minY, float maxY, float red, float green, float blue, float alpha) {
        float x0 = -half;
        float x1 = half;
        float z0 = -half;
        float z1 = half;

        // bottom face
        line(consumer, matrix, x0, minY, z0, x1, minY, z0, red, green, blue, alpha);
        line(consumer, matrix, x1, minY, z0, x1, minY, z1, red, green, blue, alpha);
        line(consumer, matrix, x1, minY, z1, x0, minY, z1, red, green, blue, alpha);
        line(consumer, matrix, x0, minY, z1, x0, minY, z0, red, green, blue, alpha);

        // top face
        line(consumer, matrix, x0, maxY, z0, x1, maxY, z0, red, green, blue, alpha);
        line(consumer, matrix, x1, maxY, z0, x1, maxY, z1, red, green, blue, alpha);
        line(consumer, matrix, x1, maxY, z1, x0, maxY, z1, red, green, blue, alpha);
        line(consumer, matrix, x0, maxY, z1, x0, maxY, z0, red, green, blue, alpha);

        // verticals
        line(consumer, matrix, x0, minY, z0, x0, maxY, z0, red, green, blue, alpha);
        line(consumer, matrix, x1, minY, z0, x1, maxY, z0, red, green, blue, alpha);
        line(consumer, matrix, x1, minY, z1, x1, maxY, z1, red, green, blue, alpha);
        line(consumer, matrix, x0, minY, z1, x0, maxY, z1, red, green, blue, alpha);
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float red, float green, float blue, float alpha) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 1.0E-5F) {
            nx /= length;
            ny /= length;
            nz /= length;
        } else {
            nx = 0.0F;
            ny = 1.0F;
            nz = 0.0F;
        }
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(nx, ny, nz).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(nx, ny, nz).endVertex();
    }
}
