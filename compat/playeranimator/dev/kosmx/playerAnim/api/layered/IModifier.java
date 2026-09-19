package dev.kosmx.playerAnim.api.layered;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import org.jetbrains.annotations.NotNull;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public interface IModifier extends IAnimation {
    @NotNull
    default FirstPersonMode getFirstPersonMode(float tickDelta) {
        return FirstPersonMode.DISABLED;
    }

    @NotNull
    default FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        return new FirstPersonConfiguration();
    }
}
