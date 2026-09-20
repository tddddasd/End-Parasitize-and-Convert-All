package dev.kosmx.playerAnim.minecraftApi;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;


public final class PlayerAnimationAccess {

    private PlayerAnimationAccess() {}

    /**
     * @return always an empty map in the shim - the real implementation returns the player's live
     *         {@code ModifierLayer} registry, which is what makes the animation play.
     */
    public static Map<Identifier, IAnimation> getPlayerAssociatedData(AbstractClientPlayer player) {
        return Collections.emptyMap();
    }

    /** No-op downgrade; the real API can also detach a layer. */
    public static void removePlayerAssociatedData(AbstractClientPlayer player, Identifier id) {
        // no-op downgrade
    }
}
