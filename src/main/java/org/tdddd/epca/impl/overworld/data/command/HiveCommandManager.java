package org.tdddd.epca.impl.overworld.data.command;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.c2s.HiveCommandActionPacket;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveCommandsPacket;
import org.tdddd.epca.impl.overworld.data.HiveLeaderSavedData;
import org.tdddd.epca.impl.overworld.registry.entities.ai.HiveCommandGoalInjector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 寄生体指挥系统的服务端权威入口（参考附属实现 NestCommandManager 的状态机思路，按 epca 架构重写）。
 *
 * <p>职责划分：</p>
 * <ul>
 *     <li>校验领巢者身份、选中寄生体、下达/取消指令；</li>
 *     <li>维护<b>路线库</b>：新建/选中/重命名/删除路线，追加/删除末点/清空节点，首尾相连开关。
 *     路线库是独立于寄生体的文档数据，一条路线可以被多只寄生体共用；</li>
 *     <li>把指令写进 {@link HiveCommandSavedData}（存档持久化），并给实体打上
 *     “必须保留”标记，防止自然刷没；</li>
 *     <li>把“所有领巢者的选中/指令/目标/绑定路线 + 整个路线库 + 每人编辑中的路线”
 *     打包成 {@link SyncHiveCommandsPacket} 广播给<b>所有在线领巢者</b>；</li>
 *     <li>服务端 tick 兜底清理：主人不再是领巢者、寄生体死亡/被移除、目标死亡、
 *     绑定的路线被删或不足两个点，都在这里收拾。</li>
 * </ul>
 *
 * <p>客户端所有写操作都走 C2S {@link HiveCommandActionPacket}：服务端重新校验目标、距离、
 * 路线归属与节点数，改包无法凭空指挥；菜单与按键看到的路线库永远是服务端记录的那一份。</p>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HiveCommandManager {

    /** 菜单“附近列表”能选中/下达指令的最大距离（格）。 */
    public static final double SELECT_RADIUS = 64.0;
    /** 攻击目标允许选择的最大距离（格）。 */
    public static final double TARGET_RADIUS = 128.0;
    /** 路线节点允许距离下达者的最大距离（格）。 */
    public static final double ROUTE_RADIUS = 256.0;
    /** 一个领巢者同时最多选中多少只寄生体。 */
    public static final int MAX_SELECTED = 24;
    /** 路线库最多保存多少条路线。 */
    public static final int MAX_ROUTES = 32;
    /** 高亮同步周期（tick）：每秒重发一次，实体 id 在区块重载后也能自动纠正。 */
    private static final int SYNC_INTERVAL = 20;

    private static int tickCounter;

    private HiveCommandManager() {
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /** 这只寄生体当前的指令；没有返回 null。Goal 每拍调用。 */
    @Nullable
    public static HiveParasiteCommand commandOf(Mob mob) {
        if (mob == null || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        return HiveCommandSavedData.get(level).command(mob.getUUID());
    }

    /** 指令绑定的路线；没绑定或路线已被删除时返回 null。Goal / 渲染都靠它取节点。 */
    @Nullable
    public static HiveRoute routeOf(@Nullable Mob mob, @Nullable HiveParasiteCommand command) {
        if (mob == null || command == null || !command.hasRoute()) {
            return null;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        return HiveCommandSavedData.get(server).route(command.routeId());
    }

    public static boolean isParasite(Entity entity) {
        return entity instanceof Mob && ParasiteEvents.isParasiteEntity(entity);
    }

    // ------------------------------------------------------------------
    // C2S 入口
    // ------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, HiveCommandActionPacket msg) {
        if (player == null || msg == null || msg.action() == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!HiveLeaderSavedData.get(server.overworld()).isLeader(player.getUUID())) {
            message(player, "epca.hive_command.msg.only_leader");
            return;
        }
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        switch (msg.action()) {
            case OPEN -> openMenu(player, server, data);
            case SELECT -> select(player, msg.entityId());
            case CLEAR_SELECTION -> clearSelection(player);
            case APPLY -> apply(player, data, msg.mode(), msg.entityId(), msg.routeId());
            case CLEAR_COMMAND -> clearCommands(player);
            case ROUTE_CREATE -> routeCreate(player, data);
            case ROUTE_SELECT -> routeSelect(player, data, msg.routeId());
            case ROUTE_RENAME -> routeRename(player, data, msg.routeId(), msg.routeName());
            case ROUTE_DELETE -> routeDelete(player, data, msg.routeId());
            case ROUTE_ADD_POINT -> routeAddPoint(player, data, msg.routeId(), msg.pos());
            case ROUTE_REMOVE_LAST -> routeRemoveLast(player, data, msg.routeId());
            case ROUTE_CLEAR -> routeClear(player, data, msg.routeId());
            case ROUTE_TOGGLE_CLOSED -> routeToggleClosed(player, data, msg.routeId());
            case ROUTE_BIND_EDITING -> bindEditingRoute(player, data);
        }
    }

    private static void openMenu(ServerPlayer player, MinecraftServer server, HiveCommandSavedData data) {
        ModNetwork.sendToPlayer(player, new SyncHiveCommandsPacket(
                buildHighlights(server, data), data.routeSnapshot(), data.editing(), true));
    }

    private static void select(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof Mob mob) || !mob.isAlive() || !ParasiteEvents.isParasiteEntity(mob)) {
            return;
        }
        if (player.distanceTo(mob) > SELECT_RADIUS) {
            message(player, "epca.hive_command.msg.too_far");
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HiveCommandGoalInjector.ensure(mob);
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        UUID leader = player.getUUID();
        UUID parasite = mob.getUUID();
        if (data.isSelected(leader, parasite)) {
            data.removeSelected(leader, parasite);
        } else if (data.selection(leader).size() >= MAX_SELECTED) {
            message(player, "epca.hive_command.msg.selection_full");
            return;
        } else {
            data.addSelected(leader, parasite);
            // 指挥权独占：别的领巢者已经指挥着这只寄生体时，选中即接管，
            // 否则高亮归属会和菜单里的“已选”列表对不上。
            HiveParasiteCommand existing = data.command(parasite);
            if (existing != null && !leader.equals(existing.owner())) {
                existing.setOwner(leader);
                data.putCommand(existing);
            }
        }
        data.setDirty();
        broadcast(server);
    }

    private static void clearSelection(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        data.clearSelection(player.getUUID());
        broadcast(server);
    }

    // ------------------------------------------------------------------
    // 路线库编辑（服务端权威）
    // ------------------------------------------------------------------

    /** 取“这次操作要改哪条路线”：优先用包里的 id，其次用该领巢者编辑中的路线。 */
    @Nullable
    private static HiveRoute resolveRoute(HiveCommandSavedData data, ServerPlayer player, String routeId) {
        if (routeId != null && !routeId.isEmpty()) {
            HiveRoute route = data.route(routeId);
            if (route != null) {
                return route;
            }
        }
        String editingId = data.editingRouteId(player.getUUID());
        return editingId == null ? null : data.route(editingId);
    }

    private static String dimensionOf(ServerPlayer player) {
        return player.level().dimension().location().toString();
    }

    private static void routeCreate(ServerPlayer player, HiveCommandSavedData data) {
        if (data.routeCount() >= MAX_ROUTES) {
            message(player, "epca.hive_command.msg.route_full", MAX_ROUTES);
            return;
        }
        HiveRoute route = new HiveRoute(UUID.randomUUID().toString());
        route.setDimension(dimensionOf(player));
        data.putRoute(route);
        data.setEditingRouteId(player.getUUID(), route.id());
        message(player, "epca.hive_command.msg.route_created");
        broadcast(player.getServer());
    }

    private static void routeSelect(ServerPlayer player, HiveCommandSavedData data, String routeId) {
        HiveRoute route = data.route(routeId);
        if (route == null) {
            message(player, "epca.hive_command.msg.route_missing");
            return;
        }
        data.setEditingRouteId(player.getUUID(), route.id());
        broadcast(player.getServer());
    }

    private static void routeRename(ServerPlayer player, HiveCommandSavedData data, String routeId, String name) {
        HiveRoute route = data.route(routeId);
        if (route == null) {
            message(player, "epca.hive_command.msg.route_missing");
            return;
        }
        route.setName(name);
        data.setDirty();
        message(player, "epca.hive_command.msg.route_renamed", route.name());
        broadcast(player.getServer());
    }

    private static void routeDelete(ServerPlayer player, HiveCommandSavedData data, String routeId) {
        HiveRoute route = data.route(routeId);
        if (route == null) {
            message(player, "epca.hive_command.msg.route_missing");
            return;
        }
        if (!data.removeRoute(routeId)) {
            return;
        }
        if (data.editingRouteId(player.getUUID()) == null) {
            String next = data.routes().keySet().stream().findFirst().orElse(null);
            data.setEditingRouteId(player.getUUID(), next);
        }
        message(player, "epca.hive_command.msg.route_deleted");
        broadcast(player.getServer());
    }

    /**
     * 追加节点：按键与菜单通用。routeId 为空时加到“当前编辑路线”，
     * 没有编辑路线就自动新建一条并设为编辑中，因此第一次按键也能直接落第一个点。
     */
    private static void routeAddPoint(ServerPlayer player, HiveCommandSavedData data, String routeId,
                                      @Nullable BlockPos rawPos) {
        HiveRoute route = resolveRoute(data, player, routeId);
        if (route == null) {
            if (data.routeCount() >= MAX_ROUTES) {
                message(player, "epca.hive_command.msg.route_full", MAX_ROUTES);
                return;
            }
            route = new HiveRoute(UUID.randomUUID().toString());
            route.setDimension(dimensionOf(player));
            data.putRoute(route);
        }
        data.setEditingRouteId(player.getUUID(), route.id());

        // 服务端兜底：没给坐标就用玩家脚下；超距/跨维度一律拒绝。
        BlockPos pos = rawPos == null ? player.blockPosition() : rawPos.immutable();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > ROUTE_RADIUS * ROUTE_RADIUS) {
            message(player, "epca.hive_command.msg.too_far");
            return;
        }
        String dimension = dimensionOf(player);
        if (route.dimension().isEmpty()) {
            route.setDimension(dimension);
        } else if (!route.dimension().equals(dimension)) {
            message(player, "epca.hive_command.msg.route_wrong_dimension");
            return;
        }

        HiveRoute.AddResult result = route.addPoint(pos);
        switch (result) {
            case ADDED -> message(player, "epca.hive_command.msg.route_point_added",
                    route.size(), pos.getX(), pos.getY(), pos.getZ());
            case TOO_CLOSE -> message(player, "epca.hive_command.msg.route_point_too_close",
                    route.distanceToLast(pos));
            case FULL -> message(player, "epca.hive_command.msg.route_point_full", HiveRoute.MAX_POINTS);
            case CLOSED_LOOP -> message(player, "epca.hive_command.msg.route_closed_loop");
        }
        data.setDirty();
        broadcast(player.getServer());
    }

    private static void routeRemoveLast(ServerPlayer player, HiveCommandSavedData data, String routeId) {
        HiveRoute route = resolveRoute(data, player, routeId);
        if (route == null) {
            return;
        }
        if (route.removeLastPoint()) {
            data.setDirty();
            message(player, "epca.hive_command.msg.route_point_removed", route.size());
            broadcast(player.getServer());
        }
    }

    private static void routeClear(ServerPlayer player, HiveCommandSavedData data, String routeId) {
        HiveRoute route = resolveRoute(data, player, routeId);
        if (route == null) {
            return;
        }
        route.clearPoints();
        data.setDirty();
        message(player, "epca.hive_command.msg.route_cleared");
        broadcast(player.getServer());
    }

    private static void routeToggleClosed(ServerPlayer player, HiveCommandSavedData data, String routeId) {
        HiveRoute route = resolveRoute(data, player, routeId);
        if (route == null) {
            return;
        }
        if (route.size() < 3) {
            message(player, "epca.hive_command.msg.route_too_short");
            return;
        }
        route.setClosed(!route.closed());
        data.setDirty();
        message(player, route.closed() ? "epca.hive_command.msg.route_closed_on"
                : "epca.hive_command.msg.route_closed_off");
        broadcast(player.getServer());
    }

    // ------------------------------------------------------------------
    // 下达指令
    // ------------------------------------------------------------------

    /** 菜单「下达指令」：模式 + 攻击目标 + 选中的路线。 */
    private static void apply(ServerPlayer player, HiveCommandSavedData data, int modeId, int targetEntityId,
                              String routeId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (data.selection(player.getUUID()).isEmpty()) {
            message(player, "epca.hive_command.msg.select_first");
            return;
        }
        HiveCommandMode mode = HiveCommandMode.byId(modeId);

        UUID targetUuid = null;
        if (targetEntityId != HiveCommandHighlight.NO_ENTITY) {
            Entity picked = player.level().getEntity(targetEntityId);
            if (picked instanceof LivingEntity living && living.isAlive()
                    && !ParasiteEvents.isParasiteByTagOrInterface(living)
                    && !(living instanceof net.minecraft.world.entity.player.Player targetPlayer
                        && (targetPlayer.isCreative() || targetPlayer.isSpectator()))
                    && !(living instanceof net.minecraft.world.entity.monster.Creeper)
                    && !(living instanceof net.minecraft.world.entity.decoration.ArmorStand)
                    && player.distanceTo(living) <= TARGET_RADIUS) {
                targetUuid = living.getUUID();
            } else if (picked != null) {
                message(player, "epca.hive_command.msg.invalid_target");
            }
        }

        HiveRoute route = null;
        if (mode == HiveCommandMode.PATROL) {
            route = resolveRoute(data, player, routeId);
            if (route == null) {
                message(player, "epca.hive_command.msg.route_missing");
                return;
            }
        }
        applyOrder(player, data, mode, targetUuid, false, route);
    }

    /** 按键「结束并下达」：当前编辑路线 -> 选中的寄生体，模式固定巡逻，保留各自原有攻击目标。 */
    private static void bindEditingRoute(ServerPlayer player, HiveCommandSavedData data) {
        String editingId = data.editingRouteId(player.getUUID());
        HiveRoute route = editingId == null ? null : data.route(editingId);
        if (route == null) {
            message(player, "epca.hive_command.msg.route_missing");
            return;
        }
        if (data.selection(player.getUUID()).isEmpty()) {
            message(player, "epca.hive_command.msg.select_first");
            return;
        }
        applyOrder(player, data, HiveCommandMode.PATROL, null, true, route);
    }

    private static void applyOrder(ServerPlayer player, HiveCommandSavedData data, HiveCommandMode mode,
                                   @Nullable UUID targetUuid, boolean keepTarget, @Nullable HiveRoute route) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        LinkedHashSet<UUID> selected = data.selection(player.getUUID());
        if (selected.isEmpty()) {
            message(player, "epca.hive_command.msg.select_first");
            return;
        }
        if (mode == HiveCommandMode.PATROL) {
            if (route == null) {
                message(player, "epca.hive_command.msg.route_missing");
                return;
            }
            // 首尾相连判定不变：末点与首点重合就当闭合，并把重复的末点去掉。
            route.normalizeLoop();
            if (route.size() < 2) {
                message(player, "epca.hive_command.msg.route_too_short");
                return;
            }
            data.setDirty();
        }

        int applied = 0;
        for (UUID parasite : new ArrayList<>(selected)) {
            Entity entity = findEntity(server, parasite);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || !ParasiteEvents.isParasiteEntity(mob)) {
                continue;
            }
            if (mode != HiveCommandMode.FOLLOW && mob.level() != player.level()) {
                // 看守/巡逻的坐标是玩家所在维度的，跨维度的成员跳过（跟随允许跨维度追主人）
                continue;
            }
            HiveCommandGoalInjector.ensure(mob);
            HiveParasiteCommand command = data.commandOrCreate(parasite, player.getUUID());
            command.setOwner(player.getUUID());
            command.setMode(mode);
            if (!keepTarget) {
                command.setTarget(targetUuid);
            }
            command.setRouteId(mode == HiveCommandMode.PATROL ? route.id() : "");
            command.setDimension(mob.level().dimension().location().toString());
            if (mode == HiveCommandMode.GUARD) {
                command.setGuardPos(mob.blockPosition());
            }
            command.touch();
            data.putCommand(command);
            mob.setPersistenceRequired();
            mob.getNavigation().stop();
            mob.setTarget(null);
            clearForcedTarget(mob);
            applied++;
        }
        if (applied == 0) {
            message(player, "epca.hive_command.msg.nothing_applied");
            return;
        }
        message(player, mode == HiveCommandMode.PATROL
                ? "epca.hive_command.msg.applied_patrol" : "epca.hive_command.msg.applied");
        broadcast(server);
    }

    private static void clearCommands(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        LinkedHashSet<UUID> selected = data.selection(player.getUUID());
        if (selected.isEmpty()) {
            message(player, "epca.hive_command.msg.select_first");
            return;
        }
        for (UUID parasite : new ArrayList<>(selected)) {
            data.removeCommand(parasite);
            Entity entity = findEntity(server, parasite);
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
                clearForcedTarget(mob);
            }
        }
        broadcast(server);
    }

    /** 巡逻索敌越界时由 Goal 调用：清掉显式目标并立刻同步给所有领巢者。 */
    public static void clearTarget(Mob mob, HiveParasiteCommand command) {
        if (mob == null || command == null || command.target() == null) {
            return;
        }
        command.setTarget(null);
        command.touch();
        MinecraftServer server = mob.getServer();
        if (server != null) {
            HiveCommandSavedData.get(server).setDirty();
            broadcast(server);
        }
    }

    private static void clearForcedTarget(Mob mob) {
        mob.getPersistentData().remove(ParasiteEvents.FORCED_TARGET_UUID_KEY);
        mob.getPersistentData().remove(ParasiteEvents.LAST_FORCED_SWITCH_TICK_KEY);
    }

    private static void message(ServerPlayer player, String key, Object... args) {
        Component text = args.length == 0 ? Component.translatable(key) : Component.translatable(key, args);
        player.displayClientMessage(text, true);
    }

    // ------------------------------------------------------------------
    // 同步
    // ------------------------------------------------------------------

    /** 给所有在线领巢者广播一份完整状态（只发给领巢者，普通玩家收不到）。 */
    public static int broadcast(MinecraftServer server) {
        if (server == null) {
            return 0;
        }
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        List<HiveCommandHighlight> highlights = buildHighlights(server, data);
        SyncHiveCommandsPacket packet = new SyncHiveCommandsPacket(highlights, data.routeSnapshot(),
                data.editing(), false);
        int sent = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isLeader(server, player.getUUID())) {
                ModNetwork.sendToPlayer(player, packet);
                sent++;
            }
        }
        return sent;
    }

    /** 直接读主世界 SavedData，避免依赖 ServerLifecycleHooks 的当前服务器时序。 */
    private static boolean isLeader(MinecraftServer server, UUID uuid) {
        return HiveLeaderSavedData.get(server.overworld()).isLeader(uuid);
    }

    private static List<HiveCommandHighlight> buildHighlights(MinecraftServer server, HiveCommandSavedData data) {
        Map<UUID, HiveCommandHighlight> out = new LinkedHashMap<>();

        for (HiveParasiteCommand command : data.commands().values()) {
            Entity parasite = findEntity(server, command.parasite());
            int parasiteId = parasite == null ? HiveCommandHighlight.NO_ENTITY : parasite.getId();
            int targetId = HiveCommandHighlight.NO_ENTITY;
            if (command.target() != null) {
                Entity target = findEntity(server, command.target());
                // 实体 id 只在同一维度内有意义：目标在别的维度时绝不能把 id 发给客户端，
                // 否则客户端可能在本维度用同一个 id 套到无关实体上画红线。
                if (target != null && command.dimension().equals(
                        target.level().dimension().location().toString())) {
                    targetId = target.getId();
                }
            }
            String dimension = parasite == null ? command.dimension()
                    : parasite.level().dimension().location().toString();
            out.put(command.parasite(), new HiveCommandHighlight(
                    command.parasite(), parasiteId, command.owner(), command.mode().ordinal(),
                    targetId, command.routeId(), dimension,
                    data.isSelected(command.owner(), command.parasite())));
        }

        for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : data.selections().entrySet()) {
            UUID leader = entry.getKey();
            for (UUID parasite : entry.getValue()) {
                if (out.containsKey(parasite)) {
                    continue;
                }
                Entity entity = findEntity(server, parasite);
                int parasiteId = entity == null ? HiveCommandHighlight.NO_ENTITY : entity.getId();
                String dimension = entity == null ? "" : entity.level().dimension().location().toString();
                out.put(parasite, new HiveCommandHighlight(parasite, parasiteId, leader,
                        HiveCommandHighlight.MODE_NONE, HiveCommandHighlight.NO_ENTITY,
                        "", dimension, true));
            }
        }
        return new ArrayList<>(out.values());
    }

    @Nullable
    public static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 服务端 tick / 清理
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % SYNC_INTERVAL != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        boolean changed = prune(server, data);
        if (changed) {
            data.setDirty();
        }
        // 周期重发：实体 id 在区块重载后会变，客户端只有 id，必须定期对齐；
        // 同时它也顺带覆盖了“指令刚改过”的即时广播。
        broadcast(server);
    }

    private static boolean prune(MinecraftServer server, HiveCommandSavedData data) {
        boolean changed = false;
        Iterator<Map.Entry<UUID, HiveParasiteCommand>> commands = data.commands().entrySet().iterator();
        while (commands.hasNext()) {
            HiveParasiteCommand command = commands.next().getValue();
            if (!isLeader(server, command.owner())) {
                commands.remove();
                changed = true;
                continue;
            }
            Entity parasite = findEntity(server, command.parasite());
            if (parasite != null) {
                if (!(parasite instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()) {
                    commands.remove();
                    changed = true;
                    continue;
                }
                String dimension = mob.level().dimension().location().toString();
                if (!dimension.equals(command.dimension())) {
                    // 跟随跨维度传送后，指令的维度标记跟着实体走；看守点重新落在脚下
                    command.setDimension(dimension);
                    if (command.mode() == HiveCommandMode.GUARD) {
                        command.setGuardPos(mob.blockPosition());
                    }
                    changed = true;
                }
            }
            if (command.target() != null) {
                Entity target = findEntity(server, command.target());
                if (target != null && (!(target instanceof LivingEntity living) || !living.isAlive()
                        || ParasiteEvents.isParasiteByTagOrInterface(living))) {
                    command.setTarget(null);
                    changed = true;
                }
            }
            // 绑定的路线被删掉 / 节点不足 2 个：退回跟随，别让寄生虫傻站着
            if (command.hasRoute()) {
                HiveRoute route = data.route(command.routeId());
                if (route == null || route.size() < 2) {
                    command.setRouteId("");
                    if (command.mode() == HiveCommandMode.PATROL) {
                        command.setMode(HiveCommandMode.FOLLOW);
                    }
                    changed = true;
                }
            } else if (command.mode() == HiveCommandMode.PATROL) {
                command.setMode(HiveCommandMode.FOLLOW);
                changed = true;
            }
        }

        Iterator<Map.Entry<UUID, LinkedHashSet<UUID>>> selections = data.selections().entrySet().iterator();
        while (selections.hasNext()) {
            Map.Entry<UUID, LinkedHashSet<UUID>> entry = selections.next();
            if (!isLeader(server, entry.getKey())) {
                selections.remove();
                changed = true;
                continue;
            }
            entry.getValue().removeIf(uuid -> {
                Entity entity = findEntity(server, uuid);
                return entity != null && (!(entity instanceof Mob mob) || !mob.isAlive() || mob.isRemoved());
            });
            if (entry.getValue().isEmpty()) {
                selections.remove();
                changed = true;
            }
        }
        return changed;
    }

    /** 实体真正消失（死亡/被移除）时清引用；区块卸载不触发清理，读回来还能继续指挥。 */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.getEntity().isRemoved()) {
            return;
        }
        // 跨维度传送时实体的 level 已经换成新维度，旧维度的离场事件不能当成“消失”，
        // 否则跟随跨维度会立刻把路线/目标清掉。真正的死亡/移除由 LivingDeathEvent 与 tick 兜底处理。
        if (event.getEntity().level() != event.getLevel()) {
            return;
        }
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }
        dropEntity(server, event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }
        dropEntity(server, event.getEntity().getUUID());
    }

    private static void dropEntity(MinecraftServer server, UUID uuid) {
        HiveCommandSavedData data = HiveCommandSavedData.get(server);
        boolean changed = false;
        if (data.command(uuid) != null) {
            data.removeCommand(uuid);
            changed = true;
        }
        UUID owner = data.selectionOwner(uuid);
        if (owner != null) {
            data.removeFromAllSelections(uuid);
            changed = true;
        }
        // 被当作攻击目标死掉：清掉所有指向它的目标引用
        for (HiveParasiteCommand command : data.commands().values()) {
            if (uuid.equals(command.target())) {
                command.setTarget(null);
                changed = true;
            }
        }
        if (changed) {
            data.setDirty();
            broadcast(server);
        }
    }
}
