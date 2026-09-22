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

/**
 * Client-only renderer of {@code epca:contaminated_water}.
 *
 * <p>The entity has no model of its own (it is an invisible volume), so this renderer only draws the
 * effect that belongs to the part of its volume which touches water:</p>
 * <ol>
 *   <li>the small red gas clouds spawned by {@link GasCloudManager} at random water positions, and</li>
 *   <li>the dark-red micro rectangles ({@link WaterSpec}) floating on the water surface and
 *       suspended inside the water.</li>
 * </ol>
 *
 * <p>Both go through {@link GasCloudRenderer}, which is the very same camera-relative billboard plus
 * custom-pipeline path the reshape mobs use, so the water effect cannot drift away from the proven
 * gas cloud rendering. Nothing here talks to the server: the clouds are derived from the entity's
 * synced position and its bounding box, and no entity data is added.</p>
 *
 * <h2>26.1.2</h2>
 * <p>Extract-then-submit: {@link #extractRenderState} copies the entity id into the render state,
 * because submission no longer receives the live entity. The pose stack handed to
 * {@link #submit} is the camera-relative entity transform built by {@code EntityRenderDispatcher},
 * so it is used directly as the billboard basis, exactly like the GeckoLib pre-render matrix in
 * {@code GasCloudLayer}.</p>
 */
public class ContaminatedWaterRenderer extends EntityRenderer<ContaminatedWater,
        ContaminatedWaterRenderer.State> {

    /** Minimum wall-clock gap between two diagnostics for the same entity. */
    private static final long DEBUG_REPEAT_MS = 1000L;

    /** Per-entity last diagnostics wall-clock, so the submit path logs at most once per second. */
    private static final Map<Integer, Long> DEBUG_LAST_LOG = new HashMap<>();

    /** Set once the pipeline has been observed missing, so the warning is not repeated every frame. */
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
            // One throttled line per water entity: how large the water footprint is and how many
            // clouds/specks are alive for it.
            debugWater(ownerId, state, clouds.size(), specs.size());
        }
        if (clouds.isEmpty() && specs.isEmpty()) {
            return;
        }
        if (!GasCloudRenderType.isPipelineRegistered()) {
            // The custom pipeline is required; never draw a fallback.
            if (GasCloudManager.DEBUG && !debugPipelineWarned) {
                debugPipelineWarned = true;
                epca.LOGGER.warn("[gascloud] water: PIPELINE NOT REGISTERED - the contaminated water "
                        + "effect is being skipped for every entity.");
            }
            return;
        }

        float partialTick = state.partialTick;
        // The entity dispatch pose already is the camera-relative entity transform; it is copied
        // because the collector keeps the pose stack alive until the geometry is flushed.
        PoseStack.Pose basis = poseStack.last().copy();
        GasCloudRenderer.submit(collector, poseStack, basis, state.x, state.y, state.z,
                partialTick, clouds);
        GasCloudRenderer.submitSpecs(collector, poseStack, basis, state.x, state.y, state.z,
                partialTick, specs);
    }

    /**
     * Prints one line per second per water entity: the cached water block count of the entity
     * volume and how many clouds and specks are alive for it. Dead code unless
     * {@link GasCloudManager#DEBUG} is on.
     */
    private static void debugWater(int ownerId, State state, int clouds, int specks) {
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
        epca.LOGGER.info("[gascloud] water: owner={} waterBlocks={} clouds={} specks={} "
                        + "pos=({}, {}, {}) pipelineReady={}",
                ownerId, GasCloudManager.getWaterBlockCount(ownerId), clouds, specks,
                fmt(state.x), fmt(state.y), fmt(state.z), GasCloudRenderType.isPipelineRegistered());
    }

    /** Short numeric formatter for the diagnostics. */
    private static String fmt(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    /**
     * Client-only render state of the water entity. The effect state itself lives in
     * {@link GasCloudManager} keyed by the entity id, so the render state only has to carry that id
     * from extraction to submission.
     */
    public static class State extends EntityRenderState {
        /** Entity id of the water entity this state was extracted from. */
        public int entityId;
    }
}
