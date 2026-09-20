package dev.kosmx.playerAnim.api.layered;

import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;

import java.util.ArrayList;
import java.util.List;


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
