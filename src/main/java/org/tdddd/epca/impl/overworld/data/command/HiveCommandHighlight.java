package org.tdddd.epca.impl.overworld.data.command;

import java.util.UUID;

/**
 * 服务器 -> 所有领巢者的高亮同步条目（纯 DTO，客户端渲染/菜单都只读它）。
 *
 * <p>一条记录 = 一只寄生体的高亮：</p>
 * <ul>
 *     <li>{@code owner}：指挥它的领巢者 UUID。客户端拿它和自己比较：
 *     自己的绿色/红色，其他领巢者的蓝色/橙色；</li>
 *     <li>{@code mode} 为 {@link #MODE_NONE} 表示“只是被选中、还没下达指令”；</li>
 *     <li>{@code targetId} 为 {@link #NO_ENTITY} 表示没有攻击目标；</li>
 *     <li>{@code routeId} 是它绑定的路线 id（路线库里的 {@link HiveRoute#id()}，空字符串 = 没绑定）。
 * 路线节点本身在同步包的路线库字段里，不在每条高亮里重复。</li>
 * </ul>
 */
public final class HiveCommandHighlight {

    public static final int NO_ENTITY = -1;
    /** 没有指令模式：仅被选中（绿/蓝描边，没有路线/目标）。 */
    public static final int MODE_NONE = -1;

    private final UUID parasite;
    private final int entityId;
    private final UUID owner;
    private final int mode;
    private final int targetId;
    private final String routeId;
    private final String dimension;
    /** 这只寄生体是否还在该领巢者的“选中名单”里（菜单判断“已选”用）。 */
    private final boolean selected;

    public HiveCommandHighlight(UUID parasite, int entityId, UUID owner, int mode, int targetId,
                                String routeId, String dimension, boolean selected) {
        this.parasite = parasite;
        this.entityId = entityId;
        this.owner = owner;
        this.mode = mode;
        this.targetId = targetId;
        this.routeId = routeId == null ? "" : routeId;
        this.dimension = dimension == null ? "" : dimension;
        this.selected = selected;
    }

    public UUID parasite() {
        return parasite;
    }

    public int entityId() {
        return entityId;
    }

    public UUID owner() {
        return owner;
    }

    public int mode() {
        return mode;
    }

    public int targetId() {
        return targetId;
    }

    public String routeId() {
        return routeId;
    }

    public String dimension() {
        return dimension;
    }

    public boolean selected() {
        return selected;
    }

    public boolean hasCommand() {
        return mode != MODE_NONE;
    }

    public HiveCommandMode commandMode() {
        return HiveCommandMode.byId(mode);
    }
}
