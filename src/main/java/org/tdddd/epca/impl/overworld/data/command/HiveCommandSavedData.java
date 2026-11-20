package org.tdddd.epca.impl.overworld.data.command;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 寄生体指挥系统的世界存档：主世界 SavedData。
 *
 * <p>保存四份数据：</p>
 * <ul>
 *     <li>{@code routes}：路线库：路线 id -> {@link HiveRoute}（节点 / 名称 / 首尾相连 / 维度）；</li>
 *     <li>{@code commands}：寄生体 UUID -> {@link HiveParasiteCommand}，
 *     即跟随/看守/巡逻/攻击目标/绑定的路线 id；</li>
 *     <li>{@code selections}：领巢者 UUID -> 选中的寄生体 UUID 列表（菜单里勾选的那批）；</li>
 *     <li>{@code editing}：领巢者 UUID -> 正在编辑的路线 id（按键加点时服务端据此决定加到哪条）。</li>
 * </ul>
 *
 * <p>服务器重启、玩家重登、换维度都不会丢；实体死亡/消失由 {@link HiveCommandManager}
 * 在服务端 tick 与离场事件里清理引用。</p>
 */
public final class HiveCommandSavedData extends SavedData {

    private static final String DATA_NAME = "epca_hive_commands";
    private static final String TAG_COMMANDS = "Commands";
    private static final String TAG_SELECTIONS = "Selections";
    private static final String TAG_ROUTES = "Routes";
    private static final String TAG_EDITING = "Editing";
    private static final String TAG_LEADER = "Leader";
    private static final String TAG_SELECTED = "Selected";
    private static final String TAG_ROUTE_ID = "RouteId";

    private final Map<UUID, HiveParasiteCommand> commands = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> selections = new LinkedHashMap<>();
    private final Map<String, HiveRoute> routes = new LinkedHashMap<>();
    private final Map<UUID, String> editing = new LinkedHashMap<>();

    public HiveCommandSavedData() {
    }

