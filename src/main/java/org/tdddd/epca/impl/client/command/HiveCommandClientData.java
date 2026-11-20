package org.tdddd.epca.impl.client.command;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandHighlight;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端只读的指挥缓存：由 {@code SyncHiveCommandsPacket} 在主线程整表替换。
 * 渲染器与菜单都从这里取数据，不向服务端查询、不修改任何状态。
 *
 * <p>三份数据：</p>
 * <ul>
 *     <li>{@code ENTRIES}：每只被指挥寄生体的高亮（归属 / 模式 / 目标 / 绑定路线 id）；</li>
 *     <li>{@code ROUTES}：<b>服务端路线库的整份快照</b>，所有路线都画、都在菜单里列出来；</li>
 *     <li>{@code EDITING}：每个领巢者正在编辑哪条路线（渲染器用它把“编辑中”的路线画得更亮）。</li>
 * </ul>
 *
 * <p>路线库是服务端权威数据：客户端只是显示服务端刚广播的那一份，本地不做草稿，
 * 所以同步包永远只会带来<b>更新</b>，不会把玩家刚加的节点冲掉（加的节点在服务端，下一秒就回传）。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HiveCommandClientData {

    private static final List<HiveCommandHighlight> ENTRIES = new ArrayList<>();
    private static final List<HiveRoute> ROUTES = new ArrayList<>();
    private static final Map<UUID, String> EDITING = new LinkedHashMap<>();
    /** 服务端只会把指挥同步包发给领巢者：收到过就说明自己是领巢者（不依赖登录时序）。 */
    private static boolean receivedSync;

    private HiveCommandClientData() {
    }

    /** 主线程调用：整表替换；openMenu 为 true 时打开菜单。 */
    public static void apply(List<HiveCommandHighlight> highlights, List<HiveRoute> routes,
                             Map<UUID, String> editing, boolean openMenu) {
        ENTRIES.clear();
        if (highlights != null) {
            ENTRIES.addAll(highlights);
        }
        ROUTES.clear();
        if (routes != null) {
            ROUTES.addAll(routes);
        }
        EDITING.clear();
        if (editing != null) {
            EDITING.putAll(editing);
        }
        receivedSync = true;
        if (openMenu) {
            openScreen();
        }
    }

    public static boolean hasReceivedSync() {
        return receivedSync;
    }

    public static List<HiveCommandHighlight> entries() {
        return ENTRIES;
    }

    /** 服务端路线库快照（按创建顺序）。 */
    public static List<HiveRoute> routes() {
        return ROUTES;
    }

    @Nullable
    public static HiveRoute route(String routeId) {
        if (routeId == null || routeId.isEmpty()) {
            return null;
        }
        for (HiveRoute route : ROUTES) {
            if (routeId.equals(route.id())) {
                return route;
            }
        }
        return null;
    }

    /** 该领巢者正在编辑的路线 id；没有返回 null。 */
    @Nullable
    public static String editingRouteId(@Nullable UUID leader) {
        if (leader == null) {
            return null;
        }
        String routeId = EDITING.get(leader);
        return routeId == null || routeId.isEmpty() ? null : routeId;
    }

    @Nullable
    public static HiveRoute editingRoute(@Nullable UUID leader) {
        return route(editingRouteId(leader));
    }

    /** 该路线被几只寄生体绑定（菜单里显示“x 只”） */
    public static int bindCount(String routeId) {
        if (routeId == null || routeId.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (HiveCommandHighlight entry : ENTRIES) {
            if (routeId.equals(entry.routeId())) {
                count++;
            }
        }
        return count;
    }

    public static List<HiveCommandHighlight> entriesOwnedBy(UUID owner) {
        List<HiveCommandHighlight> out = new ArrayList<>();
        if (owner == null) {
            return out;
        }
        for (HiveCommandHighlight entry : ENTRIES) {
            if (owner.equals(entry.owner())) {
                out.add(entry);
            }
        }
        return out;
    }

    public static boolean isSelectedBy(UUID owner, UUID parasite) {
        if (owner == null || parasite == null) {
            return false;
        }
        for (HiveCommandHighlight entry : ENTRIES) {
            if (owner.equals(entry.owner()) && parasite.equals(entry.parasite()) && entry.selected()) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        ENTRIES.clear();
        ROUTES.clear();
        EDITING.clear();
        receivedSync = false;
    }

    public static void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof HiveCommandScreen) {
            return;
        }
        minecraft.setScreen(new HiveCommandScreen());
    }
}
