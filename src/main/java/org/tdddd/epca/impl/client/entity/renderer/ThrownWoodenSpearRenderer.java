package org.tdddd.epca.impl.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.ThrownWoodenSpear;
import org.tdddd.epca.impl.epca;

// 26.1.2: EntityRenderer<T, S> + extract/submit; items are drawn through ItemModelResolver ->
// ItemStackRenderState#submit instead of the deleted ItemRenderer#renderStatic.
public class ThrownWoodenSpearRenderer extends EntityRenderer<ThrownWoodenSpear, ThrownWoodenSpearRenderer.SpearRenderState> {

    /** 26.1.2 render state: the resolved item model plus the motion angles computed in the extract phase. */
    public static class SpearRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yaw;
        public float pitch;
    }

    private final ItemModelResolver itemModelResolver;

    public ThrownWoodenSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public SpearRenderState createRenderState() {
        return new SpearRenderState();
    }

    @Override
    public void extractRenderState(ThrownWoodenSpear entity, SpearRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        double motionX = entity.getDeltaMovement().x;
        double motionY = entity.getDeltaMovement().y;
        double motionZ = entity.getDeltaMovement().z;

        if (motionX * motionX + motionY * motionY + motionZ * motionZ < 1.0E-6) {
            // Landed / stuck: the entity zeroes its motion and freezes its own rotation, so
            // atan2(0, 0) would snap every stuck spear to a fixed -90/225 pose. Reuse the
            // orientation it had in flight - exactly what vanilla arrows/tridents render from.
            state.yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F;
            state.pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 225.0F;
        } else {
            state.yaw = (float) (Math.atan2(motionX, motionZ) * (180.0 / Math.PI)) - 90;
            state.pitch = (float) (Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)) * (180.0 / Math.PI)) + 225.0F;
        }

        this.itemModelResolver.updateForNonLiving(state.item, entity.getPickupItem(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(SpearRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        poseStack.translate(0, -0.4, 0);

        poseStack.scale(4f, 4f, 4f);
        poseStack.translate(0.3D, -0.2D, 0.0D);

        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /**
     * 26.1.2: {@code EntityRenderer} no longer declares {@code getTextureLocation}. This renderer never used the
     * texture (it draws an item model), so the override was dropped; the constant is kept for reference.
     */
    public static Identifier textureLocation() {
        return Identifier.fromNamespaceAndPath(epca.MODID, "textures/entity/wooden_spear.png");
    }
}