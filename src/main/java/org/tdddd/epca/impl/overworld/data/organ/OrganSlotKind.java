package org.tdddd.epca.impl.overworld.data.organ;

public enum OrganSlotKind {

    /**
     * 紫色外器官槽：需要生物质点解锁（头部 / 躯干各 2 格）。
     */
    PURPLE(true, false, false),

    /**
     * 躯干内部器官槽（红色）：默认解锁，但修改需要附近有任意召唤柱（一阶或二阶）。
     */
    RED_TORSO_INNER(false, true, true),

    /**
     * 头部内部器官槽（红色）：默认解锁，但修改需要附近有二阶召唤柱。
     */
    RED_HEAD_INNER(false, true, true),

    /**
     * 四肢内部器官槽（红色）：默认解锁，且修改完全不需要召唤柱 / beckon。
     */
    RED_LIMB_INNER(false, true, false);

    private final boolean needsUnlock;
    private final boolean defaultUnlocked;
    private final boolean summonPillarRequired;

    OrganSlotKind(boolean needsUnlock, boolean defaultUnlocked, boolean summonPillarRequired) {
        this.needsUnlock = needsUnlock;
        this.defaultUnlocked = defaultUnlocked;
        this.summonPillarRequired = summonPillarRequired;
    }

    public boolean needsUnlock() {
        return needsUnlock;
    }

    public boolean defaultUnlocked() {
        return defaultUnlocked;
    }

    public boolean isInnerGrid() {
        return this != PURPLE;
    }

    /**
     * 是否受“召唤柱 / beckon 7×7×7 范围”限制。
     * 四肢内部槽故意返回 false：更换器官时不再依赖召唤柱。
     */
    public boolean summonPillarRequired() {
        return summonPillarRequired;
    }
}
