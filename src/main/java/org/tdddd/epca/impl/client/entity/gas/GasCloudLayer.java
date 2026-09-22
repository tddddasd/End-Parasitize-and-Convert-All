package org.tdddd.epca.impl.client.entity.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GeckoLib render layer that draws the shader-rendered gas clouds owned by one entity.
 *
 * <p>Registered through {@link EpcaGeoRenderer#addLayerProvider(IGeoLayerProvider)} on the
 * longarms and yelloweye renderers, so it runs after the main model pass with a {@link PoseStack}
 * already translated to the entity's world position.</p>
 *
 * <p>The layer is only a thin adapter now: it decides whether this owner may draw at all, fetches
 * the clouds the manager holds for it and hands everything to
 * {@link GasCloudRenderer#submit(net.minecraft.world.entity.Entity, PoseStack,
 * MultiBufferSource, float, List)}, which owns the camera-relative billboard frame, the per-quad
 * math and the custom pipeline. The {@code epca:contaminated_water} renderer calls the very same
 * helper, so both features are guaranteed to draw through the proven path, and a fix to that path
 * cannot fix one and miss the other.</p>
 *
 * <p>All clouds are drawn through the custom {@link GasCloudRenderType}, which is backed by the
 * custom core shader in {@code assets/epca/shaders/core/gas_cloud.json}. When that shader is not
 * built yet (or failed to build) the layer draws nothing rather than falling back silently.</p>
 *
 * <p>Diagnostics: while {@link GasCloudManager#DEBUG} is on, the submit path prints one
 * {@code [gascloud] layer:} line per second per owning entity (clouds drawn, alpha range, shader
 * state), one {@code [gascloud] layer space:} line with the pose/camera/cloud numbers, and one
 * single line if the shader was never built. Turning that flag off removes every log statement
 * from this class.</p>
 *
 * <h2>Pose space (proved, not assumed)</h2>
 * <p>The PoseStack a GeckoLib render layer receives is in <b>camera-relative world space</b>:
 * {@code LevelRenderer.renderEntity} passes {@code Mth.lerp(partialTick, entity.xOld, entity.getX()) - camX}
 * (and the same for Y/Z) to {@code EntityRenderDispatcher.render}, which does
 * {@code poseStack.pushPose(); poseStack.translate(x + renderOffset.x, ...)} before calling the
 * renderer; {@code EntityRenderer#getRenderOffset} defaults to {@link Vec3#ZERO} and neither
 * {@code EpcaGeoRenderer} nor GeckoLib overrides it. GeckoLib's {@code actuallyRender} keeps its
 * entity rotation inside its own push/pop and {@code scaleModelForRender} does nothing while
 * {@code scaleWidth/scaleHeight == 1}, so at layer time the pose translation column equals
 * {@code entityInterpolatedPos - cameraPos} exactly.</p>
 * <p>{@link GasCloudRenderer} therefore offsets every cloud by
 * {@code cloudPos - entity.getPosition(partialTick)} and the resulting vertex space position is
 * {@code cloudPos - cameraPos}. The {@code [gascloud] layer space:} line proves it numerically:
 * {@code basisResidual} must be ~0 and {@code final} must equal {@code expected=cloud-camera}.</p>
 */
public final class GasCloudLayer implements IGeoLayerProvider {

    /**
     * Shared instance for the longarms renderer. It is gated on
     * {@link ModEntities#RESHAPE_LONGARMS} so the same instance can safely be attached to a
     * renderer that also serves other entity types.
     */
    private static final GasCloudLayer LONGARMS =
            new GasCloudLayer(ModEntities.RESHAPE_LONGARMS.get());

    /** Shared instance for the yelloweye renderer. */
    private static final GasCloudLayer YELLOWEYE =
            new GasCloudLayer(ModEntities.RESHAPE_YELLOWEYE.get());

    /** Entity raw types this layer instance is allowed to draw for. */
    private final EntityType<?>[] ownerTypes;

    // =================================================================
    //  Diagnostics (see GasCloudManager#DEBUG)
    // =================================================================

    /** Per-owner wall clock of the last "clouds drawn" line, so it prints at most once a second. */
    private static final Map<Integer, Long> DEBUG_LAST_SUBMIT_LOG = new HashMap<>();
    /** Latches the single "shader is not ready" line so it cannot spam the log. */
    private static boolean debugShaderWarned;

    /** Milliseconds between two "clouds drawn" lines for the same owner. */
    private static final long DEBUG_SUBMIT_LOG_INTERVAL_MS = 1000L;

    /**
     * Positive control #2 switch (one solid green quad through the built-in
     * {@link RenderType#debugQuads()} at the cloud's position). Off for normal runs.
     */
    private static final boolean DEBUG_SUBMIT_GREEN_CONTROL_QUAD = false;

    private GasCloudLayer(EntityType<?>... ownerTypes) {
        this.ownerTypes = ownerTypes;
    }

    /** The layer instance owned by the longarms renderer. */
    public static GasCloudLayer forLongarms() {
        return LONGARMS;
    }

    /** The layer instance owned by the yelloweye renderer. */
    public static GasCloudLayer forYelloweye() {
        return YELLOWEYE;
    }

    private boolean isOwner(LivingEntity entity) {
        for (EntityType<?> type : this.ownerTypes) {
            if (entity.getType() == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void renderAdditionalLayer(
            EpcaGeoRenderer renderer,
            LivingEntity entity,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            PoseStack poseStack,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (!isOwner(entity)) {
            return;
        }
        if (!GasCloudRenderType.isShaderReady()) {
            // The custom core shader is required; never draw a fallback. Reported once, even before
            // any cloud exists, because it is the one failure that silently kills the whole feature.
            if (GasCloudManager.DEBUG && !debugShaderWarned) {
                debugShaderWarned = true;
                epca.LOGGER.info("[gascloud] layer: shader NOT ready - clouds for owner={} ({}) are not drawn; "
                                + "RegisterShadersEvent -> GasCloudRenderType.registerShader never ran",
                        entity.getId(), entity.getType());
            }
            return;
        }
        List<GasCloud> clouds = GasCloudManager.getCloudsFor(entity.getId());
        if (clouds.isEmpty()) {
            return;
        }

        GasCloudRenderer.Submission submission =
                GasCloudRenderer.submit(entity, poseStack, bufferSource, partialTick, clouds);

        if (DEBUG_SUBMIT_GREEN_CONTROL_QUAD && GasCloudManager.DEBUG && submission.probeCloud != null) {
            // Positive control #2: the same world position, drawn with a BUILT-IN known-good
            // RenderType (RenderType.debugQuads()) instead of the custom shader. Off for normal
            // runs (see DEBUG_SUBMIT_GREEN_CONTROL_QUAD). Left in place because it is the cheapest
            // way to re-run that control when the custom pipeline is ever suspected again.
            drawDebugQuad(bufferSource, poseStack, submission, entity, partialTick);
        }
        if (GasCloudManager.DEBUG && submission.cloudsDrawn > 0
                && debugWindowOpen(entity.getId())) {
            logSubmit(entity, submission);
            if (submission.probeCloud != null) {
                logSpace(entity, submission, partialTick);
            }
        }
    }

    /**
     * Opens (at most once per second per owner) the throttling window shared by the
     * {@code layer: drawn=...} and {@code layer space: ...} lines. The whole block is dead code
     * unless {@link GasCloudManager#DEBUG} is on.
     */
    private static boolean debugWindowOpen(int ownerId) {
        return GasCloudManager.debugWindowOpen(DEBUG_LAST_SUBMIT_LOG, ownerId,
                DEBUG_SUBMIT_LOG_INTERVAL_MS);
    }

    /**
     * Positive control #2: one solid green, camera-facing 1.5 block quad at the cloud's world
     * position, submitted through the built-in {@link RenderType#debugQuads()}.
     *
     * <p>It is deliberately drawn through the vanilla type, i.e. without the custom gas shader,
     * custom texture or custom vertex format, so it isolates "nothing from this layer reaches the
     * screen" from "the custom render type/shader produces nothing".</p>
     */
    private static void drawDebugQuad(MultiBufferSource bufferSource, PoseStack poseStack,
                                      GasCloudRenderer.Submission submission,
                                      LivingEntity entity, float partialTick) {
        Vec3 center = submission.probeCloud;
        Vec3 origin = entity.getPosition(partialTick);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        poseStack.pushPose();
        poseStack.translate(center.x - origin.x, center.y - origin.y, center.z - origin.z);
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                -minecraft.gameRenderer.getMainCamera().getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                minecraft.gameRenderer.getMainCamera().getXRot()));
        org.joml.Matrix4f matrix = poseStack.last().pose();
        float half = GasCloudRenderer.QUAD_HALF_EXTENT * 1.5F;
        // RenderType.debugQuads() is DefaultVertexFormat.POSITION_COLOR, so only vertex + color
        // are required; the format has no tex/lightmap/normal element.
        debugVertex(consumer, matrix, -half, -half);
        debugVertex(consumer, matrix, half, -half);
        debugVertex(consumer, matrix, half, half);
        debugVertex(consumer, matrix, -half, half);
        poseStack.popPose();
    }

    private static void debugVertex(VertexConsumer consumer, org.joml.Matrix4f matrix, float x, float y) {
        consumer.vertex(matrix, x, y, 0.0F).color(0.0F, 1.0F, 0.0F, 1.0F).endVertex();
    }

    /**
     * Numeric proof that a cloud lands where it should: the pose's translation column is read
     * before the cloud offset is applied, so
     * {@code poseBase + applied} must equal {@code cloudPos - cameraPos} and
     * {@code poseBase} must equal {@code entityInterp - cameraPos}.
     */
    private static void logSpace(LivingEntity entity, GasCloudRenderer.Submission submission,
                                 float partialTick) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 entityInterp = entity.getPosition(partialTick);
        Vec3 cloudPos = submission.probeCloud;
        epca.LOGGER.info("[gascloud] layer space: poseBase=({},{},{}) entityInterp=({},{},{}) "
                        + "camera=({},{},{}) cloudAbs=({},{},{}) applied=({},{},{}) "
                        + "final=poseBase+applied=({},{},{}) expected=cloud-camera=({},{},{}) "
                        + "basisResidual=({},{},{})",
                fmt(submission.poseBaseX), fmt(submission.poseBaseY), fmt(submission.poseBaseZ),
                fmt(entityInterp.x), fmt(entityInterp.y), fmt(entityInterp.z),
                fmt(camera.x), fmt(camera.y), fmt(camera.z),
                fmt(cloudPos.x), fmt(cloudPos.y), fmt(cloudPos.z),
                fmt(submission.probeAppliedX), fmt(submission.probeAppliedY), fmt(submission.probeAppliedZ),
                fmt(submission.poseBaseX + submission.probeAppliedX),
                fmt(submission.poseBaseY + submission.probeAppliedY),
                fmt(submission.poseBaseZ + submission.probeAppliedZ),
                fmt(cloudPos.x - camera.x), fmt(cloudPos.y - camera.y), fmt(cloudPos.z - camera.z),
                fmt(submission.poseBaseX - (entityInterp.x - camera.x)),
                fmt(submission.poseBaseY - (entityInterp.y - camera.y)),
                fmt(submission.poseBaseZ - (entityInterp.z - camera.z)));
    }

    /** Locale-independent three-decimal formatting for the space probe (log4j would print 17 digits). */
    private static String fmt(double value) {
        return GasCloudRenderer.fmt(value);
    }

    /**
     * Prints one line per second per owner: how many clouds the layer actually submitted, their
     * alpha range and the shader state. Dead code unless {@link GasCloudManager#DEBUG} is on.
     */
    private static void logSubmit(LivingEntity entity, GasCloudRenderer.Submission submission) {
        epca.LOGGER.info("[gascloud] layer: drawn={}/{} owner={} ({}) shaderReady={} alpha={}..{}",
                submission.cloudsDrawn, submission.cloudsTracked,
                entity.getId(), entity.getType(), GasCloudRenderType.isShaderReady(),
                fmt(submission.minAlpha), fmt(submission.maxAlpha));
    }
}
