package dev.kosmx.playerAnim.core.data;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>Opaque holder for a loaded keyframe animation. The real class also parses {@code .emotecraft}
 * / {@code .json} animation files; the shim never produces an instance because
 * {@code PlayerAnimationRegistry.getAnimation(...)} always returns {@code null}.
 * See {@code PORT-STATUS.md} -> "player-animator 降级".
 */
public class KeyframeAnimation {

    private final String name;
    private final int length;

    public KeyframeAnimation(String name, int length) {
        this.name = name;
        this.length = length;
        this.bendable = true;
        this.allowOverlap = false;
        this.isPlaying = false;
    }

    /** Kept for signature parity with the real data class. */
    public boolean bendable;
    public boolean allowOverlap;
    public boolean isPlaying;

    public String getName() {
        return name;
    }

    public int getLength() {
        return length;
    }

    public int getEmoteTime() {
        return length;
    }
}
