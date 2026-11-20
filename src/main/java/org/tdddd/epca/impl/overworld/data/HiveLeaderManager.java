package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncHiveLeadersPacket;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganSavedData;

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

            initOrganDefaults(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    private static void initOrganDefaults(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return;
        HiveLeaderOrganSavedData.initDefaultsIfAbsent(player);
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

    /**
     * 给单个玩家补发一份领巢者名单。
     *
     * <p>客户端 {@code HiveLeaderClientCache} 是空集合起步的，而 {@link #broadcastLeaders()} 只在
     * 名单<b>变动</b>时广播：如果玩家上一次游戏就已经是领巢者，本次会话的客户端缓存会一直是空的，
     * 指挥系统的紫线/节点/线框会被身份守卫整帧挡掉且零报错。所以登录 / 换维度都要补发一次。</p>
     */
    public static void syncTo(ServerPlayer player) {
        if (player == null) return;
        HiveLeaderSavedData data = dataFor(player);
        if (data == null) return;
        ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncHiveLeadersPacket(data.getLeaders()));
    }

    /**
     * 优先用玩家自己的服务器实例取主世界数据，避免 {@link ServerLifecycleHooks#getCurrentServer()}
     * 在登录时序里还没设好的情况（{@code PlayerList#placeNewPlayer} 早期）。
     */
    private static HiveLeaderSavedData dataFor(ServerPlayer player) {
        MinecraftServer server = player.server != null ? player.server : ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return HiveLeaderSavedData.get(server.overworld());
    }

    private static void broadcastLeaders() {
        HiveLeaderSavedData data = getSavedData();
        if (data == null) return;
        Set<UUID> leaders = data.getLeaders();
        ModNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncHiveLeadersPacket(leaders));
    }
}
