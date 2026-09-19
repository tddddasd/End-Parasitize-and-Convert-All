package dev.kosmx.playerAnim.api.layered;

import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>Reproduces the real generic shape {@code ModifierLayer<T extends IAnimation>} so that
 * EPCA's {@code ModifierLayer<IAnimation>} declarations, the
 * {@code (ModifierLayer<IAnimation>) ...get(id)} downcasts and {@code setAnimation(...)} calls
 * all keep compiling.
 *
 * <p><b>Behavioural loss:</b> {@link #setAnimation(IAnimation)} only stores the value; the real
 * implementation also drives the player's skeletal animation and first-person pose. The
 * server-side game logic in the affected files is untouched.
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public class ModifierLayer<T extends IAnimation> extends AbstractModifier implements IAnimation {

    private final List<IModifier> modifiers = new ArrayList<>();
    private T animation;
    private boolean active;

    public ModifierLayer() {}

    public ModifierLayer(T animation) {
        this.animation = animation;
        this.active = animation != null;
    }

    public void addModifierBefore(IModifier modifier) {
        if (modifier != null) {
            modifiers.add(0, modifier);
        }
    }

    public void addModifierAfter(IModifier modifier) {
        if (modifier != null) {
            modifiers.add(modifier);
        }
    }

    public void removeModifier(IModifier modifier) {
        modifiers.remove(modifier);
    }

    public T getAnimation() {
        return animation;
    }

    /** No-op downgrade: records the animation but does not tick or render it. */
    public void setAnimation(T animation) {
        this.animation = animation;
        this.active = animation != null;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        // no-op downgrade
    }
}
