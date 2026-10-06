package org.tdddd.epca.impl.overworld.data.organ;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * STAGE A
 *
 * <p>SPEC</p>
 *
 * <table border="1">
 *   <caption> -&gt; </caption>
 *   <tr><th></th><th>JSON id</th><th></th><th></th></tr>
 *   <tr><td>{@link #BASE}</td><td>{@code base}</td>
 *       <td>16  + 39  + 33 </td>
 *       <td> {@code group == null} </td></tr>
 *   <tr><td>{@link #OUTER}</td><td>{@code outer}</td>
 *       <td>16  2 /  2 /  3 /  3</td>
 *       <td>{@link OrganSlotGroup#isOuter()}</td></tr>
 *   <tr><td>{@link #HEAD_INNER}</td><td>{@code head_inner}</td>
 *       <td>33 </td>
 *       <td>{@link OrganSlotGroup#isHeadInner()}</td></tr>
 *   <tr><td>{@link #ARM}</td><td>{@code arm}</td>
 *       <td> /  3 </td>
 *       <td>{@link OrganSlotGroup#isArm()}</td></tr>
 * </table>
 *
 * <h2></h2>
 * <p><b></b> =
 * {@link #matches(OrganSlotGroup)} </p>
 * <ul>
 *   <li> = {@code base} + {@code outer} + {@code arm}</li>
 *   <li>// = {@code base} + {@code outer}</li>
 *   <li> = {@code base} + {@code head_inner}</li>
 *   <li> = {@code base}</li>
 * </ul>
 * <p> {@code OrganStatManager}  JSON schema
 * {@code "condition": "arm"} <b></b> {@code outer}
 * </p>
 *
 * <h2></h2>
 * <p> {@link OrganSlotGroup}  kind /
 * </p>
 */
public enum OrganSlotCondition {

    /** JSON  {@code attributes}  */
    BASE("base"),

    /** 16  */
    OUTER("outer"),

    /** 33  */
    HEAD_INNER("head_inner"),

    /**  /  */
    ARM("arm");

    private final String id;

    OrganSlotCondition(String id) {
        this.id = id;
    }

    /** JSON  */
    public String id() {
        return id;
    }

    /** {@code group == null}  {@link #BASE}  */
    public boolean matches(@Nullable OrganSlotGroup group) {
        return switch (this) {
            case BASE -> true;
            case OUTER -> group != null && group.isOuter();
            case HEAD_INNER -> group != null && group.isHeadInner();
            case ARM -> group != null && group.isArm();
        };
    }

    /**
     * JSON  -&gt;  {@code null}
     *
     * <p>{@code base}/{@code any}
     * {@code outer}/{@code outer_organ}{@code head_inner}
     * {@code arm}/{@code arms}/{@code left_or_right_arm}</p>
     *
     * <p><b></b> {@code head}  {@code head_inner} {@code head}
     * </p>
     */
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

