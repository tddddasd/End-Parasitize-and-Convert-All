package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import java.util.List;

@Mod.EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class InfestedRaidBarrierRenderer {

    private static final int STACKS = 20;
    private static final int SLICES = 40;

    private static final float ALPHA_FLOOR = 0.10F;
    private static final float ALPHA_RIM = 0.13F;
    private static final float ALPHA_SHIMMER = 0.10F;

    private static final float PINK_R = 1.0F;
    private static final float PINK_G = 0.42F;
    private static final float PINK_B = 0.84F;

    private static final double SHIMMER_FREQ = 0.10D;
    private static final double SHIMMER_SPEED = 1.6D;

    private InfestedRaidBarrierRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        List<InfestedRaidBarrierData.Barrier> active =
                ClientRaidBarriers.activeIn(mc.level.dimension().location());
        if (active.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer shell = buffer.getBuffer(RenderType.debugQuads());
        long now = System.currentTimeMillis();
        for (InfestedRaidBarrierData.Barrier barrier : active) {
            drawSphere(matrix, shell, barrier, cam, now);
        }
        poseStack.popPose();
        buffer.endBatch();
    }

    private static void drawSphere(Matrix4f matrix, VertexConsumer shell,
                                   InfestedRaidBarrierData.Barrier barrier, Vec3 cam, long now) {
        float fade = barrier.fade(now);
        if (fade <= 0.002F) return;

        Vec3 center = Vec3.atCenterOf(barrier.center());
        float radius = (float) barrier.radius();
        double seconds = now / 1000.0D;

        for (int i = 0; i < STACKS; i++) {
            double phi0 = Math.PI * i / STACKS;
            double phi1 = Math.PI * (i + 1) / STACKS;
            for (int j = 0; j < SLICES; j++) {
                double th0 = 2.0D * Math.PI * j / SLICES;
                double th1 = 2.0D * Math.PI * (j + 1) / SLICES;

                vertex(matrix, shell, point(center, radius, phi0, th0), center, cam, fade, seconds);
                vertex(matrix, shell, point(center, radius, phi1, th0), center, cam, fade, seconds);
                vertex(matrix, shell, point(center, radius, phi1, th1), center, cam, fade, seconds);
                vertex(matrix, shell, point(center, radius, phi0, th1), center, cam, fade, seconds);
            }
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer vc, Vec3 p, Vec3 center, Vec3 cam,
                               float fade, double seconds) {
        Vec3 normal = p.subtract(center).normalize();
        Vec3 toCam = cam.subtract(p).normalize();
        double rim = Math.pow(1.0D - Math.abs(normal.dot(toCam)), 2.0D);

        double band = 0.5D + 0.5D * Math.sin((p.x + p.y * 1.4D + p.z * 0.8D) * SHIMMER_FREQ * 6.2831853D
                - seconds * SHIMMER_SPEED);
        float shimmer = (float) (band * band);

        float alpha = (ALPHA_FLOOR + ALPHA_RIM * (float) rim + ALPHA_SHIMMER * shimmer) * fade;
        alpha = Math.min(1.0F, alpha);

        float r = PINK_R;
        float g = PINK_G + (0.95F - PINK_G) * shimmer * 0.75F;
        float b = PINK_B + (1.0F - PINK_B) * shimmer * 0.75F;

        vc.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .color(r, g, b, alpha)
                .endVertex();
    }

    private static Vec3 point(Vec3 center, float radius, double phi, double theta) {
        double sinPhi = Math.sin(phi);
        return new Vec3(
                center.x + radius * sinPhi * Math.cos(theta),
                center.y + radius * Math.cos(phi),
                center.z + radius * sinPhi * Math.sin(theta));
    }
}
