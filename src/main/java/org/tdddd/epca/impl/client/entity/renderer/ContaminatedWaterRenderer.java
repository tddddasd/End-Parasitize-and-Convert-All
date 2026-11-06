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

public class ContaminatedWaterRenderer extends EntityRenderer<ContaminatedWater> {

    private static final long DEBUG_RENDER_LOG_INTERVAL_MS = 1000L;

    private static final Map<Integer, Long> DEBUG_WATER_RENDER_LOG = new HashMap<>();

    public ContaminatedWaterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ContaminatedWater entity) {

        return null;
    }

    @Override
    public void render(ContaminatedWater entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

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
