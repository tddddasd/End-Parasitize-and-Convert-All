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
import org.tdddd.epca.impl.utils.entity.BillboardRenderHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Single generic Geo renderer for ALL EPCA entities.
 *
 * <p>Custom per-entity behaviors are applied here via interfaces:</p>
 * <ul>
 *   <li>{@link IMotionAligned} — rotate to face velocity direction (projectiles)</li>
 *   <li>{@link IOverlayRenderable} — translucent overlay layer (villager plains, wolf collar)</li>
 *   <li>{@link IHeadRotatable} — head rotation, applied to the head bone snapshot</li>
 * </ul>
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>GeckoLib 5 split rendering into render-state extraction and submission, and the
 * animatable is no longer reachable while drawing. The EPCA customisations were re-expressed
 * on the GeckoLib 5 hooks:</p>
 * <ul>
 *   <li>tint / hurt-overlay → {@link #getRenderColor} / {@link #getPackedOverlay}
 *       (both still receive the animatable),</li>
 *   <li>translucent render type → {@link #getRenderType(EntityRenderState, Identifier)},</li>
 *   <li>billboard / motion-aligned rotation → {@link #adjustRenderPose},</li>
 *   <li>head rotation → a {@code RenderPassInfo.BoneUpdater} installed in
 *       {@link #preRenderPass} (GeckoLib 4 mutated the bone directly),</li>
 *   <li>overlays and afterimages → a single delegate {@link GeoRenderLayer} that
 *       re-submits the model with another texture/colour/alpha.</li>
 * </ul>
 *
 * <h3>Why {@code RenderPassInfo} is used raw here</h3>
 * <p>{@code com.geckolib.renderer.base.RenderPassInfo<R extends GeoRenderState>} is bounded by
 * GeckoLib's {@code GeoRenderState}, which vanilla's {@code EntityRenderState} only gains at
 * runtime through GeckoLib's mixin. GeckoLib's own {@code GeoEntityRenderer<T, R extends EntityRenderState>}
 * therefore cannot be specialised to a concrete {@code R} from mod code, and neither can
 * {@code RenderPassInfo<EntityRenderState>} be written down. EPCA renders every entity type with
 * one renderer, so the delegate layer and its helpers take the erased {@code RenderPassInfo}
 * type; the values flowing through it are the vanilla {@code EntityRenderState}s GeckoLib
 * itself creates.</p>
 */
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

    /**
     * Register an additional layer provider. Called from subclass constructors
     * to attach per-entity-type layer rendering (afterimages, wool, etc.).
     */
    public void addLayerProvider(IGeoLayerProvider provider) {
        layerProviders.add(provider);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Render-state extraction
    // ═══════════════════════════════════════════════════════════════

    /**
     * GeckoLib 5 only hands the animatable to the extraction phase. {@link EpcaGeoModel}
     * and {@link IGeoLayerProvider} need it later on, so it is parked in the render state.
     */
    @Override
    public void captureDefaultRenderState(T animatable, Void renderData, EntityRenderState renderState, float partialTick) {
        EpcaGeoModel.setRenderEntity(renderState, animatable);
        super.captureDefaultRenderState(animatable, renderData, renderState, partialTick);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Color effects
    // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    //  Rotation: April Fools billboard  /  motion-aligned projectiles
    // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    //  Head rotation  (IHeadRotatable)
    // ═══════════════════════════════════════════════════════════════

    /**
     * GeckoLib 5 replaced direct bone mutation with bone updaters. The updater is resolved
     * lazily when the render pass compiles its bone snapshots, which happens after
     * {@code preRenderPass} and before the model is submitted.
     */
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
    }

    // ═══════════════════════════════════════════════════════════════
    //  Public helpers for external renderers / layers
    // ═══════════════════════════════════════════════════════════════

    /**
     * Re-submits the current model pass with an explicit colour and alpha.
     *
     * <p>GeckoLib 4 exposed {@code reRender(...)} and allowed callers to hand in a fresh
     * {@code VertexConsumer}; GeckoLib 5 removed both. The equivalent is to push the desired
     * colour into the render state (which is what {@code submitRenderTasks} reads) and ask it
     * to submit the already-compiled bone snapshots again with another render type.</p>
     */
    public void submitModelWithArgb(RenderPassInfo passInfo, SubmitNodeCollector collector,
                                    int order, RenderType renderType,
                                    float red, float green, float blue, float alpha) {
        int a = clampChannel(alpha);
        int r = clampChannel(red);
        int g = clampChannel(green);
        int b = clampChannel(blue);
        submitWithColor(passInfo, collector, order, renderType, (a << 24) | (r << 16) | (g << 8) | b);
    }

    /** Same as {@link #submitModelWithArgb} but keeps the pass's own RGB and only changes alpha. */
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

    // ═══════════════════════════════════════════════════════════════
    //  GeckoLib render layer → overlays + registered IGeoLayerProviders
    // ═══════════════════════════════════════════════════════════════

    /**
     * A GeckoLib {@link GeoRenderLayer} that first re-draws the model for
     * {@link IOverlayRenderable} entities and then dispatches to all registered
     * {@link IGeoLayerProvider} instances.
     *
     * <p>{@code GeoRenderLayer} is extended raw for the reason documented on
     * {@link EpcaGeoRenderer}: its {@code R} is bounded by {@code GeoRenderState}, which a
     * concrete vanilla render state cannot satisfy at compile time. The override signatures are
     * therefore the erased ones; dispatch at runtime is unchanged.</p>
     */
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

    /**
     * Helper for layers that need to hide whole bone sub-trees (GeckoLib 4 did this by
     * calling {@code CoreGeoBone#setHidden}); GeckoLib 5 expresses it as a bone updater.
     */
    public static void addBoneHider(RenderPassInfo passInfo, String boneName, boolean hide) {
        passInfo.addBoneUpdater((info, snapshots) ->
                snapshots.ifPresent(boneName, snapshot -> snapshot.skipRender(hide)));
    }
}
