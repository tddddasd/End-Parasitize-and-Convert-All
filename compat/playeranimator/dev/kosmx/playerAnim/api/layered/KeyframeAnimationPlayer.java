package dev.kosmx.playerAnim.api.layered;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>EPCA constructs this as {@code new KeyframeAnimationPlayer(keyframe)} with the value returned
 * by {@code PlayerAnimationRegistry.getAnimation(Identifier)} (which this shim always returns
 * {@code null} for, so the constructor is never reached at runtime).
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public class KeyframeAnimationPlayer implements IAnimation {

    private final KeyframeAnimation animation;
    private int tick;

    public KeyframeAnimationPlayer(KeyframeAnimation animation) {
        this.animation = animation;
    }

    public KeyframeAnimation getData() {
        return animation;
    }

    public boolean isActive() {
        return animation != null;
    }

    public void tick() {
        tick++;
    }

    public void setCurrentTick(int tick) {
        this.tick = tick;
    }

    public int getCurrentTick() {
        return tick;
    }

    public void stop() {
        // no-op downgrade
    }
}
