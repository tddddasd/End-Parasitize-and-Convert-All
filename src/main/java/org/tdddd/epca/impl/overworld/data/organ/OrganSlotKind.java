package org.tdddd.epca.impl.overworld.data.organ;

public enum OrganSlotKind {

    PURPLE(true, false),

    RED_TORSO_INNER(false, true),

    RED_HEAD_INNER(false, true);

    private final boolean needsUnlock;
    private final boolean defaultUnlocked;

    OrganSlotKind(boolean needsUnlock, boolean defaultUnlocked) {
        this.needsUnlock = needsUnlock;
        this.defaultUnlocked = defaultUnlocked;
    }

    public boolean needsUnlock() {
        return needsUnlock;
    }

    public boolean defaultUnlocked() {
        return defaultUnlocked;
    }

    public boolean isInnerGrid() {
        return this == RED_TORSO_INNER || this == RED_HEAD_INNER;
    }
}
