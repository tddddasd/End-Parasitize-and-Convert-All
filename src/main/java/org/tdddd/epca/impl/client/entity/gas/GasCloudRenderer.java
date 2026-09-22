package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Shared, stateless drawing helper for every shader-rendered billboard of this mod.
 *
 * <p>The camera-relative billboard math and the submission through the custom gas cloud pipeline
 * used to live inside {@link GasCloudLayer}. They are extracted here so that both callers share
 * exactly one copy of the proven path:</p>
 * <ul>
 *   <li>{@link GasCloudLayer} (the reshape mobs: longarms and yelloweye), which resets the pose to
 *       the GeckoLib pass's pre-render matrix, and</li>
 *   <li>{@code ContaminatedWaterRenderer} (the water entity), whose pose stack already is the
 *       camera-relative entity transform built by {@code EntityRenderDispatcher#submit}.</li>
 * </ul>
 *
 * <p>Every billboard is drawn the same way: push, reset the top pose to the caller's world-aligned
 * basis, translate by {@code elementPos - anchor}, rotate by the camera orientation, submit the
 * quads through {@link SubmitNodeCollector#submitCustomGeometry} with
 * {@link GasCloudRenderType#get()}, then pop. The quads themselves are emitted by
 * {@link #emitQuad}, which is the only place the four billboard vertices are written; the gas style
 * passes white as the vertex colour, the spec style passes its own dark red.</p>
 *
 * <h2>26.1.2</h2>
 * <p>Two type parameters and the extract/submit split make the rendered entity unavailable during
 * submission, so the callers pass the already-extracted values ({@code state.x/y/z} as the anchor)
 * instead of the live entity. The pose is copied by the collector, so the callback may safely run
 * after the pop.</p>
 */
public final class GasCloudRenderer {

    /** Half-size of one sub-quad billboard, in blocks per unit of scale. */
    public static final float QUAD_HALF_EXTENT = 0.5F;

    /** Rotation speed (degrees per tick) of an individual sub-quad around its own fixed roll. */
    public static final float SPIN_DEGREES_PER_TICK = 1.4F;

    /** Alpha below which a billboard is not worth submitting. */
    public static final float MIN_VISIBLE_ALPHA = 0.004F;

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private GasCloudRenderer() {
    }

    /**
     * Statistics of one submit pass. {@link GasCloudLayer} feeds them into its existing
     * {@code [gascloud]} diagnostics; the water renderer only checks whether anything was drawn.
     */
    public static final class Stats {
        /** Billboards that passed the alpha cull and were submitted. */
        public int submitted;
        /** Geometry quads submitted. */
        public int quads;
        /** Lowest submitted alpha, or {@link Float#MAX_VALUE} when nothing was submitted. */
        public float minAlpha = Float.MAX_VALUE;
        /** Highest submitted alpha, or 0 when nothing was submitted. */
        public float maxAlpha;
    }

    /** Emits the quads of one billboard in the already camera-facing local frame. */
    @FunctionalInterface
    private interface QuadEmitter {
        void emit(VertexConsumer consumer, PoseStack.Pose pose, float partialTick, float alpha);
    }

    // =================================================================
    //  Entry points
    // =================================================================

    /**
     * Submits every visible gas cloud of the given list.
     *
     * @param basis     the world-aligned, camera-relative entity pose the billboards are anchored
     *                  to (the GeckoLib pre-render matrix for the reshape layer, the entity
     *                  dispatch pose for the water entity)
     * @param anchorX   interpolated world X of the rendered entity ({@code EntityRenderState#x})
     * @param anchorY   interpolated world Y of the rendered entity
     * @param anchorZ   interpolated world Z of the rendered entity
     */
    public static Stats submit(SubmitNodeCollector collector, PoseStack poseStack, PoseStack.Pose basis,
                               double anchorX, double anchorY, double anchorZ,
                               float partialTick, List<GasCloud> clouds) {
        Stats stats = new Stats();
        if (clouds.isEmpty()) {
            return stats;
        }
        RenderContext context = RenderContext.of();
        for (GasCloud cloud : clouds) {
            float alpha = cloud.getAlpha(partialTick);
            if (alpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            Vec3 position = cloud.getPosition(partialTick);
            submitBillboard(collector, poseStack, basis, anchorX, anchorY, anchorZ, context, position,
                    partialTick, alpha,
                    (consumer, pose, tick, fade) -> emitCloud(consumer, pose, cloud, tick, fade));
            stats.submitted++;
            stats.quads += cloud.getSubQuadCount();
            stats.minAlpha = Math.min(stats.minAlpha, alpha);
            stats.maxAlpha = Math.max(stats.maxAlpha, alpha);
        }
        return stats;
    }

    /**
     * Submits every visible water speck of the given list. Specks use the same pipeline, the same
     * pose math and the same quad writer as the gas clouds; only the fragment style differs, which
     * is selected by {@link GasCloudRenderType#SPEC_STYLE_CHANNEL}.
     */
    public static Stats submitSpecs(SubmitNodeCollector collector, PoseStack poseStack, PoseStack.Pose basis,
                                    double anchorX, double anchorY, double anchorZ,
                                    float partialTick, List<WaterSpec> specs) {
        Stats stats = new Stats();
        if (specs.isEmpty()) {
            return stats;
        }
        RenderContext context = RenderContext.of();
        for (WaterSpec spec : specs) {
            float alpha = spec.getAlpha(partialTick);
            if (alpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            Vec3 position = spec.getPosition(partialTick);
            submitBillboard(collector, poseStack, basis, anchorX, anchorY, anchorZ, context, position,
                    partialTick, alpha,
                    (consumer, pose, tick, fade) -> emitSpec(consumer, pose, spec, fade));
            stats.submitted++;
            stats.quads += 1;
            stats.minAlpha = Math.min(stats.minAlpha, alpha);
            stats.maxAlpha = Math.max(stats.maxAlpha, alpha);
        }
        return stats;
    }

    // =================================================================
    //  Billboard / pipeline path
    // =================================================================

    /** The per-call rendering state both entry points need (render type plus camera rotation). */
    private static final class RenderContext {
        private final RenderType renderType;
        private final Quaternionf cameraRotation;

        private RenderContext(RenderType renderType, Quaternionf cameraRotation) {
            this.renderType = renderType;
            this.cameraRotation = cameraRotation;
        }

        private static RenderContext of() {
            // A copy, because Camera#rotation() hands out its live quaternion.
            Quaternionf rotation =
                    new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            return new RenderContext(GasCloudRenderType.get(), rotation);
        }
    }

    /**
     * Draws one billboard at a world position through the proven camera-relative path: reset the
     * top pose to the caller's basis, translate by {@code position - anchor}, apply the camera
     * billboard rotation and submit the quads with the custom render type.
     */
    private static void submitBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                        PoseStack.Pose basis,
                                        double anchorX, double anchorY, double anchorZ,
                                        RenderContext context, Vec3 position,
                                        float partialTick, float alpha, QuadEmitter emitter) {
        poseStack.pushPose();
        // See EndermanAfterimageLayer: the live pose may already contain the entity rotation, so
        // the world-aligned transform is rebuilt from the caller's basis.
        poseStack.last().set(basis);
        poseStack.translate(position.x - anchorX, position.y - anchorY, position.z - anchorZ);
        poseStack.mulPose(context.cameraRotation);

        // The pose is copied by the collector, so the callback can safely run after the pop.
        collector.submitCustomGeometry(poseStack, context.renderType,
                (pose, consumer) -> emitter.emit(consumer, pose, partialTick, alpha));

        poseStack.popPose();
    }

    // =================================================================
    //  Quad emission
    // =================================================================

    /** Emits every sub-quad of one gas cloud in the already camera-facing local frame. */
    public static void emitCloud(VertexConsumer consumer, PoseStack.Pose pose, GasCloud cloud,
                                 float partialTick, float alpha) {
        float scale = cloud.getScale(partialTick);
        float spread = cloud.getSpreadFactor(partialTick);
        int age = cloud.getAge();
        int seedChannel = GasCloudRenderType.packSeed(cloud.getRenderSeed());

        for (int index = 0; index < cloud.getSubQuadCount(); index++) {
            GasCloud.SubQuad quad = cloud.getSubQuad(index);
            Quaternionf rotation = new Quaternionf()
                    .rotateY(quad.yaw * DEG_TO_RAD)
                    .rotateX(quad.pitch * DEG_TO_RAD)
                    .rotateZ((quad.roll + quad.spinPhase + age * quad.spinSpeed * SPIN_DEGREES_PER_TICK)
                            * DEG_TO_RAD);
            float half = QUAD_HALF_EXTENT * scale * quad.scale;
            Vector3f right = rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F)).mul(half);
            Vector3f up = rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F)).mul(half);

            // The gas style keeps the white vertex colour: the tint comes from the shader.
            emitQuad(consumer, pose,
                    quad.offsetX * scale * spread,
                    quad.offsetY * scale * spread,
                    quad.offsetZ * scale * spread,
                    right, up,
                    1.0F, 1.0F, 1.0F, alpha,
                    seedChannel, index);
        }
    }

    /**
     * Emits the single hard-edged quad of one water speck. The quad is axis aligned inside the
     * camera-facing local frame (no per-quad rotation), which is what makes it read as a crisp tiny
     * rectangle instead of a puff. The dark red travels in the vertex colour and the style channel
     * switches the fragment shader to the solid style.
     */
    public static void emitSpec(VertexConsumer consumer, PoseStack.Pose pose, WaterSpec spec, float alpha) {
        float half = spec.getHalfExtent();
        Vector3f right = new Vector3f(half, 0.0F, 0.0F);
        Vector3f up = new Vector3f(0.0F, half, 0.0F);
        emitQuad(consumer, pose, 0.0F, 0.0F, 0.0F, right, up,
                WaterSpec.WATER_SPEC_COLOR_RED, WaterSpec.WATER_SPEC_COLOR_GREEN,
                WaterSpec.WATER_SPEC_COLOR_BLUE, alpha,
                0, GasCloudRenderType.SPEC_STYLE_CHANNEL);
    }

    /**
     * Emits one camera-facing quad of the given half extent around the given centre.
     * The vertices are unlit: the pipeline declares no lightmap sampler and the alpha carries the
     * element's lifetime fade.
     *
     * @param red         vertex colour red (1.0 for the gas style, the speck red for the spec style)
     * @param seedChannel the packed per-cloud noise seed (see
     *                    {@link GasCloudRenderType#packSeed(float)})
     * @param styleChannel the sub-quad index for the gas style, or
     *                     {@link GasCloudRenderType#SPEC_STYLE_CHANNEL} for the hard-edged style
     */
    public static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                float centerX, float centerY, float centerZ,
                                Vector3f right, Vector3f up,
                                float red, float green, float blue, float alpha,
                                int seedChannel, int styleChannel) {
        // Bottom-left
        emitVertex(consumer, pose, centerX - right.x - up.x, centerY - right.y - up.y,
                centerZ - right.z - up.z, 0.0F, 1.0F, red, green, blue, alpha, seedChannel, styleChannel);
        // Bottom-right
        emitVertex(consumer, pose, centerX + right.x - up.x, centerY + right.y - up.y,
                centerZ + right.z - up.z, 1.0F, 1.0F, red, green, blue, alpha, seedChannel, styleChannel);
        // Top-right
        emitVertex(consumer, pose, centerX + right.x + up.x, centerY + right.y + up.y,
                centerZ + right.z + up.z, 1.0F, 0.0F, red, green, blue, alpha, seedChannel, styleChannel);
        // Top-left
        emitVertex(consumer, pose, centerX - right.x + up.x, centerY - right.y + up.y,
                centerZ - right.z + up.z, 0.0F, 0.0F, red, green, blue, alpha, seedChannel, styleChannel);
    }

    /**
     * Writes one vertex. The element sequence must follow
     * {@link GasCloudRenderType#GAS_CLOUD_VERTEX_FORMAT}: Position, Color, UV0, UV1.
     */
    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   float x, float y, float z, float u, float v,
                                   float red, float green, float blue, float alpha,
                                   int seedChannel, int styleChannel) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setUv1(seedChannel, styleChannel);
    }
}
