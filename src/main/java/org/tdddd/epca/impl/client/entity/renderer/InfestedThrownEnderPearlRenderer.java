package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.overworld.registry.entities.entity.misc.InfestedThrownEnderPearl;
import org.tdddd.epca.impl.epca;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 26.1.2: EntityRenderer<T, S> + extract/submit; the item is resolved into an ItemStackRenderState by
// ItemModelResolver (ItemRenderer#renderStatic is gone).
public class InfestedThrownEnderPearlRenderer
        extends EntityRenderer<InfestedThrownEnderPearl, InfestedThrownEnderPearlRenderer.PearlRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(epca.MODID, "textures/item/infested_ender_pearl.png");

    /** 26.1.2 render state: resolved ender-pearl item model + the interpolated yaw used by the old render(). */
    public static class PearlRenderState extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yaw;
    }

    private final ItemModelResolver itemModelResolver;

    public InfestedThrownEnderPearlRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.5F;
    }

    @Override
    public PearlRenderState createRenderState() {
        return new PearlRenderState();
    }

    @Override
    public void extractRenderState(InfestedThrownEnderPearl entity, PearlRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        this.itemModelResolver.updateForNonLiving(state.item, new ItemStack(Items.ENDER_PEARL),
                ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(PearlRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90F - state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(45F));

        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /**
     * 26.1.2: {@code EntityRenderer} no longer declares {@code getTextureLocation}; this renderer draws an item
     * model, so the override was dropped and the constant is kept for reference.
     */
    public static Identifier textureLocation() {
        return TEXTURE;
    }
}
