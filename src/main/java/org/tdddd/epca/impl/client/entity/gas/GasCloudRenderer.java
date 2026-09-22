package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Locale;

/**
 * Shared draw helper for every shader-rendered cloud of the mod.
 *
 * <p>It owns the one camera-relative billboard path that is known to work (it was proved in game by
 * the reshape-mob gas clouds) and is used by two callers:</p>
 * <ol>
 *   <li>{@link GasCloudLayer}, the GeckoLib layer of the reshape mobs (must keep drawing exactly
 *       what it drew before this class existed), and</li>
 *   <li>{@code ContaminatedWaterRenderer}, which draws the water-touching clouds and the dark-red
 *       micro rectangles of {@code epca:contaminated_water} into the {@link MultiBufferSource} an
 *       {@code EntityRenderer} receives.</li>
 * </ol>
 * Neither caller duplicates the pose, billboard or quad math any more.</p>
 *
 * <h2>Pose space</h2>
 * <p>Both callers hand this class a {@link PoseStack} whose translation column is
 * {@code entityInterpolatedPos - cameraPos} (vanilla {@code EntityRenderDispatcher.render} always
 * translates by exactly that; {@code getRenderOffset} defaults to {@link Vec3#ZERO}). A cloud at
 * absolute world position {@code cloudPos} is therefore placed with the offset
 * {@code cloudPos - entityInterpolatedPos}, which makes the resulting vertex-space position
 * {@code cloudPos - cameraPos}. The offset is computed from {@code entity.getPosition(partialTick)}
 * for both callers so it cancels the pose translation exactly instead of leaving the sub-tick
 * interpolation residual the old layer-only code had.</p>
 *
 * <h2>Per-quad data rides in vertex attributes</h2>
 * <p>{@code ShaderInstance.apply()} uploads uniforms once per RenderType flush while every quad of
 * the pass is buffered beforehand, so per-quad data must travel in the vertex attributes:</p>
 * <ul>
 *   <li>{@code Color.rgb} - the quad's own colour (white for gas clouds, dark red for water
 *       specks), {@code Color.a} - the quad's own fade, so each quad fades independently;</li>
 *   <li>{@code UV2.x} - the packed render seed, {@code UV2.y} - the sub-quad index, which the
 *       fragment shader turns into a per-cloud noise silhouette. The water specks reuse this slot
 *       as a <em>style</em> channel: {@link GasCloudRenderType#SPEC_STYLE_CHANNEL} in
 *       {@code UV2.y} selects the hard-edged rectangle branch of
 *       {@code assets/epca/shaders/core/gas_cloud.fsh} and {@code UV2.x} is unused there.</li>
 * </ul>
 */
public final class GasCloudRenderer {

    /** Half-size of one billboard quad, in blocks per unit of scale. */
    public static final float QUAD_HALF_EXTENT = 0.5F;

    /** Slowest / fastest rotation (degrees per tick) of an individual gas sub-quad. */
    public static final float SPIN_DEGREES_PER_TICK = 1.4F;

    /** Alpha below which a quad is not worth submitting. */
    public static final float MIN_VISIBLE_ALPHA = 0.004F;

    /**
     * Alpha below which a water speck's fade-in/fade-out ends: below this a speck is invisible
     * enough that submitting it is pure waste, so it is skipped without being removed early.
     */
    private static final float SPECK_MIN_VISIBLE_ALPHA = 0.004F;

    private GasCloudRenderer() {
    }

    /**
     * Outcome of one {@link #submit} call, used by the callers for their throttled
     * {@code [gascloud]} diagnostics. Every field is a plain value; nothing here references live
     * world state.
     */
    public static final class Submission {

        /** Clouds that were actually submitted (alpha above the visible threshold). */
        public final int cloudsDrawn;
        /** Clouds the manager holds for this owner, whether submitted or not. */
        public final int cloudsTracked;
        /** Specks that were actually submitted. */
        public final int specksDrawn;
        /** Lowest / highest submitted cloud alpha; 0 when nothing was submitted. */
        public final float minAlpha;
        public final float maxAlpha;
        /** Pose translation column before any cloud offset was applied. */
        public final float poseBaseX;
        public final float poseBaseY;
        public final float poseBaseZ;
        /** Absolute world position of the first submitted cloud; null when nothing was submitted. */
        public final Vec3 probeCloud;
        /** Offset this helper applied for the probe cloud: {@code probeCloud - entityInterp}. */
        public final double probeAppliedX;
        public final double probeAppliedY;
        public final double probeAppliedZ;

        private Submission(int cloudsDrawn, int cloudsTracked, int specksDrawn,
                           float minAlpha, float maxAlpha,
                           float poseBaseX, float poseBaseY, float poseBaseZ,
                           Vec3 probeCloud,
                           double probeAppliedX, double probeAppliedY, double probeAppliedZ) {
            this.cloudsDrawn = cloudsDrawn;
            this.cloudsTracked = cloudsTracked;
            this.specksDrawn = specksDrawn;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.poseBaseX = poseBaseX;
            this.poseBaseY = poseBaseY;
            this.poseBaseZ = poseBaseZ;
            this.probeCloud = probeCloud;
            this.probeAppliedX = probeAppliedX;
            this.probeAppliedY = probeAppliedY;
            this.probeAppliedZ = probeAppliedZ;
        }

