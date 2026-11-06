package org.tdddd.epca.impl.client.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

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

        if (!FrozenGhostRender.isActive()
                && animatable instanceof LivingEntity living
                && animatable instanceof IHeadRotatable rotatable) {
            float partialTick = animationState.getPartialTick();
            float currentTime = living.tickCount + partialTick;
            float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
            HeadRotationHandler.applyHeadRotation(
                    living.getId(), rotatable, this, currentTime, partialTick, bodyYaw);
        }
    }
}
