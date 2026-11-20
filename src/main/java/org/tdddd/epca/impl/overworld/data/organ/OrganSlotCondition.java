package org.tdddd.epca.impl.overworld.data.organ;

import javax.annotation.Nullable;
import java.util.Locale;

public enum OrganSlotCondition {

    BASE("base"),

    OUTER("outer"),

    HEAD_INNER("head_inner"),

    ARM("arm"),

    /** 四肢内部槽（2026-11 布局改造新增）：同时覆盖手臂与腿部的内部器官槽。 */
    LIMB_INNER("limb_inner"),

    /** 腿内部槽。 */
    LEG("leg");

    private final String id;

    OrganSlotCondition(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean matches(@Nullable OrganSlotGroup group) {
        return switch (this) {
            case BASE -> true;
            case OUTER -> group != null && group.isOuter();
            case HEAD_INNER -> group != null && group.isHeadInner();
            case ARM -> group != null && group.isArm();
            case LIMB_INNER -> group != null && group.isLimbInner();
            case LEG -> group != null && group.isLeg();
        };
    }

    @Nullable
    public static OrganSlotCondition byId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "base", "any" -> BASE;
            case "outer", "outer_organ" -> OUTER;
            case "head_inner" -> HEAD_INNER;
            case "arm", "arms", "left_or_right_arm" -> ARM;
            case "limb_inner", "limbs", "arm_leg_inner" -> LIMB_INNER;
            case "leg", "legs", "left_or_right_leg" -> LEG;
            default -> null;
        };
    }
}