        /** True when nothing at all was submitted for this owner. */
        public boolean isEmpty() {
            return this.cloudsDrawn == 0 && this.specksDrawn == 0;
        }
    }

    /**
     * Draws every live gas cloud of one owner through the custom pipeline.
     *
     * @param owner       the entity the {@code PoseStack} is already translated to
     * @param poseStack   camera-relative entity pose (see the class comment)
     * @param bufferSource the buffer source of the current render pass
     * @param partialTick sub-tick interpolation
     * @param clouds      the clouds the manager holds for {@code owner}
     * @return the submission outcome, for the caller's diagnostics
     */
    public static Submission submit(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                    float partialTick, List<GasCloud> clouds) {
        return submit(owner, poseStack, bufferSource, partialTick, clouds, null);
    }

    /**
     * Draws every live gas cloud of one owner plus, optionally, its water specks.
     *
     * <p>Both kinds go through the same camera-relative billboard frame, the same
     * {@link GasCloudRenderType} and therefore the same custom core shader; only the per-quad
     * attributes differ.</p>
     *
     * @param specks water specks to draw as well, or {@code null} for none
     */
    public static Submission submit(Entity owner, PoseStack poseStack, MultiBufferSource bufferSource,
                                    float partialTick, List<GasCloud> clouds, List<WaterSpec> specks) {
        Minecraft minecraft = Minecraft.getInstance();
        float cameraYaw = minecraft.gameRenderer.getMainCamera().getYRot();
        float cameraPitch = minecraft.gameRenderer.getMainCamera().getXRot();

        // Read the pose translation column before touching the pose. Vanilla built it as
        // entityInterpolatedPos - cameraPos, which is what the numeric [gascloud] probe verifies.
        Matrix4f pose = poseStack.last().pose();
        float poseBaseX = pose.m30();
        float poseBaseY = pose.m31();
        float poseBaseZ = pose.m32();

        VertexConsumer consumer = bufferSource.getBuffer(GasCloudRenderType.get());
        Vec3 origin = owner.getPosition(partialTick);

        int cloudsDrawn = 0;
        float minAlpha = Float.MAX_VALUE;
        float maxAlpha = 0.0F;
        Vec3 probeCloud = null;
        double probeAppliedX = 0.0D;
        double probeAppliedY = 0.0D;
        double probeAppliedZ = 0.0D;

        if (clouds != null) {
            for (GasCloud cloud : clouds) {
                float alpha = cloud.getAlpha(partialTick);
                if (alpha < MIN_VISIBLE_ALPHA) {
                    continue;
                }
                Vec3 position = cloud.getPosition(partialTick);
                if (cloudsDrawn == 0) {
                    // Only the first submitted cloud of this owner is probed, so the numeric
                    // diagnostic line stays short; every cloud uses the same offset formula.
                    probeCloud = position;
                    probeAppliedX = position.x - origin.x;
                    probeAppliedY = position.y - origin.y;
                    probeAppliedZ = position.z - origin.z;
                }
                // Per-cloud values are packed into vertex attributes, never uniforms: the shader is
                // applied once per RenderType flush, so a uniform would carry only the last cloud.
                int packedSeed = GasCloudRenderType.packSeed(cloud.getRenderSeed());
                renderCloud(consumer, poseStack, cloud, partialTick, alpha, packedSeed,
                        cameraYaw, cameraPitch, origin);
                cloudsDrawn++;
                minAlpha = Math.min(minAlpha, alpha);
                maxAlpha = Math.max(maxAlpha, alpha);
            }
        }

        int specksDrawn = 0;
        if (specks != null) {
            for (WaterSpec speck : specks) {
                // The spec's fade-in is 20% of a 30-80 tick life, so a fresh speck can be below the
                // visible threshold for a couple of ticks; skipping it is cheaper than drawing an
                // invisible quad and it keeps the diagnostic count meaningful.
                float alpha = speck.getAlpha(partialTick);
                if (alpha < SPECK_MIN_VISIBLE_ALPHA) {
                    continue;
                }
                renderSpec(consumer, poseStack, speck, partialTick, alpha, cameraYaw, cameraPitch, origin);
                specksDrawn++;
            }
        }

        if (cloudsDrawn == 0) {
            minAlpha = 0.0F;
        }
        return new Submission(cloudsDrawn, clouds == null ? 0 : clouds.size(), specksDrawn,
                minAlpha, maxAlpha, poseBaseX, poseBaseY, poseBaseZ,
                probeCloud, probeAppliedX, probeAppliedY, probeAppliedZ);
    }

