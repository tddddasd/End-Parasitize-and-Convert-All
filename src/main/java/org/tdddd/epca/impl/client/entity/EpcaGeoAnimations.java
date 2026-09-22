package org.tdddd.epca.impl.client.entity;

/**
 * Shared GeckoLib animation constants for every EPCA animatable.
 *
 * <p>The classes of this package ({@link EpcaGeoModel}, {@code EpcaGeoRenderer}, the
 * {@code IHeadRotatable}/{@code IAutoRenderableEntity} marker interfaces, ...) are the common
 * GeckoLib plumbing of the mod, so this is where a value that every controller has to agree on
 * belongs. The class is a pure constant holder and is never instantiated.</p>
 *
 * <p>It lives in the client package but is only a compile-time {@code int}, so a common-side
 * animatable such as {@code SwallowCystBlockEntity} can reference it too - the same direction the
 * {@code impl.overworld} entities already use when they import the marker interfaces from here.</p>
 */
public final class EpcaGeoAnimations {

    /**
     * Transition length, in ticks, of every {@code AnimationController} of the mod.
     *
     * <p>This is the second argument of {@code new AnimationController<>(animatable, name, N,
     * predicate)}: how long GeckoLib blends between the previous and the next animation. Every
     * controller therefore has to be constructed with this constant rather than a literal, so the
     * whole mod animates with one consistent cross-fade and the value can be changed in one place.</p>
     *
     * <p>The tree used to mix 0/2/3/4/5/6; {@code 0} meant an instant, un-blended switch on those
     * controllers. {@code 2} is the value the user asked for everywhere.</p>
     */
    public static final int GEO_TRANSITION_TICKS = 2;

    private EpcaGeoAnimations() {
    }
}
