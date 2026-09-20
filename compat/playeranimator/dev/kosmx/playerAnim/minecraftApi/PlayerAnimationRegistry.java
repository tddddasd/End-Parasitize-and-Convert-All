package dev.kosmx.playerAnim.minecraftApi;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.impl.ShimBridge;
import net.minecraft.resources.Identifier;


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
