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
import org.tdddd.epca.impl.client.entity.model.InfestedEndermanModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEnderman;

public class InfestedEndermanRenderer extends EpcaGeoRenderer<InfestedEnderman> {

    private static final EndermanAfterimageLayer AFTERIMAGE_LAYER = new EndermanAfterimageLayer();

    public InfestedEndermanRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedEndermanModel());
        addLayerProvider(AFTERIMAGE_LAYER);
        addLayerProvider(new GlowLayer());
    }

    @Override
    public void preRenderPass(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        super.preRenderPass(passInfo, collector);

        if (EpcaGeoModel.entityOf(passInfo.renderState()) instanceof InfestedEnderman enderman) {
            Entity carried = enderman.getCarriedEntity();
            if (carried != null && enderman.level().isClientSide()) {
                passInfo.addBonePositionListener("carry_locator", (bonePos, relativePos, modelPos) ->
                        carried.setPos(bonePos.x, bonePos.y, bonePos.z));
            }
        }
    }

    private static final class GlowLayer implements IGeoLayerProvider {
        @Override
        public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
            if (!(EpcaGeoModel.entityOf(passInfo.renderState()) instanceof InfestedEnderman enderman)) return;
            if (!enderman.isGlowEnabled()) return;
            if (enderman.getVariant() != InfestedEnderman.Variant.UNSTABLE) return;
            emit(passInfo, collector, enderman);
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
