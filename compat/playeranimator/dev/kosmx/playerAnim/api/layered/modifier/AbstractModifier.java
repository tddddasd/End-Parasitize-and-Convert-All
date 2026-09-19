package dev.kosmx.playerAnim.api.layered.modifier;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>EPCA's {@code FirstPersonModifier} extends this class and overrides
 * {@link #getFirstPersonMode(float)} / {@link #getFirstPersonConfiguration(float)}, so both
 * methods must exist here with the exact signatures. Nothing calls them in the no-op build.
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public abstract class AbstractModifier implements IModifier {

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        return FirstPersonMode.DISABLED;
    }

    @Override
    public @NotNull FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
        return new FirstPersonConfiguration();
    }
}
