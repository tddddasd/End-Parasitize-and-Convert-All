package org.tdddd.epca.impl.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.client.ClientColorEffect;
import org.tdddd.epca.impl.utils.entity.BillboardRenderHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

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
 *   <li>{@link IHeadRotatable} — handled in {@link EpcaGeoModel#setCustomAnimations}</li>
 * </ul>
 */
public class EpcaGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private final List<IGeoLayerProvider> layerProviders = new ArrayList<>();

    public EpcaGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EpcaGeoModel<>());
        addRenderLayer(new OuterLayerDelegate());
    }

    public EpcaGeoRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        addRenderLayer(new OuterLayerDelegate());
    }

    /**
     * Register an additional layer provider. Called from subclass constructors
     * to attach per-entity-type layer rendering (afterimages, wool, etc.).
     */
    public void addLayerProvider(IGeoLayerProvider provider) {
        layerProviders.add(provider);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Color effects
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.animatable = animatable;

        if (animatable instanceof LivingEntity living) {
            var effect = ClientColorEffect.getEffect(living);
            if (effect != null) {
                packedOverlay = OverlayTexture.NO_OVERLAY;
                float[] rgb = effect.getColorRGB();
                red = rgb[0];
                green = rgb[1];
                blue = rgb[2];
            }
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture,
                                     @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Rotation: April Fools billboard  /  motion-aligned projectiles
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void applyRotations(T entity, PoseStack poseStack, float rotationYaw,
                                   float partialTicks, float ageInTicks) {
        if (entity instanceof IMotionAligned) {
            applyMotionAlignedRotation(entity, poseStack);
        } else if (isAprilFoolsDay() && entity instanceof LivingEntity living) {
            BillboardRenderHelper.applyBillboardTransform(poseStack, living, partialTicks);
        } else {
            super.applyRotations(entity, poseStack, rotationYaw, partialTicks, ageInTicks);
        }
    }

    private void applyMotionAlignedRotation(T entity, PoseStack poseStack) {
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
    //  Overlay rendering  (IOverlayRenderable)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (entity instanceof IOverlayRenderable overlay) {
            renderOverlay(entity, overlay, poseStack, bufferSource, partialTick, packedLight);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderOverlay(T entity, IOverlayRenderable overlay, PoseStack poseStack,
                               MultiBufferSource bufferSource, float partialTick, int packedLight) {
        ResourceLocation tex = overlay.getOverlayTexture();
        if (tex == null) return;

        float[] color = overlay.getOverlayColor();
        BakedGeoModel baked = getGeoModel().getBakedModel(getGeoModel().getModelResource(entity));
        RenderType rt = RenderType.entityTranslucent(tex);
        VertexConsumer buf = bufferSource.getBuffer(rt);

        poseStack.pushPose();
        this.animatable = entity;
        this.preRender(poseStack, entity, baked, bufferSource, buf, false,
                partialTick, packedLight, getOverlayCoords(entity, 0), color[0], color[1], color[2], 1.0F);
        this.actuallyRender(poseStack, entity, baked, rt, bufferSource, buf,
                false, partialTick, packedLight, getOverlayCoords(entity, 0),
                color[0], color[1], color[2], 1.0F);
        poseStack.popPose();
    }

    private static int getOverlayCoords(Entity entity, float u) {
        if (entity instanceof LivingEntity living) {
            return LivingEntityRenderer.getOverlayCoords(living, u);
        }
        return OverlayTexture.NO_OVERLAY;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Public helper for external renderers (afterimages, layers, etc.)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Renders the model with explicit color/alpha using a specific RenderType.
     * Public wrapper around {@link #actuallyRender} for use by subclasses and
     * external callers that need to draw the model with custom transparency
     * (e.g. afterimage ghost layers).
     */
    public void renderModelWithAlpha(PoseStack poseStack, T entity, BakedGeoModel model,
                                      RenderType renderType, MultiBufferSource bufferSource,
                                      VertexConsumer buffer, float partialTick,
                                      int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha) {
        this.animatable = entity;

        this.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer,
                false, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ═══════════════════════════════════════════════════════════════
    //  GeckoLib render layer → delegates to registered IGeoLayerProviders
    // ═══════════════════════════════════════════════════════════════

    /**
     * A GeckoLib {@link GeoRenderLayer} that dispatches to all
     * registered {@link IGeoLayerProvider} instances for this renderer.
     * Mirrors the {@code UniversalDelegateLayer} pattern from OpenSRP.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private class OuterLayerDelegate extends GeoRenderLayer<T> {

        OuterLayerDelegate() {
            super(EpcaGeoRenderer.this);
        }

        @Override
        public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
            if (!(entity instanceof LivingEntity living)) return;
            for (IGeoLayerProvider provider : layerProviders) {
                provider.renderAdditionalLayer(
                        EpcaGeoRenderer.this,
                        living, bakedModel, renderType,
                        bufferSource, buffer, poseStack,
                        partialTick, packedLight, packedOverlay);
            }
        }
    }
}
