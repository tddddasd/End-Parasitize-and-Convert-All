package dev.kosmx.playerAnim.minecraftApi;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>EPCA registers two animation factories in {@code PlayerAnimator#onClientSetup}
 * ({@code epca:stab}, {@code epca:kill_stick}). Registration is accepted and discarded.
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public final class PlayerAnimationFactory {

    /** Singleton factory EPCA reaches through {@code PlayerAnimationFactory.ANIMATION_DATA_FACTORY}. */
    public static final PlayerAnimationFactory ANIMATION_DATA_FACTORY = new PlayerAnimationFactory();

    private PlayerAnimationFactory() {}

    /**
     * No-op downgrade. The real implementation stores the factory and lazily creates the
     * per-player {@code ModifierLayer} the first time the player is rendered.
     *
     * @param id       animation identifier (kept for signature parity)
     * @param priority factory priority
     * @param factory  per-player animation factory (never invoked by the shim)
     */
    public void registerFactory(Identifier id, int priority,
                                Function<AbstractClientPlayer, IAnimation> factory) {
        // no-op downgrade: nothing is created, so PlayerAnimationAccess will hand out an empty map
    }
}
