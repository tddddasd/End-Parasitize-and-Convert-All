package dev.kosmx.playerAnim.minecraftApi;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.impl.ShimBridge;
import net.minecraft.resources.Identifier;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>EPCA calls {@code PlayerAnimationRegistry.getAnimation(id)} and null-checks the result before
 * wrapping it in a {@code KeyframeAnimationPlayer}. Always returning {@code null} makes both call
 * sites no-ops.
 *
 * <p>The reflective guard is here on purpose: if a genuine player-animator implementation ever
 * becomes visible, the call is delegated to it and any failure degrades back to {@code null}
 * instead of crashing the client.
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public final class PlayerAnimationRegistry {

    private PlayerAnimationRegistry() {}

    /** @return {@code null} in the downgrade build - i.e. "no such animation". */
    public static KeyframeAnimation getAnimation(Identifier id) {
        Object real = ShimBridge.tryReal(
                "dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry",
                "getAnimation",
                new Class<?>[] { Identifier.class },
                id);
        if (real instanceof KeyframeAnimation keyframe) {
            return keyframe;
        }
        ShimBridge.warnDisabledOnce();
        return null;
    }

    /** Kept for signature parity with the real registry. */
    public static boolean isRegistered(Identifier id) {
        return getAnimation(id) != null;
    }
}
