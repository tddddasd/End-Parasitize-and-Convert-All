package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.tdddd.epca.impl.overworld.registry.entities.entity.BlackHole;

/**
 * Draws the black hole as a solid black cube resting on one corner, tilted 45 degrees and spinning
 * anticlockwise about the vertical axis.
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHole> {

    private static final float R = 0.02F;
    private static final float G = 0.02F;
    private static final float B = 0.03F;
    private static final float A = 1.0F;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHole entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(BlackHole entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getVisualScale();
        if (scale <= 0.0F) {
            return;
        }
        float spin = (entity.tickCount + partialTick) * 2.5F;
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-spin));   // anticlockwise
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0F));   // on one corner
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(45.0F));
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugFilledBox());
        float r = (float) entity.getVisualRadius();
        poseStack.popPose();

        // the lensing ring: same cube silhouette, 15 percent larger, light wound around it
        poseStack.pushPose();
        net.minecraft.client.Camera camera =
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));
        BlackHoleLensRenderType.draw(poseStack, r * 1.15F, (float) Math.toRadians(spin));
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** Draws one hole (cube + lens ring) at the current pose. Used by the render level pass. */
    public static void renderHole(PoseStack poseStack, MultiBufferSource bufferSource,
                                  float radius, float spinRadians) {
        // The hole itself is a round black disc that always faces the camera, so every player sees a
        // circle regardless of their viewing angle.
        VertexConsumer discBuffer = bufferSource.getBuffer(RenderType.debugFilledBox());
        poseStack.pushPose();
        net.minecraft.client.Camera camera =
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));
        drawDisc(discBuffer, poseStack.last().pose(), radius);
        // A visible annulus around the disc, so the ring is present even if the lens shader cannot
        // sample the scene texture.
        drawAnnulus(discBuffer, poseStack.last().pose(), radius * 1.15F, radius * 1.55F);
        poseStack.popPose();

        poseStack.pushPose();
        net.minecraft.client.Camera lensCamera =
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-lensCamera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(lensCamera.getXRot()));
        BlackHoleLensRenderType.draw(poseStack, radius * 1.15F, spinRadians);
        poseStack.popPose();
    }


    /** A single camera facing black disc, approximated with a triangle fan of quads. */
    private static void drawDisc(VertexConsumer buffer, Matrix4f pose, float r) {
        final int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
            vertex(buffer, pose, 0.0F, 0.0F, 0.0F);
            vertex(buffer, pose, (float) Math.cos(a0) * r, (float) Math.sin(a0) * r, 0.0F);
            vertex(buffer, pose, (float) Math.cos(a1) * r, (float) Math.sin(a1) * r, 0.0F);
            vertex(buffer, pose, 0.0F, 0.0F, 0.0F);
        }
    }

    /** A ring of quads between two radii, in the same plane as the disc. */
    private static void drawAnnulus(VertexConsumer buffer, Matrix4f pose, float inner, float outer) {
        final int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0 * i) / segments;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segments;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            // two triangles per segment, each as a degenerate quad, so the ring stays solid
            tri(buffer, pose, c0 * inner, s0 * inner, c1 * inner, s1 * inner, c1 * outer, s1 * outer);
            tri(buffer, pose, c0 * inner, s0 * inner, c1 * outer, s1 * outer, c0 * outer, s0 * outer);
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.vertex(pose, ax, ay, az).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
        buffer.vertex(pose, bx, by, bz).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
        buffer.vertex(pose, cx, cy, cz).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
        buffer.vertex(pose, dx, dy, dz).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
    }

    /** One triangle written as a degenerate quad (a, b, c, c). */
    private static void tri(VertexConsumer buffer, Matrix4f pose,
                            float ax, float ay, float bx, float by, float cx, float cy) {
        buffer.vertex(pose, ax, ay, 0.0F).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
        buffer.vertex(pose, bx, by, 0.0F).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
        buffer.vertex(pose, cx, cy, 0.0F).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
        buffer.vertex(pose, cx, cy, 0.0F).color(0.05F, 0.04F, 0.07F, 1.0F).endVertex();
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z) {
        buffer.vertex(pose, x, y, z).color(0.02F, 0.02F, 0.03F, 1.0F).endVertex();
    }
}
