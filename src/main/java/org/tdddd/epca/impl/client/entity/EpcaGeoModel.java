package org.tdddd.epca.impl.client.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;

public class EpcaGeoModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {

    public static final DataTicket<Entity> RENDER_ENTITY =
            DataTickets.create("epca_render_entity", Entity.class);

    public static Entity entityOf(GeoRenderState renderState) {
        return renderState.getGeckolibData(RENDER_ENTITY);
    }

    public static Entity entityOf(Object renderState) {
        return renderState instanceof GeoRenderState state ? state.getGeckolibData(RENDER_ENTITY) : null;
    }

    public static <E extends Entity> E entityOf(Object renderState, Class<E> type) {
        Entity entity = entityOf(renderState);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }

    public static void setRenderEntity(Object renderState, Entity entity) {
        if (renderState instanceof GeoRenderState state) {
            state.addGeckolibData(RENDER_ENTITY, entity);
        }
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        Entity entity = entityOf(renderState);
        if (entity instanceof IAutoRenderableEntity r) {
            Identifier rl = r.model();
            if (rl != null) return rl;
        }
        return entity == null ? null : EpcaEntityManager.getModel(entity.getType());
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Entity entity = entityOf(renderState);
        if (entity instanceof IAutoRenderableEntity r) {
            Identifier rl = r.texture();
            if (rl != null) return rl;
        }
        return entity == null ? null : EpcaEntityManager.getTexture(entity.getType());
    }

    @Override
    public Identifier getAnimationResource(T entity) {
        if (entity instanceof IAutoRenderableEntity r) {
            Identifier rl = r.animation();
            if (rl != null) return rl;
        }
        return EpcaEntityManager.getAnimation(entity.getType());
    }
}