    /**
     * Submits one gas cloud: camera billboard, then a push/pop per sub-quad carrying that quad's
     * random offset, orientation and slow spin.
     */
    private static void renderCloud(VertexConsumer consumer, PoseStack poseStack, GasCloud cloud,
                                    float partialTick, float alpha, int packedSeed,
                                    float cameraYaw, float cameraPitch, Vec3 origin) {
        Vec3 position = cloud.getPosition(partialTick);
        float scale = cloud.getScale(partialTick);
        float spread = cloud.getSpreadFactor(partialTick);
        int age = cloud.getAge();

        poseStack.pushPose();
        poseStack.translate(position.x - origin.x, position.y - origin.y, position.z - origin.z);
        // Camera-facing billboard frame.
        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

        for (int i = 0; i < cloud.getSubQuadCount(); i++) {
            GasCloud.SubQuad quad = cloud.getSubQuad(i);
            poseStack.pushPose();
            poseStack.translate(quad.offsetX * scale * spread,
                    quad.offsetY * scale * spread,
                    quad.offsetZ * scale * spread);
            poseStack.mulPose(Axis.YP.rotationDegrees(quad.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(quad.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    quad.roll + (quad.spinPhase + age * quad.spinSpeed * SPIN_DEGREES_PER_TICK)));

            float half = QUAD_HALF_EXTENT * scale * quad.scale;
            drawBillboardQuad(consumer, poseStack.last().pose(), half,
                    quad.colorR, quad.colorG, quad.colorB, alpha, packedSeed, i);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /**
     * Submits one water speck: a single, unrotated camera-facing rectangle.
     *
     * <p>It is deliberately just the shared camera billboard frame plus one quad, so a speck reads
     * as a crisp axis-aligned rectangle on screen. The hard edge comes from the shader: the sub-quad
     * channel is set to {@link GasCloudRenderType#SPEC_STYLE_CHANNEL}, which selects the branch that
     * skips the texture, the radial falloff and the noise mask.</p>
     */
    private static void renderSpec(VertexConsumer consumer, PoseStack poseStack, WaterSpec speck,
                                   float partialTick, float alpha, float cameraYaw, float cameraPitch,
                                   Vec3 origin) {
        Vec3 position = speck.getPosition(partialTick);
        poseStack.pushPose();
        poseStack.translate(position.x - origin.x, position.y - origin.y, position.z - origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

        float half = speck.getHalfExtent();
        // UV2.y carries the style channel, so the seed in UV2.x is irrelevant for a speck and 0 is
        // as good as any value; the shader never reads it in the spec branch.
        drawBillboardQuad(consumer, poseStack.last().pose(), half,
                speck.getColorRed(), speck.getColorGreen(), speck.getColorBlue(), alpha,
                0, GasCloudRenderType.SPEC_STYLE_CHANNEL);
        poseStack.popPose();
    }

    /**
     * Emits one camera-facing quad of the given half extent, centred on the current pose origin.
     *
     * <p>{@code UV2} is the lightmap slot. {@code VertexFormatElement.ELEMENT_UV2} is a signed
     * 16-bit pair, hence the clamp in {@link GasCloudRenderType#packSeed(float)}. The render type
     * sets {@code NO_LIGHTMAP}, so nothing else consumes this slot, and
     * {@code POSITION_COLOR_TEX_LIGHTMAP} has no overlay element, so no overlay coordinates are
     * written.</p>
     *
     * <p>V spans the whole quad ({@code v} in {@code [0, 1]}); the fragment shader divides it by
     * {@code gas_cloud.fsh}'s {@code GAS_TEXTURE_FRAMES}, which maps the quad onto frame 0 (the top
     * ninth) of the 9-frame INFESTIVE_GAS strip. This is the same division the 26.1.2 twin does, so
     * both trees sample exactly the same sprite. (The 1.20.1 tree used to scale V up in the shader
     * while emitting {@code v} in {@code [0, 1/9]} here; the two cancelled out and the whole
     * 9-frame strip was sampled instead of frame 0.)</p>
     */
    private static void drawBillboardQuad(VertexConsumer consumer, Matrix4f matrix, float half,
                                          float red, float green, float blue, float alpha,
                                          int packedSeed, int subQuadIndex) {
        // Top of the sprite is v = 0, bottom is v = 1 (the texture's frame 0 band is at the top).
        float vBottom = 1.0F;
        float vTop = 0.0F;

        // Bottom-left
        consumer.vertex(matrix, -half, -half, 0.0F)
                .color(red, green, blue, alpha)
                .uv(0.0F, vBottom)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
        // Bottom-right
        consumer.vertex(matrix, half, -half, 0.0F)
                .color(red, green, blue, alpha)
                .uv(1.0F, vBottom)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
        // Top-right
        consumer.vertex(matrix, half, half, 0.0F)
                .color(red, green, blue, alpha)
                .uv(1.0F, vTop)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
        // Top-left
        consumer.vertex(matrix, -half, half, 0.0F)
                .color(red, green, blue, alpha)
                .uv(0.0F, vTop)
                .uv2(packedSeed, subQuadIndex)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** Locale-independent three-decimal formatting for the space probe (log4j prints 17 digits). */
    public static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
