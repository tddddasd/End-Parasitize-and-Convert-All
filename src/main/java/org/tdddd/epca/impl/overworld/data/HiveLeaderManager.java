package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeadersPacket;

import java.util.Set;
import java.util.UUID;

public class HiveLeaderManager {
    public static HiveLeaderSavedData getSavedData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel overworld = server.overworld();
        return HiveLeaderSavedData.get(overworld);
    }

    public static boolean isHiveLeader(UUID uuid) {
        HiveLeaderSavedData data = getSavedData();
        return data != null && data.isLeader(uuid);
    }

    public static boolean addHiveLeader(UUID uuid) {
        HiveLeaderSavedData data = getSavedData();
        if (data != null) {
            data.addLeader(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    public static boolean removeHiveLeader(UUID uuid) {
        HiveLeaderSavedData data = getSavedData();
        if (data != null) {
            data.removeLeader(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    private static void broadcastLeaders() {
        HiveLeaderSavedData data = getSavedData();
        if (data == null) return;
        Set<UUID> leaders = data.getLeaders();
        ModNetwork.sendToAll(new SyncHiveLeadersPacket(leaders));
    }

    /**
     * 把当前领巢者集合补发给一名玩家。
     *
     * <p>0.148n 修复：{@link #broadcastLeaders()} 只在“加入/移除领巢者”时被调用，
     * 玩家登录 / 换维度都<b>不会</b>补发。于是只要领巢者身份是上一次游戏就存进存档的，
     * 本次会话客户端的 {@code HiveLeaderClientCache} 就一直是空的
     * （指挥系统紫线因此每帧被守卫拦掉、生物量 HUD 与寄生诅咒判定也会看错人）。
     * 登录时补一发即可，代价一个包。</p>
     */
    public static void syncTo(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) return;
        HiveLeaderSavedData data = getSavedData();
        if (data == null) return;
        ModNetwork.sendToPlayer(player, new SyncHiveLeadersPacket(data.getLeaders()));
    }
}
