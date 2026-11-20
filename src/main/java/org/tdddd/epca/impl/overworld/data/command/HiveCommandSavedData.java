package org.tdddd.epca.impl.overworld.data.command;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 寄生体指挥系统的世界存档：主世界 SavedData（数据名 {@code epca:hive_commands}，
 * 落盘文件 {@code data/epca/hive_commands.dat}，语义等价于 1.20.1 的 {@code epca_hive_commands}）。
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
 *
 * <p><b>26.1.2 差异</b>：{@code SavedData} 不再有 {@code save(CompoundTag)} / {@code load(CompoundTag)}，
 * 改为 {@link SavedDataType} + {@link Codec}，由 {@code SavedDataStorage.computeIfAbsent(type)} 读盘。
 * 字段名（Routes / Commands / Selections / Editing）与 1.20.1 的 NBT 键一一对应。</p>
 */
public final class HiveCommandSavedData extends SavedData {

    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("epca", "hive_commands");

    public static final Codec<HiveCommandSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HiveRoute.CODEC.listOf().optionalFieldOf("Routes", List.<HiveRoute>of())
                    .forGetter(HiveCommandSavedData::routeSnapshot),
            HiveParasiteCommand.CODEC.listOf().optionalFieldOf("Commands", List.<HiveParasiteCommand>of())
                    .forGetter(HiveCommandSavedData::commandSnapshot),
            SelectionEntry.CODEC.listOf().optionalFieldOf("Selections", List.<SelectionEntry>of())
                    .forGetter(HiveCommandSavedData::selectionSnapshot),
            EditingEntry.CODEC.listOf().optionalFieldOf("Editing", List.<EditingEntry>of())
                    .forGetter(HiveCommandSavedData::editingSnapshot)
    ).apply(instance, HiveCommandSavedData::create));

    public static final SavedDataType<HiveCommandSavedData> TYPE =
            new SavedDataType<>(DATA_ID, HiveCommandSavedData::new, CODEC);

    /** 一名领巢者的选中名单（存档条目）。 */
    public record SelectionEntry(UUID leader, List<UUID> selected) {
        public static final Codec<SelectionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("Leader").forGetter(SelectionEntry::leader),
                UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("Selected", List.<UUID>of())
                        .forGetter(SelectionEntry::selected)
        ).apply(instance, SelectionEntry::new));
    }

    /** 一名领巢者“正在编辑哪条路线”（存档条目）。 */
    public record EditingEntry(UUID leader, String routeId) {
        public static final Codec<EditingEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("Leader").forGetter(EditingEntry::leader),
                Codec.STRING.optionalFieldOf("RouteId", "").forGetter(EditingEntry::routeId)
        ).apply(instance, EditingEntry::new));
    }

    private final Map<UUID, HiveParasiteCommand> commands = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> selections = new LinkedHashMap<>();
    private final Map<String, HiveRoute> routes = new LinkedHashMap<>();
    private final Map<UUID, String> editing = new LinkedHashMap<>();

    public HiveCommandSavedData() {
    }

    private static HiveCommandSavedData create(List<HiveRoute> routeList, List<HiveParasiteCommand> commandList,
                                               List<SelectionEntry> selectionList,
                                               List<EditingEntry> editingList) {
        HiveCommandSavedData data = new HiveCommandSavedData();
        if (routeList != null) {
            for (HiveRoute route : routeList) {
                if (route != null && !route.id().isEmpty()) {
                    data.routes.put(route.id(), route);
                }
            }
        }
        if (commandList != null) {
            for (HiveParasiteCommand command : commandList) {
                if (command != null) {
                    data.commands.put(command.parasite(), command);
                }
            }
        }
        if (selectionList != null) {
            for (SelectionEntry entry : selectionList) {
                if (entry == null || entry.leader() == null || entry.selected() == null
                        || entry.selected().isEmpty()) {
                    continue;
                }
                data.selections.put(entry.leader(), new LinkedHashSet<>(entry.selected()));
            }
        }
        if (editingList != null) {
            for (EditingEntry entry : editingList) {
                if (entry == null || entry.leader() == null) {
                    continue;
                }
                String routeId = entry.routeId();
                if (routeId != null && !routeId.isEmpty()) {
                    data.editing.put(entry.leader(), routeId);
                }
            }
        }
        data.migrateLegacyRoutes();
        return data;
    }

    /**
     * 取世界存档。（{@code level} 可以是任意维度：内部一律转到主世界的数据存储，
     * 和 1.20.1 的 {@code server.overworld().getDataStorage()} 完全一致 ——
     * 绝不能按维度各存一份，否则换维度后指令/路线库就“消失”了。）
     */
    public static HiveCommandSavedData get(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            throw new IllegalStateException("HiveCommandSavedData is server-side only");
        }
        return get(server);
    }

    /** 主世界 SavedData（数据名 {@code epca:hive_commands}）。 */
    public static HiveCommandSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * 旧存档迁移：把“写在指令里的路线”搬进路线库，并让指令只保留路线 id。
     * 迁移后旧字段不再写回，反复加载也不会重复创建。
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

    // ------------------------------------------------------------------
    // Codec 取值快照
    // ------------------------------------------------------------------

    private List<HiveCommandSavedData.SelectionEntry> selectionSnapshot() {
        List<SelectionEntry> out = new ArrayList<>(selections.size());
        for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : selections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            out.add(new SelectionEntry(entry.getKey(), List.copyOf(entry.getValue())));
        }
        return out;
    }

    private List<HiveCommandSavedData.EditingEntry> editingSnapshot() {
        List<EditingEntry> out = new ArrayList<>(editing.size());
        for (Map.Entry<UUID, String> entry : editing.entrySet()) {
            if (entry.getValue().isEmpty() || !routes.containsKey(entry.getValue())) {
                continue;
            }
            out.add(new EditingEntry(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private List<HiveParasiteCommand> commandSnapshot() {
        return List.copyOf(commands.values());
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
