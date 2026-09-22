package org.tdddd.epca.impl.client.entity.gas;

import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeYelloweye;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GeckoLib render layer that draws the shader-rendered gas clouds owned by one entity.
 *
 * <p>Registered through {@code EpcaGeoRenderer#addLayerProvider(IGeoLayerProvider)} on the
 * longarms and yelloweye renderers, so it runs after the main model pass with a {@link PoseStack}
 * that has already been positioned by the entity render dispatcher. Every cloud position is
 * therefore converted with {@code cloudPos - renderedEntityPos}.</p>
 *
 * <p>Sub-quads are camera-facing billboards: the pose is first reset to the pass's pre-render
 * matrix (world aligned, no GeckoLib entity rotation), translated to the cloud, and rotated by the
 * camera orientation. Each sub-quad then applies its own fixed yaw/pitch/roll plus a slow spin,
 * scales by the current cloud scale and its own 0.6-1.6 multiplier, and is offset by its random
 * local position scaled by the cloud's outward spread factor (the "slowly spreading outward"
 * dissipation used by the yelloweye clouds).</p>
 *
 * <h2>26.1.2</h2>
 * <p>GeckoLib 5 split rendering into a render-state extraction phase and a submission phase, and
 * the entity render pipeline turned immediate drawing into {@code SubmitNodeCollector} submissions.
 * The layer therefore no longer receives a {@code MultiBufferSource} or a live {@code VertexConsumer};
 * it submits geometry through
 * {@link SubmitNodeCollector#submitCustomGeometry(PoseStack, RenderType, SubmitNodeCollector.CustomGeometryRenderer)}
 * with {@link GasCloudRenderType}'s custom pipeline. The per-draw uniforms of the 1.20.1 shader are
 * replaced by vertex data (fade in the colour alpha, per-cloud noise seed in the {@code UV1} slot),
 * as documented on {@link GasCloudRenderType}.</p>
 */
public final class GasCloudLayer implements IGeoLayerProvider {

    /** Minimum wall-clock gap between two diagnostics for the same entity. */
    private static final long DEBUG_REPEAT_MS = 1000L;

    /** Per-entity last diagnostics wall-clock, so the submit path logs at most once per second. */
    private static final Map<Integer, Long> DEBUG_LAST_LOG = new HashMap<>();

    /** Set once the pipeline has been observed missing, so the warning is not repeated every frame. */
    private static boolean debugPipelineWarned;

    /** Latches the single "geometry reached the collector" line so it prints once per session. */
    private static boolean debugFirstFlushLogged;

    /** Per-owner wall clock of the last numeric space probe line. */
    private static final Map<Integer, Long> DEBUG_LAST_PROBE = new HashMap<>();

    /** Per-owner wall clock of the last green positive-control quad. */
    private static final Map<Integer, Long> DEBUG_LAST_CONTROL = new HashMap<>();

    /** Half extent of the green positive-control quad, in blocks (0.75 => a 1.5 block square). */
    private static final float DEBUG_CONTROL_HALF_EXTENT = 0.75F;

    /**
     * Positive control #2 switch (one solid green quad through the built-in
     * {@code RenderTypes.debugQuads()} at the first cloud's world position).
     *
     * <p>It was switched on for one test round together with the shader's opaque control and proved
     * the space, the built-in submission path and the flush are all correct. It is <b>off</b> for
     * normal runs so no green square is ever submitted; flip it to true only while re-running that
     * control. Same name as the 1.20.1 twin so the two trees stay diff-able.</p>
     */
    private static final boolean DEBUG_SUBMIT_GREEN_CONTROL_QUAD = false;

    /**
     * Entity ids already reported as "reachable but no clouds tracked". Logged once per entity
     * instead of once per second: an idle mob legitimately has no clouds for minutes on end.
     */
    private static final Set<Integer> DEBUG_NO_CLOUD_REPORTED = new HashSet<>();

    /**
     * Shared instance for the longarms renderer. It is gated on
     * {@link ModEntities#RESHAPE_LONGARMS} so the renderer cannot accidentally draw clouds for a
     * different entity type.
     */
    private static final GasCloudLayer LONGARMS = new GasCloudLayer(ModEntities.RESHAPE_LONGARMS.get());

    /** Shared instance for the yelloweye renderer. */
    private static final GasCloudLayer YELLOWEYE = new GasCloudLayer(ModEntities.RESHAPE_YELLOWEYE.get());

    /** The entity type this layer instance is allowed to draw for. */
    private final EntityType<?> ownerType;

    private GasCloudLayer(EntityType<?> ownerType) {
        this.ownerType = ownerType;
    }

    /** The layer instance owned by the longarms renderer. */
    public static GasCloudLayer forLongarms() {
        return LONGARMS;
    }

    /** The layer instance owned by the yelloweye renderer. */
    public static GasCloudLayer forYelloweye() {
        return YELLOWEYE;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (!(entity instanceof LivingEntity living) || living.getType() != this.ownerType) {
            // Only warn when this really is a reshape mob attached to the wrong layer instance; every
            // other entity type flowing through the shared renderer is expected and not interesting.
            if (entity instanceof ReshapeLongarms || entity instanceof ReshapeYelloweye) {
                debugSkipped(entity.getId(), "owner-type mismatch: entity=" + entity.getType()
                        + " expected=" + this.ownerType);
            }
            return;
        }
        if (!(passInfo.renderState() instanceof EntityRenderState state)) {
            debugSkipped(living.getId(), "renderState is not an EntityRenderState: "
                    + passInfo.renderState().getClass().getName());
            return;
        }
        if (!GasCloudRenderType.isPipelineRegistered()) {
            // The custom pipeline is required; never draw a fallback.
            if (GasCloudManager.DEBUG && !debugPipelineWarned) {
                debugPipelineWarned = true;
                epca.LOGGER.warn("[gascloud] layer: PIPELINE NOT REGISTERED - the gas cloud geometry is "
                        + "being skipped for every entity. RegisterRenderPipelinesEvent either did not "
                        + "reach ClientHandler.onRegisterRenderPipelines or it threw.");
            }
            return;
        }
        List<GasCloud> clouds = GasCloudManager.getCloudsFor(entity.getId());
        if (clouds.isEmpty()) {
            if (GasCloudManager.DEBUG && DEBUG_NO_CLOUD_REPORTED.add(living.getId())) {
                epca.LOGGER.info("[gascloud] layer: REACHED entityId={} kind={} but the manager tracks no "
                                + "clouds for it (idle, or the manager never saw it) - {}",
                        living.getId(), living.getType(), GasCloudManager.debugSummary());
            }
            return;
        }

        float partialTick = state.partialTick;
        PoseStack poseStack = passInfo.poseStack();

        if (GasCloudManager.DEBUG) {
            // Numeric space probe: settles whether the pose basis really is the camera-relative
            // entity transform, and where the applied offset lands the cloud.
            debugTransformProbe(living, state, clouds.get(0), partialTick, passInfo);
            // Positive control: a built-in POSITION_COLOR pipeline submitted through the very same
            // submitCustomGeometry entry point, so it separates "our custom RenderType is broken"
            // from "this submission path is broken". Off by default: it is a visible green square.
            if (DEBUG_SUBMIT_GREEN_CONTROL_QUAD) {
                debugSubmitPositiveControl(living, collector, poseStack, state, clouds.get(0), partialTick,
                        passInfo);
            }
        }

        // The camera-relative billboard math and the submission live in GasCloudRenderer, shared
        // with ContaminatedWaterRenderer so both go through the same proven pipeline path.
        GasCloudRenderer.Stats stats = GasCloudRenderer.submit(collector, poseStack,
                passInfo.getPreRenderMatrixPose(), state.x, state.y, state.z, partialTick, clouds);

        if (GasCloudManager.DEBUG) {
            GasCloudManager.debugLayerDrew(stats.submitted);
            debugSubmitted(living, clouds.size(), stats.submitted, stats.minAlpha, stats.maxAlpha,
                    stats.quads);
            if (stats.submitted > 0 && !debugFirstFlushLogged) {
                // One-shot proof that the render type really reached the collector with geometry.
                debugFirstFlushLogged = true;
                epca.LOGGER.info("[gascloud] layer: FIRST FLUSH entityId={} ({}) clouds={} quads={} "
                                + "renderType={} shaderReady={} {}",
                        living.getId(), living.getType(), stats.submitted, stats.quads,
                        GasCloudRenderType.get(), GasCloudRenderType.isPipelineRegistered(),
                        GasCloudManager.debugSummary());
            }
        }
    }

    /**
     * Prints one line per second per owner: how many clouds the layer actually submitted, their
     * alpha range and the shader state. The whole block is dead code unless
     * {@link GasCloudManager#DEBUG} is on.
     */
    private static void debugSubmitted(LivingEntity entity, int tracked, int drawn,
                                       float minAlpha, float maxAlpha, int quads) {
        int ownerId = entity.getId();
        long now = System.currentTimeMillis();
        Long last = DEBUG_LAST_LOG.get(ownerId);
        if (last != null && now - last < DEBUG_REPEAT_MS) {
            return;
        }
        if (DEBUG_LAST_LOG.size() > 256) {
            // Never let the throttle map grow with every entity id a session ever saw.
            DEBUG_LAST_LOG.clear();
        }
        DEBUG_LAST_LOG.put(ownerId, now);
        epca.LOGGER.info("[gascloud] layer: drawn={}/{} quads={} owner={} ({}) shaderReady={} alpha={}..{}",
                drawn, tracked, quads, ownerId, entity.getType(), GasCloudRenderType.isPipelineRegistered(),
                drawn == 0 ? 0.0F : minAlpha, maxAlpha);
    }

    /** Logs why the layer drew nothing, at most once per second per entity id. */
    private static void debugSkipped(int entityId, String reason) {
        if (!GasCloudManager.DEBUG) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = DEBUG_LAST_LOG.get(entityId);
        if (previous != null && now - previous < DEBUG_REPEAT_MS) {
            return;
        }
        DEBUG_LAST_LOG.put(entityId, now);
        epca.LOGGER.info("[gascloud] layer: SKIP entityId={} {}", entityId, reason);
    }

    /**
     * Numeric space probe (diagnostics only), at most once per second per owner.
     *
     * <p>26.1.2 semantics this checks: {@code EntityRenderState.x/y/z} are ABSOLUTE interpolated
     * world coordinates ({@code EntityRenderer.extractRenderState} writes them from
     * {@code entity.xOld}/{@code entity.getX()}), while {@code LevelRenderer.submitEntities} passes
     * {@code state.x - camX} to {@code EntityRenderDispatcher.submit}, so the pose basis should be
     * the camera-relative entity transform and {@code cloudPos - state} should be the entity-local
     * offset that lands the cloud on {@code cloudPos - camera}. If
     * {@code basisMinus(entityAbs-camera)} is not ~0, the pose basis is not what the offset math
     * assumes and the basis has to be rebuilt from the camera instead.</p>
     */
    @SuppressWarnings("rawtypes")
    private static void debugTransformProbe(LivingEntity entity, EntityRenderState state, GasCloud cloud,
                                            float partialTick, RenderPassInfo passInfo) {
        int ownerId = entity.getId();
        long now = System.currentTimeMillis();
        Long last = DEBUG_LAST_PROBE.get(ownerId);
        if (last != null && now - last < DEBUG_REPEAT_MS) {
            return;
        }
        if (DEBUG_LAST_PROBE.size() > 256) {
            DEBUG_LAST_PROBE.clear();
        }
        DEBUG_LAST_PROBE.put(ownerId, now);

        // Translation of the pose the layer resets to: passInfo.getPreRenderMatrixPose().
        Matrix4f basis = passInfo.getPreRenderMatrixPose().pose();
        Vec3 basisTranslation = new Vec3(basis.m30(), basis.m31(), basis.m32());
        Vec3 entityAbs = entity.getPosition(partialTick);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        Vec3 cloudPos = cloud.getPosition(partialTick);
        Vec3 expected = new Vec3(cloudPos.x - camera.x, cloudPos.y - camera.y, cloudPos.z - camera.z);
        Vec3 offset = new Vec3(cloudPos.x - state.x, cloudPos.y - state.y, cloudPos.z - state.z);
        Vec3 basisExpected = new Vec3(entityAbs.x - camera.x, entityAbs.y - camera.y, entityAbs.z - camera.z);

        epca.LOGGER.info("[gascloud] probe: owner={} basisTranslation=({}, {}, {}) entityAbs=({}, {}, {}) "
                        + "stateXYZ=({}, {}, {}) camera=({}, {}, {}) appliedOffset=({}, {}, {}) "
                        + "expectedCameraRelative=({}, {}, {}) basisMinus=(entityAbs-camera)=({}, {}, {})",
                ownerId,
                fmt(basisTranslation.x), fmt(basisTranslation.y), fmt(basisTranslation.z),
                fmt(entityAbs.x), fmt(entityAbs.y), fmt(entityAbs.z),
                fmt(state.x), fmt(state.y), fmt(state.z),
                fmt(camera.x), fmt(camera.y), fmt(camera.z),
                fmt(offset.x), fmt(offset.y), fmt(offset.z),
                fmt(expected.x), fmt(expected.y), fmt(expected.z),
                fmt(basisTranslation.x - basisExpected.x),
                fmt(basisTranslation.y - basisExpected.y),
                fmt(basisTranslation.z - basisExpected.z));
    }

    /**
     * Positive control (diagnostics only), at most once per second per owner: submits one large
     * solid-green quad through {@code RenderTypes.debugQuads()} at the first cloud's world position,
     * using the same pose basis/offset/billboard rotation as the gas clouds.
     *
     * <p>That render type is {@code RenderPipelines.DEBUG_QUADS} over
     * {@code DefaultVertexFormat.POSITION_COLOR} ({@code Position}, {@code Color}), QUADS,
     * {@code BlendFunction.TRANSLUCENT}, depth {@code LESS_THAN_OR_EQUAL} without write and
     * {@code cull = false}, with no sampler - a built-in pipeline that needs no mod-side
     * registration. A visible green square proves geometry, space and the
     * {@code submitCustomGeometry} path are fine, which leaves the custom pipeline/RenderType or the
     * shader output as the fault; an invisible one proves the space or the submission path.</p>
     */
    @SuppressWarnings("rawtypes")
    private static void debugSubmitPositiveControl(LivingEntity entity, SubmitNodeCollector collector,
                                                   PoseStack poseStack, EntityRenderState state,
                                                   GasCloud cloud, float partialTick,
                                                   RenderPassInfo passInfo) {
        int ownerId = entity.getId();
        long now = System.currentTimeMillis();
        Long last = DEBUG_LAST_CONTROL.get(ownerId);
        if (last != null && now - last < DEBUG_REPEAT_MS) {
            return;
        }
        if (DEBUG_LAST_CONTROL.size() > 256) {
            DEBUG_LAST_CONTROL.clear();
        }
        DEBUG_LAST_CONTROL.put(ownerId, now);

        Vec3 position = cloud.getPosition(partialTick);
        Quaternionf cameraRotation =
                new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        float half = DEBUG_CONTROL_HALF_EXTENT;

        poseStack.pushPose();
        poseStack.last().set(passInfo.getPreRenderMatrixPose());
        poseStack.translate(position.x - state.x, position.y - state.y, position.z - state.z);
        poseStack.mulPose(cameraRotation);
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
            consumer.addVertex(pose, -half, -half, 0.0F).setColor(0.0F, 1.0F, 0.25F, 1.0F);
            consumer.addVertex(pose, half, -half, 0.0F).setColor(0.0F, 1.0F, 0.25F, 1.0F);
            consumer.addVertex(pose, half, half, 0.0F).setColor(0.0F, 1.0F, 0.25F, 1.0F);
            consumer.addVertex(pose, -half, half, 0.0F).setColor(0.0F, 1.0F, 0.25F, 1.0F);
        });
        poseStack.popPose();

        epca.LOGGER.info("[gascloud] control: green debugQuads quad owner={} pos=({}, {}, {}) half={} "
                        + "renderType={}",
                ownerId, fmt(position.x), fmt(position.y), fmt(position.z), half,
                RenderTypes.debugQuads());
    }

    /** Short numeric formatter for the diagnostics. */
    private static String fmt(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }
}
