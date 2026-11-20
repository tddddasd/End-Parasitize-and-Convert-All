package org.tdddd.epca.impl.network.packet.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.epca.impl.client.command.HiveCommandClientData;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandHighlight;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务器 -> 所有领巢者的完整指挥状态同步包。
 *
 * <p>三部分：</p>
 * <ul>
 *     <li>{@code highlights}：每只被指挥寄生体的归属/模式/攻击目标/绑定路线 id；</li>
 *     <li>{@code routes}：<b>整个路线库</b>（路线 id、名称、节点、首尾相连、维度）。
 *     所有路线都会被画出来，客户端不再按寄生体猜节点；</li>
 *     <li>{@code editing}：每个领巢者“正在编辑哪条路线”，按键加点时服务端据此决定目标路线。</li>
 * </ul>
 *
 * <p>{@code openMenu} 只在响应菜单按键时置 true：客户端收到后更新数据并打开
 * {@code HiveCommandScreen}；其它广播不会强行弹界面。</p>
 *
 * <p>路线库是服务端权威数据：客户端只是把它画出来、列出来。编辑操作都先发 C2S，
 * 服务端改完立刻广播，因此“编辑中的路线”不会被同步包冲掉。</p>
 */
public final class SyncHiveCommandsPacket {

    private final List<HiveCommandHighlight> highlights;
    private final List<HiveRoute> routes;
    private final Map<UUID, String> editing;
    private final boolean openMenu;

    public SyncHiveCommandsPacket(List<HiveCommandHighlight> highlights, List<HiveRoute> routes,
                                  Map<UUID, String> editing, boolean openMenu) {
        this.highlights = highlights == null ? List.of() : List.copyOf(highlights);
        this.routes = routes == null ? List.of() : List.copyOf(routes);
        this.editing = editing == null ? Map.of() : new LinkedHashMap<>(editing);
        this.openMenu = openMenu;
    }

    public List<HiveCommandHighlight> highlights() {
        return highlights;
    }

    public List<HiveRoute> routes() {
        return routes;
    }

    public Map<UUID, String> editing() {
        return editing;
    }

    public boolean openMenu() {
        return openMenu;
    }

    public static void encode(SyncHiveCommandsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.highlights.size());
        for (HiveCommandHighlight highlight : msg.highlights) {
            buf.writeUUID(highlight.parasite());
            buf.writeInt(highlight.entityId());
            buf.writeUUID(highlight.owner());
            buf.writeByte(highlight.mode());
            buf.writeInt(highlight.targetId());
            buf.writeBoolean(highlight.selected());
            buf.writeUtf(highlight.routeId(), 64);
            buf.writeUtf(highlight.dimension(), 256);
        }

        buf.writeVarInt(msg.routes.size());
        for (HiveRoute route : msg.routes) {
            buf.writeUtf(route.id(), 64);
            buf.writeUtf(route.name(), HiveRoute.MAX_NAME_LENGTH);
            buf.writeUtf(route.dimension(), 256);
            buf.writeBoolean(route.closed());
            List<BlockPos> points = route.points();
            buf.writeVarInt(points.size());
            for (BlockPos pos : points) {
                buf.writeBlockPos(pos);
            }
        }

        buf.writeVarInt(msg.editing.size());
        for (Map.Entry<UUID, String> entry : msg.editing.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue(), 64);
        }

        buf.writeBoolean(msg.openMenu);
    }

    public static SyncHiveCommandsPacket decode(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 512);
        List<HiveCommandHighlight> highlights = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID parasite = buf.readUUID();
            int entityId = buf.readInt();
            UUID owner = buf.readUUID();
            int mode = buf.readByte();
            int targetId = buf.readInt();
            boolean selected = buf.readBoolean();
            String routeId = buf.readUtf(64);
            String dimension = buf.readUtf(256);
            highlights.add(new HiveCommandHighlight(parasite, entityId, owner, mode, targetId,
                    routeId, dimension, selected));
        }

        int routeCount = Math.min(buf.readVarInt(), 256);
        List<HiveRoute> routes = new ArrayList<>(routeCount);
        for (int i = 0; i < routeCount; i++) {
            String id = buf.readUtf(64);
            String name = buf.readUtf(HiveRoute.MAX_NAME_LENGTH);
            String dimension = buf.readUtf(256);
            boolean closed = buf.readBoolean();
            int pointCount = Math.min(buf.readVarInt(), HiveRoute.MAX_POINTS);
            HiveRoute route = new HiveRoute(id);
            route.setName(name);
            route.setDimension(dimension);
            List<BlockPos> points = new ArrayList<>(pointCount);
            for (int j = 0; j < pointCount; j++) {
                points.add(buf.readBlockPos());
            }
            route.restorePoints(points, closed);
            routes.add(route);
        }

        int editingCount = Math.min(buf.readVarInt(), 64);
        Map<UUID, String> editing = new LinkedHashMap<>();
        for (int i = 0; i < editingCount; i++) {
            UUID leader = buf.readUUID();
            String routeId = buf.readUtf(64);
            if (!routeId.isEmpty()) {
                editing.put(leader, routeId);
            }
        }

        boolean openMenu = buf.readBoolean();
        return new SyncHiveCommandsPacket(highlights, routes, editing, openMenu);
    }

    public static void handle(SyncHiveCommandsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            HiveCommandClientData.apply(msg.highlights, msg.routes, msg.editing, msg.openMenu);
        });
        context.setPacketHandled(true);
    }
}
