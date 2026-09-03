package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.model.InfestedBatModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedBat;

public class InfestedBatRenderer extends EpcaGeoRenderer<InfestedBat> {

    private static final float[][] SPRINT_ROT = {
            {0, 0, 0},
            {-5, 0, 0},
            {22.5f, 0, 0},
            {0, 0, 0}
    };
    private static final float[][] SPRINT_POS = {
            {0, 0, 0},
            {0, 0, 1},
            {0, 0, -1},
            {0, 0, 0}
    };
    private static final float[][] SPRINT_SCALE = {
            {1, 1, 1},
            {1, 1, 0.7f},
            {0.9f, 0.9f, 1.5f},
            {1, 1, 1}
    };
    private static final float[] SPRINT_TIMES = {0.0f, 0.125f, 0.25f, 0.5f};

    private static final Vector3f AXIS_X = new Vector3f(1, 0, 0);
    public InfestedBatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedBatModel());
    }

    @Override
    public void render(InfestedBat entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isResting()) {
            poseStack.mulPose(new Quaternionf().setAngleAxis((float) Math.PI, 1, 0, 0));
            poseStack.translate(0, -0.8, 0);
        }

        if (entity.getCurrentState() == InfestedBat.BatState.LEAVING) {
            int totalTicks = entity.tickCount;
            float progress = (totalTicks % 10) / 10.0f;

            int idx = 0;
            for (int i = 0; i < SPRINT_TIMES.length - 1; i++) {
                if (progress >= SPRINT_TIMES[i] && progress < SPRINT_TIMES[i + 1]) {
                    idx = i;
                    break;
                }
            }
            if (progress >= SPRINT_TIMES[SPRINT_TIMES.length - 1]) {
                idx = SPRINT_TIMES.length - 2;
            }

            float t0 = SPRINT_TIMES[idx];
            float t1 = SPRINT_TIMES[idx + 1];
            float localT = (progress - t0) / (t1 - t0);
            if (Float.isNaN(localT)) localT = 0;

            float[] rot = lerpVec(SPRINT_ROT[idx], SPRINT_ROT[idx + 1], localT);
            float[] pos = lerpVec(SPRINT_POS[idx], SPRINT_POS[idx + 1], localT);
            float[] scale = lerpVec(SPRINT_SCALE[idx], SPRINT_SCALE[idx + 1], localT);

            poseStack.scale(scale[0], scale[1], scale[2]);

            if (rot[0] != 0) {
                float rad = (float) Math.toRadians(rot[0]);
                Quaternionf quat = new Quaternionf().setAngleAxis(rad, AXIS_X.x(), AXIS_X.y(), AXIS_X.z());
                poseStack.mulPose(quat);
            }

            poseStack.translate(pos[0], pos[1], pos[2]);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private float[] lerpVec(float[] a, float[] b, float t) {
        return new float[]{
                a[0] + (b[0] - a[0]) * t,
                a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t
        };
    }
}