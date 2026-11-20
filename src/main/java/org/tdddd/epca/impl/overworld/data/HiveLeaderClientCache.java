package org.tdddd.epca.impl.overworld.data;

import net.minecraft.client.Minecraft;
import org.tdddd.epca.impl.client.command.HiveCommandClientData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端领巢者 UUID 集合（服务端 {@code SyncHiveLeadersPacket} 整表替换）。
 *
 * <p>只在客户端读写：写入在 {@code SyncHiveLeadersPacket#handle}（主线程 enqueueWork），
 * 读取在 {@code BiomassHUD} / {@code ParaCurseEffect} / {@code HiveCommandRenderer}。
 * 登出时必须 {@link #clear()}，否则跨服务器 / 跨存档会残留上一个服务器的 UUID，
 * 让 HUD、诅咒特效把别的服务器的领巢者当成领巢者。</p>
 */
public class HiveLeaderClientCache {
    private static final Set<UUID> LEADERS = new HashSet<>();

    /**
     * 整表替换。
     *
     * <p>顺手处理“被撤销”：如果新名单里没有本地玩家（被撤职，或者服务端列表里根本没有自己），
     * 立刻清掉指挥系统的客户端数据 —— {@code HiveCommandClientData.clear()} 会把
     * {@code receivedSync} 复位，于是 {@code HiveCommandRenderer} 的身份守卫
     * （{@code isHiveLeader || hasReceivedSync()}）两条同时不满足，本次会话里那些紫线 / 节点 / 线框
     * 立即停止绘制，不必等下一次同步包，也不会“画到下次登录”。</p>
     *
     * <p>登录早期 {@code Minecraft#player} 还是 null 时<b>不做</b>清理：那时候本地缓存本来就是空的，
     * 而“本地玩家不在名单里”没有意义，清掉只会误伤。</p>
     *
     * <p>注意：{@link #updateLeaders} 只会由 S2C 同步包（客户端主线程）调用，专用服务器上不会被加载执行；
     * 这里直接引用客户端类与 {@code Minecraft} 是有意为之，和同包其它客户端缓存保持一致。</p>
     */
    public static void updateLeaders(Set<UUID> leaders) {
        LEADERS.clear();
        LEADERS.addAll(leaders);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (!LEADERS.contains(minecraft.player.getUUID())) {
            HiveCommandClientData.clear();
        }
    }

    /**
     * 登出（{@code ClientPlayerNetworkEvent.LoggingOut}）清空，避免跨服务器残留。
     */
    public static void clear() {
        LEADERS.clear();
    }

    public static boolean isHiveLeader(UUID uuid) {
        return LEADERS.contains(uuid);
    }
}
