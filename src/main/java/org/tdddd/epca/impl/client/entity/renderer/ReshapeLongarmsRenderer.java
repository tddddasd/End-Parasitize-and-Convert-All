package org.tdddd.epca.impl.client.entity.renderer;

import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import org.tdddd.epca.impl.client.entity.EpcaGeoModel;
import org.tdddd.epca.impl.client.entity.EpcaGeoRenderer;
import org.tdddd.epca.impl.client.entity.model.ReshapeLongarmsModel;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;

/**
 * Renderer for the reshape longarms.
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>GeckoLib 4 hid the removed back parts by mutating bones before rendering
 * ({@code model.getAnimationProcessor().getBone(name).setHidden(hide)}). GeckoLib 5 bones are
 * immutable while rendering, so the same effect is expressed as a
 * {@code RenderPassInfo.BoneUpdater} that calls {@code BoneSnapshot#skipRender} — which is exactly
 * what {@link EpcaGeoRenderer#addBoneHider} installs.</p>
 */
public class ReshapeLongarmsRenderer extends EpcaGeoRenderer<ReshapeLongarms> {

    private static final String[] BACK_PART_BONES = {
            "pustule1", "pustule2", "pustule3", "streaks4", "streaks5", "streaks6"
    };

    public ReshapeLongarmsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ReshapeLongarmsModel());
    }

    @Override
    public void preRenderPass(RenderPassInfo passInfo, SubmitNodeCollector collector) {
        super.preRenderPass(passInfo, collector);

        Entity entity = EpcaGeoModel.entityOf(passInfo.renderState());
        if (!(entity instanceof ReshapeLongarms longarms)) return;

        boolean hide = longarms.isBackPartRemoved();
        for (String boneName : BACK_PART_BONES) {
            addBoneHider(passInfo, boneName, hide);
        }
    }
}
