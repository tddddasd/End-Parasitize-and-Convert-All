package org.tdddd.epca.impl.overworld.data.command;

/**
 * 领巢者对寄生体下达的指挥模式。
 *
 * <p>三种模式互斥，同一只寄生体同一时间只有一种模式：</p>
 * <ul>
 *     <li>{@link #FOLLOW}：跟随下达指令的领巢者（保持约 3 格停候距离，不加速、不挤人）；
 *     若同时指定了攻击目标，则优先追击该目标。</li>
 *     <li>{@link #GUARD}：原地看守：站位固定在指令下达时的位置，可自行索敌并攻击，
 *     但不追击离开攻击距离的目标。</li>
 *     <li>{@link #PATROL}：沿路线点巡逻：路线点至少两个，来回巡逻；路线点大于两个且首尾相连时
 *     循环绕线；允许离开路线 ±8 格索敌，目标或自身越界就取消索敌并回到路线。</li>
 * </ul>
 */
public enum HiveCommandMode {
    FOLLOW,
    GUARD,
    PATROL;

    public static final HiveCommandMode[] VALUES = values();

    /** 网络包/NBT 里越界的编号一律回落到跟随，避免坏数据让指挥系统停摆。 */
    public static HiveCommandMode byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            return FOLLOW;
        }
        return VALUES[id];
    }

    public static HiveCommandMode byName(String name) {
        if (name == null || name.isEmpty()) {
            return FOLLOW;
        }
        for (HiveCommandMode mode : VALUES) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return FOLLOW;
    }
}
