package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * Single generic GeoModel for all EPCA entities.
 *
 * <p>Resource resolution priority:
 * <ol>
 *   <li>Entity implements {@link IAutoRenderableEntity} → use entity.model/texture/animation()</li>
 *   <li>Otherwise → lookup from {@link EpcaEntityManager} by entity type</li>
 * </ol>
 *
 * <p>Per-entity custom logic (head rotation, layer rendering, etc.) is handled via
 * interfaces ({@link IHeadRotatable}) in client events, NOT by subclassing this model.</p>
 */
public class EpcaGeoModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T entity) {
        if (entity instanceof IAutoRenderableEntity r) {
            ResourceLocation rl = r.model();
            if (rl != null) return rl;
        }
        return EpcaEntityManager.getModel(entity.getType());
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        if (entity instanceof IAutoRenderableEntity r) {
            ResourceLocation rl = r.texture();
            if (rl != null) return rl;
        }
        return EpcaEntityManager.getTexture(entity.getType());
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        if (entity instanceof IAutoRenderableEntity r) {
            ResourceLocation rl = r.animation();
            if (rl != null) return rl;
        }
        return EpcaEntityManager.getAnimation(entity.getType());
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Head rotation via interface — only for LivingEntity (needs yBodyRot)
        if (animatable instanceof LivingEntity living && animatable instanceof IHeadRotatable rotatable) {
            float partialTick = animationState.getPartialTick();
            float currentTime = living.tickCount + partialTick;
            float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            HeadRotationHandler.applyHeadRotation(
                    living.getId(), rotatable, this, currentTime, partialTick, bodyYaw);
        }
    }
}
