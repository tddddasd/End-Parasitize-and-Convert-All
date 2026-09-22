package org.tdddd.epca.impl.client.entity;

/**
 * Shared GeckoLib animation defaults for every animated object of this mod.
 *
 * <p>All {@code AnimationController}s are registered by the entity / block-entity classes
 * themselves, which live in several different packages, so the transition length they all share is
 * kept here instead of being repeated as a literal at every call site.</p>
 *
 * <p>{@link #GEO_TRANSITION_TICKS} is a compile-time constant, so referencing it from a common
 * (non-client-only) class does not load this class on a dedicated server: javac inlines the value
 * into the call site.</p>
 */
public final class EpcaAnimations {

    /**
     * Transition length of every {@code AnimationController} of this mod, in ticks.
     *
     * <p>Every controller in the mod used to carry its own literal (0, 2, 3, 4, 5 or 6), which made
     * the animation blending inconsistent between mobs: the same state change faded over a
     * different number of ticks depending on the entity. They now all use 2 ticks, which is
     * GeckoLib's own default and keeps the blend short but still smooth.</p>
     */
    public static final int GEO_TRANSITION_TICKS = 2;

    private EpcaAnimations() {
    }
}
