package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.tdddd.epca.impl.client.entity.gas.GasCloud;
import org.tdddd.epca.impl.client.entity.gas.GasCloudManager;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderer;
import org.tdddd.epca.impl.client.entity.gas.GasCloudRenderType;
import org.tdddd.epca.impl.client.entity.gas.WaterSpec;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ContaminatedWater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContaminatedWaterRenderer extends EntityRenderer<ContaminatedWater,
        ContaminatedWaterRenderer.State> {

    private static final long DEBUG_REPEAT_MS = 1000L;

    private static final Map<Integer, Long> DEBUG_LAST_LOG = new HashMap<>();

    private static boolean debugPipelineWarned;

    public ContaminatedWaterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ContaminatedWater entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        int ownerId = state.entityId;
        List<GasCloud> clouds = GasCloudManager.getCloudsFor(ownerId);
        List<WaterSpec> specs = GasCloudManager.getSpecsFor(ownerId);

        if (GasCloudManager.DEBUG) {

            debugWater(ownerId, state, clouds.size(), specs.size());
        }
        if (clouds.isEmpty() && specs.isEmpty()) {
            return;
        }
        if (!GasCloudRenderType.isPipelineRegistered()) {

            if (GasCloudManager.DEBUG && !debugPipelineWarned) {
                debugPipelineWarned = true;
                epca.LOGGER.warn("[gascloud] water: PIPELINE NOT REGISTERED - the contaminated water "
                        + "effect is being skipped for every entity.");
            }
            return;
        }

        float partialTick = state.partialTick;

        PoseStack.Pose basis = poseStack.last().copy();
        GasCloudRenderer.submit(collector, poseStack, basis, state.x, state.y, state.z,
                partialTick, clouds);
        GasCloudRenderer.submitSpecs(collector, poseStack, basis, state.x, state.y, state.z,
                partialTick, specs);
    }

    private static void debugWater(int ownerId, State state, int clouds, int specks) {
        long now = System.currentTimeMillis();
        Long last = DEBUG_LAST_LOG.get(ownerId);
        if (last != null && now - last < DEBUG_REPEAT_MS) {
            return;
        }
        if (DEBUG_LAST_LOG.size() > 256) {

            DEBUG_LAST_LOG.clear();
        }
        DEBUG_LAST_LOG.put(ownerId, now);
        epca.LOGGER.info("[gascloud] water: owner={} waterBlocks={} clouds={} specks={} "
                        + "pos=({}, {}, {}) pipelineReady={}",
                ownerId, GasCloudManager.getWaterBlockCount(ownerId), clouds, specks,
                fmt(state.x), fmt(state.y), fmt(state.z), GasCloudRenderType.isPipelineRegistered());
    }

    private static String fmt(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    public static class State extends EntityRenderState {

        public int entityId;
    }
}