    public static HiveCommandSavedData get(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            throw new IllegalStateException("HiveCommandSavedData is server-side only");
        }
        return get(server);
    }

    public static HiveCommandSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                HiveCommandSavedData::load, HiveCommandSavedData::new, DATA_NAME);
    }

    public static HiveCommandSavedData load(CompoundTag tag) {
        HiveCommandSavedData data = new HiveCommandSavedData();
        ListTag routeList = tag.getList(TAG_ROUTES, Tag.TAG_COMPOUND);
        for (int i = 0; i < routeList.size(); i++) {
            HiveRoute route = HiveRoute.load(routeList.getCompound(i));
            if (route != null) {
                data.routes.put(route.id(), route);
            }
        }
        ListTag commandList = tag.getList(TAG_COMMANDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < commandList.size(); i++) {
            HiveParasiteCommand command = HiveParasiteCommand.load(commandList.getCompound(i));
            if (command != null) {
                data.commands.put(command.parasite(), command);
            }
        }
        ListTag selectionList = tag.getList(TAG_SELECTIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < selectionList.size(); i++) {
            CompoundTag entry = selectionList.getCompound(i);
            if (!entry.hasUUID(TAG_LEADER)) {
                continue;
            }
            UUID leader = entry.getUUID(TAG_LEADER);
            LinkedHashSet<UUID> selected = new LinkedHashSet<>();
            ListTag ids = entry.getList(TAG_SELECTED, Tag.TAG_STRING);
            for (int j = 0; j < ids.size(); j++) {
                try {
                    selected.add(UUID.fromString(ids.getString(j)));
                } catch (IllegalArgumentException ignored) {
                    // 坏数据直接跳过
                }
            }
            if (!selected.isEmpty()) {
                data.selections.put(leader, selected);
            }
        }
        ListTag editingList = tag.getList(TAG_EDITING, Tag.TAG_COMPOUND);
        for (int i = 0; i < editingList.size(); i++) {
            CompoundTag entry = editingList.getCompound(i);
            if (!entry.hasUUID(TAG_LEADER)) {
                continue;
            }
            String routeId = entry.getString(TAG_ROUTE_ID);
            if (!routeId.isEmpty()) {
                data.editing.put(entry.getUUID(TAG_LEADER), routeId);
            }
        }
        data.migrateLegacyRoutes();
        return data;
    }

    /**
     * 旧存档迁移：把“写在指令里的路线”搬进路线库，并让指令只保留路线 id。
     * 迁移后旧标签不再写回，反复加载也不会重复创建。
     */
    private void migrateLegacyRoutes() {
        boolean changed = false;
        for (HiveParasiteCommand command : commands.values()) {
            List<BlockPos> legacy = command.legacyRoute();
            if (legacy == null || legacy.isEmpty()) {
                command.clearLegacyRoute();
                continue;
            }
            String routeId = command.routeId();
            if (routeId.isEmpty() || !routes.containsKey(routeId)) {
                routeId = UUID.randomUUID().toString();
                HiveRoute route = HiveRoute.fromLegacy(routeId, legacy, command.legacyClosed(),
                        command.dimension());
                route.normalizeLoop();
                routes.put(routeId, route);
                command.setRouteId(routeId);
            }
            command.clearLegacyRoute();
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag routeList = new ListTag();
        for (HiveRoute route : routes.values()) {
            routeList.add(route.save());
        }
        tag.put(TAG_ROUTES, routeList);

        ListTag commandList = new ListTag();
        for (HiveParasiteCommand command : commands.values()) {
            commandList.add(command.save());
        }
        tag.put(TAG_COMMANDS, commandList);

        ListTag selectionList = new ListTag();
        for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : selections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag selectionTag = new CompoundTag();
            selectionTag.putUUID(TAG_LEADER, entry.getKey());
            ListTag ids = new ListTag();
            for (UUID parasite : entry.getValue()) {
                ids.add(StringTag.valueOf(parasite.toString()));
            }
            selectionTag.put(TAG_SELECTED, ids);
            selectionList.add(selectionTag);
        }
        tag.put(TAG_SELECTIONS, selectionList);

        ListTag editingList = new ListTag();
        for (Map.Entry<UUID, String> entry : editing.entrySet()) {
            if (entry.getValue().isEmpty() || !routes.containsKey(entry.getValue())) {
                continue;
            }
            CompoundTag editingTag = new CompoundTag();
            editingTag.putUUID(TAG_LEADER, entry.getKey());
            editingTag.putString(TAG_ROUTE_ID, entry.getValue());
            editingList.add(editingTag);
        }
        tag.put(TAG_EDITING, editingList);
        return tag;
    }

    // ------------------------------------------------------------------
    // 路线库
    // ------------------------------------------------------------------

    /** 服务端内部使用：按创建顺序排列的路线库。 */
    public Map<String, HiveRoute> routes() {
        return routes;
    }

    /** 按创建顺序快照一份（给同步包用，避免遍历时被并发改动）。 */
    public List<HiveRoute> routeSnapshot() {
        return new ArrayList<>(routes.values());
    }

    @Nullable
    public HiveRoute route(String routeId) {
        if (routeId == null || routeId.isEmpty()) {
            return null;
        }
        return routes.get(routeId);
    }

    public boolean hasRoute(String routeId) {
        return route(routeId) != null;
    }

    public void putRoute(HiveRoute route) {
        if (route != null) {
            routes.put(route.id(), route);
            setDirty();
        }
    }

    /** 删路线：同时摘掉所有指向它的指令与编辑指针（调用方负责同步/发提示）。 */
    public boolean removeRoute(String routeId) {
        if (routeId == null || routes.remove(routeId) == null) {
            return false;
        }
        editing.entrySet().removeIf(entry -> routeId.equals(entry.getValue()));
        for (HiveParasiteCommand command : commands.values()) {
            if (routeId.equals(command.routeId())) {
                command.setRouteId("");
                if (command.mode() == HiveCommandMode.PATROL) {
                    command.setMode(HiveCommandMode.FOLLOW);
                }
            }
        }
        setDirty();
        return true;
    }

    public int routeCount() {
        return routes.size();
    }

    // ------------------------------------------------------------------
    // 正在编辑的路线（每个领巢者一个指针）
    // ------------------------------------------------------------------

    @Nullable
    public String editingRouteId(UUID leader) {
        if (leader == null) {
            return null;
        }
        String routeId = editing.get(leader);
        return routeId == null || routeId.isEmpty() ? null : routeId;
    }

    public void setEditingRouteId(UUID leader, @Nullable String routeId) {
        if (leader == null) {
            return;
        }
        if (routeId == null || routeId.isEmpty()) {
            if (editing.remove(leader) != null) {
                setDirty();
            }
            return;
        }
        if (!routeId.equals(editing.get(leader))) {
            editing.put(leader, routeId);
            setDirty();
        }
    }

    /** 服务端内部使用：领巢者 -> 编辑中路线 id（同步包用）。 */
    public Map<UUID, String> editing() {
        return editing;
    }

    // ------------------------------------------------------------------
    // 指令
    // ------------------------------------------------------------------

    @Nullable
    public HiveParasiteCommand command(UUID parasite) {
        return commands.get(parasite);
    }

    public HiveParasiteCommand commandOrCreate(UUID parasite, UUID owner) {
        HiveParasiteCommand command = commands.get(parasite);
        if (command == null) {
            command = new HiveParasiteCommand(parasite, owner);
            commands.put(parasite, command);
            setDirty();
        }
        return command;
    }

    public void putCommand(HiveParasiteCommand command) {
        commands.put(command.parasite(), command);
        setDirty();
    }

    public void removeCommand(UUID parasite) {
        if (commands.remove(parasite) != null) {
            setDirty();
        }
    }

    /** 服务端内部使用：允许遍历时通过 {@link #removeCommand(UUID)} 删除。 */
    public Map<UUID, HiveParasiteCommand> commands() {
        return commands;
    }

    // ------------------------------------------------------------------
    // 选中名单
    // ------------------------------------------------------------------

    public LinkedHashSet<UUID> selection(UUID leader) {
        return selections.computeIfAbsent(leader, key -> new LinkedHashSet<>());
    }

    public boolean isSelected(UUID leader, UUID parasite) {
        LinkedHashSet<UUID> selected = selections.get(leader);
        return selected != null && selected.contains(parasite);
    }

    /**
     * 选中一只寄生体：同一只寄生体从其它领巢者的名单里摘掉（指挥权独占，
     * 避免两个领巢者同时看到两种颜色的线却都能下指令）。
     */
    public boolean addSelected(UUID leader, UUID parasite) {
        boolean changed = false;
        for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : selections.entrySet()) {
            if (!entry.getKey().equals(leader) && entry.getValue().remove(parasite)) {
                changed = true;
            }
        }
        boolean added = selection(leader).add(parasite);
        if (added) {
            changed = true;
        }
        if (changed) {
            setDirty();
        }
        return added;
    }

    public boolean removeSelected(UUID leader, UUID parasite) {
        LinkedHashSet<UUID> selected = selections.get(leader);
        if (selected == null) {
            return false;
        }
        boolean removed = selected.remove(parasite);
        if (selected.isEmpty()) {
            selections.remove(leader);
        }
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clearSelection(UUID leader) {
        if (selections.remove(leader) != null) {
            setDirty();
        }
    }

    @Nullable
    public UUID selectionOwner(UUID parasite) {
        for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : selections.entrySet()) {
            if (entry.getValue().contains(parasite)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeFromAllSelections(UUID parasite) {
        boolean changed = false;
        for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : selections.entrySet()) {
            if (entry.getValue().remove(parasite)) {
                changed = true;
            }
        }
        selections.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (changed) {
            setDirty();
        }
    }

    /** 服务端内部使用。 */
    public Map<UUID, LinkedHashSet<UUID>> selections() {
        return selections;
    }
}
