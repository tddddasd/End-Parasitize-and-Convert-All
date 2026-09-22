package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
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

    /**
     * How a billboard is turned towards the viewer.
     *
     * <p>{@link #CAMERA_FACING} applies the camera's whole orientation, which is what the gas clouds
     * and the water specks have always done. {@link #YAW_ONLY} keeps only the camera's rotation
     * around the world Y axis, so the quad stands upright and never tilts with the camera pitch; it
     * is the same projection vanilla uses for the fire overlay
     * ({@code Mth.rotationAroundAxis(Mth.Y_AXIS, camera.orientation, ...)}) and it is what the soul
     * protection flame uses so the column always stands vertically.</p>
     */
    public enum BillboardMode {
        /** Follow the camera's whole orientation, pitch included. */
        CAMERA_FACING,
        /** Follow only the camera's yaw; the quad stays upright. */
        YAW_ONLY
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
            submitBillboardAt(collector, poseStack, basis, anchorX, anchorY, anchorZ, context, position,
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
            submitBillboardAt(collector, poseStack, basis, anchorX, anchorY, anchorZ, context, position,
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

    /**
     * Submits one camera-facing billboard quad that is not a gas cloud or a water speck.
     *
     * <p>It is the public face of the shared submission path for callers that own their own quad
     * geometry, so they inherit the proven camera frame, the custom core shader and the exact vertex
     * layout instead of duplicating them. The soul-protection flame and its embers of
     * {@code impl/client/entity/heart/SoulProtectionHeartRenderer} are the only callers. It mirrors
     * the 1.20.1 twin's {@code submitBillboard} parameter for parameter, except that 26.1.2 has no
     * entity at submission time: the caller passes the render state's interpolated world position as
     * {@code anchorX/Y/Z} plus the same world-aligned offset it would have passed there.</p>
     *
     * <p>The quad is placed at {@code anchor + (offsetX, offsetY, offsetZ)} on world-aligned axes
     * (the offset is applied before the camera rotation, so it is not rotated by the billboard
     * frame), and it may be non-square: the flame column is {@code width : height = 1 : 2.1}.</p>
     *
     * @param basis          camera-relative entity pose (see the class comment)
     * @param anchorX        interpolated world X of the rendered entity
     * @param anchorY        interpolated world Y of the rendered entity
     * @param anchorZ        interpolated world Z of the rendered entity
     * @param offsetX        world-aligned offset from the anchor
     * @param offsetY        world-aligned offset from the anchor
     * @param offsetZ        world-aligned offset from the anchor
     * @param halfWidth      half extent of the quad to the right of its centre
     * @param halfHeight     half extent of the quad above its centre
     * @param red            vertex colour red; the shader style decides what it means
     * @param green          vertex colour green; the shader style decides what it means
     * @param blue           vertex colour blue; the shader style decides what it means
     * @param alpha          vertex colour alpha, i.e. the caller's per-quad fade
     * @param packedSeed     value for the seed channel ({@code UV1.x})
     * @param styleChannel   value for the style channel ({@code UV1.y}); one of the style channels,
     *                       e.g. {@link GasCloudRenderType#HEART_STYLE_CHANNEL}
     * @param renderType     the pipeline to draw through: {@link GasCloudRenderType#get()} for the
     *                       ordinary translucent blend or {@link GasCloudRenderType#getAdditive()}
     *                       for the emissive one
     */
    public static void submitBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                       PoseStack.Pose basis,
                                       double anchorX, double anchorY, double anchorZ,
                                       double offsetX, double offsetY, double offsetZ,
                                       float halfWidth, float halfHeight,
                                       float red, float green, float blue, float alpha,
                                       int packedSeed, int styleChannel, RenderType renderType) {
        submitBillboard(collector, poseStack, basis, anchorX, anchorY, anchorZ,
                offsetX, offsetY, offsetZ, halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, styleChannel, renderType,
                BillboardMode.CAMERA_FACING);
    }

    /**
     * Same as {@link #submitBillboard(SubmitNodeCollector, PoseStack, PoseStack.Pose, double, double,
     * double, double, double, double, float, float, float, float, float, float, int, int, RenderType)}
     * but with an explicit {@link BillboardMode}, so a caller can ask for an upright (yaw only)
     * quad. The gas clouds and the water specks always pass {@link BillboardMode#CAMERA_FACING}, so
     * their behaviour is unchanged.
     */
    public static void submitBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                       PoseStack.Pose basis,
                                       double anchorX, double anchorY, double anchorZ,
                                       double offsetX, double offsetY, double offsetZ,
                                       float halfWidth, float halfHeight,
                                       float red, float green, float blue, float alpha,
                                       int packedSeed, int styleChannel, RenderType renderType,
                                       BillboardMode mode) {
        Vec3 position = new Vec3(anchorX + offsetX, anchorY + offsetY, anchorZ + offsetZ);
        submitBillboardAt(collector, poseStack, basis, anchorX, anchorY, anchorZ,
                RenderContext.of(renderType, mode), position, 0.0F, alpha,
                (consumer, pose, tick, fade) -> emitQuad(consumer, pose, 0.0F, 0.0F, 0.0F,
                        new Vector3f(halfWidth, 0.0F, 0.0F), new Vector3f(0.0F, halfHeight, 0.0F),
                        red, green, blue, alpha, packedSeed, styleChannel));
    }

    /**
     * Submits one billboard quad that rotates around the WORLD Y AXIS ONLY: it uses the camera's yaw
     * but not its pitch, so the quad stays perfectly upright however the player looks up or down.
     *
     * <p>It is the 26.1.2 face of the 1.20.1 twin's method of the same name; the projection itself
     * is {@code Mth.rotationAroundAxis(Mth.Y_AXIS, cameraOrientation, ...)}, the same helper vanilla
     * uses for the fire overlay. The soul protection flame and its embers are the only callers.</p>
     */
    public static void submitVerticalBillboard(SubmitNodeCollector collector, PoseStack poseStack,
                                               PoseStack.Pose basis,
                                               double anchorX, double anchorY, double anchorZ,
                                               double offsetX, double offsetY, double offsetZ,
                                               float halfWidth, float halfHeight,
                                               float red, float green, float blue, float alpha,
                                               int packedSeed, int styleChannel, RenderType renderType) {
        submitBillboard(collector, poseStack, basis, anchorX, anchorY, anchorZ,
                offsetX, offsetY, offsetZ, halfWidth, halfHeight,
                red, green, blue, alpha, packedSeed, styleChannel, renderType,
                BillboardMode.YAW_ONLY);
    }

    /** The per-call rendering state both entry points need (render type plus camera rotation). */
    private static final class RenderContext {
        private final RenderType renderType;
        private final Quaternionf cameraRotation;

        private RenderContext(RenderType renderType, Quaternionf cameraRotation) {
            this.renderType = renderType;
            this.cameraRotation = cameraRotation;
        }

        private static RenderContext of() {
            return of(GasCloudRenderType.get(), BillboardMode.CAMERA_FACING);
        }

        private static RenderContext of(RenderType renderType, BillboardMode mode) {
            // A copy, because Camera#rotation() hands out its live quaternion.
            Quaternionf rotation =
                    new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            if (mode == BillboardMode.YAW_ONLY) {
                // Keep only the yaw part of the camera orientation (vanilla's own projection, used
                // for the fire overlay), so the quad stays vertical whatever the camera pitch is.
                rotation = Mth.rotationAroundAxis(Mth.Y_AXIS, rotation, new Quaternionf());
            }
            return new RenderContext(renderType, rotation);
        }
    }

    /**
     * Draws one billboard at a world position through the proven camera-relative path: reset the
     * top pose to the caller's basis, translate by {@code position - anchor}, apply the camera
     * billboard rotation and submit the quads with the custom render type.
     */
    private static void submitBillboardAt(SubmitNodeCollector collector, PoseStack poseStack,
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
