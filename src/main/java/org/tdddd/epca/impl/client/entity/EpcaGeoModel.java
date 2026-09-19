package org.tdddd.epca.impl.client.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.tdddd.epca.impl.overworld.registry.entities.EpcaEntityManager;

/**
 * Single generic GeoModel for all EPCA entities.
 *
 * <p>Resource resolution priority:
 * <ol>
 *   <li>Entity implements {@link IAutoRenderableEntity} → use entity.model/texture/animation()</li>
 *   <li>Otherwise → lookup from {@link EpcaEntityManager} by entity type</li>
 * </ol>
 *
 * <p>Per-entity custom logic (head rotation) is handled by {@link EpcaGeoRenderer}
 * from the render state, NOT by subclassing this model.</p>
 *
 * <h2>GeckoLib 4 → 5.5.2</h2>
 * <p>In GeckoLib 5 the renderer no longer holds the animatable at rendering time, so
 * {@code getModelResource}/{@code getTextureResource} receive a {@link GeoRenderState}
 * instead of the entity. GeckoLib 5 still hands the animatable to
 * {@code GeoRenderer#captureDefaultRenderState} during render-state extraction, so
 * {@link EpcaGeoRenderer} stores it in the render state under {@link #RENDER_ENTITY}
 * and the two resource lookups read it back from there. That keeps EPCA's
 * "resolve model/texture per entity instance" behaviour intact.</p>
 */
public class EpcaGeoModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {

    /**
     * Render-state ticket holding the entity currently being rendered.
     *
     * <p>Filled by {@link EpcaGeoRenderer#captureDefaultRenderState} — the only
     * GeckoLib 5 hook that still receives the animatable — and consumed by
     * {@link #getModelResource(GeoRenderState)} / {@link #getTextureResource(GeoRenderState)}
     * and by the render layers.</p>
     */
    public static final DataTicket<Entity> RENDER_ENTITY =
            DataTickets.create("epca_render_entity", Entity.class);

    /** The entity stored in this render state, or {@code null} if none was captured. */
    public static Entity entityOf(GeoRenderState renderState) {
        return renderState.getGeckolibData(RENDER_ENTITY);
    }

    /**
     * Erased variant for callers that hold a vanilla render state.
     *
     * <p>{@code EntityRenderState} only implements GeckoLib's {@link GeoRenderState} at runtime
     * (GeckoLib installs it with a mixin), so a vanilla render state cannot be written as a
     * {@code GeoRenderState} at compile time. This overload takes {@code Object} and checks with
     * {@code instanceof}, which is true for every render state GeckoLib itself creates.</p>
     */
    public static Entity entityOf(Object renderState) {
        return renderState instanceof GeoRenderState state ? state.getGeckolibData(RENDER_ENTITY) : null;
    }

    /** The entity stored in this render state if it is an instance of {@code type}, else {@code null}. */
    public static <E extends Entity> E entityOf(Object renderState, Class<E> type) {
        Entity entity = entityOf(renderState);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }

    /** Store the entity being rendered in the render state (see {@link #RENDER_ENTITY}). */
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
