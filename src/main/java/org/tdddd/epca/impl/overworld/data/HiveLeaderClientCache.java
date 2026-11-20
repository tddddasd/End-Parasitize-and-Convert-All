package org.tdddd.epca.impl.overworld.data;

import net.minecraft.client.Minecraft;
import org.tdddd.epca.impl.client.command.HiveCommandClientData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端手里的“谁是领巢者”集合（由 {@code SyncHiveLeadersPacket} 整表替换）。
 *
 * <p>0.148n 修复：</p>
 * <ul>
 *     <li><b>登出清理</b>：这份集合以前从不清理，退出服务器后残留，换到另一个服务器 / 存档时
 *     {@code BiomassHUD}、{@code ParaCurseEffect} 会把上一个存档的领巢者 UUID 当成当前存档的，
 *     造成误判。现在由 {@code OrganModelClientEvents} 在 {@code LoggingOut} 时调用 {@link #clear()}。</li>
 *     <li><b>撤销即时生效</b>：以前 {@code HiveCommandRenderer} 的守卫是
 *     {@code isHiveLeader(self) || HiveCommandClientData.hasReceivedSync()}，而 {@code receivedSync}
 *     一旦为 true 就再也不会复位 —— 被撤销的领巢者本次会话仍能继续画紫线。现在收到新的领巢者集合时，
 *     如果本地玩家<b>不在</b>新集合里，就顺手 {@link HiveCommandClientData#clear()}（同时复位
 *     {@code receivedSync}），守卫立刻失效、当帧停止绘制。</li>
 * </ul>
 */
public class HiveLeaderClientCache {
    private static final Set<UUID> LEADERS = new HashSet<>();

    public static void updateLeaders(Set<UUID> leaders) {
        LEADERS.clear();
        LEADERS.addAll(leaders);
        // 撤销即时生效：本地玩家不在新集合里 → 复位指挥缓存（含 receivedSync），渲染守卫当帧拦下。
        // Minecraft.getInstance().player 为 null（尚未进入世界 / 正在断开）时什么都不做，
        // 避免把“还没登录”误当成“被撤销”。
        UUID self = selfUuid();
        if (self != null && !LEADERS.contains(self)) {
            HiveCommandClientData.clear();
        }
    }

    /** 当前本地玩家 UUID；不在世界里时返回 null。只会在客户端调用（见 {@link #updateLeaders}）。 */
    private static UUID selfUuid() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : minecraft.player.getUUID();
    }

    public static boolean isHiveLeader(UUID uuid) {
        return LEADERS.contains(uuid);
    }

    /** 登出 / 换服务器时整表清空，避免跨存档残留（见类注释）。 */
    public static void clear() {
        LEADERS.clear();
    }

    /** 当前客户端手里的领巢者数量（只给诊断日志用）。 */
    public static int size() {
        return LEADERS.size();
    }
}
