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
}