package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.tdddd.epca.impl.overworld.registry.entities.entity.reshape.ReshapeLongarms;

// 26.1.2: two type parameters + extract/submit split; this renderer draws nothing.
public class ReshapeLongarmsCustomPartRenderer extends EntityRenderer<ReshapeLongarms.CustomPart, EntityRenderState> {
    public ReshapeLongarmsCustomPartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}