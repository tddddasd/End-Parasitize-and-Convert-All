package org.tdddd.epca.impl.overworld.data.command;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 路线库里的一条独立巡逻路线（服务端权威数据，持久化在
 * {@link HiveCommandSavedData} 的 {@code Routes} 里）。
 *
 * <p>一条路线 = 一个稳定 id + 可空名称 + 若干节点 + 首尾相连开关 + 所属维度。
 * 多条路线互不影响，同一只寄生体只绑定一条路线的 id（{@link HiveParasiteCommand#routeId()}），
 * 因此多只寄生体可以共用一个路线，也可以各走各的。</p>
 *
 * <p>{@code revision} 在节点/名称/闭合任何一项变化时 +1：客户端用来判断是否需要重画，
 * 巡逻 Goal 用它判断游标是否需要纠偏（路线被编辑后立刻重新取路径）。</p>
 *
 * <p>26.1.2 差异：SavedData 改成 Codec 序列化，这里补 {@link #CODEC}；字段名、语义与 1.20.1 的
 * NBT 写法（Id / Name / Dim / Closed / Points）保持一一对应，节点仍按 {@link BlockPos#asLong()} 打包成
 * {@code long} 列表保存。</p>
 */
public final class HiveRoute {

    /** 单条路线最多保存的节点数（与菜单上限一致）。 */
    public static final int MAX_POINTS = 32;
    /** 相邻节点允许的最小中心间距（格）：小于它就当成“同一个点”，由调用方给出提示。 */
    public static final double MIN_POINT_GAP = 0.5D;
    /** 路线名的最大长度（字符）。 */
    public static final int MAX_NAME_LENGTH = 24;

    public static final Codec<HiveRoute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("Id").forGetter(HiveRoute::id),
            Codec.STRING.optionalFieldOf("Name", "").forGetter(HiveRoute::name),
            Codec.STRING.optionalFieldOf("Dim", "").forGetter(HiveRoute::dimension),
            Codec.BOOL.optionalFieldOf("Closed", false).forGetter(HiveRoute::closed),
            Codec.LONG.listOf().optionalFieldOf("Points", List.<Long>of()).forGetter(HiveRoute::packedPoints)
    ).apply(instance, HiveRoute::fromCodec));

    /** 加点结果：菜单/按键的提示文案按它分支。 */
    public enum AddResult {
        /** 已追加到末尾。 */
        ADDED,
        /** 与上一点距离小于 {@link #MIN_POINT_GAP}，没有追加（需要提示玩家）。 */
        TOO_CLOSE,
        /** 节点数已达 {@link #MAX_POINTS}。 */
        FULL,
        /** 新点与首点重合：没有追加，但把路线标记成首尾相连。 */
        CLOSED_LOOP
    }

    private final String id;
    private String name = "";
    private final List<BlockPos> points = new ArrayList<>();
    private boolean closed;
    private String dimension = "";
    private int revision;

    public HiveRoute(String id) {
        this.id = id == null ? "" : id;
    }

    private static HiveRoute fromCodec(String id, String name, String dimension, boolean closed,
                                       List<Long> packedPoints) {
        HiveRoute route = new HiveRoute(id);
        route.name = name == null ? "" : name;
        if (route.name.length() > MAX_NAME_LENGTH) {
            route.name = route.name.substring(0, MAX_NAME_LENGTH);
        }
        route.dimension = dimension == null ? "" : dimension;
        route.closed = closed;
        if (packedPoints != null) {
            for (long packed : packedPoints) {
                if (route.points.size() >= MAX_POINTS) {
                    break;
                }
                route.points.add(BlockPos.of(packed));
            }
        }
        return route;
    }

    private List<Long> packedPoints() {
        List<Long> packed = new ArrayList<>(points.size());
        for (BlockPos pos : points) {
            packed.add(pos.asLong());
        }
        return packed;
    }

    public String id() {
        return id;
    }

    /** 玩家起的名字；空字符串表示“还没起名”，界面按序号显示。 */
    public String name() {
        return name;
    }

    public void setName(@Nullable String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
        }
        if (!trimmed.equals(this.name)) {
            this.name = trimmed;
            touch();
        }
    }

    /** 只读节点视图：外部只能通过 {@link #addPoint(BlockPos)} 等接口改。 */
    public List<BlockPos> points() {
        return Collections.unmodifiableList(points);
    }

    public int size() {
        return points.size();
    }

    public boolean closed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        if (this.closed != closed) {
            this.closed = closed;
            touch();
        }
    }

    public String dimension() {
        return dimension;
    }

    public void setDimension(@Nullable String dimension) {
        String value = dimension == null ? "" : dimension;
        if (!value.equals(this.dimension)) {
            this.dimension = value;
            touch();
        }
    }

    public int revision() {
        return revision;
    }

    public void touch() {
        revision++;
    }

    /**
     * 追加一个节点。
     *
     * <p>规则：</p>
     * <ul>
     *     <li>已满 {@link #MAX_POINTS} -> {@link AddResult#FULL}；</li>
     *     <li>与首点重合且已有 3 个以上节点 -> 不追加，改为首尾相连（{@link AddResult#CLOSED_LOOP}）；</li>
     *     <li>与上一点距离 &lt; {@link #MIN_POINT_GAP} -> {@link AddResult#TOO_CLOSE}，不追加；</li>
     *     <li>其余情况追加到末尾 -> {@link AddResult#ADDED}。</li>
     * </ul>
     */
    public AddResult addPoint(@Nullable BlockPos pos) {
        if (pos == null) {
            return AddResult.TOO_CLOSE;
        }
        if (points.size() >= MAX_POINTS) {
            return AddResult.FULL;
        }
        BlockPos immutable = pos.immutable();
        if (!points.isEmpty()) {
            if (points.size() >= 3 && points.get(0).equals(immutable)) {
                setClosed(true);
                return AddResult.CLOSED_LOOP;
            }
            BlockPos last = points.get(points.size() - 1);
            if (distanceBetween(last, immutable) < MIN_POINT_GAP) {
                return AddResult.TOO_CLOSE;
            }
        }
        points.add(immutable);
        touch();
        return AddResult.ADDED;
    }

    /** 删除末点（显式操作）。 */
    public boolean removeLastPoint() {
        if (points.isEmpty()) {
            return false;
        }
        points.remove(points.size() - 1);
        if (points.size() <= 2) {
            closed = false;
        }
        touch();
        return true;
    }

    /** 清空节点（显式操作）。 */
    public void clearPoints() {
        if (!points.isEmpty()) {
            points.clear();
        }
        if (closed) {
            closed = false;
        }
        touch();
    }

    /**
     * 网络/存档还原用：整表写入节点与闭合标记，不做去重（保持服务端原样）。
     * 只给反序列化路径用，玩家操作一律走 {@link #addPoint(BlockPos)}。
     */
    public void restorePoints(@Nullable List<BlockPos> restored, boolean closed) {
        points.clear();
        if (restored != null) {
            for (BlockPos pos : restored) {
                if (pos != null && points.size() < MAX_POINTS) {
                    points.add(pos.immutable());
                }
            }
        }
        this.closed = closed;
        touch();
    }

    /** 把末点与首点重合的写法规范化成 closed 标记（读旧存档 / 下达指令时调用）。 */
    public void normalizeLoop() {
        if (points.size() > 3 && points.get(0).equals(points.get(points.size() - 1))) {
            points.remove(points.size() - 1);
            closed = true;
            touch();
        } else if (points.size() <= 2 && closed) {
            closed = false;
            touch();
        }
    }

    /** 与上一点的间距（格）；没有上一点时返回 {@link Double#MAX_VALUE}。 */
    public double distanceToLast(@Nullable BlockPos pos) {
        if (pos == null || points.isEmpty()) {
            return Double.MAX_VALUE;
        }
        return distanceBetween(points.get(points.size() - 1), pos);
    }

    private static double distanceBetween(BlockPos a, BlockPos b) {
        return Vec3.atCenterOf(a).distanceTo(Vec3.atCenterOf(b));
    }

    /** 旧存档迁移用：把一条只存在于寄生体指令里的路线变成路线库条目。 */
    public static HiveRoute fromLegacy(String id, List<BlockPos> legacyPoints, boolean legacyClosed,
                                       String dimension) {
        HiveRoute route = new HiveRoute(id);
        route.dimension = dimension == null ? "" : dimension;
        route.closed = legacyClosed;
        if (legacyPoints != null) {
            for (BlockPos pos : legacyPoints) {
                if (route.points.size() >= MAX_POINTS) {
                    break;
                }
                if (pos != null) {
                    route.points.add(pos.immutable());
                }
            }
        }
        return route;
    }
}
