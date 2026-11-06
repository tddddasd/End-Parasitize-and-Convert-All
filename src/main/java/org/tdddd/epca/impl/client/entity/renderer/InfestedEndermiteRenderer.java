package org.tdddd.epca.impl.client.entity.renderer;

import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.IGeoLayerProvider;
import org.tdddd.epca.impl.client.entity.IGlowRenderable;
import org.tdddd.epca.impl.client.entity.layer.EndermanAfterimageLayer;
import org.tdddd.epca.impl.client.entity.model.InfestedEndermiteModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.infested.InfestedEndermite;

public class InfestedEndermiteRenderer extends EpcaGeoRenderer<InfestedEndermite> {

    private static final EndermanAfterimageLayer AFTERIMAGE_LAYER = new EndermanAfterimageLayer();

    public InfestedEndermiteRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfestedEndermiteModel());
        addLayerProvider(AFTERIMAGE_LAYER);
        addLayerProvider(new GlowLayer());
    }

    private static final class GlowLayer implements IGeoLayerProvider {
        @Override
        public void submitLayer(RenderPassInfo passInfo, SubmitNodeCollector collector) {
            if (!(EpcaGeoModel.entityOf(passInfo.renderState()) instanceof InfestedEndermite endermite)) return;
            if (!endermite.isGlowEnabled()) return;
            if (endermite.getVariant() != InfestedEndermite.Variant.UNSTABLE) return;
            emit(passInfo, collector, endermite);
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
