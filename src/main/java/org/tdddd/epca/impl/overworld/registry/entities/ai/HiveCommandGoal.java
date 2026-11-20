package org.tdddd.epca.impl.overworld.registry.entities.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.events.ParasiteEvents;
import org.tdddd.epca.impl.overworld.data.command.HiveParasiteCommand;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandManager;
import org.tdddd.epca.impl.overworld.data.command.HiveCommandMode;
import org.tdddd.epca.impl.overworld.data.command.HiveRoute;
import org.tdddd.epca.impl.overworld.data.command.HiveRouteMath;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 寄生体指挥 Goal（参考附属实现 NestFollowGoal / NestScepterCommandGoal 的算法重写）。
 *
 * <p>占用 {@link Goal.Flag#MOVE} 与 {@link Goal.Flag#LOOK}，优先级 -1：有指令时压住原版的
 * 游荡/近战追击/跟随 Goal，也压住原版的随机东张西望（{@code RandomLookAroundGoal} /
 * {@code LookAtPlayerGoal}），避免多个 Goal 抢 {@code LookControl} 把脑袋拧成风车；
 * 没指令时 {@code canUse()} 返回 false，原版 AI 完全不受影响。</p>
 *
 * <p>三种模式：</p>
 * <ul>
 *     <li><b>跟随</b>：朝领巢者走，3 格内停住（不挤脸、不挡路）；主人跨维度/太远时传送到附近；
 *     有显式攻击目标时优先追击目标。寻路速度固定 1.0（= 寄生体自身速度），
 *     不调用 {@code setSprinting}、不修改移动速度属性，绝不加速。</li>
 *     <li><b>看守</b>：站在指令下达时的位置，不移动；每拍扫描 16 格内的敌人，进入攻击距离就
 *     原地挥击，目标跑出范围就放弃，绝不追击。被击退超过 1.5 格会走回站位。</li>
 *     <li><b>巡逻</b>：沿路线点走，两点时来回、大于两点且首尾相连时循环；允许离开路线
 *     ±8 格索敌，目标或自己越界就 {@code setTarget(null)} 并回到最近的路线点继续巡逻。</li>
 * </ul>
 *
 * <h2>为什么之前会“原地快速打转”（本类现在的防打转设计）</h2>
 * <p>反编译 1.20.1 的 {@code MoveControl.tick()} 可以确认：{@code MOVE_TO} 分支里直接
 * {@code mob.setYRot(rotlerp(yRot, atan2(wantedZ - z, wantedX - x) * 180/π - 90, 90))}，
 * 也就是<b>每收到一次 {@code setWantedPosition} 就把身体朝向朝目标点瞬写一次</b>（最多 90°/拍），
 * 同时 {@code Mob.setSpeed} 会顺带把 {@code zza}（前进档）拉起来；而 {@code WAIT} 分支每拍把
 * {@code zza} 清 0。旧实现有两个致命写法：</p>
 * <ol>
 *     <li>巡逻用 {@code mob.getNavigation().isDone()} 当重算条件 —— 只要当前没有路径（寻路失败、
 *     路径刚走完、被卡住），<b>每拍</b>都会 {@code nav.stop()} + 重新 A*。新建路径的首个节点中心
 *     在身体两侧来回跳，于是朝向每拍被写向相反方向 → 原地高速打转，而且路径还没走就被丢掉 → 不前进。</li>
 *     <li>寻路失败时回退到 {@code moveControl.setWantedPosition(...)}：这条回退路径<b>跟寻路上的
 *     节点目标同时存在</b>，两个不同的目标点每拍交替下发，同样让朝向来回翻。</li>
 * </ol>
 * <p>现在的规则：</p>
 * <ul>
 *     <li><b>死区</b>：目标点离身体中心 ≤ {@link #MOVE_DEAD_ZONE} 格时不下发任何移动指令，
 *     并把 {@code MoveControl} 的目标点写成自己脚下（{@code g < 2.5e-7} 直接走 {@code setZza(0)}），
 *     从根上避免“对着自己脚下的点算 atan2”。</li>
 *     <li><b>节流</b>：路径重算间隔 ≥ {@link #REPATH_INTERVAL} 拍；目标点没挪动超过
 *     {@link #REPATH_TARGET_EPS_SQR} 格² 时完全不重算；正在沿路径走时连 {@code moveTo} 都不调，
 *     也不在 {@code moveTo} 前 {@code stop()}。</li>
 *     <li><b>直推兜底</b>：只有寻路确实给不出路时才切到“直推”，且目标点固定，
 *     朝向因此稳定（{@code MoveControl} 每拍清 {@code zza}，所以必须每拍重发，但方向不变）。</li>
 *     <li><b>卡住看门狗</b>：连续 {@link #STUCK_TICKS} 拍没有实际位移就停掉移动输入并冷却
 *     {@link #STUCK_RETRY} 拍 —— 宁可站着，也不顶着墙每拍扭一次身子。</li>
 *     <li><b>朝向每拍最多一次</b>，且只朝“稳定的目标”（实体或固定站位）写一次
 *     {@code LookControl}；没有目标时不写，头部自然回到身体朝向。</li>
 * </ul>
 *
 * <p>服务端权威：Goal 每拍从 SavedData 读指令，不信任任何客户端数据。</p>
 */
public final class HiveCommandGoal extends Goal {

    /** 跟随的停候距离：到这个距离内就收队，避免贴脸挤人。 */
    private static final double FOLLOW_STOP = 3.0D;
    /** 跟随超过这个距离直接传送到主人附近（跨维度也一样）。 */
    private static final double FOLLOW_TELEPORT = 32.0D;
    /** 传送冷却（tick），防止落点不好时反复瞬移。 */
    private static final int TELEPORT_COOLDOWN = 40;
    /** 看守的索敌半径（格）：只扫这个范围，不追出去。 */
    private static final double GUARD_RADIUS = 16.0D;
    /** 看守站位偏移超过这个距离才走回去（被击退/被推）。 */
    private static final double GUARD_RETURN_DISTANCE = 1.5D;
    /** 巡逻节点到达判定（格）：进到这个范围就直接推进到下一个节点。 */
    private static final double WAYPOINT_REACH = 0.8D;
    /** 巡逻卡在同一个节点超过这么多拍，就直接跳过它（节点在墙里/够不着时别把整条路线停死）。 */
    private static final int WAYPOINT_SKIP_TICKS = 100;
    /** 巡逻索敌扫描半径（格）。 */
    private static final double PATROL_SCAN_RADIUS = 16.0D;
    /** 攻击冷却（tick），贴住目标时约每秒一下。 */
    private static final int ATTACK_COOLDOWN = 20;

    /**
     * 移动死区（格）：目标点离身体中心小于这个距离就完全不下发移动指令。
     * <p>这一步是防“原地打转”的关键：{@code MoveControl} 对很近的目标点算出的
     * {@code atan2(Δz, Δx)} 输入全是噪声级的小数，朝向每拍翻转 90°~180°，
     * 而前进档又被拉起来顶着障碍物推 —— 表现就是“不移动 + 快速打转”。</p>
     */
    private static final double MOVE_DEAD_ZONE = 0.5D;
    /** 路径重算最小间隔（tick）：20 tps 下 0.5 秒一次，与原版 {@code FollowOwnerGoal} 一致。 */
    private static final int REPATH_INTERVAL = 10;
    /** 即使目标点突然换了，两次重算之间也至少隔这么多拍。 */
    private static final int MIN_REPATH_GAP = 3;
    /** 目标点相对上次下发点偏移超过这个平方距离（格²，即 2 格）才值得立刻重算。 */
    private static final double REPATH_TARGET_EPS_SQR = 4.0D;
    /** 连续这么多拍没有实际位移就认为卡住（看门狗）。 */
    private static final int STUCK_TICKS = 30;
    /** 判定卡住后静默多久再试一次（tick）。 */
    private static final int STUCK_RETRY = 20;

    private final Mob mob;

    @Nullable
    private HiveParasiteCommand command;
    private int revision = Integer.MIN_VALUE;
    @Nullable
    private HiveCommandMode lastMode;

    /** 指令绑定的路线（路线库里的那一条）；没绑定/被删除时为 null。 */
    @Nullable
    private HiveRoute boundRoute;
    private int routeRevision = Integer.MIN_VALUE;

    /** 巡逻游标。 */
    private int waypointIndex;
    private int patrolDirection = 1;
    private int lastPathedWaypoint = -1;
    /** 当前节点是什么时候成为目标的（用于“卡太久就跳过”）。 */
    private long waypointSinceTick;

    /** 上一次真正下发的移动目标点；{@code haveGoal == false} 表示还没有/已被清空。 */
    private boolean haveGoal;
    private double goalX;
    private double goalY;
    private double goalZ;
    /** 寻路给不出路时，改由 {@code MoveControl} 直推（目标点固定，朝向稳定）。 */
    private boolean directDrive;
    /** 是否已经清过移动输入（避免在死区里每拍重复写 {@code MoveControl}）。 */
    private boolean driveIdle;

    /** 下一次允许重算路径的 tick。 */
    private long nextRepathTick;
    /** 上一次真正重算路径的 tick（用于给“目标点突变”也留一个硬下限）。 */
    private long lastPathTick;
    /** 卡住后的强制静默期：这段时间内 {@link #drive} 一律不下发移动。 */
    private long suppressDriveUntil;
    /** 身体“确实在动”的参考位置与时间（卡住看门狗）。 */
    private long lastProgressTick;
    private double progressX;
    private double progressY;
    private double progressZ;

    private long lastTeleportTick = -TELEPORT_COOLDOWN;
    private long lastAttackTick = -ATTACK_COOLDOWN;

    public HiveCommandGoal(Mob mob) {
        this.mob = mob;
        // MOVE：接管移动；LOOK：接管朝向。只抢 MOVE 会让原版 RandomLookAroundGoal /
        // LookAtPlayerGoal 继续跑，把 LookControl 抢来抢去，脑袋和身体来回拧。
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide || mob.isRemoved()) {
            return false;
        }
        HiveParasiteCommand current = HiveCommandManager.commandOf(mob);
        if (current == null) {
            return false;
        }
        if (current.mode() == HiveCommandMode.FOLLOW) {
            // 主人不在线时不再抢移动权，让原版 AI 照常活动；指令本身留在存档里。
            // 但如果还有显式攻击目标，即使主人下线也继续执行攻击命令。
            return leaderOf(current) != null || explicitTarget(current) != null;
        }
        return dimensionMatches(current);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        command = null;
        revision = Integer.MIN_VALUE;
        lastMode = null;
        boundRoute = null;
        routeRevision = Integer.MIN_VALUE;
        waypointIndex = 0;
        patrolDirection = 1;
        lastPathedWaypoint = -1;
        waypointSinceTick = mob.level().getGameTime();
        haveGoal = false;
        directDrive = false;
        driveIdle = true;
        nextRepathTick = 0L;
        lastPathTick = 0L;
        suppressDriveUntil = 0L;
        progressX = mob.getX();
        progressY = mob.getY();
        progressZ = mob.getZ();
        lastProgressTick = mob.level().getGameTime();
    }

    @Override
    public void stop() {
        stopDrive();
        mob.setTarget(null);
        command = null;
        revision = Integer.MIN_VALUE;
        lastMode = null;
        boundRoute = null;
        routeRevision = Integer.MIN_VALUE;
        haveGoal = false;
        lastPathedWaypoint = -1;
    }

    @Override
    public void tick() {
        if (!refreshCommand()) {
            return;
        }
        long now = mob.level().getGameTime();
        updateProgress(now);
        HiveParasiteCommand current = command;
        switch (current.mode()) {
            case FOLLOW -> tickFollow(current, now);
            case GUARD -> tickGuard(current, now);
            case PATROL -> tickPatrol(current, now);
        }
    }

    /** 读取最新指令与它绑定的路线；修订号变化时只做游标纠偏，避免每次改目标都把巡逻拽回起点。 */
    private boolean refreshCommand() {
        HiveParasiteCommand current = HiveCommandManager.commandOf(mob);
        if (current == null) {
            command = null;
            boundRoute = null;
            haveGoal = false;
            return false;
        }
        // 路线库是独立文档：节点被编辑（哪怕只是加了第 3 个点）都要立刻按新节点巡逻。
        HiveRoute resolved = HiveCommandManager.routeOf(mob, current);
        int resolvedRevision = resolved == null ? Integer.MIN_VALUE : resolved.revision();
        int size = resolved == null ? 0 : resolved.points().size();

        boolean modeChanged = lastMode != current.mode();
        if (current != command || modeChanged) {
            command = current;
            revision = current.revision();
            waypointIndex = 0;
            patrolDirection = 1;
            lastPathedWaypoint = -1;
            waypointSinceTick = mob.level().getGameTime();
            haveGoal = false;
            mob.setTarget(null);
        } else if (revision != current.revision() || routeRevision != resolvedRevision) {
            revision = current.revision();
            if (waypointIndex < 0 || waypointIndex >= size) {
                waypointIndex = 0;
            }
            lastPathedWaypoint = -1;
            haveGoal = false;
        }
        boundRoute = resolved;
        routeRevision = resolvedRevision;
        lastMode = current.mode();
        return true;
    }

    private boolean dimensionMatches(HiveParasiteCommand current) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        String dimension = level.dimension().location().toString();
        return current.dimension().isEmpty() || dimension.equals(current.dimension());
    }

    // ------------------------------------------------------------------
    // 跟随
    // ------------------------------------------------------------------

    private void tickFollow(HiveParasiteCommand current, long now) {
        LivingEntity target = explicitTarget(current);
        if (target != null) {
            mob.setTarget(target);
            chaseAndAttack(target, now);
            return;
        }
        mob.setTarget(null);

        ServerPlayer leader = leaderOf(current);
        if (leader == null) {
            stopDrive();
            return;
        }
        if (mob.level() != leader.level()) {
            teleportNearLeader(leader);
            return;
        }
        double distance = mob.distanceTo(leader);
        if (distance > FOLLOW_TELEPORT) {
            teleportNearLeader(leader);
            return;
        }
        if (distance <= FOLLOW_STOP) {
            // 收队距离内：彻底停住，只把朝向对准主人（目标稳定，不会来回拧）
            stopDrive();
            lookAt(leader);
            return;
        }
        drive(leader.getX(), leader.getY(), leader.getZ(), 1.0D, now);
    }

    // ------------------------------------------------------------------
    // 看守
    // ------------------------------------------------------------------

    private void tickGuard(HiveParasiteCommand current, long now) {
        BlockPos guardPos = current.guardPos();
        if (guardPos == null) {
            guardPos = mob.blockPosition();
        }
        LivingEntity target = explicitTarget(current);
        if (target == null) {
            double gx = guardPos.getX() + 0.5D;
            double gy = guardPos.getY();
            double gz = guardPos.getZ() + 0.5D;
            LivingEntity existing = mob.getTarget();
            if (existing != null && isValidTarget(existing)
                    && existing.distanceToSqr(gx, gy, gz) <= GUARD_RADIUS * GUARD_RADIUS) {
                target = existing;
            } else if ((mob.tickCount + mob.getId()) % 5 == 0) {
                // 五拍扫一次：同屏大量看守时避免每拍都做一次 32³ 的实体查询
                target = findTarget(GUARD_RADIUS, gx, gy, gz);
            }
        }
        if (target != null) {
            mob.setTarget(target);
            lookAt(target);
            if (mob.distanceToSqr(target) <= attackReachSqr(target)) {
                attack(target, now);
            }
        } else {
            mob.setTarget(null);
        }

        double dx = mob.getX() - (guardPos.getX() + 0.5D);
        double dy = mob.getY() - guardPos.getY();
        double dz = mob.getZ() - (guardPos.getZ() + 0.5D);
        if (dx * dx + dz * dz + dy * dy > GUARD_RETURN_DISTANCE * GUARD_RETURN_DISTANCE) {
            // 被击退/被推开了才走回站位；站位点固定 → 朝向稳定，且路径重算被节流
            drive(guardPos.getX() + 0.5D, guardPos.getY(), guardPos.getZ() + 0.5D, 1.0D, now);
        } else {
            stopDrive();
        }
    }

    // ------------------------------------------------------------------
    // 巡逻
    // ------------------------------------------------------------------

    private void tickPatrol(HiveParasiteCommand current, long now) {
        HiveRoute bound = boundRoute;
        if (bound == null || bound.points().size() < 2) {
            stopDrive();
            return;
        }
        List<BlockPos> route = bound.points();
        boolean closed = bound.closed() && route.size() > 2;

        LivingEntity target = explicitTarget(current);
        if (target != null && HiveRouteMath.distanceToRouteXZ(target.getX(), target.getZ(), route, closed)
                > HiveRouteMath.PATROL_CORRIDOR) {
            // 显式目标跑到路线 ±8 格外：取消这只寄生体的索敌，回路线继续巡逻
            HiveCommandManager.clearTarget(mob, current);
            target = null;
        }
        if (target == null) {
            LivingEntity existing = mob.getTarget();
            if (existing != null && (!isValidTarget(existing)
                    || HiveRouteMath.distanceToRouteXZ(existing.getX(), existing.getZ(), route, closed)
                    > HiveRouteMath.PATROL_CORRIDOR)) {
                mob.setTarget(null);
            }
            target = mob.getTarget();
        }
        if (target == null && (mob.tickCount + mob.getId()) % 5 == 0) {
            // 巡逻索敌同样五拍一扫，避免每只巡逻体每拍做一次范围查询
            target = findPatrolTarget(route, closed);
        }

        if (target != null) {
            mob.setTarget(target);
            lookAt(target);
            if (HiveRouteMath.distanceToRouteXZ(mob.getX(), mob.getZ(), route, closed)
                    > HiveRouteMath.PATROL_CORRIDOR) {
                // 自己已经被甩出走廊：无视目标，先回到最近路线点
                waypointIndex = HiveRouteMath.nearestRouteIndex(mob.getX(), mob.getZ(), route);
                lastPathedWaypoint = -1;
                haveGoal = false;
                BlockPos back = route.get(waypointIndex);
                drive(back.getX() + 0.5D, back.getY(), back.getZ() + 0.5D, 1.0D, now);
                return;
            }
            chaseAndAttack(target, now);
            return;
        }

        mob.setTarget(null);
        patrolMove(route, closed, now);
    }

    private void patrolMove(List<BlockPos> route, boolean closed, long now) {
        if (waypointIndex < 0 || waypointIndex >= route.size()) {
            waypointIndex = 0;
        }
        if (HiveRouteMath.distanceToRouteXZ(mob.getX(), mob.getZ(), route, closed)
                > HiveRouteMath.PATROL_CORRIDOR) {
            waypointIndex = HiveRouteMath.nearestRouteIndex(mob.getX(), mob.getZ(), route);
        }

        if (waypointIndex != lastPathedWaypoint) {
            // 换了节点：允许立刻重算一次（这是“目标真的变了”，不是节流能挡的正常换点）
            lastPathedWaypoint = waypointIndex;
            waypointSinceTick = now;
            haveGoal = false;
        } else if (now - waypointSinceTick > WAYPOINT_SKIP_TICKS
                && now - lastProgressTick > STUCK_TICKS) {
            // 卡在这个节点太久（节点在墙里/在高处够不着）：跳过它，别让整条路线停死
            advanceWaypoint(route.size(), closed);
            lastPathedWaypoint = waypointIndex;
            waypointSinceTick = now;
            haveGoal = false;
        }

        BlockPos waypoint = route.get(waypointIndex);
        double cx = waypoint.getX() + 0.5D;
        double cy = waypoint.getY();
        double cz = waypoint.getZ() + 0.5D;
        if (mob.distanceToSqr(cx, cy, cz) <= WAYPOINT_REACH * WAYPOINT_REACH) {
            // 到达节点立刻推进，且推进后把 haveGoal 清掉 → 只重算一次，不会每拍重算
            advanceWaypoint(route.size(), closed);
            lastPathedWaypoint = waypointIndex;
            waypointSinceTick = now;
            haveGoal = false;
            waypoint = route.get(waypointIndex);
        }
        drive(waypoint.getX() + 0.5D, waypoint.getY(), waypoint.getZ() + 0.5D, 1.0D, now);
    }

    private void advanceWaypoint(int size, boolean closed) {
        if (size < 2) {
            waypointIndex = 0;
            return;
        }
        if (closed) {
            waypointIndex = (waypointIndex + 1) % size;
            return;
        }
        if (waypointIndex >= size - 1) {
            patrolDirection = -1;
            waypointIndex = size - 2;
        } else if (waypointIndex <= 0) {
            patrolDirection = 1;
            waypointIndex = 1;
        } else {
            waypointIndex = Math.max(0, Math.min(size - 1, waypointIndex + patrolDirection));
        }
    }

    // ------------------------------------------------------------------
    // 战斗
    // ------------------------------------------------------------------

    private void chaseAndAttack(LivingEntity target, long now) {
        if (mob.distanceToSqr(target) <= attackReachSqr(target)) {
            stopDrive();
            attack(target, now);
            return;
        }
        drive(target.getX(), target.getY(), target.getZ(), 1.0D, now);
    }

    private void attack(LivingEntity target, long now) {
        if (now - lastAttackTick < ATTACK_COOLDOWN) {
            return;
        }
        lookAt(target);
        if (mob.doHurtTarget(target)) {
            lastAttackTick = now;
        }
    }

    private double attackReachSqr(LivingEntity target) {
        double reach = mob.getBbWidth() * 2.0D + target.getBbWidth() + 0.5D;
        return reach * reach;
    }

    // ------------------------------------------------------------------
    // 移动 / 朝向：这一层是“不打转”的全部关键
    // ------------------------------------------------------------------

    /**
     * 看门狗：身体多久没真的挪动过。
     *
     * <p>连续 {@link #STUCK_TICKS} 拍位移不到 0.25 格就认定卡住（顶着墙、脚被方块夹住、
     * 目标点不可达），立刻停掉移动输入并冷却 {@link #STUCK_RETRY} 拍。
     * 宁可站着不动，也不要每拍被 {@code MoveControl} 拧一次身子。</p>
     */
    private void updateProgress(long now) {
        if (mob.distanceToSqr(progressX, progressY, progressZ) > 0.0625D) {
            progressX = mob.getX();
            progressY = mob.getY();
            progressZ = mob.getZ();
            lastProgressTick = now;
            return;
        }
        if (now - lastProgressTick > STUCK_TICKS) {
            stopDrive();
            suppressDriveUntil = now + STUCK_RETRY;
            lastProgressTick = now;
        }
    }

    /**
     * 下发一次移动（带死区 + 节流 + 直推兜底）。每拍都会被调用，但绝大多数拍什么都不做。
     *
     * @param x     目标点（世界坐标）
     * @param speed 移动速度倍率（永远 1.0：不改移速）
     * @param now   当前 game time
     */
    private void drive(double x, double y, double z, double speed, long now) {
        if (now < suppressDriveUntil) {
            // 看门狗静默期内：完全不碰 MoveControl / Navigation，站着别动
            stopDrive();
            return;
        }
        if (mob.distanceToSqr(x, y, z) <= MOVE_DEAD_ZONE * MOVE_DEAD_ZONE) {
            // 死区：目标就在身上。任何移动指令都会让 MoveControl 用噪声级 Δx/Δz 算 atan2，
            // 于是朝向每拍翻转 —— 这里直接停手。
            stopDrive();
            return;
        }

        boolean freshGoal = !haveGoal
                || distanceSqr(x, y, z, goalX, goalY, goalZ) > REPATH_TARGET_EPS_SQR;

        if (!freshGoal) {
            if (directDrive) {
                // 寻路给不出路，只能直推：MoveControl 每拍都会把 zza 清 0，所以必须每拍重发。
                // 目标点固定 → 朝向稳定，不会打转（这是唯一允许每拍写朝向的地方）。
                mob.getMoveControl().setWantedPosition(x, y, z, speed);
                return;
            }
            if (now < nextRepathTick) {
                // 正沿现有路径走：这一拍什么都不做（导航自己每拍跟进路点）
                return;
            }
        } else if (now < nextRepathTick && now - lastPathTick < MIN_REPATH_GAP) {
            // 目标点确实变了，但刚算过路：仍然给一个硬下限，避免卡住看门狗的冷却被立刻绕开
            return;
        }

        nextRepathTick = now + REPATH_INTERVAL;
        lastPathTick = now;
        haveGoal = true;
        goalX = x;
        goalY = y;
        goalZ = z;
        driveIdle = false;
        // 不先 stop()：moveTo 会直接替换旧路径；先 stop 会让每一拍都从零开始 A*，
        // 首节点在身体两侧跳，正是打转的来源。
        if (mob.getNavigation().moveTo(x, y, z, speed)) {
            directDrive = false;
        } else {
            directDrive = true;
            mob.getMoveControl().setWantedPosition(x, y, z, speed);
        }
    }

    /** 彻底停下：清路径，并把 MoveControl 的目标点写成自己脚下（等于“原地待命”）。 */
    private void stopDrive() {
        if (!mob.getNavigation().isDone()) {
            mob.getNavigation().stop();
        }
        if (!driveIdle) {
            // wanted == 自身位置：MoveControl 的 MOVE_TO 分支会命中 g < 2.5E-7 的短路，
            // 直接 setZza(0) 返回 —— 既不前进，也不再按旧目标改朝向。
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0D);
            driveIdle = true;
        }
        directDrive = false;
        haveGoal = false;
    }

    /**
     * 写一次朝向。目标必须是“稳定的一个点”（实体或固定站位），
     * 并且每拍最多调用一次；目标和身体重合时不写（atan2 输入全是噪声）。
     */
    private void lookAt(@Nullable LivingEntity target) {
        if (target == null || target == mob) {
            return;
        }
        if (mob.distanceToSqr(target) < 1.0E-4D) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private static double distanceSqr(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    // ------------------------------------------------------------------
    // 目标查询
    // ------------------------------------------------------------------

    @Nullable
    private LivingEntity explicitTarget(HiveParasiteCommand current) {
        UUID targetUuid = current.target();
        if (targetUuid == null || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(targetUuid);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || !isValidTarget(living)) {
            return null;
        }
        return living;
    }

    @Nullable
    private LivingEntity findTarget(double radius, double x, double y, double z) {
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        return nearestValidTarget(mob.level().getEntitiesOfClass(LivingEntity.class, box),
                x, y, z, radius * radius);
    }

    @Nullable
    private LivingEntity findPatrolTarget(List<BlockPos> route, boolean closed) {
        AABB box = mob.getBoundingBox().inflate(PATROL_SCAN_RADIUS);
        List<LivingEntity> candidates = mob.level().getEntitiesOfClass(LivingEntity.class, box);
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (!isValidTarget(candidate)) {
                continue;
            }
            if (HiveRouteMath.distanceToRouteXZ(candidate.getX(), candidate.getZ(), route, closed)
                    > HiveRouteMath.PATROL_CORRIDOR) {
                continue;
            }
            double distSq = mob.distanceToSqr(candidate);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        return best;
    }

    @Nullable
    private LivingEntity nearestValidTarget(List<LivingEntity> candidates, double x, double y, double z,
                                            double maxDistSq) {
        LivingEntity best = null;
        double bestDistSq = maxDistSq;
        for (LivingEntity candidate : candidates) {
            if (!isValidTarget(candidate)) {
                continue;
            }
            double distSq = candidate.distanceToSqr(x, y, z);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        return best;
    }

    /** 敌对目标过滤：寄生体/领巢者/爆炸苦力怕/盔甲架/创造观战玩家都不打。 */
    private boolean isValidTarget(LivingEntity candidate) {
        if (candidate == null || candidate == mob || !candidate.isAlive()) {
            return false;
        }
        if (ParasiteEvents.isParasiteByTagOrInterface(candidate)) {
            return false;
        }
        if (candidate instanceof Creeper || candidate instanceof ArmorStand) {
            return false;
        }
        if (candidate instanceof Player player) {
            if (command != null && player.getUUID().equals(command.owner())) {
                return false;
            }
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 传送
    // ------------------------------------------------------------------

    @Nullable
    private ServerPlayer leaderOf(HiveParasiteCommand current) {
        MinecraftServer server = mob.getServer();
        if (server == null) {
            return null;
        }
        ServerPlayer leader = server.getPlayerList().getPlayer(current.owner());
        return leader != null && leader.isAlive() ? leader : null;
    }

    private void teleportNearLeader(ServerPlayer leader) {
        if (!(leader.level() instanceof ServerLevel level)) {
            return;
        }
        long now = mob.level().getGameTime();
        if (now - lastTeleportTick < TELEPORT_COOLDOWN) {
            return;
        }
        BlockPos around = leader.blockPosition();
        if (!level.isLoaded(around)) {
            return;
        }
        double x = leader.getX();
        double y = leader.getY();
        double z = leader.getZ();
        outer:
        for (int dy = 1; dy >= -2; dy--) {
            for (int r = 0; r <= 3; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos pos = around.offset(dx, dy, dz);
                        if (canStandAt(level, pos)) {
                            x = pos.getX() + 0.5D;
                            y = pos.getY();
                            z = pos.getZ() + 0.5D;
                            break outer;
                        }
                    }
                }
            }
        }
        mob.teleportTo(level, x, y, z, EnumSet.noneOf(RelativeMovement.class), mob.getYRot(), mob.getXRot());
        lastTeleportTick = now;
        // 传送后旧路径/旧朝向全部作废，下一拍重新算一次（避免传送到新位置后还按旧目标拧身子）
        stopDrive();
        progressX = mob.getX();
        progressY = mob.getY();
        progressZ = mob.getZ();
        lastProgressTick = now;
    }

    private boolean canStandAt(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isSolidRender(level, pos)) {
            return false;
        }
        BlockPos floor = pos.below();
        if (!level.getBlockState(floor).isSolidRender(level, floor)) {
            return false;
        }
        double half = mob.getBbWidth() / 2.0D;
        AABB box = new AABB(pos.getX() + 0.5D - half, pos.getY(), pos.getZ() + 0.5D - half,
                pos.getX() + 0.5D + half, pos.getY() + mob.getBbHeight(), pos.getZ() + 0.5D + half);
        return level.noCollision(mob, box);
    }
}
