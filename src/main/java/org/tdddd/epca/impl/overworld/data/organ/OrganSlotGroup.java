package org.tdddd.epca.impl.overworld.data.organ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  GUI
 *
 * <p>SPEC  1  C2C7  {@link #ALL}
 * {@link NestLeaderOrganData} <b></b>
 *  +
 * </p>
 *
 * <table border="1">
 *   <caption>SPEC  -&gt; </caption>
 *   <tr><th></th><th></th><th></th></tr>
 *   <tr><td>head</td><td>2</td><td> / </td></tr>
 *   <tr><td>torso</td><td>2</td><td> / </td></tr>
 *   <tr><td>left_arm</td><td>3</td><td> / </td></tr>
 *   <tr><td>right_arm</td><td>3</td><td> / </td></tr>
 *   <tr><td>left_leg</td><td>3</td><td> / </td></tr>
 *   <tr><td>right_leg</td><td>3</td><td> / </td></tr>
 *   <tr><td>torso_inner</td><td>3x9 = 27</td><td></td></tr>
 *   <tr><td>head_inner</td><td>3x3 = 9</td><td></td></tr>
 * </table>
 */
public final class OrganSlotGroup {

    //   ALL  size
    private static final int HEAD_FIRST = 0;                       // 2  -> [0, 2)
    private static final int TORSO_FIRST = HEAD_FIRST + 2;         // 2  -> [2, 4)
    private static final int LEFT_ARM_FIRST = TORSO_FIRST + 2;     // 3  -> [4, 7)
    private static final int RIGHT_ARM_FIRST = LEFT_ARM_FIRST + 3; // 3  -> [7, 10)
    private static final int LEFT_LEG_FIRST = RIGHT_ARM_FIRST + 3; // 3  -> [10, 13)
    private static final int RIGHT_LEG_FIRST = LEFT_LEG_FIRST + 3; // 3  -> [13, 16)
    private static final int TORSO_INNER_FIRST = RIGHT_LEG_FIRST + 3;  // 27 -> [16, 43)
    private static final int HEAD_INNER_FIRST = TORSO_INNER_FIRST + 27; // 9 -> [43, 52)

    /** STAGE 2  33  */
    public static final int HEAD_INNER_ROWS = 3;
    public static final int HEAD_INNER_COLUMNS = 3;
    /** /STAGE 2  39  */
    public static final int TORSO_INNER_ROWS = 3;
    public static final int TORSO_INNER_COLUMNS = 9;

    /** SPEC C22  */
    public static final OrganSlotGroup HEAD =
            new OrganSlotGroup("head", "头部", OrganSlotKind.PURPLE, 2, 1, 2, HEAD_FIRST);

    /** SPEC C32  */
    public static final OrganSlotGroup TORSO =
            new OrganSlotGroup("torso", "躯干", OrganSlotKind.PURPLE, 2, 1, 2, TORSO_FIRST);

    /** SPEC C43  */
    public static final OrganSlotGroup LEFT_ARM =
            new OrganSlotGroup("left_arm", "左臂", OrganSlotKind.PURPLE, 3, 1, 3, LEFT_ARM_FIRST);

    /** SPEC C43  */
    public static final OrganSlotGroup RIGHT_ARM =
            new OrganSlotGroup("right_arm", "右臂", OrganSlotKind.PURPLE, 3, 1, 3, RIGHT_ARM_FIRST);

    /** SPEC C53  */
    public static final OrganSlotGroup LEFT_LEG =
            new OrganSlotGroup("left_leg", "左腿", OrganSlotKind.PURPLE, 3, 1, 3, LEFT_LEG_FIRST);

    /** SPEC C53  */
    public static final OrganSlotGroup RIGHT_LEG =
            new OrganSlotGroup("right_leg", "右腿", OrganSlotKind.PURPLE, 3, 1, 3, RIGHT_LEG_FIRST);

    /** SPEC C6 3   9  +  */
    public static final OrganSlotGroup TORSO_INNER =
            new OrganSlotGroup("torso_inner", "躯干内部", OrganSlotKind.RED_TORSO_INNER,
                    27, TORSO_INNER_ROWS, TORSO_INNER_COLUMNS, TORSO_INNER_FIRST);

    /** SPEC C7 3  3
     *  {@link NestLeaderOrganData#DEFAULT_HEAD_INNER_FLESH_LOCALS} */
    public static final OrganSlotGroup HEAD_INNER =
            new OrganSlotGroup("head_inner", "头部内部", OrganSlotKind.RED_HEAD_INNER,
                    9, HEAD_INNER_ROWS, HEAD_INNER_COLUMNS, HEAD_INNER_FIRST);

    /**  */
    public static final List<OrganSlotGroup> ALL = List.of(
            HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, TORSO_INNER, HEAD_INNER);

    //  STAGE A SPEC  +
    // / OrganSlotCondition

    /**
     *  = 6  2 +  2 +  3 +  3 +  3 +  3
     *
     * <p> {@link OrganSlotKind#needsUnlock()}/</p>
     */
    public static final List<OrganSlotGroup> OUTER_GROUPS = List.copyOf(purpleGroups());

    /**  /  3  */
    public static final List<OrganSlotGroup> ARM_GROUPS = List.of(LEFT_ARM, RIGHT_ARM);

    /** = {@link #OUTER_GROUPS}  size 1.20.1  16 */
    public static final int OUTER_SLOT_COUNT = countSlots(OUTER_GROUPS);

    private static int countSlots(List<OrganSlotGroup> groups) {
        int total = 0;
        for (OrganSlotGroup group : groups) {
            total += group.size();
        }
        return total;
    }

    private final String id;
    private final String displayName;
    private final OrganSlotKind kind;
    private final int size;
    private final int rows;
    private final int columns;
    private final int firstIndex;

    private OrganSlotGroup(String id, String displayName, OrganSlotKind kind,
                           int size, int rows, int columns, int firstIndex) {
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
        this.size = size;
        this.rows = rows;
        this.columns = columns;
        this.firstIndex = firstIndex;
    }

    /**  /  */
    public String id() {
        return id;
    }

    /** STAGE 2  */
    public String displayName() {
        return displayName;
    }

    public OrganSlotKind kind() {
        return kind;
    }

    /**  */
    public int size() {
        return size;
    }

    /**  1 */
    public int rows() {
        return rows;
    }

    /**  1 */
    public int columns() {
        return columns;
    }

    /**  */
    public int firstIndex() {
        return firstIndex;
    }

    /**  {@code [firstIndex(), firstIndex() + size())} */
    public boolean contains(int globalIndex) {
        return globalIndex >= firstIndex && globalIndex < firstIndex + size;
    }

    /**
     * =
     *
     * <p> {@link OrganSlotKind#needsUnlock()}
     *  {@link OrganSlotCondition}  {@code kind()}</p>
     */
    public boolean isOuter() {
        return kind.needsUnlock();
    }

    /**  / {@link #ARM_GROUPS} */
    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    /**  33 */
    public boolean isHeadInner() {
        return this == HEAD_INNER;
    }

    /**  39 */
    public boolean isTorsoInner() {
        return this == TORSO_INNER;
    }

    /**  -&gt;  */
    public int globalIndex(int localIndex) {
        if (localIndex < 0 || localIndex >= size) {
            throw new IndexOutOfBoundsException(
                    "组 " + id + " 的局部下标越界: " + localIndex + " (size=" + size + ")");
        }
        return firstIndex + localIndex;
    }

    /**  -&gt;  {@code null} */
    public static OrganSlotGroup byGlobalIndex(int globalIndex) {
        for (OrganSlotGroup group : ALL) {
            if (group.contains(globalIndex)) return group;
        }
        return null;
    }

    /** 2+2+3+3+3+3+27+9 = 52 */
    public static int totalSlots() {
        int total = 0;
        for (OrganSlotGroup group : ALL) {
            total += group.size();
        }
        return total;
    }

    /**  */
    public static List<OrganSlotGroup> purpleGroups() {
        List<OrganSlotGroup> result = new ArrayList<>(6);
        for (OrganSlotGroup group : ALL) {
            if (group.kind().needsUnlock()) result.add(group);
        }
        return Collections.unmodifiableList(result);
    }

    /**  */
    public static int[] purpleSlotIndices() {
        List<Integer> indices = new ArrayList<>(16);
        for (OrganSlotGroup group : ALL) {
            if (!group.kind().needsUnlock()) continue;
            for (int i = 0; i < group.size(); i++) {
                indices.add(group.globalIndex(i));
            }
        }
        int[] array = new int[indices.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = indices.get(i);
        }
        return array;
    }

    @Override
    public String toString() {
        return "OrganSlotGroup[" + id + " x" + size + " kind=" + kind + " first=" + firstIndex + "]";
    }
}

