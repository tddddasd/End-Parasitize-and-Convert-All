package org.tdddd.epca.impl.overworld.data.organ;

/**
 * SPEC  1  C  =  =
 *
 * <p> GUI </p>
 *
 * <p>1.20.1 -&gt; 26.1.2 Minecraft API 1.20.1 </p>
 */
public enum OrganSlotKind {
    /**
     *  2 /  2 /  3 /  3 /  3 /  3 16
     *
     * <p>SPEC D2<b></b>
     *  {@code isUnlocked(i)} </p>
     */
    PURPLE(true, false),

    /**
     * 3   9  = 27
     *
     * <p>SPEC D3 777 <b></b>
     * </p>
     * <p> 22  + 1  + 4 </p>
     */
    RED_TORSO_INNER(false, true),

    /**
     * 3  3 = 9
     *
     * <p>SPEC D4 777 <b></b>
     * 7  {@link NestLeaderOrganData#DEFAULT_HEAD_INNER_FLESH_LOCALS}</p>
     */
    RED_HEAD_INNER(false, true);

    private final boolean needsUnlock;
    private final boolean defaultUnlocked;

    OrganSlotKind(boolean needsUnlock, boolean defaultUnlocked) {
        this.needsUnlock = needsUnlock;
        this.defaultUnlocked = defaultUnlocked;
    }

    /**  */
    public boolean needsUnlock() {
        return needsUnlock;
    }

    /**  {@link NestLeaderOrganData#createDefault()}  */
    public boolean defaultUnlocked() {
        return defaultUnlocked;
    }

    /**  */
    public boolean isInnerGrid() {
        return this == RED_TORSO_INNER || this == RED_HEAD_INNER;
    }
}

