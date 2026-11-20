package org.tdddd.epca.impl.overworld.data.command;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
 *
 * <p>26.1.2 差异：存档从手写 NBT 改成 {@link #CODEC}，字段名与 1.20.1 的 NBT 键
 * （Parasite / Owner / Mode / Target / GuardPos / Dim / RouteId / Rev）保持一致。</p>
 */
public final class HiveParasiteCommand {

    public static final Codec<HiveParasiteCommand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("Parasite").forGetter(HiveParasiteCommand::parasite),
            UUIDUtil.STRING_CODEC.fieldOf("Owner").forGetter(HiveParasiteCommand::owner),
            Codec.STRING.optionalFieldOf("Mode", HiveCommandMode.FOLLOW.name())
                    .forGetter(command -> command.mode.name()),
            UUIDUtil.STRING_CODEC.optionalFieldOf("Target")
                    .forGetter(HiveParasiteCommand::targetOptional),
            Codec.LONG.optionalFieldOf("GuardPos")
                    .forGetter(HiveParasiteCommand::guardPosOptional),
            Codec.STRING.optionalFieldOf("Dim", "").forGetter(command -> command.dimension),
            Codec.STRING.optionalFieldOf("RouteId", "").forGetter(command -> command.routeId),
            Codec.INT.optionalFieldOf("Rev", 0).forGetter(HiveParasiteCommand::revision),
            // 旧存档（路线直接存在指令里）的键，只用于迁移。
            Codec.LONG.listOf().optionalFieldOf("Route", List.<Long>of())
                    .forGetter(HiveParasiteCommand::legacyPacked),
            Codec.BOOL.optionalFieldOf("Closed", false).forGetter(command -> command.legacyClosed)
    ).apply(instance, HiveParasiteCommand::fromCodec));

    private final UUID parasite;
    private UUID owner;
    private HiveCommandMode mode = HiveCommandMode.FOLLOW;
    @Nullable
    private UUID target;
    @Nullable
    private BlockPos guardPos;
    /** 指令生效的维度（{@code Identifier#toString()}），换维度后用于过滤。 */
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

    private static HiveParasiteCommand fromCodec(UUID parasite, UUID owner, String modeName,
                                                 Optional<UUID> target, Optional<Long> guardPos,
                                                 String dimension, String routeId, int revision,
                                                 List<Long> legacyPoints, boolean closed) {
        HiveParasiteCommand command = new HiveParasiteCommand(parasite, owner);
        command.mode = HiveCommandMode.byName(modeName);
        command.target = target == null ? null : target.orElse(null);
        command.guardPos = guardPos == null || guardPos.isEmpty() ? null : BlockPos.of(guardPos.get());
        command.dimension = dimension == null ? "" : dimension;
        command.routeId = routeId == null ? "" : routeId;
        command.revision = revision;
        if (legacyPoints != null && !legacyPoints.isEmpty()) {
            List<BlockPos> points = new ArrayList<>(legacyPoints.size());
            for (long packed : legacyPoints) {
                if (points.size() >= HiveRoute.MAX_POINTS) {
                    break;
                }
                points.add(BlockPos.of(packed));
            }
            command.legacyRoute = points;
            command.legacyClosed = closed;
        }
        return command;
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

    private List<Long> legacyPacked() {
        if (legacyRoute == null || legacyRoute.isEmpty()) {
            return List.of();
        }
        List<Long> packed = new ArrayList<>(legacyRoute.size());
        for (BlockPos pos : legacyRoute) {
            packed.add(pos.asLong());
        }
        return packed;
    }

    // ------------------------------------------------------------------
    // Codec 取值的桥接（指令内部用可空引用，Codec 需要 Optional）
    // ------------------------------------------------------------------

    private Optional<UUID> targetOptional() {
        return Optional.ofNullable(target);
    }

    private Optional<Long> guardPosOptional() {
        return guardPos == null ? Optional.empty() : Optional.of(guardPos.asLong());
    }
}
