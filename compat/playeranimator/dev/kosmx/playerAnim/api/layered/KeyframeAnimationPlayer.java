package dev.kosmx.playerAnim.api.layered;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;


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
