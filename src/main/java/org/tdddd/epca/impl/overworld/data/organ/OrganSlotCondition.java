package org.tdddd.epca.impl.overworld.data.organ;

import javax.annotation.Nullable;
import java.util.Locale;

public enum OrganSlotCondition {

    BASE("base"),

    OUTER("outer"),

    HEAD_INNER("head_inner"),

    ARM("arm");

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
            default -> null;
        };
    }
}
