package org.tdddd.epca.impl.network.packet.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandManager;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * 客户端菜单 / 按键 -> 服务端的指挥操作包。
 *
 * <p>动作分两类：</p>
 * <ul>
 *     <li><b>寄生体指令</b>：{@code OPEN} 请求打开菜单；{@code SELECT} 切换选中；
 *     {@code CLEAR_SELECTION} 清空选中；{@code APPLY} 把模式/目标/绑定的路线下发给选中的寄生体；
 *     {@code CLEAR_COMMAND} 取消指令。</li>
 *     <li><b>路线库编辑</b>（服务端权威，改完立刻广播）：{@code ROUTE_CREATE} 新建路线并设为编辑中；
 *     {@code ROUTE_SELECT} 切换编辑中的路线；{@code ROUTE_RENAME} 重命名；{@code ROUTE_DELETE} 删除；
 *     {@code ROUTE_ADD_POINT} 追加节点（routeId 为空 = 当前编辑路线，没有就新建）；
 *     {@code ROUTE_REMOVE_LAST} / {@code ROUTE_CLEAR} / {@code ROUTE_TOGGLE_CLOSED} 编辑节点；
 *     {@code ROUTE_BIND_EDITING} 一键把当前编辑路线下达给选中的寄生体（巡逻）。</li>
 * </ul>
 *
 * <p>所有字段都在服务端重新校验：领巢者身份、距离、寄生体标签、目标是否友方、路线归属与节点数。</p>
 */
public final class HiveCommandActionPacket {

    public enum Action {
        OPEN,
        SELECT,
        CLEAR_SELECTION,
        APPLY,
        CLEAR_COMMAND,
        ROUTE_CREATE,
        ROUTE_SELECT,
        ROUTE_RENAME,
        ROUTE_DELETE,
        ROUTE_ADD_POINT,
        ROUTE_REMOVE_LAST,
        ROUTE_CLEAR,
        ROUTE_TOGGLE_CLOSED,
        ROUTE_BIND_EDITING
    }

    private final Action action;
    /** SELECT 的寄生体实体 id；APPLY 的攻击目标实体 id（-1 = 无目标）。 */
    private final int entityId;
    /** APPLY 的模式序号（{@code HiveCommandMode.ordinal()}）。 */
    private final int mode;
    /** 路线操作的目标路线 id；空字符串 = “当前正在编辑的路线”。 */
    private final String routeId;
    /** RENAME 的新名字。 */
    private final String routeName;
    /** ADD_POINT 的坐标；null = 由服务端回落到玩家脚下。 */
    @Nullable
    private final BlockPos pos;

    private HiveCommandActionPacket(Action action, int entityId, int mode, String routeId, String routeName,
                                    @Nullable BlockPos pos) {
        this.action = action;
        this.entityId = entityId;
        this.mode = mode;
        this.routeId = routeId == null ? "" : routeId;
        this.routeName = routeName == null ? "" : routeName;
        this.pos = pos;
    }

    public static HiveCommandActionPacket open() {
        return new HiveCommandActionPacket(Action.OPEN, -1, 0, "", "", null);
    }

    public static HiveCommandActionPacket select(int entityId) {
        return new HiveCommandActionPacket(Action.SELECT, entityId, 0, "", "", null);
    }

    public static HiveCommandActionPacket clearSelection() {
        return new HiveCommandActionPacket(Action.CLEAR_SELECTION, -1, 0, "", "", null);
    }

    public static HiveCommandActionPacket apply(int mode, int targetEntityId, String routeId) {
        return new HiveCommandActionPacket(Action.APPLY, targetEntityId, mode, routeId, "", null);
    }

    public static HiveCommandActionPacket clearCommand() {
        return new HiveCommandActionPacket(Action.CLEAR_COMMAND, -1, 0, "", "", null);
    }

    public static HiveCommandActionPacket routeCreate() {
        return new HiveCommandActionPacket(Action.ROUTE_CREATE, -1, 0, "", "", null);
    }

    public static HiveCommandActionPacket routeSelect(String routeId) {
        return new HiveCommandActionPacket(Action.ROUTE_SELECT, -1, 0, routeId, "", null);
    }

    public static HiveCommandActionPacket routeRename(String routeId, String name) {
        return new HiveCommandActionPacket(Action.ROUTE_RENAME, -1, 0, routeId, name, null);
    }

    public static HiveCommandActionPacket routeDelete(String routeId) {
        return new HiveCommandActionPacket(Action.ROUTE_DELETE, -1, 0, routeId, "", null);
    }

    /** routeId 传空字符串 = 加到“当前编辑路线”；没有编辑路线时服务端会自动新建一条。 */
    public static HiveCommandActionPacket routeAddPoint(String routeId, @Nullable BlockPos pos) {
        return new HiveCommandActionPacket(Action.ROUTE_ADD_POINT, -1, 0, routeId, "", pos);
    }

    public static HiveCommandActionPacket routeRemoveLast(String routeId) {
        return new HiveCommandActionPacket(Action.ROUTE_REMOVE_LAST, -1, 0, routeId, "", null);
    }

    public static HiveCommandActionPacket routeClear(String routeId) {
        return new HiveCommandActionPacket(Action.ROUTE_CLEAR, -1, 0, routeId, "", null);
    }

    public static HiveCommandActionPacket routeToggleClosed(String routeId) {
        return new HiveCommandActionPacket(Action.ROUTE_TOGGLE_CLOSED, -1, 0, routeId, "", null);
    }

    public static HiveCommandActionPacket routeBindEditing() {
        return new HiveCommandActionPacket(Action.ROUTE_BIND_EDITING, -1, 0, "", "", null);
    }

    public Action action() {
        return action;
    }

    public int entityId() {
        return entityId;
    }

    public int mode() {
        return mode;
    }

    public String routeId() {
        return routeId;
    }

    public String routeName() {
        return routeName;
    }

    @Nullable
    public BlockPos pos() {
        return pos;
    }

    public static void encode(HiveCommandActionPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action.ordinal());
        buf.writeInt(msg.entityId);
        buf.writeByte(msg.mode);
        buf.writeUtf(msg.routeId, 64);
        buf.writeUtf(msg.routeName, HiveRoute.MAX_NAME_LENGTH);
        buf.writeBoolean(msg.pos != null);
        if (msg.pos != null) {
            buf.writeBlockPos(msg.pos);
        }
    }

    public static HiveCommandActionPacket decode(FriendlyByteBuf buf) {
        Action[] values = Action.values();
        int actionId = buf.readByte();
        Action action = actionId >= 0 && actionId < values.length ? values[actionId] : Action.OPEN;
        int entityId = buf.readInt();
        int mode = buf.readByte();
        String routeId = buf.readUtf(64);
        String routeName = buf.readUtf(HiveRoute.MAX_NAME_LENGTH);
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new HiveCommandActionPacket(action, entityId, mode, routeId, routeName, pos);
    }

    public static void handle(HiveCommandActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer player = context.getSender();
            if (player != null) {
                HiveCommandManager.handleAction(player, msg);
            }
        });
        context.setPacketHandled(true);
    }
}
