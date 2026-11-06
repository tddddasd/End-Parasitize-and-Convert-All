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

public final class GasCloudLayer implements IGeoLayerProvider {

    private static final GasCloudLayer LONGARMS =
            new GasCloudLayer(ModEntities.RESHAPE_LONGARMS.get());

    private static final GasCloudLayer YELLOWEYE =
            new GasCloudLayer(ModEntities.RESHAPE_YELLOWEYE.get());

    private final EntityType<?>[] ownerTypes;

    private static final Map<Integer, Long> DEBUG_LAST_SUBMIT_LOG = new HashMap<>();

    private static boolean debugShaderWarned;

    private static final long DEBUG_SUBMIT_LOG_INTERVAL_MS = 1000L;

    private static final boolean DEBUG_SUBMIT_GREEN_CONTROL_QUAD = false;

    private GasCloudLayer(EntityType<?>... ownerTypes) {
        this.ownerTypes = ownerTypes;
    }

    public static GasCloudLayer forLongarms() {
        return LONGARMS;
    }

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

    private static boolean debugWindowOpen(int ownerId) {
        return GasCloudManager.debugWindowOpen(DEBUG_LAST_SUBMIT_LOG, ownerId,
                DEBUG_SUBMIT_LOG_INTERVAL_MS);
    }

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

        debugVertex(consumer, matrix, -half, -half);
        debugVertex(consumer, matrix, half, -half);
        debugVertex(consumer, matrix, half, half);
        debugVertex(consumer, matrix, -half, half);
        poseStack.popPose();
    }

    private static void debugVertex(VertexConsumer consumer, org.joml.Matrix4f matrix, float x, float y) {
        consumer.vertex(matrix, x, y, 0.0F).color(0.0F, 1.0F, 0.0F, 1.0F).endVertex();
    }

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

    private static String fmt(double value) {
        return GasCloudRenderer.fmt(value);
    }

    private static void logSubmit(LivingEntity entity, GasCloudRenderer.Submission submission) {
        epca.LOGGER.info("[gascloud] layer: drawn={}/{} owner={} ({}) shaderReady={} alpha={}..{}",
                submission.cloudsDrawn, submission.cloudsTracked,
                entity.getId(), entity.getType(), GasCloudRenderType.isShaderReady(),
                fmt(submission.minAlpha), fmt(submission.maxAlpha));
    }
}
