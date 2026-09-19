package dev.kosmx.playerAnim.api.firstPerson;

/**
 * Compile-time downgrade shim for <b>player-animator</b> (no 26.1.2 release).
 *
 * <p>The fluent setters are kept because EPCA's {@code PlayerAnimator#registerPlayerAnimation}
 * chains them. They only record state; nothing reads it in the no-op build.
 */
public class FirstPersonConfiguration {
    private boolean showRightArm = true;
    private boolean showLeftArm = true;
    private boolean showRightItem = true;
    private boolean showLeftItem = true;

    public FirstPersonConfiguration() {}

    public FirstPersonConfiguration(boolean showRightArm, boolean showLeftArm,
                                    boolean showRightItem, boolean showLeftItem) {
        this.showRightArm = showRightArm;
        this.showLeftArm = showLeftArm;
        this.showRightItem = showRightItem;
        this.showLeftItem = showLeftItem;
    }

    public FirstPersonConfiguration setShowRightArm(boolean value) {
        this.showRightArm = value;
        return this;
    }

    public FirstPersonConfiguration setShowLeftArm(boolean value) {
        this.showLeftArm = value;
        return this;
    }

    public FirstPersonConfiguration setShowRightItem(boolean value) {
        this.showRightItem = value;
        return this;
    }

    public FirstPersonConfiguration setShowLeftItem(boolean value) {
        this.showLeftItem = value;
        return this;
    }

    public boolean isShowRightArm() { return showRightArm; }

    public boolean isShowLeftArm() { return showLeftArm; }

    public boolean isShowRightItem() { return showRightItem; }

    public boolean isShowLeftItem() { return showLeftItem; }
}
