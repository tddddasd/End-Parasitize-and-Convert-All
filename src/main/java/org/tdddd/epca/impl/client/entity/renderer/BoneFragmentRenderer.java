package org.tdddd.epca.impl.client.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

// 26.1.2: EntityRenderer now has two type parameters (T entity, S render state) and the old
// render(entity, yaw, partialTicks, poseStack, buffer, light) hook was replaced by
// extractRenderState + submit. This renderer draws nothing, so it only supplies an empty render state.
public class BoneFragmentRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {
    public BoneFragmentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}