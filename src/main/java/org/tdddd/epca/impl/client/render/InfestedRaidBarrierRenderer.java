package org.tdddd.epca.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.raid.InfestedRaidBarrierData;

import java.util.List;

/**
 * Draws the raid-victory barrier: a huge, very transparent pink sphere with an animated
 * enchantment-like shimmer sweeping over it.
 *
 * <p>Geometry is a closed UV sphere (phi 0..PI, theta 0..2*PI) drawn with no back-face culling, so the
 * whole shell is visible from inside and outside alike.</p>
 *
 * <p>The look comes from two per-vertex terms:</p>
 * <ul>
 *   <li>a Fresnel-ish alpha, {@code 1 - |dot(normal, view)|}, which keeps the silhouette denser than the
 *       middle so the sphere reads as a shell rather than a solid ball;</li>
 *   <li>a travelling sine band that pushes the colour towards white and lifts the alpha, which is what
 *       gives the "enchanted" sweep.</li>
 * </ul>
 *
 * <p><b>Why this render type.</b> {@code RenderTypes.debugQuads()} is the translucent
 * {@code POSITION_COLOR} pipeline ({@code BlendFunction.TRANSLUCENT}, cull off, depth write off, no alpha
 * cutout), so the sphere can fade all the way to zero alpha without popping. The obvious alternative,
 * {@code RenderTypes.entityTranslucent(...)}, carries {@code ALPHA_CUTOUT 0.1} and discards every fragment
 * below 0.1 alpha - that is exactly what punched holes into the sphere and made it look unclosed - and
 * {@code RenderTypes.entityGlint()} expects a {@code POSITION_TEX} vertex and tests depth with
 * {@code EQUAL}, so it cannot be layered over an independent sphere at all.</p>
 */
@EventBusSubscriber(modid = epca.MODID, value = Dist.CLIENT)
public final class InfestedRaidBarrierRenderer {
    /** Sphere tessellation; the seam closes exactly because phi ends at PI and theta at 2*PI. */
    private static final int STACKS = 20;
    private static final int SLICES = 40;

    /** Alpha budget per vertex (before the fade-in/out multiplier). */
    private static final float ALPHA_FLOOR = 0.10F;
    private static final float ALPHA_RIM = 0.13F;
    private static final float ALPHA_SHIMMER = 0.10F;

    private static final float PINK_R = 1.0F;
    private static final float PINK_G = 0.42F;
    private static final float PINK_B = 0.84F;

    /** Spatial frequency / speed of the travelling shimmer band. */
    private static final double SHIMMER_FREQ = 0.10D;
    private static final double SHIMMER_SPEED = 1.6D;

    private InfestedRaidBarrierRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        List<InfestedRaidBarrierData.Barrier> active =
                ClientRaidBarriers.activeIn(mc.level.dimension().identifier());
        if (active.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer shell = buffer.getBuffer(RenderTypes.debugQuads());
        long now = System.currentTimeMillis();
        for (InfestedRaidBarrierData.Barrier barrier : active) {
            drawSphere(pose, shell, barrier, cam, now);
        }
        poseStack.popPose();
        buffer.endBatch();
    }

    private static void drawSphere(PoseStack.Pose pose, VertexConsumer shell,
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

                Vec3 p0 = point(center, radius, phi0, th0);
                Vec3 p1 = point(center, radius, phi1, th0);
                Vec3 p2 = point(center, radius, phi1, th1);
                Vec3 p3 = point(center, radius, phi0, th1);

                vertex(shell, pose, p0, center, cam, fade, seconds);
                vertex(shell, pose, p1, center, cam, fade, seconds);
                vertex(shell, pose, p2, center, cam, fade, seconds);
                vertex(shell, pose, p3, center, cam, fade, seconds);
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, Vec3 center, Vec3 cam,
                               float fade, double seconds) {
        Vec3 normal = p.subtract(center).normalize();
        Vec3 toCam = cam.subtract(p).normalize();
        double rim = Math.pow(1.0D - Math.abs(normal.dot(toCam)), 2.0D);

        // Travelling band: bright where sin(...) peaks, giving the enchantment-like sweep.
        double band = 0.5D + 0.5D * Math.sin((p.x + p.y * 1.4D + p.z * 0.8D) * SHIMMER_FREQ * 6.2831853D
                - seconds * SHIMMER_SPEED);
        float shimmer = (float) (band * band);

        float alpha = (ALPHA_FLOOR + ALPHA_RIM * (float) rim + ALPHA_SHIMMER * shimmer) * fade;
        alpha = Math.min(1.0F, alpha);

        // Blend the pink towards white on the band crest so the sweep reads as a highlight.
        float r = PINK_R;
        float g = PINK_G + (0.95F - PINK_G) * shimmer * 0.75F;
        float b = PINK_B + (1.0F - PINK_B) * shimmer * 0.75F;

        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(r, g, b, alpha);
    }

    private static Vec3 point(Vec3 center, float radius, double phi, double theta) {
        double sinPhi = Math.sin(phi);
        return new Vec3(
                center.x + radius * sinPhi * Math.cos(theta),
                center.y + radius * Math.cos(phi),
                center.z + radius * sinPhi * Math.sin(theta));
    }
}
