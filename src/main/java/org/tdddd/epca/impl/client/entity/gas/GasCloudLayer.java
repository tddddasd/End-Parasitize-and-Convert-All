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

public final class GasCloudLayer implements IGeoLayerProvider {

    private static final long DEBUG_REPEAT_MS = 1000L;

    private static final Map<Integer, Long> DEBUG_LAST_LOG = new HashMap<>();

    private static boolean debugPipelineWarned;

    private static boolean debugFirstFlushLogged;

    private static final Map<Integer, Long> DEBUG_LAST_PROBE = new HashMap<>();

    private static final Map<Integer, Long> DEBUG_LAST_CONTROL = new HashMap<>();

    private static final float DEBUG_CONTROL_HALF_EXTENT = 0.75F;

    private static final boolean DEBUG_SUBMIT_GREEN_CONTROL_QUAD = false;

    private static final Set<Integer> DEBUG_NO_CLOUD_REPORTED = new HashSet<>();

    private static final GasCloudLayer LONGARMS = new GasCloudLayer(ModEntities.RESHAPE_LONGARMS.get());

    private static final GasCloudLayer YELLOWEYE = new GasCloudLayer(ModEntities.RESHAPE_YELLOWEYE.get());

    private final EntityType<?> ownerType;

    private GasCloudLayer(EntityType<?> ownerType) {
        this.ownerType = ownerType;
    }

    public static GasCloudLayer forLongarms() {
        return LONGARMS;
    }

    public static GasCloudLayer forYelloweye() {
        return YELLOWEYE;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (!(entity instanceof LivingEntity living) || living.getType() != this.ownerType) {

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

            debugTransformProbe(living, state, clouds.get(0), partialTick, passInfo);

            if (DEBUG_SUBMIT_GREEN_CONTROL_QUAD) {
                debugSubmitPositiveControl(living, collector, poseStack, state, clouds.get(0), partialTick,
                        passInfo);
            }
        }

        GasCloudRenderer.Stats stats = GasCloudRenderer.submit(collector, poseStack,
                passInfo.getPreRenderMatrixPose(), state.x, state.y, state.z, partialTick, clouds);

        if (GasCloudManager.DEBUG) {
            GasCloudManager.debugLayerDrew(stats.submitted);
            debugSubmitted(living, clouds.size(), stats.submitted, stats.minAlpha, stats.maxAlpha,
                    stats.quads);
            if (stats.submitted > 0 && !debugFirstFlushLogged) {

                debugFirstFlushLogged = true;
                epca.LOGGER.info("[gascloud] layer: FIRST FLUSH entityId={} ({}) clouds={} quads={} "
                                + "renderType={} shaderReady={} {}",
                        living.getId(), living.getType(), stats.submitted, stats.quads,
                        GasCloudRenderType.get(), GasCloudRenderType.isPipelineRegistered(),
                        GasCloudManager.debugSummary());
            }
        }
    }

    private static void debugSubmitted(LivingEntity entity, int tracked, int drawn,
                                       float minAlpha, float maxAlpha, int quads) {
        int ownerId = entity.getId();
        long now = System.currentTimeMillis();
        Long last = DEBUG_LAST_LOG.get(ownerId);
        if (last != null && now - last < DEBUG_REPEAT_MS) {
            return;
        }
        if (DEBUG_LAST_LOG.size() > 256) {

            DEBUG_LAST_LOG.clear();
        }
        DEBUG_LAST_LOG.put(ownerId, now);
        epca.LOGGER.info("[gascloud] layer: drawn={}/{} quads={} owner={} ({}) shaderReady={} alpha={}..{}",
                drawn, tracked, quads, ownerId, entity.getType(), GasCloudRenderType.isPipelineRegistered(),
                drawn == 0 ? 0.0F : minAlpha, maxAlpha);
    }

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

    private static String fmt(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }
}
