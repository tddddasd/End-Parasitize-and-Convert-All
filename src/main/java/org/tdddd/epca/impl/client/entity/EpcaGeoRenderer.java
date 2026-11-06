package org.tdddd.epca.impl.client.entity;

import net.minecraft.world.entity.animal.wolf.Wolf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.client.ClientColorEffect;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedSpider;
import org.tdddd.epca.impl.utils.entity.BillboardRenderHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class EpcaGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T, EntityRenderState> {

    private final List<IGeoLayerProvider> layerProviders = new ArrayList<>();

    public EpcaGeoRenderer(EntityRendererProvider.Context renderManager) {
        this(renderManager, new EpcaGeoModel<>());
    }

    public EpcaGeoRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        withRenderLayer(new OuterLayerDelegate());
    }

    public void addLayerProvider(IGeoLayerProvider provider) {
        layerProviders.add(provider);
    }

    @Override
    public void captureDefaultRenderState(T animatable, Void renderData, EntityRenderState renderState, float partialTick) {
        EpcaGeoModel.setRenderEntity(renderState, animatable);
        super.captureDefaultRenderState(animatable, renderData, renderState, partialTick);
    }

    @Override
    public int getRenderColor(T animatable, Void renderData, float partialTick) {
        if (animatable instanceof LivingEntity living) {
            var effect = ClientColorEffect.getEffect(living);
            if (effect != null) {
                return effect.getColorARGB();
            }
        }
        return super.getRenderColor(animatable, renderData, partialTick);
    }

    @Override
    public int getPackedOverlay(T animatable, Void renderData, float partialTick, float whiteOverlay) {
        if (animatable instanceof LivingEntity living && ClientColorEffect.getEffect(living) != null) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(animatable, renderData, partialTick, whiteOverlay);
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo passInfo) {
        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (entity instanceof IMotionAligned) {
            applyMotionAlignedRotation(entity, passInfo.poseStack());
        } else if (isAprilFoolsDay() && entity instanceof LivingEntity living) {
            BillboardRenderHelper.applyBillboardTransform(
                    passInfo.poseStack(), living, passInfo.renderState().getPartialTick());
        } else {
            super.adjustRenderPose(passInfo);
        }
    }

    private void applyMotionAlignedRotation(Entity entity, PoseStack poseStack) {
        double mx = entity.getDeltaMovement().x;
        double my = entity.getDeltaMovement().y;
        double mz = entity.getDeltaMovement().z;
        float yaw = (float) (Math.atan2(mx, mz) * (180.0 / Math.PI));
        float pitch = (float) (Math.atan2(my, Math.sqrt(mx * mx + mz * mz)) * (-180.0 / Math.PI));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    private static boolean isAprilFoolsDay() {
        return LocalDate.now().getMonthValue() == 4 && LocalDate.now().getDayOfMonth() == 1;
    }

    private static final String CAVE_SHOOT_LEG_BONE = "legs";

    private static final float CAVE_SHOOT_LEG_OFFSET_Y = -1.0F;

    @Override
    public void preRenderPass(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        super.preRenderPass(passInfo, collector);

        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (entity instanceof LivingEntity living
                && entity instanceof IHeadRotatable rotatable
                && rotatable.shouldRotateHead()) {
            float partialTick = passInfo.renderState().getPartialTick();
            float currentTime = living.tickCount + partialTick;
            float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            float radians = HeadRotationHandler.computeHeadRotation(
                    living.getId(), rotatable, currentTime, partialTick, bodyYaw);
            String boneName = rotatable.getHeadBoneName();
            passInfo.addBoneUpdater((info, snapshots) ->
                    snapshots.ifPresent(boneName, snapshot -> snapshot.setRotY(radians)));
        }

        if (entity instanceof InfestedSpider spider
                && spider.getVariant() == InfestedSpider.Variant.CAVE
                && spider.isShooting()) {
            passInfo.addBoneUpdater((info, snapshots) ->
                    snapshots.ifPresent(CAVE_SHOOT_LEG_BONE, snapshot -> snapshot.setTranslation(
                            snapshot.getTranslateX(),
                            snapshot.getTranslateY() + CAVE_SHOOT_LEG_OFFSET_Y,
                            snapshot.getTranslateZ())));
        }
    }

    public void submitModelWithArgb(RenderPassInfo passInfo, SubmitNodeCollector collector,
                                    int order, RenderType renderType,
                                    float red, float green, float blue, float alpha) {
        int a = clampChannel(alpha);
        int r = clampChannel(red);
        int g = clampChannel(green);
        int b = clampChannel(blue);
        submitWithColor(passInfo, collector, order, renderType, (a << 24) | (r << 16) | (g << 8) | b);
    }

    public void submitModelWithAlpha(RenderPassInfo passInfo, SubmitNodeCollector collector,
                                     int order, RenderType renderType, float alpha) {
        GeoRenderState state = passInfo.renderState();
        Integer previous = state.getGeckolibData(DataTickets.RENDER_COLOR);
        int base = previous != null ? previous : 0xFFFFFFFF;
        submitWithColor(passInfo, collector, order, renderType,
                (clampChannel(alpha) << 24) | (base & 0x00FFFFFF));
    }

    private void submitWithColor(RenderPassInfo passInfo, SubmitNodeCollector collector,
                                 int order, RenderType renderType, int argb) {
        GeoRenderState state = passInfo.renderState();
        Integer previous = state.getGeckolibData(DataTickets.RENDER_COLOR);
        state.addGeckolibData(DataTickets.RENDER_COLOR, argb);
        try {
            submitRenderTasks(passInfo, collector.order(order), renderType);
        } finally {
            if (previous != null) {
                state.addGeckolibData(DataTickets.RENDER_COLOR, previous);
            }
        }
    }

    private static int clampChannel(float value) {
        int channel = (int) (value * 255.0F);
        return channel < 0 ? 0 : Math.min(channel, 255);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private class OuterLayerDelegate extends GeoRenderLayer {

        OuterLayerDelegate() {
            super(EpcaGeoRenderer.this);
        }

        @Override
        public void addRenderData(GeoAnimatable animatable, Object renderData,
                                  GeoRenderState renderState, float partialTick) {
            for (IGeoLayerProvider provider : layerProviders) {
                provider.addLayerData(renderState, partialTick);
            }
        }

        @Override
        public void submitRenderTask(RenderPassInfo passInfo, SubmitNodeCollector collector) {
            Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
            int order = 0;

            if (entity instanceof IOverlayRenderable overlay) {
                Identifier tex = overlay.getOverlayTexture();
                if (tex != null) {
                    float[] color = overlay.getOverlayColor();
                    submitModelWithArgb(passInfo, collector, order++, RenderTypes.entityTranslucent(tex),
                            color[0], color[1], color[2], 1.0F);
                }
            }

            for (IGeoLayerProvider provider : layerProviders) {
                provider.submitLayer(passInfo, collector);
            }
        }
    }

    public static void addBoneHider(RenderPassInfo passInfo, String boneName, boolean hide) {
        passInfo.addBoneUpdater((info, snapshots) ->
                snapshots.ifPresent(boneName, snapshot -> snapshot.skipRender(hide)));
    }
}
