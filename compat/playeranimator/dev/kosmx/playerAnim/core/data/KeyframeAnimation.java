package dev.kosmx.playerAnim.core.data;


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
