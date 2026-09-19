package org.tdddd.epca.impl.client.entity.renderer;

import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.client.entity.model.WalkingEndermanHeadModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.WalkingEndermanHead;

/**
 * Renderer for the walking enderman head.
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>The deleted {@code render(...)} override did two things; both were re-expressed on GeckoLib 5
 * hooks: the carried entity is positioned through a
 * {@code RenderPassInfo.BonePositionListener} added in {@link #preRenderPass}, and the glow pass
 * became a {@link IGeoLayerProvider} that re-submits the model with
 * {@code RenderTypes.eyes(glowTexture)}.</p>
 */
public class WalkingEndermanHeadRenderer extends EpcaGeoRenderer<WalkingEndermanHead> {

    private static final EndermanAfterimageLayer AFTERIMAGE_LAYER = new EndermanAfterimageLayer();

    public WalkingEndermanHeadRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WalkingEndermanHeadModel());
        addLayerProvider(AFTERIMAGE_LAYER);
        addLayerProvider(new GlowLayer());
    }

    @Override
    public void preRenderPass(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        super.preRenderPass(passInfo, collector);

        if (EpcaGeoModel.entityOf(passInfo.renderState()) instanceof WalkingEndermanHead head) {
            Entity carried = head.getCarriedEntity();
            if (carried != null && head.level().isClientSide()) {
                passInfo.addBonePositionListener("carry_locator", (bonePos, relativePos, modelPos) ->
                        carried.setPos(bonePos.x, bonePos.y, bonePos.z));
            }
        }
    }

    /** Glow overlay: the unstable variant renders again with its emissive texture. */
    private static final class GlowLayer implements IGeoLayerProvider {
        @Override
        public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
            if (!(EpcaGeoModel.entityOf(passInfo.renderState()) instanceof WalkingEndermanHead head)) return;
            if (!head.isGlowEnabled()) return;
            if (head.getVariant() != WalkingEndermanHead.Variant.UNSTABLE) return;
            emit(passInfo, collector, head);
        }

        private static void emit(RenderPassInfo passInfo, SubmitNodeCollector collector, IGlowRenderable glow) {
            Identifier glowTex = glow.getGlowTexture();
            if (glowTex == null) return;
            if (!(passInfo.renderer() instanceof EpcaGeoRenderer<?> renderer)) return;
            float[] color = glow.getGlowColor();
            renderer.submitModelWithArgb(passInfo, collector, 20, RenderTypes.eyes(glowTex),
                    color[0], color[1], color[2], 1.0F);
        }
    }
}
