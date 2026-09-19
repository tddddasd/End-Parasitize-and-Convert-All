package org.tdddd.epca.impl.overworld.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.SyncNestLeadersPacket;

import java.util.Set;
import java.util.UUID;

public class NestLeaderManager {
    public static NestLeaderSavedData getSavedData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel overworld = server.overworld();
        return NestLeaderSavedData.get(overworld);
    }

    public static boolean isNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        return data != null && data.isLeader(uuid);
    }

    public static boolean addNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        if (data != null) {
            data.addLeader(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    public static boolean removeNestLeader(UUID uuid) {
        NestLeaderSavedData data = getSavedData();
        if (data != null) {
            data.removeLeader(uuid);
            broadcastLeaders();
            return true;
        }
        return false;
    }

    private static void broadcastLeaders() {
        NestLeaderSavedData data = getSavedData();
        if (data == null) return;
        Set<UUID> leaders = data.getLeaders();
        ModNetwork.sendToAll(new SyncNestLeadersPacket(leaders));
    }
}