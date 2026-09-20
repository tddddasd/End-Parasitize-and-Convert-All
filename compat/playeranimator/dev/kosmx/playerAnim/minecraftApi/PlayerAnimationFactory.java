package dev.kosmx.playerAnim.minecraftApi;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.function.Function;


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
