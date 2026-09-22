package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
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
 * Renderer of the {@code epca:contaminated_water} entity.
 *
 * <p>The entity owns no model; everything it shows is client-only cloud work and is produced by
 * {@code GasCloudManager} from the water blocks around it:</p>
 * <ul>
 *   <li>3-5 small red gas clouds at random water positions inside its bounding box
 *       ({@link #render}), and</li>
 *   <li>10-24 dark-red micro rectangles, half floating on the water surface and half suspended in
 *       the water.</li>
 * </ul>
 * Both are handed to {@link GasCloudRenderer}, the same shared helper the reshape-mob
 * {@code GasCloudLayer} uses, so they travel through the proven camera-relative billboard path and
 * the custom {@code gas_cloud} core shader. No server-side data, no packets and no entity-data
 * changes are involved.</p>
 *
 * <h2>Pose space</h2>
 * <p>Vanilla {@code EntityRenderDispatcher.render} builds the pose this method receives as
 * {@code entityInterpolatedPos - cameraPos} (with no render offset; {@code getRenderOffset} is not
 * overridden here), which is exactly the space {@link GasCloudRenderer} expects, so the same offset
 * formula used by the GeckoLib layer works unchanged inside an {@link EntityRenderer}.</p>
 *
 * <p>Diagnostics: while {@link GasCloudManager#DEBUG} is on, this class adds one
 * {@code [gascloud] water render:} line per second per entity (clouds and specks actually submitted,
 * plus how many the manager holds), complementing the manager's own
 * {@code [gascloud] water: owner=... waterBlocks=... clouds=... specks=...} line.</p>
 */
public class ContaminatedWaterRenderer extends EntityRenderer<ContaminatedWater> {

    /** Milliseconds between two {@code [gascloud] water render:} lines for the same entity. */
    private static final long DEBUG_RENDER_LOG_INTERVAL_MS = 1000L;

    /** Per-owner wall clock of the last line of this renderer, so it cannot spam the log. */
    private static final Map<Integer, Long> DEBUG_WATER_RENDER_LOG = new HashMap<>();

    public ContaminatedWaterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ContaminatedWater entity) {
        // Nothing of this entity is textured: the clouds and specks are shader-drawn and the entity
        // itself has no model, so there is deliberately no texture to return. Vanilla only uses this
        // for the name tag / hitbox debug outline, both of which are irrelevant here.
        return null;
    }

    @Override
    public void render(ContaminatedWater entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // The custom core shader is required; drawing without it would show nothing anyway
        // (the render type has no built-in fallback), so skip the work entirely.
        if (!GasCloudRenderType.isShaderReady()) {
            return;
        }

        List<GasCloud> clouds = GasCloudManager.getCloudsFor(entity.getId());
        List<WaterSpec> specks = GasCloudManager.getWaterSpecksFor(entity.getId());
        if (clouds.isEmpty() && specks.isEmpty()) {
            return;
        }

        GasCloudRenderer.Submission submission =
                GasCloudRenderer.submit(entity, poseStack, buffer, partialTick, clouds, specks);

        if (GasCloudManager.DEBUG && !submission.isEmpty()
                && GasCloudManager.debugWindowOpen(DEBUG_WATER_RENDER_LOG, entity.getId(),
                DEBUG_RENDER_LOG_INTERVAL_MS)) {
            epca.LOGGER.info(
                    "[gascloud] water render: owner={} clouds={} specks={} trackedClouds={} trackedSpecks={}",
                    entity.getId(), submission.cloudsDrawn, submission.specksDrawn,
                    clouds.size(), specks.size());
        }
    }
}
