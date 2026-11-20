package org.tdddd.epca.impl.overworld.data.organ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 器官槽位分组。
 *
 * <p>布局（全局下标 = 数组下标，全端一致）：</p>
 * <pre>
 *   0..1    头部外器官（紫，2 格，1×2）
 *   2..3    躯干外器官（紫，2 格，1×2）
 *   4..6    左臂内部器官（红，1 列 × 3 行）
 *   7..9    右臂内部器官（红，1 列 × 3 行）
 *   10..12  左腿内部器官（红，1 列 × 3 行）
 *   13..15  右腿内部器官（红，1 列 × 3 行）
 *   16..30  躯干内部器官（红，5 列 × 3 行 = 15 格，横向长方形，从左到右 / 从上到下编号）
 *   31..34  头部内部器官（红，2 列 × 2 行 = 4 格，UI 上以四个紧贴的 45° 菱形绘制）
 * </pre>
 */
public final class OrganSlotGroup {

    private static final int HEAD_FIRST = 0;
    private static final int TORSO_FIRST = HEAD_FIRST + 2;
    private static final int LEFT_ARM_FIRST = TORSO_FIRST + 2;
    private static final int RIGHT_ARM_FIRST = LEFT_ARM_FIRST + 3;
    private static final int LEFT_LEG_FIRST = RIGHT_ARM_FIRST + 3;
    private static final int RIGHT_LEG_FIRST = LEFT_LEG_FIRST + 3;
    private static final int TORSO_INNER_FIRST = RIGHT_LEG_FIRST + 3;
    private static final int HEAD_INNER_FIRST = TORSO_INNER_FIRST + 15;

    /** 头部内部：UI 上绘制成 2×2 的 45° 斜放菱形（四个菱形紧贴，拼成一个大菱形）。 */
    public static final int HEAD_INNER_ROWS = 2;
    public static final int HEAD_INNER_COLUMNS = 2;

    /** 躯干内部：宽 5 × 高 3 的横向长方形槽区（15 格，从左到右、从上到下编号）。 */
    public static final int TORSO_INNER_ROWS = 3;
    public static final int TORSO_INNER_COLUMNS = 5;

    public static final OrganSlotGroup HEAD =
            new OrganSlotGroup("head", "头部", OrganSlotKind.PURPLE, 2, 1, 2, HEAD_FIRST);

    public static final OrganSlotGroup TORSO =
            new OrganSlotGroup("torso", "躯干", OrganSlotKind.PURPLE, 2, 1, 2, TORSO_FIRST);

    public static final OrganSlotGroup LEFT_ARM =
            new OrganSlotGroup("left_arm", "左臂", OrganSlotKind.RED_LIMB_INNER, 3, 3, 1, LEFT_ARM_FIRST);

    public static final OrganSlotGroup RIGHT_ARM =
            new OrganSlotGroup("right_arm", "右臂", OrganSlotKind.RED_LIMB_INNER, 3, 3, 1, RIGHT_ARM_FIRST);

    public static final OrganSlotGroup LEFT_LEG =
            new OrganSlotGroup("left_leg", "左腿", OrganSlotKind.RED_LIMB_INNER, 3, 3, 1, LEFT_LEG_FIRST);

    public static final OrganSlotGroup RIGHT_LEG =
            new OrganSlotGroup("right_leg", "右腿", OrganSlotKind.RED_LIMB_INNER, 3, 3, 1, RIGHT_LEG_FIRST);

    public static final OrganSlotGroup TORSO_INNER =
            new OrganSlotGroup("torso_inner", "躯干内部", OrganSlotKind.RED_TORSO_INNER,
                    15, TORSO_INNER_ROWS, TORSO_INNER_COLUMNS, TORSO_INNER_FIRST);

    public static final OrganSlotGroup HEAD_INNER =
            new OrganSlotGroup("head_inner", "头部内部", OrganSlotKind.RED_HEAD_INNER,
                    4, HEAD_INNER_ROWS, HEAD_INNER_COLUMNS, HEAD_INNER_FIRST);

    public static final List<OrganSlotGroup> ALL = List.of(
            HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, TORSO_INNER, HEAD_INNER);

    public static final List<OrganSlotGroup> OUTER_GROUPS = List.copyOf(purpleGroups());

    public static final List<OrganSlotGroup> ARM_GROUPS = List.of(LEFT_ARM, RIGHT_ARM);

    public static final List<OrganSlotGroup> LEG_GROUPS = List.of(LEFT_LEG, RIGHT_LEG);

    public static final List<OrganSlotGroup> LIMB_INNER_GROUPS =
            List.of(LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG);

    public static final List<OrganSlotGroup> INNER_GROUPS =
            List.of(HEAD_INNER, TORSO_INNER, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG);

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

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public OrganSlotKind kind() {
        return kind;
    }

    public int size() {
        return size;
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public int firstIndex() {
        return firstIndex;
    }

    public boolean contains(int globalIndex) {
        return globalIndex >= firstIndex && globalIndex < firstIndex + size;
    }

    public boolean isOuter() {
        return kind.needsUnlock();
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public boolean isLimbInner() {
        return isArm() || isLeg();
    }

    public boolean isHeadInner() {
        return this == HEAD_INNER;
    }

    public boolean isTorsoInner() {
        return this == TORSO_INNER;
    }

    public int globalIndex(int localIndex) {
        if (localIndex < 0 || localIndex >= size) {
            throw new IndexOutOfBoundsException(
                    "组 " + id + " 的局部下标越界: " + localIndex + " (size=" + size + ")");
        }
        return firstIndex + localIndex;
    }

    public static OrganSlotGroup byGlobalIndex(int globalIndex) {
        for (OrganSlotGroup group : ALL) {
            if (group.contains(globalIndex)) return group;
        }
        return null;
    }

    public static int totalSlots() {
        int total = 0;
        for (OrganSlotGroup group : ALL) {
            total += group.size();
        }
        return total;
    }

    public static List<OrganSlotGroup> purpleGroups() {
        List<OrganSlotGroup> result = new ArrayList<>(6);
        for (OrganSlotGroup group : ALL) {
            if (group.kind().needsUnlock()) result.add(group);
        }
        return Collections.unmodifiableList(result);
    }

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
