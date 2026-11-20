package org.tdddd.epca.impl.overworld.data.command;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 一只寄生体当前收到的指挥指令（服务端权威数据，持久化在存档 SavedData 里）。
 *
 * <p>数据归属：{@code parasite} 是被指挥的寄生体 UUID，{@code owner} 是下达指令的领巢者 UUID。
 * 指令本身是长期任务，服务器重启、换维度、重登都不丢；实体死亡/消失时由
 * {@link HiveCommandManager} 清理。</p>
 *
 * <p>巡逻不再把节点存在指令里，而是只存 {@code routeId}：真实节点在
 * {@link HiveCommandSavedData} 的路线库里（{@link HiveRoute}）。这样多条路线可以共存、
 * 一条路线可以被多只寄生体共用，编辑路线时所有绑定它的寄生体立刻按新节点巡逻。</p>
 *
 * <p>{@code revision} 是修订号：目标、模式、绑定路线任何一项变化都会 +1，
 * 指挥 Goal 在每拍读取时对比修订号，发现变化就重置巡逻游标/丢弃过期寻路。</p>
 */
public final class HiveParasiteCommand {

    private static final String TAG_PARASITE = "Parasite";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_MODE = "Mode";
    private static final String TAG_TARGET = "Target";
    private static final String TAG_GUARD_POS = "GuardPos";
    private static final String TAG_DIMENSION = "Dim";
    private static final String TAG_ROUTE_ID = "RouteId";
    private static final String TAG_REVISION = "Rev";

    /** 旧存档（路线直接存在指令里）的键，只用于迁移。 */
    private static final String TAG_LEGACY_ROUTE = "Route";
    private static final String TAG_LEGACY_CLOSED = "Closed";

    private final UUID parasite;
    private UUID owner;
    private HiveCommandMode mode = HiveCommandMode.FOLLOW;
    @Nullable
    private UUID target;
    @Nullable
    private BlockPos guardPos;
    /** 指令生效的维度（{@code ResourceLocation.toString()}），换维度后用于过滤。 */
    private String dimension = "";
    /** 绑定的路线 id（路线库里 {@link HiveRoute#id()}）；空字符串 = 没绑定。 */
    private String routeId = "";
    private int revision;

    /** 迁移用：旧存档里直接写在指令上的节点与闭合标记，读完立刻被搬进路线库。 */
    @Nullable
    private List<BlockPos> legacyRoute;
    private boolean legacyClosed;

    public HiveParasiteCommand(UUID parasite, UUID owner) {
        this.parasite = Objects.requireNonNull(parasite, "parasite");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public UUID parasite() {
        return parasite;
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        touch();
    }

    public HiveCommandMode mode() {
        return mode;
    }

    public void setMode(HiveCommandMode mode) {
        this.mode = mode == null ? HiveCommandMode.FOLLOW : mode;
        touch();
    }

    @Nullable
    public UUID target() {
        return target;
    }

    public void setTarget(@Nullable UUID target) {
        this.target = target;
        touch();
    }

    @Nullable
    public BlockPos guardPos() {
        return guardPos;
    }

    public void setGuardPos(@Nullable BlockPos guardPos) {
        this.guardPos = guardPos;
    }

    public String dimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension == null ? "" : dimension;
        touch();
    }

    /** 绑定的路线 id；空字符串 = 没有绑定任何路线。 */
    public String routeId() {
        return routeId;
    }

    public void setRouteId(@Nullable String routeId) {
        String value = routeId == null ? "" : routeId;
        if (!value.equals(this.routeId)) {
            this.routeId = value;
            touch();
        }
    }

    public boolean hasRoute() {
        return !routeId.isEmpty();
    }

    public int revision() {
        return revision;
    }

    /** 任何指令字段变化都要调用，Goal 靠它发现“指令改过了”。 */
    public void touch() {
        revision++;
    }

    // ------------------------------------------------------------------
    // 旧存档迁移
    // ------------------------------------------------------------------

    @Nullable
    public List<BlockPos> legacyRoute() {
        return legacyRoute;
    }

    public boolean legacyClosed() {
        return legacyClosed;
    }

    public void clearLegacyRoute() {
        legacyRoute = null;
        legacyClosed = false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_PARASITE, parasite);
        tag.putUUID(TAG_OWNER, owner);
        tag.putString(TAG_MODE, mode.name());
        if (target != null) {
            tag.putUUID(TAG_TARGET, target);
        }
        if (guardPos != null) {
            tag.putLong(TAG_GUARD_POS, guardPos.asLong());
        }
        tag.putString(TAG_DIMENSION, dimension);
        tag.putString(TAG_ROUTE_ID, routeId);
        tag.putInt(TAG_REVISION, revision);
        return tag;
    }

    @Nullable
    public static HiveParasiteCommand load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID(TAG_PARASITE) || !tag.hasUUID(TAG_OWNER)) {
            return null;
        }
        HiveParasiteCommand command = new HiveParasiteCommand(tag.getUUID(TAG_PARASITE), tag.getUUID(TAG_OWNER));
        command.mode = HiveCommandMode.byName(tag.getString(TAG_MODE));
        if (tag.hasUUID(TAG_TARGET)) {
            command.target = tag.getUUID(TAG_TARGET);
        }
        if (tag.contains(TAG_GUARD_POS, Tag.TAG_LONG)) {
            command.guardPos = BlockPos.of(tag.getLong(TAG_GUARD_POS));
        }
        command.dimension = tag.getString(TAG_DIMENSION);
        command.routeId = tag.getString(TAG_ROUTE_ID);
        command.revision = tag.getInt(TAG_REVISION);
        // 旧版本把节点直接写在指令里：读出来等 HiveCommandSavedData 迁移成路线库条目。
        long[] legacyPoints = tag.getLongArray(TAG_LEGACY_ROUTE);
        if (legacyPoints.length > 0) {
            List<BlockPos> points = new ArrayList<>(legacyPoints.length);
            for (long packed : legacyPoints) {
                if (points.size() >= HiveRoute.MAX_POINTS) {
                    break;
                }
                points.add(BlockPos.of(packed));
            }
            command.legacyRoute = points;
            command.legacyClosed = tag.getBoolean(TAG_LEGACY_CLOSED);
        }
        return command;
    }
}
